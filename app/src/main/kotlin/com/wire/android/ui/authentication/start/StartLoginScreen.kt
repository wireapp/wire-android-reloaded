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

package com.wire.android.ui.authentication.start

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.config.LocalCustomUiConfigurationProvider
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.NewLoginContainer
import com.wire.android.ui.authentication.login.email.LoginEmailState
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultEmailNext
import com.wire.android.ui.common.textfield.WireAutoFillType
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.configuration.server.ServerConfig

@WireDestination(
    style = PopUpNavigationAnimation::class,
)
@Composable
fun StartLoginScreen(
    navigator: Navigator,
    viewModel: StartLoginViewModel = hiltViewModel()
) {
    WelcomeContent(
        viewModel.state.isThereActiveSession,
        viewModel.loginState,
        viewModel.state.links,
        viewModel.userIdentifierTextState,
        navigator::navigateBack,
        navigator::navigate
    )
}

@Composable
private fun WelcomeContent(
    isThereActiveSession: Boolean,
    loginEmailState: LoginEmailState,
    state: ServerConfig.Links,
    userIdentifierState: TextFieldState,
    navigateBack: () -> Unit,
    navigate: (NavigationCommand) -> Unit
) {
    NewLoginContainer(
        canNavigateBack = isThereActiveSession,
        onNavigateBack = navigateBack
    ) {
        NewWelcomeExperienceContent(
            loginEmailState = loginEmailState,
            links = state,
            userIdentifierState = userIdentifierState,
            navigateBack = navigateBack,
            navigate = navigate
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NewWelcomeExperienceContent(
    loginEmailState: LoginEmailState,
    links: ServerConfig.Links,
    userIdentifierState: TextFieldState,
    navigateBack: () -> Unit,
    navigate: (NavigationCommand) -> Unit,
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_wire_logo),
            tint = MaterialTheme.colorScheme.onBackground,
            contentDescription = stringResource(id = R.string.content_description_welcome_wire_logo),
            modifier = Modifier.size(dimensions().spacing120x)
        )

        Text(
            text = stringResource(id = R.string.enterprise_login_welcome),
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .padding(
                    vertical = MaterialTheme.wireDimensions.welcomeVerticalSpacing,
                    horizontal = MaterialTheme.wireDimensions.welcomeButtonHorizontalPadding
                )
                .semantics {
                    testTagsAsResourceId = true
                }
        ) {
            val error = when (loginEmailState.flowState) {
                is LoginState.Error.TextFieldError.InvalidValue -> stringResource(R.string.login_error_invalid_user_identifier)
                else -> null
            }
            EmailOrSSOCodeInput(userIdentifierState, error)
            VerticalSpace.x8()
            LoginNextButton(
                loading = loginEmailState.flowState is LoginState.Loading,
                enabled = loginEmailState.loginEnabled,
                onClick = { navigate(NavigationCommand(LoginScreenDestination(userHandle = userIdentifierState.text.toString()))) })
        }

        if (LocalCustomUiConfigurationProvider.current.isAccountCreationAllowed) {
            val termsUrl = stringResource(id = R.string.url_terms_of_use_legal)
            WelcomeFooter(
                modifier = Modifier.padding(horizontal = MaterialTheme.wireDimensions.welcomeTextHorizontalPadding),
                onTermsAndConditionClick = { CustomTabsHelper.launchUrl(context, termsUrl) }
            )
        }
    }
}

@Composable
private fun LoginNextButton(
    loading: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = modifier) {
        val text = if (loading) stringResource(R.string.label_logging_in) else stringResource(R.string.enterprise_login_next)
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

@Composable
private fun EmailOrSSOCodeInput(
    userIdentifierState: TextFieldState,
    error: String?
) {
    WireTextField(
        autoFillType = WireAutoFillType.Login,
        textState = userIdentifierState,
        placeholderText = stringResource(R.string.enterprise_login_user_identifier_label_placeholder),
        labelText = stringResource(R.string.enterprise_login_user_identifier_label),
        state = when {
            error != null -> WireTextFieldState.Error(error)
            else -> WireTextFieldState.Default
        },
        semanticDescription = stringResource(R.string.content_description_login_email_field),
        keyboardOptions = KeyboardOptions.DefaultEmailNext,
        modifier = Modifier.testTag("emailField"),
        testTag = "userIdentifierInput",
    )
}

@Composable
private fun WelcomeFooter(onTermsAndConditionClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.enterprise_login_title_terms_description),
            style = MaterialTheme.wireTypography.subline01,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.enterprise_login_title_terms_link),
            style = MaterialTheme.wireTypography.subline01.copy(textDecoration = TextDecoration.Underline),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTermsAndConditionClick,
                    onClickLabel = stringResource(R.string.enterprise_login_title_terms_link)
                )
        )

        Spacer(modifier = Modifier.height(MaterialTheme.wireDimensions.welcomeVerticalPadding))
    }
}

@PreviewMultipleThemes
@Composable
@Preview(showSystemUi = true)
fun PreviewWelcomeScreen() {
    WireTheme {
        WelcomeContent(
            isThereActiveSession = false,
            state = ServerConfig.DEFAULT,
            loginEmailState = LoginEmailState(),
            userIdentifierState = TextFieldState(),
            navigateBack = {},
            navigate = {}
        )
    }
}
