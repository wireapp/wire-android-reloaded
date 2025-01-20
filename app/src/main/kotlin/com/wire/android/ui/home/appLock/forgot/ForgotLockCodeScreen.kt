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
package com.wire.android.ui.home.appLock.forgot

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.authentication.welcome.WelcomeScreenNavArgs
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.dialogErrorStrings
import com.wire.android.util.ui.PreviewMultipleThemes

@RootNavGraph
@WireDestination
@Composable
fun ForgotLockCodeScreen(
    navigator: Navigator,
    viewModel: ForgotLockScreenViewModel = hiltViewModel(),
) {
    with(viewModel.state) {
        LaunchedEffect(completed) {
            if (completed) navigator.navigate(NavigationCommand(WelcomeScreenDestination(WelcomeScreenNavArgs()), BackStackMode.CLEAR_WHOLE))
        }
        ForgotLockCodeScreenContent(
            scrollState = rememberScrollState(),
            onResetDevice = viewModel::onResetDevice,
        )
        if (dialogState is ForgotLockCodeDialogState.Visible) {
            if (dialogState.loading) {
                ForgotLockCodeResettingDeviceDialog()
            } else {
                ForgotLockCodeResetDeviceDialog(
                    passwordTextState = viewModel.passwordTextState,
                    username = dialogState.username,
                    isPasswordRequired = dialogState.passwordRequired,
                    isPasswordValid = dialogState.passwordValid,
                    isResetDeviceEnabled = dialogState.resetDeviceEnabled,
                    onResetDeviceClicked = viewModel::onResetDeviceConfirmed,
                    onDialogDismissed = viewModel::onDialogDismissed,
                )
            }
        }
        if (error != null) {
            val (title, message) = error.dialogErrorStrings(LocalContext.current.resources)
            WireDialog(
                title = title,
                text = message,
                onDismiss = viewModel::onErrorDismissed,
                optionButton1Properties = WireDialogButtonProperties(
                    onClick = viewModel::onErrorDismissed,
                    text = stringResource(id = R.string.label_ok),
                    type = WireDialogButtonType.Primary,
                ),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgotLockCodeScreenContent(
    scrollState: ScrollState,
    onResetDevice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold { internalPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(weight = 1f, fill = true)
                    .verticalScroll(scrollState)
                    .padding(MaterialTheme.wireDimensions.spacing16x)
                    .semantics { testTagsAsResourceId = true }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = stringResource(id = R.string.content_description_welcome_wire_logo),
                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing56x)
                )
                Text(
                    text = stringResource(id = R.string.settings_forgot_lock_screen_title),
                    style = MaterialTheme.wireTypography.title02,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        top = MaterialTheme.wireDimensions.spacing32x,
                        bottom = MaterialTheme.wireDimensions.spacing16x
                    )
                )
                Text(
                    text = stringResource(id = R.string.settings_forgot_lock_screen_description),
                    style = MaterialTheme.wireTypography.body01,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        top = MaterialTheme.wireDimensions.spacing8x,
                        bottom = MaterialTheme.wireDimensions.spacing8x
                    )
                )
                Text(
                    text = stringResource(id = R.string.settings_forgot_lock_screen_warning),
                    style = MaterialTheme.wireTypography.body01,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(
                        top = MaterialTheme.wireDimensions.spacing8x,
                        bottom = MaterialTheme.wireDimensions.spacing8x
                    )
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            Surface(
                shadowElevation = scrollState.rememberBottomBarElevationState().value,
                color = MaterialTheme.wireColorScheme.background,
                modifier = Modifier.semantics { testTagsAsResourceId = true }
            ) {
                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                    ContinueButton(enabled = true, onContinue = onResetDevice)
                }
            }
        }
    }
}

@Composable
private fun ContinueButton(
    enabled: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        WirePrimaryButton(
            text = stringResource(R.string.settings_forgot_lock_screen_reset_device),
            onClick = onContinue,
            state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_device_button")
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewForgotLockCodeScreen() {
    WireTheme {
        ForgotLockCodeScreenContent(rememberScrollState(), {})
    }
}
