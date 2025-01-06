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

import androidx.compose.foundation.text.input.TextFieldState
import com.wire.kalium.logic.feature.user.migration.MigrateFromPersonalToTeamFailure

data class TeamMigrationState(
    val teamNameTextState: TextFieldState = TextFieldState(),
    val shouldShowMigrationLeaveDialog: Boolean = false,
    val isMigrating: Boolean = false,
    val currentStep: Int = 0,
    val username: String = "",
    val teamUrl: String = "",
    val migrationFailure: MigrateFromPersonalToTeamFailure? = null
)
