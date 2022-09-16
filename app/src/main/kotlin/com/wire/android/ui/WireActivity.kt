package com.wire.android.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.navigateToItem
import com.wire.android.navigation.popWithArguments
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.dialogs.CustomBEDeeplinkDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.userprofile.self.MaxAccountReachedDialog
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.ui.updateScreenSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalCoroutinesApi::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@AndroidEntryPoint
class WireActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    val viewModel: WireActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(currentScreenManager)
        viewModel.handleDeepLink(intent)
        setComposableContent()
        askNotificationPermission()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (viewModel.handleDeepLinkOnNewIntent(intent)) {
            recreate()
        }
        super.onNewIntent(intent)
    }

    private fun setComposableContent() {
        setContent {
            WireTheme {
                val scope = rememberCoroutineScope()
                val navController = rememberAnimatedNavController()
                val startDestination = viewModel.startNavigationRoute()
                Scaffold {
                    NavigationGraph(navController = navController, startDestination, viewModel.navigationArguments())
                }
                setUpNavigation(navController, scope)

                handleCustomBackendDialog(viewModel.globalAppState.customBackendDialog.shouldShowDialog)
                maxAccountDialog(
                    viewModel::openProfile,
                    viewModel::dismissMaxAccountDialog,
                    viewModel.globalAppState.maxAccountDialog
                )
                AccountLongedOutDialog(viewModel.globalAppState.blockUserUI, viewModel::navigateToNextAccountOrWelcome)
            }
        }
    }

    @Composable
    private fun handleCustomBackendDialog(shouldShow: Boolean) {
        if (shouldShow) {
            CustomBEDeeplinkDialog(viewModel)
        }
    }

    @Composable
    private fun maxAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit, shouldShow: Boolean) {
        if (shouldShow) {
            MaxAccountReachedDialog(
                onConfirm = onConfirm,
                onDismiss = onDismiss,
                buttonText = R.string.max_account_reached_dialog_button_open_profile
            )
        }
    }

    @Composable
    fun AccountLongedOutDialog(reason: CurrentSessionErrorState?, navigateAway: () -> Unit) {
        appLogger.e("AccountLongedOutDialog: $reason")
        reason?.let {
            val (@StringRes title: Int, @StringRes text: Int) = when (reason) {
                CurrentSessionErrorState.SessionExpired -> {
                    R.string.session_expired_error_title to R.string.session_expired_error_message
                }
                CurrentSessionErrorState.RemovedClient -> {
                    R.string.removed_client_error_title to R.string.removed_client_error_message
                }
                CurrentSessionErrorState.DeletedAccount -> {
                    R.string.deleted_user_error_title to R.string.deleted_user_error_message
                }
            }

            WireDialog(
                title = stringResource(id = title),
                text = stringResource(id = text),
                onDismiss = remember { { } },
                optionButton1Properties = WireDialogButtonProperties(
                    text = stringResource(R.string.label_ok),
                    onClick = navigateAway,
                    type = WireDialogButtonType.Primary
                )
            )
        }
    }

    @Composable
    private fun setUpNavigation(
        navController: NavHostController,
        scope: CoroutineScope
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        // with the static key here we're sure that this effect wouldn't be canceled or restarted
        LaunchedEffect("key") {
            navigationManager.navigateState
                .onEach { command ->
                    if (command == null) return@onEach
                    keyboardController?.hide()
                    navController.navigateToItem(command)
                }
                .launchIn(scope)

            navigationManager.navigateBack
                .onEach {
                    if (!navController.popWithArguments(it)) finish()
                }
                .launchIn(scope)

            navController.addOnDestinationChangedListener { controller, _, _ ->
                keyboardController?.hide()
                updateScreenSettings(controller)
            }

            navController.addOnDestinationChangedListener(currentScreenManager)
        }
    }
}
