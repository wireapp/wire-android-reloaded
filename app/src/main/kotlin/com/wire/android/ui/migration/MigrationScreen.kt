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

package com.wire.android.ui.migration

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.wire.android.R
import com.wire.android.migration.MigrationData
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.authentication.welcome.WelcomeScreenNavArgs
import com.wire.android.ui.common.SettingUpWireScreenContent
import com.wire.android.ui.common.SettingUpWireScreenType
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.stringWithStyledArgs

@RootNavGraph
@WireDestination(
    navArgsDelegate = MigrationNavArgs::class
)
@Composable
fun MigrationScreen(
    navigator: Navigator,
    viewModel: MigrationViewModel = hiltViewModel()
) {

    when (val state = viewModel.state) {
        is MigrationState.LoginRequired ->
            navigator.navigate(NavigationCommand(LoginScreenDestination(state.userHandle), BackStackMode.CLEAR_WHOLE))

        is MigrationState.Success -> navigator.navigate(
            NavigationCommand(
                if (state.currentSessionAvailable) HomeScreenDestination else WelcomeScreenDestination(WelcomeScreenNavArgs()),
                BackStackMode.CLEAR_WHOLE
            )
        )
        // other states are handled inside this composable, they need to be shown on the screen
        else -> MigrationScreenContent(viewModel.state, viewModel::retry, viewModel::finish, viewModel::accountLogin)
    }
}

@Composable
private fun MigrationScreenContent(
    state: MigrationState,
    retry: () -> Unit = {},
    finish: () -> Unit = {},
    accountLogin: (String) -> Unit = {}
) {
    SettingUpWireScreenContent(
        message = state.message(),
        title = state.title(),
        type = when (state) {
            is MigrationState.Failed.NoNetwork -> SettingUpWireScreenType.Failure(
                buttonTextResId = R.string.label_retry,
                onButtonClick = retry
            )

            is MigrationState.Failed.Account.Any -> SettingUpWireScreenType.Failure(
                buttonTextResId = R.string.label_continue,
                onButtonClick = { accountLogin(String.EMPTY) }
            )

            is MigrationState.Failed.Account.Specific -> SettingUpWireScreenType.Failure(
                buttonTextResId = R.string.label_continue,
                onButtonClick = { accountLogin(state.userHandle) }
            )

            is MigrationState.Failed.Messages -> SettingUpWireScreenType.Failure(
                buttonTextResId = R.string.label_continue,
                onButtonClick = finish
            )

            else -> SettingUpWireScreenType.Progress
        }
    )
}

@Composable
private fun MigrationState.message() = when (this) {
    is MigrationState.InProgress -> when (this.type) {
        MigrationData.Progress.Type.ACCOUNTS -> AnnotatedString(stringResource(R.string.migration_accounts_message))
        MigrationData.Progress.Type.MESSAGES -> AnnotatedString(stringResource(R.string.migration_messages_message))
        MigrationData.Progress.Type.UNKNOWN -> AnnotatedString(stringResource(R.string.migration_message_unknown))
    }

    is MigrationState.Failed.NoNetwork -> buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.wireColorScheme.error)) {
            append(stringResource(R.string.error_no_network_message))
        }
    }

    is MigrationState.Failed.Messages -> buildAnnotatedString {
        withStyle(SpanStyle(color = MaterialTheme.wireColorScheme.error)) {
            append(stringResource(R.string.migration_messages_failure, this@message.errorCode))
        }
    }

    is MigrationState.Failed.Account.Any -> AnnotatedString(stringResource(R.string.migration_login_required))
    is MigrationState.Failed.Account.Specific -> LocalContext.current.resources.stringWithStyledArgs(
        stringResId = R.string.migration_login_required_specific_account,
        normalStyle = MaterialTheme.wireTypography.body01,
        argsStyle = MaterialTheme.wireTypography.body02,
        normalColor = MaterialTheme.wireColorScheme.secondaryText,
        argsColor = MaterialTheme.wireColorScheme.secondaryText,
        stringResource(R.string.migration_login_required_specific_account_name, this.userName, this.userHandle)
    )

    is MigrationState.LoginRequired, is MigrationState.Success -> AnnotatedString("") // this should never happen
}

@Composable
private fun MigrationState.title() = when (this) {
    is MigrationState.InProgress -> {
        MigrationData.Progress.steps.indexOf(this.type).let { stepNumber ->
            if (stepNumber >= 0) stringResource(R.string.migration_title_step, stepNumber + 1, MigrationData.Progress.steps.size)
            else null
        }
    }
    else -> null
}

@Preview
@Composable
fun PreviewMigrationScreenInProgress() {
    MigrationScreenContent(state = MigrationState.InProgress(MigrationData.Progress.Type.ACCOUNTS))
}

@Preview
@Composable
fun PreviewMigrationScreenFailedNoNetwork() {
    MigrationScreenContent(state = MigrationState.Failed.NoNetwork)
}

@Preview
@Composable
fun PreviewMigrationScreenFailedAccountAny() {
    MigrationScreenContent(state = MigrationState.Failed.Account.Any)
}

@Preview
@Composable
fun PreviewMigrationScreenFailedAccountSpecific() {
    MigrationScreenContent(state = MigrationState.Failed.Account.Specific(userName = "name", userHandle = "@handle"))
}

@Preview
@Composable
fun PreviewMigrationScreenFailedMessages() {
    MigrationScreenContent(state = MigrationState.Failed.Messages(errorCode = "123"))
}
