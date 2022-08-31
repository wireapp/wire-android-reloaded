package com.wire.android.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.wire.android.R
import com.wire.android.navigation.NavigationGraph
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.navigateToItem
import com.wire.android.navigation.popWithArguments
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

                handleCustomBackendDialog(viewModel.customBackendDialogState.shouldShowDialog)
                maxAccountDialog(viewModel::openProfile, viewModel::dismissMaxAccountDialog, viewModel.maxAccountDialogState)
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
