/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.AppLockSource
import com.wire.android.feature.DisableAppLockUseCase
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.messagecomposer.SelfDeletionDuration
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.FileSharingStatus
import com.wire.kalium.logic.data.message.TeamSelfDeleteTimer
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.E2EIRequiredResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class FeatureFlagNotificationViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val currentSessionFlow: CurrentSessionFlowUseCase,
    private val globalDataStore: GlobalDataStore,
    private val disableAppLockUseCase: DisableAppLockUseCase
) : ViewModel() {

    var featureFlagState by mutableStateOf(FeatureFlagState())
        private set

    private var currentUserId by mutableStateOf<UserId?>(null)

    init {
        viewModelScope.launch { initialSync() }
    }

    /**
     * The FeatureFlagNotificationViewModel is an attempt to encapsulate the logic regarding the different user feature flags, like for
     * example the file sharing one. This means that this VM could be invoked as an extension from outside the general app lifecycle (for
     * example when trying to share a file from an external app into Wire).
     *
     * This method is therefore called to check whether the user has a valid session or not. If the user does have a valid one, it observes
     * it until the sync state is live. Once the sync state is live, it sets whether the file sharing feature is enabled or not on the VM
     * state.
     */
    private suspend fun initialSync() {
        currentSessionFlow()
            .distinctUntilChanged()
            .collectLatest { currentSessionResult ->
                when (currentSessionResult) {
                    is CurrentSessionResult.Failure -> {
                        currentUserId = null
                        appLogger.e("Failure while getting current session from FeatureFlagNotificationViewModel")
                        featureFlagState = FeatureFlagState( // no session, clear feature flag state to default and set NO_USER
                            fileSharingRestrictedState = FeatureFlagState.SharingRestrictedState.NO_USER
                        )
                    }

                    is CurrentSessionResult.Success -> {
                        featureFlagState = FeatureFlagState() // new session, clear feature flag state to default and wait until synced
                        val userId = currentSessionResult.accountInfo.userId
                        currentUserId = userId
                        coreLogic.getSessionScope(userId).observeSyncState()
                            .firstOrNull { it == SyncState.Live }?.let {
                                observeStatesAfterInitialSync(userId)
                            }
                    }
                }
            }
    }

    private suspend fun observeStatesAfterInitialSync(userId: UserId) {
        coroutineScope {
            launch { setFileSharingState(userId) }
            launch { observeTeamSettingsSelfDeletionStatus(userId) }
            launch { setGuestRoomLinkFeatureFlag(userId) }
            launch { setE2EIRequiredState(userId) }
            launch { setTeamAppLockFeatureFlag(userId) }
            launch { observeCallEndedBecauseOfConversationDegraded(userId) }
        }
    }

    private suspend fun setFileSharingState(userId: UserId) {
        coreLogic.getSessionScope(userId).observeFileSharingStatus().collect { fileSharingStatus ->
            val state: FeatureFlagState.FileSharingState = when (fileSharingStatus.state) {
                FileSharingStatus.Value.Disabled -> FeatureFlagState.FileSharingState.DisabledByTeam
                FileSharingStatus.Value.EnabledAll -> FeatureFlagState.FileSharingState.AllowAll
                is FileSharingStatus.Value.EnabledSome -> FeatureFlagState.FileSharingState.AllowSome(
                    (fileSharingStatus.state as FileSharingStatus.Value.EnabledSome).allowedType
                )
            }
            featureFlagState = featureFlagState.copy(
                isFileSharingState = state,
                showFileSharingDialog = fileSharingStatus.isStatusChanged ?: false
            )
        }
    }

    private suspend fun setGuestRoomLinkFeatureFlag(userId: UserId) {
            coreLogic.getSessionScope(userId).observeGuestRoomLinkFeatureFlag()
                .collect { guestRoomLinkStatus ->
                    guestRoomLinkStatus.isGuestRoomLinkEnabled?.let {
                        featureFlagState = featureFlagState.copy(isGuestRoomLinkEnabled = it)
                    }
                    guestRoomLinkStatus.isStatusChanged?.let {
                        featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = it)
                    }
                }
        }

    private suspend fun setTeamAppLockFeatureFlag(userId: UserId) {
            coreLogic.getSessionScope(userId).appLockTeamFeatureConfigObserver()
                .distinctUntilChanged()
                .collectLatest { appLockConfig ->
                    appLockConfig?.isStatusChanged?.let { isStatusChanged ->
                        val shouldBlockApp = if (isStatusChanged) {
                            true
                        } else {
                            (!isUserAppLockSet() && appLockConfig.isEnforced)
                        }

                        featureFlagState = featureFlagState.copy(
                            isTeamAppLockEnabled = appLockConfig.isEnforced,
                            shouldShowTeamAppLockDialog = shouldBlockApp
                        )
                    }
                }
        }

    private suspend fun observeTeamSettingsSelfDeletionStatus(userId: UserId) {
            coreLogic.getSessionScope(userId).observeTeamSettingsSelfDeletionStatus()
                .collect { teamSettingsSelfDeletingStatus ->
                    val areSelfDeletedMessagesEnabled =
                        teamSettingsSelfDeletingStatus.enforcedSelfDeletionTimer !is TeamSelfDeleteTimer.Disabled
                    val shouldShowSelfDeletingMessagesDialog =
                        teamSettingsSelfDeletingStatus.hasFeatureChanged ?: false
                    val enforcedTimeoutDuration: SelfDeletionDuration =
                        with(teamSettingsSelfDeletingStatus.enforcedSelfDeletionTimer) {
                            when (this) {
                                TeamSelfDeleteTimer.Disabled,
                                TeamSelfDeleteTimer.Enabled -> SelfDeletionDuration.None

                                is TeamSelfDeleteTimer.Enforced -> this.enforcedDuration.toSelfDeletionDuration()
                            }
                        }
                    featureFlagState = featureFlagState.copy(
                        areSelfDeletedMessagesEnabled = areSelfDeletedMessagesEnabled,
                        shouldShowSelfDeletingMessagesDialog = shouldShowSelfDeletingMessagesDialog,
                        enforcedTimeoutDuration = enforcedTimeoutDuration
                    )
                }
        }

    private suspend fun setE2EIRequiredState(userId: UserId) {
        coreLogic.getSessionScope(userId).observeE2EIRequired().collect { result ->
            val state = when (result) {
                E2EIRequiredResult.NoGracePeriod.Create -> FeatureFlagState.E2EIRequired.NoGracePeriod.Create
                E2EIRequiredResult.NoGracePeriod.Renew -> FeatureFlagState.E2EIRequired.NoGracePeriod.Renew
                is E2EIRequiredResult.WithGracePeriod.Create -> FeatureFlagState.E2EIRequired.WithGracePeriod.Create(
                    result.timeLeft
                )

                is E2EIRequiredResult.WithGracePeriod.Renew -> FeatureFlagState.E2EIRequired.WithGracePeriod.Renew(
                    result.timeLeft
                )

                E2EIRequiredResult.NotRequired -> null
            }
            featureFlagState = featureFlagState.copy(e2EIRequired = state)
        }
    }

    private fun observeCallEndedBecauseOfConversationDegraded(userId: UserId) = viewModelScope.launch {
        coreLogic.getSessionScope(userId).calls.observeEndCallDialog().collect {
            featureFlagState = featureFlagState.copy(showCallEndedBecauseOfConversationDegraded = true)
        }
    }

    fun dismissSelfDeletingMessagesDialog() {
        featureFlagState = featureFlagState.copy(shouldShowSelfDeletingMessagesDialog = false)
        viewModelScope.launch {
            currentUserId?.let {
                coreLogic.getSessionScope(it).markSelfDeletingMessagesAsNotified()
            }
        }
    }

    fun dismissFileSharingDialog() {
        featureFlagState = featureFlagState.copy(showFileSharingDialog = false)
        viewModelScope.launch {
            currentUserId?.let { coreLogic.getSessionScope(it).markFileSharingStatusAsNotified() }
        }
    }

    fun dismissGuestRoomLinkDialog() {
        viewModelScope.launch {
            currentUserId?.let {
                coreLogic.getSessionScope(it).markGuestLinkFeatureFlagAsNotChanged()
            }
        }
        featureFlagState = featureFlagState.copy(shouldShowGuestRoomLinkDialog = false)
    }

    fun dismissTeamAppLockDialog() {
        featureFlagState = featureFlagState.copy(shouldShowTeamAppLockDialog = false)
    }

    fun markTeamAppLockStatusAsNot() {
        viewModelScope.launch {
            currentUserId?.let {
                coreLogic.getSessionScope(it).markTeamAppLockStatusAsNotified()
            }
        }
    }

    fun confirmAppLockNotEnforced() {
        viewModelScope.launch {
            when (globalDataStore.getAppLockSource()) {
                AppLockSource.Manual -> {}

                AppLockSource.TeamEnforced -> disableAppLockUseCase()
            }
        }
    }

    fun isUserAppLockSet() = globalDataStore.isAppLockPasscodeSet()

    fun getE2EICertificate() {
        // TODO do the magic
        featureFlagState = featureFlagState.copy(e2EIRequired = null)
    }

    fun snoozeE2EIdRequiredDialog(result: FeatureFlagState.E2EIRequired.WithGracePeriod) {
        featureFlagState = featureFlagState.copy(
            e2EIRequired = null,
            e2EISnoozeInfo = FeatureFlagState.E2EISnooze(result.timeLeft)
        )
        currentUserId?.let { userId ->
            viewModelScope.launch {
                coreLogic.getSessionScope(userId).markE2EIRequiredAsNotified(result.timeLeft)
            }
        }
    }

    fun dismissSnoozeE2EIdRequiredDialog() {
        featureFlagState = featureFlagState.copy(e2EISnoozeInfo = null)
    }

    fun dismissCallEndedBecauseOfConversationDegraded() {
        featureFlagState = featureFlagState.copy(showCallEndedBecauseOfConversationDegraded = false)
    }
}
