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

package com.wire.android.ui.userprofile.self

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.mapper.OtherAccountMapper
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.notification.WireNotificationManager
import com.wire.android.ui.legalhold.banner.LegalHoldUIState
import com.wire.android.ui.userprofile.self.dialog.StatusDialogData
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.auth.LogoutUseCase
import com.wire.kalium.logic.feature.call.usecase.EndCallUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.team.GetUpdatedSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.UpdateSelfAvailabilityStatusUseCase
import com.wire.kalium.logic.functional.getOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// TODO cover this class with unit test
// Suppress for now after removing mockMethodForAvatar it should not complain
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class SelfUserProfileViewModel @Inject constructor(
    @CurrentAccount private val selfUserId: UserId,
    private val dataStore: UserDataStore,
    private val getSelf: GetSelfUserUseCase,
    private val getSelfTeam: GetUpdatedSelfTeamUseCase,
    private val canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase,
    private val observeValidAccounts: ObserveValidAccountsUseCase,
    private val updateStatus: UpdateSelfAvailabilityStatusUseCase,
    private val logout: LogoutUseCase,
    private val observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
    private val dispatchers: DispatcherProvider,
    private val otherAccountMapper: OtherAccountMapper,
    private val observeEstablishedCalls: ObserveEstablishedCallsUseCase,
    private val accountSwitch: AccountSwitchUseCase,
    private val endCall: EndCallUseCase,
    private val isReadOnlyAccount: IsReadOnlyAccountUseCase,
    private val notificationManager: WireNotificationManager,
    private val globalDataStore: GlobalDataStore,
    private val qualifiedIdMapper: QualifiedIdMapper,
    private val anonymousAnalyticsManager: AnonymousAnalyticsManager,
    private val getTeamUrl: GetTeamUrlUseCase
) : ViewModel() {

    var userProfileState by mutableStateOf(SelfUserProfileState(userId = selfUserId, isAvatarLoading = true))
        private set

    private lateinit var establishedCallsList: StateFlow<List<Call>>

    init {
        viewModelScope.launch {
            fetchSelfUser()
            observeEstablishedCall()
            fetchIsReadOnlyAccount()
            observeLegalHoldStatus()
            markCreateTeamNoticeAsRead()
        }
    }

    suspend fun checkIfUserAbleToMigrateToTeamAccount() {
        userProfileState = userProfileState.copy(isAbleToMigrateToTeamAccount = canMigrateFromPersonalToTeam())
    }

    private suspend fun fetchIsReadOnlyAccount() {
        val isReadOnlyAccount = isReadOnlyAccount()
        userProfileState = userProfileState.copy(isReadOnlyAccount = isReadOnlyAccount)
    }

    private fun observeEstablishedCall() {
        viewModelScope.launch {
            establishedCallsList = observeEstablishedCalls()
                .distinctUntilChanged()
                .flowOn(dispatchers.io())
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
        }
    }

    private fun markCreateTeamNoticeAsRead() {
        viewModelScope.launch {
            if (getSelf().first().teamId == null && !dataStore.isCreateTeamNoticeRead().first()) {
                dataStore.setIsCreateTeamNoticeRead(true)
            }
        }
    }

    fun isUserInCall(): Boolean = establishedCallsList.value.isNotEmpty()

    fun reloadNewPickedAvatar(avatarAssetId: String) {
        updateUserAvatar(avatarAssetId = avatarAssetId.toQualifiedID(qualifiedIdMapper))
    }

    private fun fetchSelfUser() {
        viewModelScope.launch {
            val self = getSelf().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))
            val selfTeam = getSelfTeam().getOrNull()
            val validAccounts =
                observeValidAccounts().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))

            combine(self, validAccounts) { selfUser: SelfUser, list: List<Pair<SelfUser, Team?>> ->
                Pair(
                    selfUser,
                    list.filter { it.first.id != selfUser.id }
                        .map { (selfUser, _) -> otherAccountMapper.toOtherAccount(selfUser) }
                )
            }
                .distinctUntilChanged()
                .collect { (selfUser, otherAccounts) ->
                    with(selfUser) {
                        // Load user avatar raw image data
                        completePicture?.let { updateUserAvatar(it) }

                        // Update user data state
                        userProfileState = userProfileState.copy(
                            status = availabilityStatus,
                            fullName = name.orEmpty(),
                            userName = handle.orEmpty(),
                            teamName = selfTeam?.name,
                            teamUrl = getTeamUrl().takeIf { userType == UserType.OWNER || userType == UserType.ADMIN },
                            otherAccounts = otherAccounts,
                            avatarAsset = userProfileState.avatarAsset,
                            isAvatarLoading = false,
                            accentId = accentId
                        )
                    }
                }
        }
    }

    private fun observeLegalHoldStatus() {
        viewModelScope.launch {
            observeLegalHoldStatusForSelfUser()
                .map { legalHoldState ->
                    when (legalHoldState) {
                        is LegalHoldStateForSelfUser.Enabled -> LegalHoldUIState.Active
                        is LegalHoldStateForSelfUser.PendingRequest -> LegalHoldUIState.Pending
                        is LegalHoldStateForSelfUser.Disabled -> LegalHoldUIState.None
                    }
                }
                .collectLatest { userProfileState = userProfileState.copy(legalHoldStatus = it) }
        }
    }

    private fun showErrorMessage() {
        userProfileState = userProfileState.copy(errorMessageCode = ErrorCodes.DownloadUserInfoError)
    }

    private fun showLoadingAvatar(show: Boolean) {
        userProfileState = userProfileState.copy(isAvatarLoading = show)
    }

    private fun updateUserAvatar(avatarAssetId: UserAssetId) {
        if (avatarAssetId == userProfileState.avatarAsset?.userAssetId) {
            return
        }

        // We try to download the user avatar on a separate thread so that we don't block the display of the user's info
        viewModelScope.launch {
            showLoadingAvatar(true)
            try {
                userProfileState = userProfileState.copy(
                    avatarAsset = UserAvatarAsset(avatarAssetId)
                )
                // Update avatar asset id on user data store
                // TODO: obtain the asset id through a useCase once we also store assets ids
                withContext(dispatchers.io()) { dataStore.updateUserAvatarAssetId(avatarAssetId.toString()) }
            } catch (e: ClassCastException) {
                appLogger.e("There was an error while downloading the user avatar", e)
                // Show error snackbar if avatar download fails
                showErrorMessage()
            }

            showLoadingAvatar(false)
        }
    }

    fun logout(wipeData: Boolean, switchAccountActions: SwitchAccountActions) {
        viewModelScope.launch {
            userProfileState = userProfileState.copy(isLoggingOut = true)
            launch {
                // TODO instead of ending calls show toast that cannot logout during call
                establishedCallsList.value.forEach { call ->
                    endCall(call.conversationId)
                }
            }.join()

            val logoutReason = if (wipeData) LogoutReason.SELF_HARD_LOGOUT else LogoutReason.SELF_SOFT_LOGOUT
            logout(logoutReason, waitUntilCompletes = true)
            if (wipeData) {
                // TODO this should be moved to some service that will clear all the data in the app
                dataStore.clear()
            }

            notificationManager.stopObservingOnLogout(selfUserId)
            accountSwitch(SwitchAccountParam.TryToSwitchToNextAccount).also {
                if (it == SwitchAccountResult.NoOtherAccountToSwitch) {
                    globalDataStore.clearAppLockPasscode()
                }
            }.callAction(switchAccountActions)
        }
    }

    fun switchAccount(userId: UserId, switchAccountActions: SwitchAccountActions) {
        viewModelScope.launch {
            accountSwitch(SwitchAccountParam.SwitchToAccount(userId))
                .callAction(switchAccountActions)
        }
    }

    fun dismissStatusDialog() {
        userProfileState = userProfileState.copy(statusDialogData = null)
    }

    fun changeStatus(status: UserAvailabilityStatus) {
        setNotShowStatusRationaleAgainIfNeeded(status)
        viewModelScope.launch { updateStatus(status) }
        dismissStatusDialog()
    }

    fun dialogCheckBoxStateChanged(isChecked: Boolean) {
        userProfileState.run {
            userProfileState = copy(statusDialogData = statusDialogData?.changeCheckBoxState(isChecked))
        }
    }

    fun changeStatusClick(status: UserAvailabilityStatus) {
        if (userProfileState.status == status) return

        viewModelScope.launch {
            if (shouldShowStatusRationaleDialog(status)) {
                val statusDialogInfo = when (status) {
                    UserAvailabilityStatus.AVAILABLE -> StatusDialogData.StateAvailable()
                    UserAvailabilityStatus.BUSY -> StatusDialogData.StateBusy()
                    UserAvailabilityStatus.AWAY -> StatusDialogData.StateAway()
                    UserAvailabilityStatus.NONE -> StatusDialogData.StateNone()
                }
                userProfileState = userProfileState.copy(statusDialogData = statusDialogInfo)
            } else {
                changeStatus(status)
            }
        }
    }

    private fun setNotShowStatusRationaleAgainIfNeeded(status: UserAvailabilityStatus) {
        userProfileState.statusDialogData.let { dialogState ->
            if (dialogState?.isCheckBoxChecked == true) {
                viewModelScope.launch { dataStore.dontShowStatusRationaleAgain(status) }
            }
        }
    }

    private suspend fun shouldShowStatusRationaleDialog(status: UserAvailabilityStatus): Boolean =
        dataStore.shouldShowStatusRationaleFlow(status).first()

    fun clearErrorMessage() {
        userProfileState = userProfileState.copy(errorMessageCode = null)
    }

    fun trackQrCodeClick() {
        anonymousAnalyticsManager.sendEvent(AnalyticsEvent.QrCode.Click(!userProfileState.teamName.isNullOrBlank()))
    }

    fun sendPersonalToTeamMigrationEvent() {
        anonymousAnalyticsManager.sendEvent(
            AnalyticsEvent.PersonalTeamMigration.ClickedPersonalTeamMigrationCta(
                createTeamButtonClicked = true
            )
        )
    }

    sealed class ErrorCodes {
        data object DownloadUserInfoError : ErrorCodes()
    }
}
