/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.util.CustomTabsHelper
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun FileRestrictionDialog(
    isFileSharingEnabled: Boolean,
    hideDialogStatus: () -> Unit,
) {
    val text: String = stringResource(id = if (isFileSharingEnabled) R.string.sharing_files_enabled else R.string.sharing_files_disabled)

    WireDialog(
        title = stringResource(id = R.string.team_settings_changed),
        text = text,
        onDismiss = hideDialogStatus,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = hideDialogStatus,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun SelfDeletingMessagesDialog(
    isSelfDeletingMessagesEnabled: Boolean,
    enforcedTimeout: Int?,
    hideDialogStatus: () -> Unit,
) {
    val text: String = if (isSelfDeletingMessagesEnabled) {
        enforcedTimeout?.let { rawTimeout ->
            val formattedTimeout = formatEnforcedTimeout(rawTimeout)
            stringResource(id = R.string.self_deleting_messages_team_setting_enabled_enforced_timeout, formattedTimeout)
        } ?: stringResource(id = R.string.self_deleting_messages_team_setting_enabled)
    } else stringResource(id = R.string.self_deleting_messages_team_setting_disabled)

    WireDialog(
        title = stringResource(id = R.string.team_settings_changed),
        text = text,
        onDismiss = hideDialogStatus,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = hideDialogStatus,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
private fun formatEnforcedTimeout(rawTimeoutInSeconds: Int): String {
    val timeout10Seconds = 10.seconds
    val timeout5Minutes = 300.seconds
    val timeout1Hour = 3600.seconds
    val timeout1Day = 86400.seconds
    val timeout1Week = 604800.seconds
    val timeout4Weeks = 2419200.seconds

    return when (rawTimeoutInSeconds.toDuration(DurationUnit.SECONDS)) {
        timeout10Seconds -> stringResource(id = R.string.self_deleting_messages_team_setting_enabled_enforced_timeout_seconds)
        timeout5Minutes -> stringResource(id = R.string.self_deleting_messages_team_setting_enabled_enforced_timeout_minutes)
        timeout1Hour -> stringResource(id = R.string.self_deleting_messages_team_setting_enabled_enforced_timeout_one_hour)
        timeout1Day -> stringResource(id = R.string.self_deleting_messages_team_setting_enabled_enforced_timeout_one_day)
        timeout1Week -> stringResource(id = R.string.self_deleting_messages_team_setting_enabled_enforced_timeout_one_week)
        timeout4Weeks -> stringResource(id = R.string.self_deleting_messages_team_setting_enabled_enforced_timeout_four_weeks)
        else -> stringResource(id = R.string.self_deleting_messages_team_setting_enabled)
    }
}

@Composable
fun GuestRoomLinkFeatureFlagDialog(
    isGuestRoomLinkEnabled: Boolean,
    onDismiss: () -> Unit,
) {
    val text: String =
        stringResource(id = if (isGuestRoomLinkEnabled) R.string.guest_room_link_enabled else R.string.guest_room_link_disabled)

    WireDialog(
        title = stringResource(id = R.string.team_settings_changed),
        text = text,
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
fun WelcomeNewUserDialog(
    dismissDialog: () -> Unit,
    context: Context = LocalContext.current
) {
    val welcomeToNewAndroidUrl = stringResource(id = R.string.url_welcome_to_new_android)
    WireDialog(
        title = stringResource(id = R.string.welcome_migration_dialog_title),
        text = stringResource(id = R.string.welcome_migration_dialog_content),
        onDismiss = dismissDialog,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = {
                dismissDialog.invoke()
                CustomTabsHelper.launchUrl(context, welcomeToNewAndroidUrl)
            },
            text = stringResource(id = R.string.label_learn_more),
            type = WireDialogButtonType.Primary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = dismissDialog,
            text = stringResource(id = R.string.welcome_migration_dialog_continue),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Preview
@Composable
fun previewFileRestrictionDialog() {
    FileRestrictionDialog(true) {}
}

@Preview
@Composable
fun previewGuestRoomLinkFeatureFlagDialog() {
    GuestRoomLinkFeatureFlagDialog(true) {}
}

@Preview
@Composable
fun previewWelcomeNewUserDialog() {
    WelcomeNewUserDialog({})
}
