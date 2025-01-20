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

package com.wire.android.ui.authentication.login.email

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.authentication.login.LoginErrorDialog
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.isProxyAuthRequired
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.DefaultPassword
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.textfield.clearAutofillTree
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LoginEmailScreen(
    onSuccess: (initialSyncCompleted: Boolean, isE2EIRequired: Boolean) -> Unit,
    onRemoveDeviceNeeded: () -> Unit,
    loginEmailViewModel: LoginEmailViewModel,
    scrollState: ScrollState = rememberScrollState()
) {
    val scope = rememberCoroutineScope()

    clearAutofillTree()

    LoginEmailContent(
        scrollState = scrollState,
        loginEmailState = loginEmailViewModel.loginState,
        userIdentifierTextState = loginEmailViewModel.userIdentifierTextState,
        proxyIdentifierState = loginEmailViewModel.proxyIdentifierTextState,
        proxyPasswordState = loginEmailViewModel.proxyPasswordTextState,
        passwordTextState = loginEmailViewModel.passwordTextState,
        isProxyAuthRequired = loginEmailViewModel.serverConfig.isProxyAuthRequired,
        apiProxyUrl = loginEmailViewModel.serverConfig.apiProxy?.host,
        onDialogDismiss = loginEmailViewModel::clearLoginErrors,
        onRemoveDeviceOpen = {
            loginEmailViewModel.clearLoginErrors()
            onRemoveDeviceNeeded()
        },
        onLoginButtonClick = loginEmailViewModel::login,
        onUpdateApp = loginEmailViewModel::updateTheApp,
        forgotPasswordUrl = loginEmailViewModel.serverConfig.forgotPassword,
        scope = scope
    )

    LaunchedEffect(loginEmailViewModel.loginState.flowState) {
        (loginEmailViewModel.loginState.flowState as? LoginState.Success)?.let {
            onSuccess(it.initialSyncCompleted, it.isE2EIRequired)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoginEmailContent(
    scrollState: ScrollState,
    userIdentifierTextState: TextFieldState,
    passwordTextState: TextFieldState,
    proxyIdentifierState: TextFieldState,
    proxyPasswordState: TextFieldState,
    loginEmailState: LoginEmailState,
    isProxyAuthRequired: Boolean,
    apiProxyUrl: String?,
    onDialogDismiss: () -> Unit,
    onRemoveDeviceOpen: () -> Unit,
    onLoginButtonClick: () -> Unit,
    onUpdateApp: () -> Unit,
    forgotPasswordUrl: String,
    scope: CoroutineScope
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
    ) {

        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = true)
                .verticalScroll(scrollState)
                .padding(MaterialTheme.wireDimensions.spacing16x)
                .semantics {
                    testTagsAsResourceId = true
                }
        ) {
            if (isProxyAuthRequired) {
                Text(
                    text = stringResource(R.string.label_wire_credentials),
                    style = MaterialTheme.wireTypography.title03.copy(
                        color = colorsScheme().secondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = MaterialTheme.wireDimensions.spacing16x
                        )
                )
            }
            UserIdentifierInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
                userIdentifierState = userIdentifierTextState,
                error = when (loginEmailState.flowState) {
                    is LoginState.Error.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_user_identifier)
                    else -> null
                },
                isEnabled = loginEmailState.userIdentifierEnabled,
            )
            PasswordInput(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
                passwordState = passwordTextState,
            )
            ForgotPasswordLabel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.wireDimensions.spacing16x),
                forgotPasswordUrl = forgotPasswordUrl
            )
            if (isProxyAuthRequired) {
                ProxyScreen(
                    proxyIdentifierState = proxyIdentifierState,
                    proxyPasswordState = proxyPasswordState,
                    proxyState = loginEmailState,
                    apiProxyUrl = apiProxyUrl,
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        Surface(
            color = MaterialTheme.wireColorScheme.surface,
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            }
        ) {
            Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                LoginButton(
                    modifier = Modifier.fillMaxWidth(),
                    loading = loginEmailState.flowState is LoginState.Loading,
                    enabled = loginEmailState.loginEnabled,
                ) {
                    scope.launch {
                        onLoginButtonClick()
                    }
                }
            }
        }
    }

    if (loginEmailState.flowState is LoginState.Error.DialogError) {
        LoginErrorDialog(loginEmailState.flowState, onDialogDismiss, onUpdateApp)
    } else if (loginEmailState.flowState is LoginState.Error.TooManyDevicesError) {
        onRemoveDeviceOpen()
    }
}

@Composable
private fun UserIdentifierInput(
    userIdentifierState: TextFieldState,
    error: String?,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    WireTextField(
        autoFillType = WireAutoFillType.Login,
        textState = userIdentifierState,
        placeholderText = stringResource(R.string.login_user_identifier_placeholder),
        labelText = stringResource(R.string.login_user_identifier_label),
        state = when {
            !isEnabled -> WireTextFieldState.Disabled
            error != null -> WireTextFieldState.Error(error)
            else -> WireTextFieldState.Default
        },
        semanticDescription = stringResource(R.string.content_description_login_email_field),
        keyboardOptions = KeyboardOptions.DefaultEmailNext,
        modifier = modifier.testTag("emailField"),
        testTag = "userIdentifierInput"
    )
}

@Composable
private fun PasswordInput(passwordState: TextFieldState, modifier: Modifier = Modifier) {
    val keyboardController = LocalSoftwareKeyboardController.current
    WirePasswordTextField(
        textState = passwordState,
        keyboardOptions = KeyboardOptions.DefaultPassword.copy(imeAction = ImeAction.Done),
        onKeyboardAction = { keyboardController?.hide() },
        semanticDescription = stringResource(R.string.content_description_login_password_field),
        modifier = modifier.testTag("passwordField"),
        autoFill = true,
        testTag = "PasswordInput"
    )
}

@Composable
private fun ForgotPasswordLabel(forgotPasswordUrl: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val context = LocalContext.current
        Text(
            text = stringResource(R.string.login_forgot_password),
            style = MaterialTheme.wireTypography.body02.copy(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colorScheme.primary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { openForgotPasswordPage(context, forgotPasswordUrl) },
                    onClickLabel = stringResource(R.string.content_description_open_link_label)
                )
                .testTag("Forgot password?")
        )
    }
}

private fun openForgotPasswordPage(context: Context, forgotPasswordUrl: String) {
    CustomTabsHelper.launchUrl(context, forgotPasswordUrl).also {
        appLogger.d(forgotPasswordUrl)
    }
}

@Composable
private fun LoginButton(
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        val text = if (loading) stringResource(R.string.label_logging_in) else stringResource(R.string.label_login)
        WirePrimaryButton(
            text = text,
            onClick = onClick,
            state = if (enabled) WireButtonState.Default else WireButtonState.Disabled,
            loading = loading,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("loginButton")
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewLoginEmailScreen() = WireTheme {
    LoginEmailContent(
        scrollState = rememberScrollState(),
        loginEmailState = LoginEmailState(),
        userIdentifierTextState = TextFieldState(),
        passwordTextState = TextFieldState(),
        proxyIdentifierState = TextFieldState(),
        proxyPasswordState = TextFieldState(),
        isProxyAuthRequired = true,
        apiProxyUrl = "",
        onDialogDismiss = { },
        onRemoveDeviceOpen = { },
        onLoginButtonClick = { },
        onUpdateApp = {},
        forgotPasswordUrl = "",
        scope = rememberCoroutineScope()
    )
}
