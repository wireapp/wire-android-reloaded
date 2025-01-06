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
package com.wire.android.ui.userprofile.teammigration

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamFailure
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamResult
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class TeamMigrationViewModelTest {

    @Test
    fun `given dialog state, when showMigrationLeaveDialog is called, then update shouldShowMigrationLeaveDialog to true`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .arrange()

            viewModel.showMigrationLeaveDialog()

            assertEquals(true, viewModel.teamMigrationState.shouldShowMigrationLeaveDialog)
        }

    @Test
    fun `given dialog state, when hideMigrationLeaveDialog is called, then update shouldShowMigrationLeaveDialog to false`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .arrange()

            viewModel.hideMigrationLeaveDialog()

            assertEquals(false, viewModel.teamMigrationState.shouldShowMigrationLeaveDialog)
        }

    @Test
    fun `given close modal event, when sendPersonalToTeamMigrationDismissed is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalToTeamMigrationDismissed()

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.ClickedPersonalTeamMigrationCta(
                        dismissCreateTeamButtonClicked = true
                    )
                )
            }
        }

    @Test
    fun `given the step of migration flow, when sendPersonalTeamCreationFlowStartedEvent is called, then send the event`() =
        runTest {
            val step = 2
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowStartedEvent(2)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowStarted(step)
                )
            }
        }

    @Test
    fun `given modalLeaveClicked event, when sendPersonalTeamCreationFlowCanceledEvent is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowCanceledEvent(modalLeaveClicked = true)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCanceled(
                        teamName = viewModel.teamMigrationState.teamNameTextState.text.toString(),
                        modalLeaveClicked = true
                    )
                )
            }
        }

    @Test
    fun `given modalContinueClicked event, when sendPersonalTeamCreationFlowCanceledEvent is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowCanceledEvent(modalContinueClicked = true)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCanceled(
                        teamName = viewModel.teamMigrationState.teamNameTextState.text.toString(),
                        modalContinueClicked = true
                    )
                )
            }
        }

    @Test
    fun `given modalOpenTeamManagementButtonClicked event, when sendPersonalTeamCreationFlowCompletedEvent is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowCompletedEvent(
                modalOpenTeamManagementButtonClicked = true
            )

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCompleted(
                        teamName = viewModel.teamMigrationState.teamNameTextState.text.toString(),
                        modalOpenTeamManagementButtonClicked = true
                    )
                )
            }
        }

    @Test
    fun `given backToWireButtonClicked event, when sendPersonalTeamCreationFlowCompletedEvent is called, then send the event`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .arrange()

            viewModel.sendPersonalTeamCreationFlowCompletedEvent(backToWireButtonClicked = true)

            verify(exactly = 1) {
                arrangement.anonymousAnalyticsManager.sendEvent(
                    AnalyticsEvent.PersonalTeamMigration.PersonalTeamCreationFlowCompleted(
                        teamName = viewModel.teamMigrationState.teamNameTextState.text.toString(),
                        backToWireButtonClicked = true
                    )
                )
            }
        }

    @Test
    fun `given team name, when migrateFromPersonalToTeamAccount return success, then call use case and onSuccess`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withMigrateFromPersonalToTeamSuccess()
                .arrange()

            val onSuccess = mockk<() -> Unit>(relaxed = true)

            viewModel.migrateFromPersonalToTeamAccount(onSuccess)

            coVerify(exactly = 1) {
                arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
            }
            verify(exactly = 1) { onSuccess() }
        }

    @Test
    fun `given team name, when migrateFromPersonalToTeamAccount return unknown failure, then call use case and handle the failure`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withMigrateFromPersonalToTeamErrorUnknown()
                .arrange()

            val onSuccess = {}

            viewModel.migrateFromPersonalToTeamAccount(onSuccess)

            coVerify(exactly = 1) {
                arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
            }
            Assertions.assertNotNull(viewModel.teamMigrationState.migrationFailure)
            viewModel.failureHandled()
            Assertions.assertNull(viewModel.teamMigrationState.migrationFailure)
        }

    @Test
    fun `given team name, when migrateFromPersonalToTeamAccount return user already in team failure, then call use case and handle the failure`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withMigrateFromPersonalToTeamErrorAlreadyInTeam()
                .arrange()

            val onSuccess = {}

            viewModel.migrateFromPersonalToTeamAccount(onSuccess)

            coVerify(exactly = 1) {
                arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
            }
            Assertions.assertNotNull(viewModel.teamMigrationState.migrationFailure)
            viewModel.failureHandled()
            Assertions.assertNull(viewModel.teamMigrationState.migrationFailure)
        }

    @Test
    fun `given team name, when migrateFromPersonalToTeamAccount return no network failure, then call use case and handle the failure`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withMigrateFromPersonalToTeamErrorNoNetwork()
                .arrange()

            val onSuccess = {}

            viewModel.migrateFromPersonalToTeamAccount(onSuccess)

            coVerify(exactly = 1) {
                arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
            }
            Assertions.assertNotNull(viewModel.teamMigrationState.migrationFailure)
            viewModel.failureHandled()
            Assertions.assertNull(viewModel.teamMigrationState.migrationFailure)
        }

    @Test
    fun `given team name with spaces at start or end, when invoking migration, then trim the name`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withMigrateFromPersonalToTeamSuccess()
            .arrange()
        val onSuccess = {}
        viewModel.teamMigrationState.teamNameTextState.setTextAndPlaceCursorAtEnd(" ${Arrangement.TEAM_NAME} ")
        // when
        viewModel.migrateFromPersonalToTeamAccount(onSuccess)
        // then
        coVerify(exactly = 1) {
            arrangement.migrateFromPersonalToTeam(Arrangement.TEAM_NAME)
        }
    }

    private class Arrangement {

        @MockK
        lateinit var anonymousAnalyticsManager: AnonymousAnalyticsManager

        @MockK
        lateinit var migrateFromPersonalToTeam: MigrateFromPersonalToTeamUseCase

        @MockK
        lateinit var getSelfUser: GetSelfUserUseCase

        @MockK
        lateinit var getTeamUrl: GetTeamUrlUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { getSelfUser() } returns flowOf()
            coEvery { getTeamUrl() } returns "TeamUrl"
        }

        fun arrange() = this to TeamMigrationViewModel(
            anonymousAnalyticsManager = anonymousAnalyticsManager,
            migrateFromPersonalToTeam = migrateFromPersonalToTeam,
            getSelfUser = getSelfUser,
            getTeamUrl = getTeamUrl
        ).also { viewModel ->
            viewModel.teamMigrationState.teamNameTextState.setTextAndPlaceCursorAtEnd(TEAM_NAME)
        }

        fun withMigrateFromPersonalToTeamSuccess() = apply {
            coEvery { migrateFromPersonalToTeam(any()) } returns MigrateFromPersonalToTeamResult.Success
        }

        fun withMigrateFromPersonalToTeamErrorUnknown() = apply {
            coEvery { migrateFromPersonalToTeam(any()) } returns MigrateFromPersonalToTeamResult.Error(
                MigrateFromPersonalToTeamFailure.UnknownError(NetworkFailure.ProxyError(null))
            )
        }

        fun withMigrateFromPersonalToTeamErrorAlreadyInTeam() = apply {
            coEvery { migrateFromPersonalToTeam(any()) } returns MigrateFromPersonalToTeamResult.Error(
                MigrateFromPersonalToTeamFailure.UserAlreadyInTeam()
            )
        }

        fun withMigrateFromPersonalToTeamErrorNoNetwork() = apply {
            coEvery { migrateFromPersonalToTeam(any()) } returns MigrateFromPersonalToTeamResult.Error(
                MigrateFromPersonalToTeamFailure.NoNetwork
            )
        }

        companion object {
            const val TEAM_NAME = "teamName"
        }
    }
}
