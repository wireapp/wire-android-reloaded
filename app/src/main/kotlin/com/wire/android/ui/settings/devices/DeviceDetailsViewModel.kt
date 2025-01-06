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
package com.wire.android.ui.settings.devices

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.CurrentAccount
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceDialogState
import com.wire.android.ui.authentication.devices.remove.RemoveDeviceError
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.android.ui.navArgs
import com.wire.android.ui.settings.devices.model.DeviceDetailsState
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.data.client.DeleteClientParam
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.mlspublickeys.MLSPublicKeyType
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.ClientFingerprintUseCase
import com.wire.kalium.logic.feature.client.DeleteClientResult
import com.wire.kalium.logic.feature.client.DeleteClientUseCase
import com.wire.kalium.logic.feature.client.GetClientDetailsResult
import com.wire.kalium.logic.feature.client.ObserveClientDetailsUseCase
import com.wire.kalium.logic.feature.client.Result
import com.wire.kalium.logic.feature.client.UpdateClientVerificationStatusUseCase
import com.wire.kalium.logic.feature.debug.BreakSessionUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.E2EIEnrollmentResult
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class DeviceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @CurrentAccount
    private val currentUserId: UserId,
    private val deleteClient: DeleteClientUseCase,
    private val observeClientDetails: ObserveClientDetailsUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val fingerprintUseCase: ClientFingerprintUseCase,
    private val updateClientVerificationStatus: UpdateClientVerificationStatusUseCase,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val mlsClientIdentity: GetMLSClientIdentityUseCase,
    private val breakSession: BreakSessionUseCase,
    isE2EIEnabledUseCase: IsE2EIEnabledUseCase
) : SavedStateViewModel(savedStateHandle) {

    private val deviceDetailsNavArgs: DeviceDetailsNavArgs = savedStateHandle.navArgs()
    private val deviceId: ClientId = deviceDetailsNavArgs.clientId
    private val userId: UserId = deviceDetailsNavArgs.userId

    val passwordTextState: TextFieldState = TextFieldState()
    var state: DeviceDetailsState by mutableStateOf(
        DeviceDetailsState(
            isSelfClient = isSelfClient,
            isE2EIEnabled = isE2EIEnabledUseCase()
        )
    )
        private set

    init {
        observeDeviceDetails()
        getClientFingerPrint()
        observeUserName()
        getE2eiCertificate()
        observePasswordTextChanges()
    }

    private val isSelfClient: Boolean
        get() = currentUserId == userId

    private fun observePasswordTextChanges() {
        viewModelScope.launch {
            passwordTextState.textAsFlow().distinctUntilChanged().collectLatest { newPassword ->
                updateStateIfDialogVisible {
                    state.copy(
                        removeDeviceDialogState = it.copy(removeEnabled = newPassword.isNotEmpty()),
                        error = RemoveDeviceError.None
                    )
                }
            }
        }
    }

    private fun observeUserName() {
        if (!isSelfClient) {
            viewModelScope.launch {
                observeUserInfo(userId).collect { result ->
                    when (result) {
                        GetUserInfoResult.Failure -> {
                            /* no-op */
                        }

                        is GetUserInfoResult.Success -> state =
                            state.copy(userName = result.otherUser.name)
                    }
                }
            }
        }
    }

    private fun getE2eiCertificate() {
        viewModelScope.launch {
            state = mlsClientIdentity(deviceId).fold({
                state.copy(isE2eiCertificateActivated = false, isLoadingCertificate = false)
            }, { mlsClientIdentity ->
                state.copy(
                    isE2eiCertificateActivated = true,
                    mlsClientIdentity = mlsClientIdentity,
                    isLoadingCertificate = false,
                    device = state.device.updateE2EICertificate(mlsClientIdentity)
                )
            })
        }
    }

    fun enrollE2EICertificate() {
        state = state.copy(isLoadingCertificate = true, startGettingE2EICertificate = true)
    }

    fun handleE2EIEnrollmentResult(result: Either<CoreFailure, E2EIEnrollmentResult>) {
        result.fold({
            state = state.copy(
                isLoadingCertificate = false,
                startGettingE2EICertificate = false,
                isE2EICertificateEnrollError = true,
            )
        }, {
            state = if (it is E2EIEnrollmentResult.Finalized) {
                getE2eiCertificate()
                state.copy(
                    isE2EICertificateEnrollSuccess = true,
                    startGettingE2EICertificate = false
                )
            } else {
                state.copy(
                    isLoadingCertificate = false,
                    isE2EICertificateEnrollError = true,
                    startGettingE2EICertificate = false,
                )
            }
        })
    }

    private fun getClientFingerPrint() {
        viewModelScope.launch {
            state = when (val result = fingerprintUseCase(userId, deviceId)) {
                is Result.Failure -> state.copy(fingerPrint = null)
                is Result.Success -> state.copy(fingerPrint = result.fingerprint.decodeToString())
            }
        }
    }

    private fun observeDeviceDetails() {
        viewModelScope.launch {
            observeClientDetails(userId, deviceId).collect { result ->
                state = when (result) {
                    is GetClientDetailsResult.Failure.Generic -> {
                        appLogger.e("Error getting self clients $result")
                        state.copy(error = RemoveDeviceError.InitError)
                    }

                    is GetClientDetailsResult.Success -> {
                        state.copy(
                            device = state.device.updateFromClient(result.client),
                            isCurrentDevice = result.isCurrentClient,
                            removeDeviceDialogState = RemoveDeviceDialogState.Hidden,
                            canBeRemoved = !result.isCurrentClient && isSelfClient && result.client.type != ClientType.LegalHold,
                            mlsCipherSuiteSignature = MLSPublicKeyType.from(
                                result.client.mlsPublicKeys?.keys?.firstOrNull().orEmpty()
                            ).let { mapCipherSuiteSignatureToShortName(it) }
                        )
                    }
                }
            }
        }
    }

    private fun mapCipherSuiteSignatureToShortName(signature: MLSPublicKeyType): String {
        return when (signature) {
            MLSPublicKeyType.ECDSA_SECP256R1_SHA256 -> "P256"
            MLSPublicKeyType.ECDSA_SECP384R1_SHA384 -> "P384"
            MLSPublicKeyType.ECDSA_SECP521R1_SHA512 -> "P521"
            MLSPublicKeyType.ED25519 -> "ED25519"
            MLSPublicKeyType.ED448 -> "ED448"
            is MLSPublicKeyType.Unknown -> "Unknown"
        }
    }

    fun removeDevice(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val isPasswordRequired: Boolean = when (val passwordRequiredResult = isPasswordRequired()) {
                is IsPasswordRequiredUseCase.Result.Failure -> {
                    state = state.copy(error = RemoveDeviceError.GenericError(passwordRequiredResult.cause))
                    return@launch
                }

                is IsPasswordRequiredUseCase.Result.Success -> passwordRequiredResult.value
            }
            when (isPasswordRequired) {
                true -> showDeleteClientDialog(state.device)
                false -> deleteDevice(null, onSuccess)
            }
        }
    }

    private fun showDeleteClientDialog(device: Device) {
        passwordTextState.clearText()
        state = device.let { RemoveDeviceDialogState.Visible(it) }.let {
            state.copy(
                error = RemoveDeviceError.None,
                removeDeviceDialogState = it
            )
        }
    }

    private suspend fun deleteDevice(password: String?, onSuccess: () -> Unit) {
        when (val result = deleteClient(DeleteClientParam(password, deviceId))) {
            is DeleteClientResult.Failure.Generic -> state = state.copy(
                error = RemoveDeviceError.GenericError(result.genericFailure)
            )

            DeleteClientResult.Failure.InvalidCredentials -> state = state.copy(
                error = RemoveDeviceError.InvalidCredentialsError
            )

            DeleteClientResult.Failure.PasswordAuthRequired -> showDeleteClientDialog(state.device)
            DeleteClientResult.Success -> onSuccess()
        }
    }

    fun onRemoveConfirmed(onSuccess: () -> Unit) {
        (state.removeDeviceDialogState as? RemoveDeviceDialogState.Visible)?.let { dialogStateVisible ->
            updateStateIfDialogVisible {
                state.copy(
                    removeDeviceDialogState = it.copy(loading = true, removeEnabled = false)
                )
            }
            viewModelScope.launch {
                deleteDevice(passwordTextState.text.toString(), onSuccess)
                updateStateIfDialogVisible { state.copy(removeDeviceDialogState = it.copy(loading = false)) }
            }
        }
    }

    private fun updateStateIfDialogVisible(newValue: (RemoveDeviceDialogState.Visible) -> DeviceDetailsState) {
        if (state.removeDeviceDialogState is RemoveDeviceDialogState.Visible) {
            state = newValue(state.removeDeviceDialogState as RemoveDeviceDialogState.Visible)
        }
    }

    fun onUpdateVerificationStatus(status: Boolean) {
        viewModelScope.launch {
            updateClientVerificationStatus(userId, deviceId, status)
        }
    }

    fun onDialogDismissed() {
        passwordTextState.clearText()
        state = state.copy(removeDeviceDialogState = RemoveDeviceDialogState.Hidden)
    }

    fun clearDeleteClientError() {
        state = state.copy(error = RemoveDeviceError.None)
    }

    fun hideEnrollE2EICertificateError() {
        state = state.copy(isE2EICertificateEnrollError = false)
    }

    fun hideEnrollE2EICertificateSuccess() {
        state = state.copy(isE2EICertificateEnrollSuccess = false)
    }

    fun breakSession() {
        viewModelScope.launch { breakSession(userId, deviceId) }
    }
}
