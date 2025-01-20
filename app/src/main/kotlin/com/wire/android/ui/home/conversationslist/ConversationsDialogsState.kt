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
package com.wire.android.ui.home.conversationslist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.model.LeaveGroupDialogState

@Suppress("LongParameterList")
class ConversationsDialogsState(
    val leaveGroupDialogState: VisibilityState<LeaveGroupDialogState>,
    val deleteGroupDialogState: VisibilityState<GroupDialogState>,
    val deleteGroupLocallyDialogState: VisibilityState<GroupDialogState>,
    val blockUserDialogState: VisibilityState<BlockUserDialogState>,
    val unblockUserDialogState: VisibilityState<UnblockUserDialogState>,
    val clearContentDialogState: VisibilityState<DialogState>,
    val archiveConversationDialogState: VisibilityState<DialogState>,
    requestInProgress: Boolean
) {
    var requestInProgress: Boolean by mutableStateOf(requestInProgress)
}

@Composable
fun rememberConversationsDialogsState(requestInProgress: Boolean): ConversationsDialogsState {

    val leaveGroupDialogState = rememberVisibilityState<LeaveGroupDialogState>()
    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val deleteGroupLocallyDialogState = rememberVisibilityState<GroupDialogState>()
    val blockUserDialogState = rememberVisibilityState<BlockUserDialogState>()
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()
    val clearContentDialogState = rememberVisibilityState<DialogState>()
    val archiveConversationDialogState = rememberVisibilityState<DialogState>()

    val conversationsDialogsState = remember {
        ConversationsDialogsState(
            leaveGroupDialogState,
            deleteGroupDialogState,
            deleteGroupLocallyDialogState,
            blockUserDialogState,
            unblockUserDialogState,
            clearContentDialogState,
            archiveConversationDialogState,
            requestInProgress,
        )
    }

    LaunchedEffect(requestInProgress) {
        if (!requestInProgress) {
            leaveGroupDialogState.dismiss()
            deleteGroupDialogState.dismiss()
            blockUserDialogState.dismiss()
            unblockUserDialogState.dismiss()
            clearContentDialogState.dismiss()
            archiveConversationDialogState.dismiss()
        }

        conversationsDialogsState.requestInProgress = requestInProgress
    }

    return conversationsDialogsState
}
