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
package com.wire.android.ui.userprofile.teammigration.step3

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.popUpTo
import com.wire.android.R
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.destinations.TeamMigrationDoneStepScreenDestination
import com.wire.android.ui.destinations.TeamMigrationTeamPlanStepScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.teammigration.PersonalToTeamMigrationNavGraph
import com.wire.android.ui.userprofile.teammigration.TeamMigrationState
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel
import com.wire.android.ui.userprofile.teammigration.common.BottomLineButtons
import com.wire.android.ui.userprofile.teammigration.common.BulletList
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamFailure

const val TEAM_MIGRATION_CONFIRMATION_STEP = 3

@PersonalToTeamMigrationNavGraph
@WireDestination(
    style = SlideNavigationAnimation::class
)
@Composable
fun TeamMigrationConfirmationStepScreen(
    navigator: DestinationsNavigator,
    teamMigrationViewModel: TeamMigrationViewModel
) {
    val state = teamMigrationViewModel.teamMigrationState

    TeamMigrationConfirmationStepScreenContent(
        isMigrating = state.isMigrating,
        onContinueButtonClicked = {
            teamMigrationViewModel.setIsMigratingState(true)
            teamMigrationViewModel.migrateFromPersonalToTeamAccount(
                onSuccess = {
                    teamMigrationViewModel.setIsMigratingState(false)
                    navigator.navigate(TeamMigrationDoneStepScreenDestination)
                }
            )
        },
        onBackPressed = {
            navigator.popBackStack()
        }
    )

    HandleErrors(
        teamMigrationState = state,
        onFailureHandled = teamMigrationViewModel::failureHandled,
        goBackToFirstStep = {
            navigator.navigate(
                direction = TeamMigrationTeamPlanStepScreenDestination,
                builder = {
                    popUpTo(TeamMigrationTeamPlanStepScreenDestination) {
                        inclusive = false
                    }
                }
            )
        }
    )

    LaunchedEffect(Unit) {
        teamMigrationViewModel.sendPersonalTeamCreationFlowStartedEvent(TEAM_MIGRATION_CONFIRMATION_STEP)
        teamMigrationViewModel.setCurrentStep(TEAM_MIGRATION_CONFIRMATION_STEP)
    }
}

@Composable
private fun HandleErrors(
    teamMigrationState: TeamMigrationState,
    onFailureHandled: () -> Unit,
    goBackToFirstStep: () -> Unit,
) {
    val failure = teamMigrationState.migrationFailure ?: return

    when (failure) {
        is MigrateFromPersonalToTeamFailure.UserAlreadyInTeam -> {
            ErrorDialog(
                title = stringResource(R.string.personal_to_team_migration_error_title_already_in_team),
                message = stringResource(R.string.personal_to_team_migration_error_message_already_in_team),
                buttonText = stringResource(id = R.string.label_ok),
                onDismiss = {
                    onFailureHandled()
                }
            )
        }

        is MigrateFromPersonalToTeamFailure.NoNetwork -> {
            ErrorDialog(
                title = stringResource(R.string.personal_to_team_migration_error_title),
                message = stringResource(R.string.personal_to_team_migration_error_message_slow_network),
                buttonText = stringResource(id = R.string.label_try_again),
                onDismiss = {
                    onFailureHandled()
                    goBackToFirstStep()
                }
            )
        }

        else -> {
            ErrorDialog(
                title = stringResource(R.string.personal_to_team_migration_error_title),
                message = stringResource(R.string.personal_to_team_migration_error_message_unknown_error),
                buttonText = stringResource(id = R.string.label_try_again),
                onDismiss = {
                    onFailureHandled()
                    goBackToFirstStep()
                }
            )
        }
    }
}

@Composable
private fun ErrorDialog(
    title: String,
    message: String,
    buttonText: String,
    onDismiss: () -> Unit,
) {
    WireDialog(
        title = title,
        text = message,
        onDismiss = onDismiss,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = buttonText,
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
private fun TeamMigrationConfirmationStepScreenContent(
    modifier: Modifier = Modifier,
    isMigrating: Boolean = false,
    onContinueButtonClicked: () -> Unit = { },
    onBackPressed: () -> Unit = { }
) {

    val agreedToMigrationTerms = remember {
        mutableStateOf(false)
    }

    val acceptedWireTermsOfUse = remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x
                )
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier
                    .padding(top = dimensions().spacing24x)
                    .align(alignment = Alignment.CenterHorizontally),
                text = stringResource(R.string.personal_to_team_migration_step_label, 3),
                style = MaterialTheme.wireTypography.subline01,
                color = colorsScheme().secondaryText,
            )
            Text(
                modifier = Modifier
                    .padding(
                        top = dimensions().spacing8x,
                        bottom = dimensions().spacing56x
                    )
                    .align(alignment = Alignment.CenterHorizontally),
                text = stringResource(R.string.personal_to_team_migration_confirmation_step),
                style = MaterialTheme.wireTypography.title01,
                color = colorsScheme().onBackground
            )
            val messages = listOf(
                stringResource(R.string.personal_to_team_migration_confirmation_step_bullet_list_first_item),
                stringResource(R.string.personal_to_team_migration_confirmation_step_bullet_list_second_item),
                stringResource(R.string.personal_to_team_migration_confirmation_step_bullet_list_third_item),
            )
            BulletList(messages)

            Row(
                modifier = Modifier.padding(top = dimensions().spacing48x)
            ) {
                WireCheckbox(
                    checked = agreedToMigrationTerms.value,
                    onCheckedChange = { agreedToMigrationTerms.value = it }
                )

                Text(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    text = stringResource(R.string.personal_to_team_migration_confirmation_step_terms),
                    style = MaterialTheme.wireTypography.subline01,
                )
            }
            Row {
                WireCheckbox(
                    checked = acceptedWireTermsOfUse.value,
                    onCheckedChange = { acceptedWireTermsOfUse.value = it }
                )
                WireTermsOfUseWithLink()
            }
        }
        val isContinueButtonEnabled = agreedToMigrationTerms.value && acceptedWireTermsOfUse.value
        BottomLineButtons(
            isMigrating = isMigrating,
            isContinueButtonEnabled = isContinueButtonEnabled,
            onContinue = onContinueButtonClicked,
            backButtonContentDescription = stringResource(R.string.personal_to_team_migration_back_button_confirmation_content_description),
            onBack = onBackPressed
        )
    }
}

@Composable
private fun RowScope.WireTermsOfUseWithLink() {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {

        append(stringResource(R.string.personal_to_team_migration_confirmation_step_wire_terms_of_use_accept_label))
        append(" ")
        val urlTermsOfUse = stringResource(R.string.url_terms_of_use_legal)
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "terms",
                styles = TextLinkStyles(SpanStyle(color = colorsScheme().primary)),
                linkInteractionListener = {
                    CustomTabsHelper.launchUrl(context, urlTermsOfUse)
                },
            ),
        ) {
            append(stringResource(R.string.personal_to_team_migration_confirmation_step_wire_terms_of_use_label))
        }
    }

    Text(
        modifier = Modifier.align(Alignment.CenterVertically),
        text = annotatedString,
        style = MaterialTheme.wireTypography.subline01,
    )
}

@PreviewMultipleThemes
@Composable
private fun TeamMigrationConfirmationStepPreview() {
    WireTheme {
        TeamMigrationConfirmationStepScreenContent()
    }
}
