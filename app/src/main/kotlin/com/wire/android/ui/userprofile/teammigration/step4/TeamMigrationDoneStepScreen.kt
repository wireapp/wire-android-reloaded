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
package com.wire.android.ui.userprofile.teammigration.step4

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace.x32
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.teammigration.PersonalToTeamMigrationNavGraph
import com.wire.android.ui.userprofile.teammigration.TeamMigrationViewModel
import com.wire.android.ui.userprofile.teammigration.common.BulletList
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes

const val TEAM_MIGRATION_DONE_STEP = 4

@PersonalToTeamMigrationNavGraph
@WireDestination(
    style = SlideNavigationAnimation::class
)
@Composable
fun TeamMigrationDoneStepScreen(
    navigator: Navigator,
    teamMigrationViewModel: TeamMigrationViewModel
) {
    val context = LocalContext.current

    TeamMigrationDoneStepContent(
        onBackToWireClicked = {
            teamMigrationViewModel.sendPersonalTeamCreationFlowCompletedEvent(
                backToWireButtonClicked = true
            )
            navigator.navigate(
                NavigationCommand(
                    HomeScreenDestination,
                    BackStackMode.CLEAR_WHOLE
                )
            )
        },
        onOpenTeamManagementClicked = {
            val teamManagementUrl = teamMigrationViewModel.teamMigrationState.teamUrl

            teamMigrationViewModel.sendPersonalTeamCreationFlowCompletedEvent(
                modalOpenTeamManagementButtonClicked = true
            )
            CustomTabsHelper.launchUrl(context, teamManagementUrl)
        },
        username = teamMigrationViewModel.teamMigrationState.username,
        teamName = teamMigrationViewModel.teamMigrationState.teamNameTextState.text.toString()
    )

    LaunchedEffect(Unit) {
        teamMigrationViewModel.setCurrentStep(TEAM_MIGRATION_DONE_STEP)
    }

    BackHandler { }
}

@Composable
private fun TeamMigrationDoneStepContent(
    onBackToWireClicked: () -> Unit,
    onOpenTeamManagementClicked: () -> Unit,
    username: String,
    teamName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                text = stringResource(R.string.personal_to_team_migration_step_label, 4),
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
                text = stringResource(R.string.personal_to_team_migration_done_step, username),
                style = MaterialTheme.wireTypography.title01,
                color = colorsScheme().onBackground
            )

            Text(
                text = stringResource(
                    R.string.personal_to_team_migration_done_step_now_team_member_label,
                    teamName
                ),
                style = MaterialTheme.wireTypography.body01,
                color = colorsScheme().onBackground
            )

            x32()

            Text(
                text = stringResource(R.string.personal_to_team_migration_done_step_go_to_team_label),
                style = MaterialTheme.wireTypography.body01,
                color = colorsScheme().onBackground
            )

            val messages = listOf(
                stringResource(R.string.personal_to_team_migration_done_step_bullet_list_first_item),
                stringResource(R.string.personal_to_team_migration_done_step_bullet_list_second_item)
            )
            BulletList(messages)
        }
        WireSecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x
                ),
            text = stringResource(R.string.personal_to_team_migration_back_to_wire_button),
            onClick = onBackToWireClicked
        )
        WirePrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = dimensions().spacing6x,
                    start = dimensions().spacing16x,
                    end = dimensions().spacing16x,
                    bottom = dimensions().spacing32x
                ),
            text = stringResource(R.string.to_team_management_action),
            onClick = onOpenTeamManagementClicked
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun TeamMigrationDoneStepScreenPreview() {
    WireTheme {
        TeamMigrationDoneStepContent({}, {}, username = "John", teamName = "teamName")
    }
}
