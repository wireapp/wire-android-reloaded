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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.MainBackgroundComponent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogContent
import com.wire.android.ui.common.dialogs.FeatureDisabledWithProxyDialogState
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialog
import com.wire.android.ui.common.dialogs.MaxAccountsReachedDialogState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.StartLoginScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.delay

@RootNavGraph(start = true)
@WireDestination(
    style = PopUpNavigationAnimation::class,
    navArgsDelegate = WelcomeScreenNavArgs::class,
)
@Composable
fun WelcomeScreen(
    navigator: Navigator,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    WelcomeContent(
        viewModel.state,
        navigator::navigateBack,
        navigator::navigate
    )
}

@Composable
private fun WelcomeContent(
    state: WelcomeScreenState,
    navigateBack: () -> Unit,
    navigate: (NavigationCommand) -> Unit
) {
    MainBackgroundComponent()
    val enterpriseDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val createPersonalAccountDisabledWithProxyDialogState = rememberVisibilityState<FeatureDisabledWithProxyDialogState>()
    val context = LocalContext.current
    val maxAccountsReachedDialogState = rememberVisibilityState<MaxAccountsReachedDialogState>()
    MaxAccountsReachedDialog(dialogState = maxAccountsReachedDialogState) { navigateBack() }
    if (state.maxAccountsReached) {
        maxAccountsReachedDialogState.show(maxAccountsReachedDialogState.savedState ?: MaxAccountsReachedDialogState)
    }
    FeatureDisabledWithProxyDialogContent(
        dialogState = enterpriseDisabledWithProxyDialogState,
        onActionButtonClicked = {
            CustomTabsHelper.launchUrl(context, state.links.teams)
        }
    )
    FeatureDisabledWithProxyDialogContent(dialogState = createPersonalAccountDisabledWithProxyDialogState)

    LaunchedEffect(Unit) {
        if (state.maxAccountsReached.not()) {
            when (state.startLoginDestination) {
                StartLoginDestination.Default -> {
                    delay(1_000) // small delay to resolve
                    navigate(NavigationCommand(StartLoginScreenDestination))
                }

                StartLoginDestination.CustomBackend -> navigate(NavigationCommand(StartLoginScreenDestination())) // todo pass parameter from deeplink processor
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
@Preview(showSystemUi = true)
fun PreviewWelcomeScreen() {
    WireTheme {
        WelcomeContent(
            state = WelcomeScreenState(ServerConfig.DEFAULT),
            navigateBack = {},
            navigate = {})
    }
}
