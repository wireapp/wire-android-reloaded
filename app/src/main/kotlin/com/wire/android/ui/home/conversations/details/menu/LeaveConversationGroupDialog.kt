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

package com.wire.android.ui.home.conversations.details.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.WireLabelledCheckbox
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversationslist.model.LeaveGroupDialogState

@Composable
internal fun LeaveConversationGroupDialog(
    dialogState: VisibilityState<LeaveGroupDialogState>,
    isLoading: Boolean,
    onLeaveGroup: (LeaveGroupDialogState) -> Unit,
) {
    VisibilityState(dialogState) { state ->
        WireDialog(
            title = stringResource(id = R.string.leave_group_conversation_dialog_title, state.conversationName),
            text = stringResource(id = R.string.leave_group_conversation_dialog_description),
            buttonsHorizontalAlignment = true,
            onDismiss = dialogState::dismiss,
            dismissButtonProperties = WireDialogButtonProperties(
                onClick = dialogState::dismiss,
                text = stringResource(id = R.string.label_cancel),
                state = WireButtonState.Default
            ),
            optionButton1Properties = WireDialogButtonProperties(
                onClick = { onLeaveGroup(state) },
                text = stringResource(id = R.string.label_leave),
                type = WireDialogButtonType.Primary,
                state =
                if (isLoading)
                    WireButtonState.Disabled
                else
                    WireButtonState.Error,
                loading = isLoading
            )
        ) {
            WireLabelledCheckbox(
                label = stringResource(R.string.leave_group_conversation_dialog_delete_fully_check),
                checked = state.shouldDelete,
                onCheckClicked = remember { { dialogState.show(state.copy(shouldDelete = it)) } },
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
