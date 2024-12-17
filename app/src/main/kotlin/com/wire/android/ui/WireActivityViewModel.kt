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

package com.wire.android.ui

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.ObserveIfE2EIRequiredDuringLoginUseCaseProvider
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.migration.MigrationManager
import com.wire.android.services.ServicesManager
import com.wire.android.ui.authentication.devices.model.displayName
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialogState
import com.wire.android.ui.common.dialogs.CustomServerDialogState
import com.wire.android.ui.common.dialogs.CustomServerNoNetworkDialogState
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.workmanager.worker.cancelPeriodicPersistentWebsocketCheckWorker
import com.wire.android.workmanager.worker.enqueuePeriodicPersistentWebsocketCheckWorker
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.client.Client
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveRecentlyEndedCallMetadataUseCase
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.NewClientResult
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase
import com.wire.kalium.logic.feature.debug.SynchronizeExternalDataResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.util.DateTimeUtil.toIsoDateTimeString
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WireActivityViewModel @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val dispatchers: DispatcherProvider,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
    private val doesValidSessionExist: Lazy<DoesValidSessionExistUseCase>,
    private val getServerConfigUseCase: Lazy<GetServerConfigUseCase>,
    private val deepLinkProcessor: Lazy<DeepLinkProcessor>,
    private val authServerConfigProvider: Lazy<AuthServerConfigProvider>,
    private val getSessions: Lazy<GetSessionsUseCase>,
    private val accountSwitch: Lazy<AccountSwitchUseCase>,
    private val migrationManager: Lazy<MigrationManager>,
    private val servicesManager: Lazy<ServicesManager>,
    private val observeSyncStateUseCaseProviderFactory: ObserveSyncStateUseCaseProvider.Factory,
    private val observeIfAppUpdateRequired: Lazy<ObserveIfAppUpdateRequiredUseCase>,
    private val observeNewClients: Lazy<ObserveNewClientsUseCase>,
    private val clearNewClientsForUser: Lazy<ClearNewClientsForUserUseCase>,
    private val currentScreenManager: Lazy<CurrentScreenManager>,
    private val observeScreenshotCensoringConfigUseCaseProviderFactory: ObserveScreenshotCensoringConfigUseCaseProvider.Factory,
    private val globalDataStore: Lazy<GlobalDataStore>,
    private val observeIfE2EIRequiredDuringLoginUseCaseProviderFactory: ObserveIfE2EIRequiredDuringLoginUseCaseProvider.Factory,
    private val workManager: Lazy<WorkManager>,
    private val observeRecentlyEndedCallMetadata: ObserveRecentlyEndedCallMetadataUseCase,
    private val analyticsManager: AnonymousAnalyticsManager
) : ViewModel() {

    var globalAppState: GlobalAppState by mutableStateOf(GlobalAppState())
        private set

    private val _observeSyncFlowState: MutableStateFlow<SyncState?> = MutableStateFlow(null)
    val observeSyncFlowState: StateFlow<SyncState?> = _observeSyncFlowState

    private val observeCurrentAccountInfo: SharedFlow<AccountInfo?> = currentSessionFlow.get().invoke()
        .map { (it as? CurrentSessionResult.Success)?.accountInfo }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    private val observeCurrentValidUserId: SharedFlow<UserId?> = observeCurrentAccountInfo
        .map {
            if (it?.isValid() == true) it.userId else null
        }
        .distinctUntilChanged()
        .flowOn(dispatchers.io())
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)

    init {
        observeSyncState()
        observeUpdateAppState()
        observeNewClientState()
        observeScreenshotCensoringConfigState()
        observeAppThemeState()
        observeLogoutState()
        trackRecentlyEndedCall()
    }

    private fun trackRecentlyEndedCall() {
        viewModelScope.launch {
            observeRecentlyEndedCallMetadata()
                .collect { metadata ->
                    analyticsManager.sendEvent(AnalyticsEvent.RecentlyEndedCallEvent(metadata))
                }
        }
    }

    private suspend fun shouldEnrollToE2ei(): Boolean = observeCurrentValidUserId.first()?.let {
        observeIfE2EIRequiredDuringLoginUseCaseProviderFactory.create(it)
            .observeIfE2EIIsRequiredDuringLogin().first() ?: false
    } ?: false

    private fun observeAppThemeState() {
        viewModelScope.launch(dispatchers.io()) {
            globalDataStore.get().selectedThemeOptionFlow()
                .distinctUntilChanged()
                .collect {
                    globalAppState = globalAppState.copy(themeOption = it)
                }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch(dispatchers.io()) {
            observeCurrentValidUserId
                .flatMapLatest { userId ->
                    userId?.let {
                        observeSyncStateUseCaseProviderFactory.create(userId).observeSyncState()
                    } ?: flowOf(null)
                }
                .distinctUntilChanged()
                .flowOn(dispatchers.io())
                .collect {
                    _observeSyncFlowState.emit(it)
                }
        }
    }

    private fun observeLogoutState() {
        viewModelScope.launch(dispatchers.io()) {
            observeCurrentAccountInfo
                .collect {
                    if (it is AccountInfo.Invalid) {
                        handleInvalidSession(it.logoutReason)
                    }
                }
        }
    }

    private fun observeUpdateAppState() {
        viewModelScope.launch(dispatchers.io()) {
            observeIfAppUpdateRequired.get().invoke(BuildConfig.VERSION_CODE)
                .distinctUntilChanged()
                .collect {
                    globalAppState = globalAppState.copy(updateAppDialog = it)
                }
        }
    }

    private fun observeNewClientState() {
        viewModelScope.launch(dispatchers.io()) {
            currentScreenManager.get().observeCurrentScreen(this)
                .flatMapLatest {
                    if (it.isGlobalDialogAllowed()) {
                        observeNewClients.get().invoke()
                    } else {
                        flowOf(NewClientResult.Empty)
                    }
                }
                .collect {
                    val newClientDialog = NewClientsData.fromUseCaseResul(it)
                    globalAppState = globalAppState.copy(newClientDialog = newClientDialog)
                }
        }
    }

    private fun observeScreenshotCensoringConfigState() {
        viewModelScope.launch(dispatchers.io()) {
            observeCurrentValidUserId
                .flatMapLatest { currentValidUserId ->
                    currentValidUserId?.let {
                        observeScreenshotCensoringConfigUseCaseProviderFactory.create(it)
                            .observeScreenshotCensoringConfig()
                            .map { result ->
                                result is ObserveScreenshotCensoringConfigResult.Enabled
                            }
                    } ?: flowOf(false)
                }
                .collect {
                    globalAppState = globalAppState.copy(screenshotCensoringEnabled = it)
                }
        }
    }

    suspend fun initialAppState(): InitialAppState = withContext(dispatchers.io()) {
        when {
            shouldMigrate() -> InitialAppState.NOT_MIGRATED
            shouldLogIn() -> InitialAppState.NOT_LOGGED_IN
            shouldEnrollToE2ei() -> InitialAppState.ENROLL_E2EI
            else -> InitialAppState.LOGGED_IN
        }
    }

    private suspend fun handleInvalidSession(logoutReason: LogoutReason) {
        withContext(dispatchers.main()) {
            when (logoutReason) {
                LogoutReason.SELF_SOFT_LOGOUT, LogoutReason.SELF_HARD_LOGOUT -> {
                    // Self logout is handled from the Self user profile screen directly
                }

                LogoutReason.MIGRATION_TO_CC_FAILED, LogoutReason.REMOVED_CLIENT ->
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.RemovedClient)

                LogoutReason.DELETED_ACCOUNT ->
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.DeletedAccount)

                LogoutReason.SESSION_EXPIRED ->
                    globalAppState =
                        globalAppState.copy(blockUserUI = CurrentSessionErrorState.SessionExpired)
            }
        }
    }

    private fun isSharingIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_SEND_MULTIPLE
    }

    fun handleSynchronizeExternalData(
        data: InputStream
    ) {
        viewModelScope.launch(dispatchers.io()) {
            when (val currentSession = coreLogic.getGlobalScope().session.currentSession()) {
                is CurrentSessionResult.Failure.Generic -> null
                CurrentSessionResult.Failure.SessionNotFound -> null
                is CurrentSessionResult.Success -> {
                    coreLogic.sessionScope(currentSession.accountInfo.userId) {
                        when (val result = debug.synchronizeExternalData(InputStreamReader(data).readText())) {
                            is SynchronizeExternalDataResult.Success -> {
                                appLogger.d("Synchronized external data")
                            }

                            is SynchronizeExternalDataResult.Failure -> {
                                appLogger.d("Failed to Synchronize external data: ${result.coreFailure}")
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("ComplexMethod")
    fun handleDeepLink(
        intent: Intent?,
        onIsSharingIntent: () -> Unit,
        onOpenConversation: (DeepLinkResult.OpenConversation) -> Unit,
        onSSOLogin: (DeepLinkResult.SSOLogin) -> Unit,
        onMigrationLogin: (DeepLinkResult.MigrationLogin) -> Unit,
        onOpenOtherUserProfile: (DeepLinkResult.OpenOtherUserProfile) -> Unit,
        onAuthorizationNeeded: () -> Unit,
        onCannotLoginDuringACall: () -> Unit
    ) {
        viewModelScope.launch(dispatchers.io()) {
            if (shouldMigrate()) {
                // means User is Logged in, but didn't finish the migration yet.
                // so we need to finish migration first.
                return@launch
            }
            val isSharingIntent = isSharingIntent(intent)
            when (val result = deepLinkProcessor.get().invoke(intent?.data, isSharingIntent)) {
                DeepLinkResult.AuthorizationNeeded -> onAuthorizationNeeded()
                is DeepLinkResult.SSOLogin -> onSSOLogin(result)
                is DeepLinkResult.CustomServerConfig -> onCustomServerConfig(result.url)
                is DeepLinkResult.Failure.OngoingCall -> onCannotLoginDuringACall()
                is DeepLinkResult.Failure.Unknown -> appLogger.e("unknown deeplink failure")
                is DeepLinkResult.JoinConversation -> onConversationInviteDeepLink(
                    result.code,
                    result.key,
                    result.domain
                ) { conversationId -> onOpenConversation(DeepLinkResult.OpenConversation(conversationId, result.switchedAccount)) }

                is DeepLinkResult.MigrationLogin -> onMigrationLogin(result)
                is DeepLinkResult.OpenConversation -> onOpenConversation(result)
                is DeepLinkResult.OpenOtherUserProfile -> onOpenOtherUserProfile(result)

                DeepLinkResult.SharingIntent -> onIsSharingIntent()
                DeepLinkResult.Unknown -> appLogger.e("unknown deeplink result $result")
            }
        }
    }

    fun dismissCustomBackendDialog() {
        globalAppState = globalAppState.copy(customBackendDialog = null)
    }

    fun customBackendDialogProceedButtonClicked(onProceed: () -> Unit) {
        val backendDialogState = globalAppState.customBackendDialog
        if (backendDialogState is CustomServerDetailsDialogState) {
            viewModelScope.launch {
                authServerConfigProvider.get()
                    .updateAuthServer(backendDialogState.serverLinks)
                dismissCustomBackendDialog()
                if (checkNumberOfSessions() >= BuildConfig.MAX_ACCOUNTS) {
                    globalAppState = globalAppState.copy(maxAccountDialog = true)
                } else {
                    onProceed()
                }
            }
        }
    }

    // TODO: needs to be covered with test once hard logout is validated to be used
    fun doHardLogout(
        clearUserData: (userId: UserId) -> Unit,
        switchAccountActions: SwitchAccountActions
    ) {
        viewModelScope.launch {
            coreLogic.getGlobalScope().session.currentSession().takeIf {
                it is CurrentSessionResult.Success
            }?.let {
                val currentUserId = (it as CurrentSessionResult.Success).accountInfo.userId
                coreLogic.getSessionScope(currentUserId).logout(LogoutReason.SELF_HARD_LOGOUT)
                clearUserData(currentUserId)
            }
            accountSwitch.get().invoke(SwitchAccountParam.TryToSwitchToNextAccount).also {
                if (it == SwitchAccountResult.NoOtherAccountToSwitch) {
                    globalDataStore.get().clearAppLockPasscode()
                }
            }.callAction(switchAccountActions)
        }
    }

    fun dismissNewClientsDialog(userId: UserId) {
        globalAppState = globalAppState.copy(newClientDialog = null)
        viewModelScope.launch {
            doesValidSessionExist.get().invoke(userId).let {
                if (it is DoesValidSessionExistResult.Success && it.doesValidSessionExist) {
                    clearNewClientsForUser.get().invoke(userId)
                }
            }
        }
    }

    fun switchAccount(userId: UserId, actions: SwitchAccountActions, onComplete: () -> Unit) {
        viewModelScope.launch {
            accountSwitch.get().invoke(SwitchAccountParam.SwitchToAccount(userId))
                .callAction(actions)
            onComplete()
        }
    }

    fun tryToSwitchAccount(actions: SwitchAccountActions) {
        viewModelScope.launch {
            globalAppState = globalAppState.copy(blockUserUI = null)
            accountSwitch.get().invoke(SwitchAccountParam.TryToSwitchToNextAccount)
                .callAction(actions)
        }
    }

    private suspend fun checkNumberOfSessions(): Int {
        getSessions.get().invoke().let {
            return when (it) {
                is GetAllSessionsResult.Success -> {
                    it.sessions.filterIsInstance<AccountInfo.Valid>().size
                }

                is GetAllSessionsResult.Failure.Generic -> 0
                GetAllSessionsResult.Failure.NoSessionFound -> 0
            }
        }
    }

    private suspend fun loadServerConfig(url: String): ServerConfig.Links? =
        when (val result = getServerConfigUseCase.get().invoke(url)) {
            is GetServerConfigResult.Success -> result.serverConfigLinks
            is GetServerConfigResult.Failure.Generic -> {
                appLogger.e("something went wrong during handling the custom server deep link: ${result.genericFailure}")
                null
            }
        }

    fun onCustomServerConfig(customServerUrl: String) {
        viewModelScope.launch(dispatchers.io()) {
            val customBackendDialogData = loadServerConfig(customServerUrl)
                ?.let { serverLinks -> CustomServerDetailsDialogState(serverLinks = serverLinks) }
                ?: CustomServerNoNetworkDialogState(customServerUrl)

            globalAppState = globalAppState.copy(
                customBackendDialog = customBackendDialogData
            )
        }
    }

    private suspend fun onConversationInviteDeepLink(
        code: String,
        key: String,
        domain: String?,
        onSuccess: (ConversationId) -> Unit
    ) = when (val currentSession = coreLogic.getGlobalScope().session.currentSession()) {
        is CurrentSessionResult.Failure.Generic -> null
        CurrentSessionResult.Failure.SessionNotFound -> null
        is CurrentSessionResult.Success -> {
            coreLogic.sessionScope(currentSession.accountInfo.userId) {
                when (val result = conversations.checkIConversationInviteCode(code, key, domain)) {
                    is CheckConversationInviteCodeUseCase.Result.Success -> {
                        if (result.isSelfMember) {
                            // TODO; display messsage that user is already a member and ask if they want to navigate to the conversation
                            appLogger.d("user is already a member of the conversation")
                            onSuccess(result.conversationId)
                        } else {
                            globalAppState =
                                globalAppState.copy(
                                    conversationJoinedDialog = JoinConversationViaCodeState.Show(
                                        result.name,
                                        code,
                                        key,
                                        domain,
                                        result.isPasswordProtected
                                    )
                                )
                        }
                    }

                    is CheckConversationInviteCodeUseCase.Result.Failure -> globalAppState =
                        globalAppState.copy(
                            conversationJoinedDialog = JoinConversationViaCodeState.Error(result)
                        )
                }
            }
        }
    }

    fun onJoinConversationFlowCompleted() {
        globalAppState = globalAppState.copy(conversationJoinedDialog = null)
    }

    private suspend fun shouldLogIn(): Boolean = observeCurrentValidUserId.first() == null

    private suspend fun shouldMigrate(): Boolean = migrationManager.get().shouldMigrate()

    fun dismissMaxAccountDialog() {
        globalAppState = globalAppState.copy(maxAccountDialog = false)
    }

    fun observePersistentConnectionStatus() {
        viewModelScope.launch {
            coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus()
                .let { result ->
                    when (result) {
                        is ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure -> {
                            appLogger.e("Failure while fetching persistent web socket status flow from wire activity")
                        }

                        is ObservePersistentWebSocketConnectionStatusUseCase.Result.Success -> {
                            result.persistentWebSocketStatusListFlow.collect { statuses ->

                                if (statuses.any { it.isPersistentWebSocketEnabled }) {
                                    if (!servicesManager.get()
                                            .isPersistentWebSocketServiceRunning()
                                    ) {
                                        servicesManager.get().startPersistentWebSocketService()
                                        workManager.get()
                                            .enqueuePeriodicPersistentWebsocketCheckWorker()
                                    }
                                } else {
                                    servicesManager.get().stopPersistentWebSocketService()
                                    workManager.get().cancelPeriodicPersistentWebsocketCheckWorker()
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun CurrentScreen.isGlobalDialogAllowed(): Boolean = when (this) {
        is CurrentScreen.ImportMedia,
        is CurrentScreen.DeviceManager -> false

        is CurrentScreen.InBackground,
        is CurrentScreen.Conversation,
        is CurrentScreen.Home,
        is CurrentScreen.OtherUserProfile,
        is CurrentScreen.AuthRelated,
        is CurrentScreen.SomeOther -> true
    }
}

sealed class CurrentSessionErrorState {
    data object RemovedClient : CurrentSessionErrorState()
    data object DeletedAccount : CurrentSessionErrorState()
    data object SessionExpired : CurrentSessionErrorState()
}

sealed class NewClientsData(open val clientsInfo: List<NewClientInfo>, open val userId: UserId) {
    data class CurrentUser(
        override val clientsInfo: List<NewClientInfo>,
        override val userId: UserId
    ) : NewClientsData(clientsInfo, userId)

    data class OtherUser(
        override val clientsInfo: List<NewClientInfo>,
        override val userId: UserId,
        val userName: String?,
        val userHandle: String?
    ) : NewClientsData(clientsInfo, userId)

    companion object {
        fun fromUseCaseResul(result: NewClientResult): NewClientsData? = when (result) {
            is NewClientResult.InCurrentAccount -> {
                CurrentUser(
                    result.newClients.map(NewClientInfo::fromClient),
                    result.userId
                )
            }

            is NewClientResult.InOtherAccount -> {
                OtherUser(
                    result.newClients.map(NewClientInfo::fromClient),
                    result.userId,
                    result.userName,
                    result.userHandle
                )
            }

            else -> null
        }
    }
}

data class NewClientInfo(val date: String, val deviceInfo: UIText) {
    companion object {
        fun fromClient(client: Client): NewClientInfo =
            NewClientInfo(
                client.registrationTime?.toIsoDateTimeString() ?: "",
                client.displayName()
            )
    }
}

data class GlobalAppState(
    val customBackendDialog: CustomServerDialogState? = null,
    val maxAccountDialog: Boolean = false,
    val blockUserUI: CurrentSessionErrorState? = null,
    val updateAppDialog: Boolean = false,
    val conversationJoinedDialog: JoinConversationViaCodeState? = null,
    val newClientDialog: NewClientsData? = null,
    val screenshotCensoringEnabled: Boolean = true,
    val themeOption: ThemeOption = ThemeOption.SYSTEM
)

enum class InitialAppState {
    NOT_MIGRATED, NOT_LOGGED_IN, LOGGED_IN, ENROLL_E2EI
}
