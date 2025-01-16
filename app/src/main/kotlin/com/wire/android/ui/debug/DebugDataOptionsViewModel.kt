/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.debug

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelPreview
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.E2EIFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenError
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import javax.inject.Inject

@ViewModelPreview
interface DebugDataOptionsViewModel {
    val infoMessage: SharedFlow<UIText> get() = MutableSharedFlow()
    fun state(): DebugDataOptionsState = DebugDataOptionsState()
    fun currentAccount(): UserId = UserId("value", "domain")
    fun checkCrlRevocationList() {}
    fun enableEncryptedProteusStorage(enabled: Boolean) {}
    fun restartSlowSyncForRecovery() {}
    fun enrollE2EICertificate() {}
    fun handleE2EIEnrollmentResult(result: Either<CoreFailure, E2EIEnrollmentResult>) {}
    fun dismissCertificateDialog() {}
    fun forceUpdateApiVersions() {}
    fun disableEventProcessing(disabled: Boolean) {}
    fun forceSendFCMToken() {}
}

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class DebugDataOptionsViewModelImpl
@Inject constructor(
    @ApplicationContext private val context: Context,
    @CurrentAccount val currentAccount: UserId,
    private val globalDataStore: GlobalDataStore,
    private val updateApiVersions: UpdateApiVersionsScheduler,
    private val mlsKeyPackageCount: MLSKeyPackageCountUseCase,
    private val restartSlowSyncProcessForRecovery: RestartSlowSyncProcessForRecoveryUseCase,
    private val checkCrlRevocationList: CheckCrlRevocationListUseCase,
    private val getCurrentAnalyticsTrackingIdentifier: GetCurrentAnalyticsTrackingIdentifierUseCase,
    private val sendFCMToken: SendFCMTokenUseCase,
    private val dispatcherProvider: DispatcherProvider,
    private val selfServerConfigUseCase: SelfServerConfigUseCase,
    private val getDefaultProtocolUseCase: GetDefaultProtocolUseCase,
) : ViewModel(), DebugDataOptionsViewModel {

    var state by mutableStateOf(
        DebugDataOptionsState()
    )

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()

    init {
        observeEncryptedProteusStorageState()
        observeMlsMetadata()
        checkIfCanTriggerManualMigration()
        setGitHashAndDeviceId()
        setAnalyticsTrackingId()
        setServerConfigData()
        setDefaultProtocol()
    }

    private fun setDefaultProtocol() {
        viewModelScope.launch {
            state = state.copy(
                defaultProtocol = when (getDefaultProtocolUseCase()) {
                    SupportedProtocol.PROTEUS -> "Proteus"
                    SupportedProtocol.MLS -> "MLS"
                }
            )
        }
    }

    private fun setServerConfigData() {
        viewModelScope.launch {
            val result = selfServerConfigUseCase()
            if (result is SelfServerConfigUseCase.Result.Success) {
                state = state.copy(
                    isFederationEnabled = result.serverLinks.metaData.federation,
                    currentApiVersion = when (result.serverLinks.metaData.commonApiVersion) {
                        CommonApiVersionType.Unknown -> "Unknown"
                        else -> result.serverLinks.metaData.commonApiVersion.version.toString()
                    },
                )
            }
        }
    }

    private fun setAnalyticsTrackingId() {
        viewModelScope.launch {
            getCurrentAnalyticsTrackingIdentifier()?.let { trackingId ->
                state = state.copy(
                    analyticsTrackingId = trackingId
                )
            }
        }
    }

    private fun setGitHashAndDeviceId() {
        viewModelScope.launch {
            val deviceId = context.getDeviceIdString() ?: "null"
            val gitBuildId = context.getGitBuildId()
            state = state.copy(
                debugId = deviceId,
                commitish = gitBuildId
            )
        }
    }

    override fun state() = state
    override fun currentAccount(): UserId = currentAccount

    override fun checkCrlRevocationList() {
        viewModelScope.launch {
            checkCrlRevocationList(
                forceUpdate = true
            )
        }
    }

    override fun enableEncryptedProteusStorage(enabled: Boolean) {
        if (enabled) {
            viewModelScope.launch {
                globalDataStore.setEncryptedProteusStorageEnabled(true)
            }
        }
    }

    override fun restartSlowSyncForRecovery() {
        viewModelScope.launch {
            restartSlowSyncProcessForRecovery()
        }
    }

    override fun enrollE2EICertificate() {
        state = state.copy(startGettingE2EICertificate = true)
    }

    override fun handleE2EIEnrollmentResult(result: Either<CoreFailure, E2EIEnrollmentResult>) {
        result.fold({
            state = state.copy(
                certificate = (it as E2EIFailure.OAuth).reason,
                showCertificate = true,
                startGettingE2EICertificate = false
            )
        }, {
            if (it is E2EIEnrollmentResult.Finalized) {
                state = state.copy(
                    certificate = it.certificate,
                    showCertificate = true,
                    startGettingE2EICertificate = false
                )
            } else {
                state.copy(
                    certificate = it.toString(),
                    showCertificate = true,
                    startGettingE2EICertificate = false
                )
            }
        })
    }

    override fun dismissCertificateDialog() {
        state = state.copy(
            showCertificate = false,
        )
    }

    override fun forceUpdateApiVersions() {
        updateApiVersions.scheduleImmediateApiVersionUpdate()
    }

    override fun disableEventProcessing(disabled: Boolean) {
        viewModelScope.launch {
            disableEventProcessing(disabled)
            state = state.copy(isEventProcessingDisabled = disabled)
        }
    }

    override fun forceSendFCMToken() {
        viewModelScope.launch {
            withContext(dispatcherProvider.io()) {
                val result = sendFCMToken()
                result.fold(
                    {
                        when (it.status) {
                            SendFCMTokenError.Reason.CANT_GET_CLIENT_ID -> {
                                _infoMessage.emit(UIText.DynamicString("Can't get client ID, error: ${it.error}"))
                            }

                            SendFCMTokenError.Reason.CANT_GET_NOTIFICATION_TOKEN -> {
                                _infoMessage.emit(UIText.DynamicString("Can't get notification token, error: ${it.error}"))
                            }

                            SendFCMTokenError.Reason.CANT_REGISTER_TOKEN -> {
                                _infoMessage.emit(UIText.DynamicString("Can't register token, error: ${it.error}"))
                            }
                        }
                    },
                    {
                        _infoMessage.emit(UIText.DynamicString("Token registered"))
                    }
                )
            }
        }
    }

    //region Private
    private fun observeEncryptedProteusStorageState() {
        viewModelScope.launch {
            globalDataStore.isEncryptedProteusStorageEnabled().collect {
                state = state.copy(isEncryptedProteusStorageEnabled = it)
            }
        }
    }

    // If status is NoNeed, it means that the user has already been migrated in and older app version,
    // or it is a new install
    // this is why we check the existence of the database file
    private fun checkIfCanTriggerManualMigration() {
        viewModelScope.launch {
            globalDataStore.getUserMigrationStatus(currentAccount.value).first()
                .let { migrationStatus ->
                    if (migrationStatus != UserMigrationStatus.NoNeed) {
                        context.getDatabasePath(currentAccount.value).let {
                            state = state.copy(
                                isManualMigrationAllowed = (it.exists() && it.isFile)
                            )
                        }
                    }
                }
        }
    }

    private fun observeMlsMetadata() {
        viewModelScope.launch {
            mlsKeyPackageCount().let {
                when (it) {
                    is MLSKeyPackageCountResult.Success -> {
                        state = state.copy(
                            keyPackagesCount = it.count,
                            mslClientId = it.clientId.value
                        )
                    }

                    is MLSKeyPackageCountResult.Failure.NetworkCallFailure -> {
                        state = state.copy(mlsErrorMessage = "Network Error!")
                    }

                    is MLSKeyPackageCountResult.Failure.FetchClientIdFailure -> {
                        state = state.copy(mlsErrorMessage = "ClientId Fetch Error!")
                    }

                    is MLSKeyPackageCountResult.Failure.Generic -> {}
                    MLSKeyPackageCountResult.Failure.NotEnabled -> {
                        state = state.copy(mlsErrorMessage = "Not Enabled!")
                    }
                }
            }
        }
    }
    //endregion
}
//endregion

@Serializable
object DebugDataOptions : ScopedArgs {
    override val key = "DebugDataOptionsKey"
}
