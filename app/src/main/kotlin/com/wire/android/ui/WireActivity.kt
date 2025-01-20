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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.ramcosta.composedestinations.spec.Route
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.config.CustomUiConfigurationProvider
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.NavigationSwitchAccountActions
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.LocalNavigator
import com.wire.android.navigation.MainNavHost
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.authentication.welcome.WelcomeScreenNavArgs
import com.wire.android.ui.calling.getIncomingCallIntent
import com.wire.android.ui.calling.getOutgoingCallIntent
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.CommonTopAppBar
import com.wire.android.ui.common.topappbar.CommonTopAppBarState
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.E2EIEnrollmentScreenDestination
import com.wire.android.ui.destinations.E2eiCertificateDetailsScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.ImportMediaScreenDestination
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.android.ui.destinations.MigrationScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfDevicesScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateUI
import com.wire.android.ui.home.E2EICertificateRevokedDialog
import com.wire.android.ui.home.E2EIRequiredDialog
import com.wire.android.ui.home.E2EIResultDialog
import com.wire.android.ui.home.E2EISnoozeDialog
import com.wire.android.ui.home.FeatureFlagState
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedDialog
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedState
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedViewModel
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedDialog
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedState
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel
import com.wire.android.ui.settings.devices.e2ei.E2EICertificateDetails
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialog
import com.wire.android.ui.userprofile.self.dialog.LogoutOptionsDialogState
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.LocalSyncStateObserver
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.SyncStateObserver
import com.wire.android.util.debug.FeatureVisibilityFlags
import com.wire.android.util.debug.LocalFeatureVisibilityFlags
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalComposeUiApi::class)
@AndroidEntryPoint
@Suppress("TooManyFunctions")
class WireActivity : AppCompatActivity() {

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    @Inject
    lateinit var lockCodeTimeManager: Lazy<LockCodeTimeManager>

    @Inject
    lateinit var switchAccountObserver: SwitchAccountObserver

    private val viewModel: WireActivityViewModel by viewModels()

    private val featureFlagNotificationViewModel: FeatureFlagNotificationViewModel by viewModels()

    private val commonTopAppBarViewModel: CommonTopAppBarViewModel by viewModels()
    private val legalHoldRequestedViewModel: LegalHoldRequestedViewModel by viewModels()
    private val legalHoldDeactivatedViewModel: LegalHoldDeactivatedViewModel by viewModels()

    val navigationCommands: MutableSharedFlow<NavigationCommand> = MutableSharedFlow()

    // This flag is used to keep the splash screen open until the first screen is drawn.
    private var shouldKeepSplashOpen = true
    private var isNavigationCollecting = false

    override fun onCreate(savedInstanceState: Bundle?) {

        appLogger.i("$TAG splash install")
        // We need to keep the splash screen open until the first screen is drawn.
        // Otherwise a white screen is displayed.
        // It's an API limitation, at some point we may need to remove it
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { shouldKeepSplashOpen }

        enableEdgeToEdge()

        lifecycleScope.launch {

            appLogger.i("$TAG persistent connection status")
            viewModel.observePersistentConnectionStatus()

            appLogger.i("$TAG legal hold requested status")
            legalHoldRequestedViewModel.observeLegalHoldRequest()

            appLogger.i("$TAG start destination")
            val startDestination = when (viewModel.initialAppState()) {
                InitialAppState.NOT_MIGRATED -> MigrationScreenDestination
                InitialAppState.NOT_LOGGED_IN -> WelcomeScreenDestination
                InitialAppState.ENROLL_E2EI -> E2EIEnrollmentScreenDestination
                InitialAppState.LOGGED_IN -> HomeScreenDestination
            }
            appLogger.i("$TAG composable content")

            setComposableContent(startDestination) {
                appLogger.i("$TAG splash hide")
                shouldKeepSplashOpen = false
                handleDeepLink(intent, savedInstanceState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.action?.equals(Intent.ACTION_SYNC) == true) {
            handleSynchronizeExternalData(intent)
            return
        }

        setIntent(intent)
        if (isNavigationCollecting) {
            /*
             * - When true then navigationCommands is subscribed and can handle navigation commands right away.
             * - When false then navigationCommands needs to be subscribed again to be able to receive and handle navigation commands.
             *
             * Activity intent is updated anyway using setIntent(intent) so that we always keep the latest intent received, so when
             * isNavigationCollecting is false then we handle it after navigationCommands is subscribed again and onComplete called again.
             * We make sure to handle particular intent only once thanks to the flag HANDLED_DEEPLINK_FLAG put into the handled intent.
             */
            handleDeepLink(intent)
        }
    }

    @Suppress("LongMethod")
    private fun setComposableContent(
        startDestination: Route,
        onComplete: () -> Unit
    ) {
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }

            HandleThemeChanges(viewModel.globalAppState.themeOption)

            CompositionLocalProvider(
                LocalFeatureVisibilityFlags provides FeatureVisibilityFlags,
                LocalSyncStateObserver provides SyncStateObserver(viewModel.observeSyncFlowState),
                LocalCustomUiConfigurationProvider provides CustomUiConfigurationProvider,
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme {
                    Column(
                        modifier = Modifier
                            .semantics { testTagsAsResourceId = true }
                    ) {
                        val navigator = rememberNavigator(this@WireActivity::finish)
                        WireTopAppBar(
                            commonTopAppBarState = commonTopAppBarViewModel.state,
                        )
                        CompositionLocalProvider(LocalNavigator provides navigator) {
                            MainNavHost(
                                navigator = navigator,
                                startDestination = startDestination,
                                modifier = Modifier.consumeWindowInsets(WindowInsets.statusBars)
                            )
                        }

                        // This setup needs to be done after the navigation graph is created, because building the graph takes some time,
                        // and if any NavigationCommand is executed before the graph is fully built, it will cause a NullPointerException.
                        SetUpNavigation(navigator, onComplete)
                        HandleScreenshotCensoring()
                        HandleDialogs(navigator::navigate)
                    }
                }
            }
        }
    }

    @Composable
    private fun WireTopAppBar(
        commonTopAppBarState: CommonTopAppBarState,
        modifier: Modifier = Modifier,
    ) {
        CommonTopAppBar(
            modifier = modifier,
            commonTopAppBarState = commonTopAppBarState,
            onReturnToCallClick = { establishedCall ->
                getOngoingCallIntent(
                    this@WireActivity,
                    establishedCall.conversationId.toString()
                ).run {
                    startActivity(this)
                }
            },
            onReturnToIncomingCallClick = {
                getIncomingCallIntent(
                    this@WireActivity,
                    it.conversationId.toString(),
                    null
                ).run {
                    startActivity(this)
                }
            },
            onReturnToOutgoingCallClick = {
                getOutgoingCallIntent(
                    this@WireActivity,
                    it.conversationId.toString()
                ).run {
                    startActivity(this)
                }
            }
        )
    }

    @Composable
    private fun HandleThemeChanges(themeOption: ThemeOption) {
        LaunchedEffect(themeOption) {
            val themeNightMode = when (themeOption) {
                ThemeOption.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeOption.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeOption.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
            val currentNightMode = AppCompatDelegate.getDefaultNightMode()
            if (themeNightMode != currentNightMode) {
                AppCompatDelegate.setDefaultNightMode(themeNightMode)
            }
        }
    }

    @Composable
    private fun SetUpNavigation(
        navigator: Navigator,
        onComplete: () -> Unit,
    ) {
        val currentKeyboardController by rememberUpdatedState(LocalSoftwareKeyboardController.current)
        val currentNavigator by rememberUpdatedState(navigator)
        LaunchedEffect(Unit) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    navigationCommands
                        .onSubscription {
                            isNavigationCollecting = true
                            onComplete()
                        }
                        .onCompletion {
                            isNavigationCollecting = false
                        }
                        .collectLatest {
                            currentKeyboardController?.hide()
                            currentNavigator.navigate(it)
                        }
                }
            }
        }

        DisposableEffect(navigator.navController) {
            val updateScreenSettingsListener =
                NavController.OnDestinationChangedListener { _, _, _ ->
                    currentKeyboardController?.hide()
                }
            navigator.navController.addOnDestinationChangedListener(updateScreenSettingsListener)
            navigator.navController.addOnDestinationChangedListener(currentScreenManager)

            onDispose {
                navigator.navController.removeOnDestinationChangedListener(
                    updateScreenSettingsListener
                )
                navigator.navController.removeOnDestinationChangedListener(currentScreenManager)
            }
        }

        DisposableEffect(switchAccountObserver, navigator) {
            NavigationSwitchAccountActions {
                lifecycleScope.launch(Dispatchers.Main) {
                    navigator.navigate(it)
                }
            }.let {
                switchAccountObserver.register(it)
                onDispose {
                    switchAccountObserver.unregister(it)
                }
            }
        }
    }

    @Composable
    private fun HandleScreenshotCensoring() {
        LaunchedEffect(viewModel.globalAppState.screenshotCensoringEnabled) {
            if (viewModel.globalAppState.screenshotCensoringEnabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    @Suppress("ComplexMethod")
    @Composable
    private fun HandleDialogs(navigate: (NavigationCommand) -> Unit) {
        val context = LocalContext.current
        val callFeedbackSheetState =
            rememberWireModalSheetState<Unit>(onDismissAction = {
                featureFlagNotificationViewModel.skipCallFeedback(false)
            })
        with(featureFlagNotificationViewModel.featureFlagState) {
            if (shouldShowTeamAppLockDialog) {
                TeamAppLockFeatureFlagDialog(
                    isTeamAppLockEnabled = isTeamAppLockEnabled,
                    onConfirm = {
                        featureFlagNotificationViewModel.dismissTeamAppLockDialog()
                        if (isTeamAppLockEnabled) {
                            val isUserAppLockSet =
                                featureFlagNotificationViewModel.isUserAppLockSet()
                            // No need to setup another app lock if the user already has one
                            if (!isUserAppLockSet) {
                                Intent(this@WireActivity, AppLockActivity::class.java)
                                    .apply {
                                        putExtra(AppLockActivity.SET_TEAM_APP_LOCK, true)
                                    }.also {
                                        startActivity(it)
                                    }
                            } else {
                                featureFlagNotificationViewModel.markTeamAppLockStatusAsNot()
                            }
                        } else {
                            with(featureFlagNotificationViewModel) {
                                markTeamAppLockStatusAsNot()
                                confirmAppLockNotEnforced()
                            }
                        }
                    }
                )
            } else {
                if (legalHoldRequestedViewModel.state is LegalHoldRequestedState.Visible) {
                    LegalHoldRequestedDialog(
                        state = legalHoldRequestedViewModel.state as LegalHoldRequestedState.Visible,
                        passwordTextState = legalHoldRequestedViewModel.passwordTextState,
                        notNowClicked = legalHoldRequestedViewModel::notNowClicked,
                        acceptClicked = legalHoldRequestedViewModel::acceptClicked,
                    )
                }
                if (legalHoldDeactivatedViewModel.state is LegalHoldDeactivatedState.Visible) {
                    LegalHoldDeactivatedDialog(
                        dialogDismissed = legalHoldDeactivatedViewModel::dismiss,
                    )
                }
                if (showFileSharingDialog) {
                    FileRestrictionDialog(
                        isFileSharingEnabled = (isFileSharingState !is FeatureFlagState.FileSharingState.DisabledByTeam),
                        hideDialogStatus = featureFlagNotificationViewModel::dismissFileSharingDialog
                    )
                }

                if (shouldShowGuestRoomLinkDialog) {
                    GuestRoomLinkFeatureFlagDialog(
                        isGuestRoomLinkEnabled = isGuestRoomLinkEnabled,
                        onDismiss = featureFlagNotificationViewModel::dismissGuestRoomLinkDialog
                    )
                }

                if (shouldShowSelfDeletingMessagesDialog) {
                    SelfDeletingMessagesDialog(
                        areSelfDeletingMessagesEnabled = areSelfDeletedMessagesEnabled,
                        enforcedTimeout = enforcedTimeoutDuration,
                        hideDialogStatus = featureFlagNotificationViewModel::dismissSelfDeletingMessagesDialog
                    )
                }
                val logoutOptionsDialogState = rememberVisibilityState<LogoutOptionsDialogState>()

                LogoutOptionsDialog(
                    dialogState = logoutOptionsDialogState,
                    checkboxEnabled = false,
                    logout = {
                        viewModel.doHardLogout(
                            { UserDataStore(context, it) },
                            NavigationSwitchAccountActions(navigate)
                        )
                        logoutOptionsDialogState.dismiss()
                    }
                )

                if (shouldShowE2eiCertificateRevokedDialog) {
                    E2EICertificateRevokedDialog(
                        onLogout = {
                            logoutOptionsDialogState.show(
                                LogoutOptionsDialogState(
                                    shouldWipeData = true
                                )
                            )
                        },
                        onContinue = featureFlagNotificationViewModel::dismissE2EICertificateRevokedDialog,
                    )
                }

                e2EIRequired?.let {
                    E2EIRequiredDialog(
                        e2EIRequired = e2EIRequired,
                        isE2EILoading = isE2EILoading,
                        getCertificate = featureFlagNotificationViewModel::enrollE2EICertificate,
                        snoozeDialog = featureFlagNotificationViewModel::snoozeE2EIdRequiredDialog
                    )
                }

                e2EISnoozeInfo?.let {
                    E2EISnoozeDialog(
                        timeLeft = e2EISnoozeInfo.timeLeft,
                        dismissDialog = featureFlagNotificationViewModel::dismissSnoozeE2EIdRequiredDialog
                    )
                }

                e2EIResult?.let {
                    E2EIResultDialog(
                        result = e2EIResult,
                        updateCertificate = featureFlagNotificationViewModel::enrollE2EICertificate,
                        snoozeDialog = featureFlagNotificationViewModel::snoozeE2EIdRequiredDialog,
                        openCertificateDetails = {
                            navigate(
                                NavigationCommand(
                                    E2eiCertificateDetailsScreenDestination(
                                        E2EICertificateDetails.DuringLoginCertificateDetails(it)
                                    )
                                )
                            )
                        },
                        dismissSuccessDialog = featureFlagNotificationViewModel::dismissSuccessE2EIdDialog,
                        isE2EILoading = isE2EILoading
                    )
                }

                UpdateAppDialog(viewModel.globalAppState.updateAppDialog, ::updateTheApp)
                JoinConversationDialog(
                    viewModel.globalAppState.conversationJoinedDialog,
                    navigate,
                    viewModel::onJoinConversationFlowCompleted
                )
                CustomBackendDialog(
                    viewModel.globalAppState,
                    viewModel::dismissCustomBackendDialog,
                    onConfirm = {
                        viewModel.customBackendDialogProceedButtonClicked {
                            navigate(NavigationCommand(WelcomeScreenDestination(WelcomeScreenNavArgs(isCustomBackend = true))))
                        }
                    },
                    onTryAgain = viewModel::onCustomServerConfig
                )
                MaxAccountDialog(
                    shouldShow = viewModel.globalAppState.maxAccountDialog,
                    onConfirm = {
                        viewModel.dismissMaxAccountDialog()
                        navigate(NavigationCommand(SelfUserProfileScreenDestination))
                    },
                    onDismiss = viewModel::dismissMaxAccountDialog
                )
                AccountLoggedOutDialog(
                    viewModel.globalAppState.blockUserUI
                ) { viewModel.tryToSwitchAccount(NavigationSwitchAccountActions(navigate)) }
                NewClientDialog(
                    viewModel.globalAppState.newClientDialog,
                    { navigate(NavigationCommand(SelfDevicesScreenDestination)) },
                    {
                        viewModel.switchAccount(
                            userId = it,
                            actions = NavigationSwitchAccountActions(navigate),
                            onComplete = { navigate(NavigationCommand(SelfDevicesScreenDestination)) })
                    },
                    viewModel::dismissNewClientsDialog
                )
            }
            if (showCallEndedBecauseOfConversationDegraded) {
                GuestCallWasEndedBecauseOfVerificationDegradedDialog(
                    featureFlagNotificationViewModel::dismissCallEndedBecauseOfConversationDegraded
                )
            }

            CallFeedbackDialog(
                sheetState = callFeedbackSheetState,
                onRated = featureFlagNotificationViewModel::rateCall,
                onSkipClicked = featureFlagNotificationViewModel::skipCallFeedback
            )

            if (startGettingE2EICertificate) {
                GetE2EICertificateUI(
                    enrollmentResultHandler = {
                        featureFlagNotificationViewModel.handleE2EIEnrollmentResult(it)
                    },
                    isNewClient = false
                )
            }
        }

        LaunchedEffect(Unit) {
            featureFlagNotificationViewModel.showCallFeedbackFlow.collectLatest {
                callFeedbackSheetState.show()
            }
        }
    }

    private fun updateTheApp() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.UPDATE_APP_URL))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            lockCodeTimeManager.get().observeAppLock()
                // Listen to one flow in a lifecycle-aware manner using flowWithLifecycle
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .first().let {
                    if (it) {
                        withContext(Dispatchers.Main) {
                            startActivity(
                                Intent(this@WireActivity, AppLockActivity::class.java)
                            )
                        }
                    }
                }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(HANDLED_DEEPLINK_FLAG, true)
        outState.putParcelable(ORIGINAL_SAVED_INTENT_FLAG, intent)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getOriginalIntent()?.let {
            this.intent = it
        }
    }

    private fun handleSynchronizeExternalData(intent: Intent) {
        if (!BuildConfig.DEBUG) {
            appLogger.e("Synchronizing external data is only allowed on debug builds")
            return
        }

        intent.data?.lastPathSegment.let { eventsPath ->
            openFileInput(eventsPath)?.let { inputStream ->
                viewModel.handleSynchronizeExternalData(inputStream)
            }
        }
    }

    @Suppress("ComplexCondition", "LongMethod")
    private fun handleDeepLink(
        intent: Intent?,
        savedInstanceState: Bundle? = null
    ) {
        val originalIntent = savedInstanceState.getOriginalIntent()
        if (intent == null
            || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
            || originalIntent == intent // This is the case when the activity is recreated and already handled
            || intent.getBooleanExtra(HANDLED_DEEPLINK_FLAG, false)
        ) {
            return
        } else {
            val navigate: (NavigationCommand) -> Unit =
                { lifecycleScope.launch { navigationCommands.emit(it) } }
            viewModel.handleDeepLink(
                intent = intent,
                onOpenConversation = {
                    if (it.switchedAccount) {
                        navigate(
                            NavigationCommand(
                                HomeScreenDestination,
                                BackStackMode.CLEAR_WHOLE
                            )
                        )
                    }
                    navigate(
                        NavigationCommand(
                            ConversationScreenDestination(it.conversationId),
                            BackStackMode.UPDATE_EXISTED
                        )
                    )
                },
                onIsSharingIntent = {
                    navigate(
                        NavigationCommand(
                            ImportMediaScreenDestination,
                            BackStackMode.UPDATE_EXISTED
                        )
                    )
                },
                onCannotLoginDuringACall = {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            resources.getString(R.string.cant_switch_account_in_call),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onAuthorizationNeeded = {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            resources.getString(R.string.deeplink_authorization_needed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onMigrationLogin = {
                    navigate(
                        NavigationCommand(
                            LoginScreenDestination(it.userHandle),
                            BackStackMode.UPDATE_EXISTED
                        )
                    )
                },
                onOpenOtherUserProfile = {
                    if (it.switchedAccount) {
                        navigate(
                            NavigationCommand(
                                HomeScreenDestination,
                                BackStackMode.CLEAR_WHOLE
                            )
                        )
                    }
                    navigate(
                        NavigationCommand(
                            OtherUserProfileScreenDestination(it.userId),
                            BackStackMode.UPDATE_EXISTED
                        )
                    )
                },
                onSSOLogin = {
                    navigate(
                        NavigationCommand(
                            LoginScreenDestination(ssoLoginResult = it),
                            BackStackMode.UPDATE_EXISTED
                        )
                    )
                }
            )
            intent.putExtra(HANDLED_DEEPLINK_FLAG, true)
        }
    }

    private fun Bundle?.getOriginalIntent(): Intent? {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION") // API 33
            this?.getParcelable(ORIGINAL_SAVED_INTENT_FLAG)
        } else {
            this?.getParcelable(ORIGINAL_SAVED_INTENT_FLAG, Intent::class.java)
        }
    }

    companion object {
        private const val HANDLED_DEEPLINK_FLAG = "deeplink_handled_flag_key"
        private const val ORIGINAL_SAVED_INTENT_FLAG = "original_saved_intent"
        private const val TAG = "WireActivity"
    }
}

val LocalActivity = staticCompositionLocalOf<Activity> {
    error("No Activity provided")
}
