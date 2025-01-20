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
package com.wire.android.ui.debug

data class DebugDataOptionsState(
    val isEventProcessingDisabled: Boolean = false,
    val keyPackagesCount: Int = 0,
    val mslClientId: String = "null",
    val mlsErrorMessage: String = "null",
    val isManualMigrationAllowed: Boolean = false,
    val debugId: String = "null",
    val commitish: String = "null",
    val certificate: String = "null",
    val showCertificate: Boolean = false,
    val startGettingE2EICertificate: Boolean = false,
    val analyticsTrackingId: String = "null",
    val isFederationEnabled: Boolean = false,
    val currentApiVersion: String = "null",
    val defaultProtocol: String = "null",
)
