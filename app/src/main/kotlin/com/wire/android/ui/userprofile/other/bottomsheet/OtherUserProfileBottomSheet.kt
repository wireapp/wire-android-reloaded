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

package com.wire.android.ui.userprofile.other.bottomsheet

import androidx.compose.runtime.Composable
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.rememberConversationSheetState
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.userprofile.other.OtherUserProfileBottomSheetEventsHandler

@Composable
fun OtherUserProfileBottomSheetContent(
    bottomSheetState: OtherUserBottomSheetState,
    eventsHandler: OtherUserProfileBottomSheetEventsHandler,
    clearContent: (DialogState) -> Unit,
    archivingStatusState: (DialogState) -> Unit,
    blockUser: (BlockUserDialogState) -> Unit,
    unblockUser: (UnblockUserDialogState) -> Unit,
    changeFavoriteState: (GroupDialogState, addToFavorite: Boolean) -> Unit,
    closeBottomSheet: () -> Unit,
    getBottomSheetVisibility: () -> Boolean
) {
    when (val state = bottomSheetState.bottomSheetContentState) {
        is BottomSheetContent.Conversation -> {
            val conversationSheetState = rememberConversationSheetState(state.conversationData)
            ConversationSheetContent(
                isBottomSheetVisible = getBottomSheetVisibility,
                conversationSheetState = conversationSheetState,
                onMutingConversationStatusChange = {
                    val mutedConversationStatus = conversationSheetState.conversationSheetContent!!.mutingConversationState
                    eventsHandler.onMutingConversationStatusChange(
                        conversationSheetState.conversationId,
                        mutedConversationStatus
                    )
                },
                changeFavoriteState = changeFavoriteState,
                moveConversationToFolder = eventsHandler::onMoveConversationToFolder,
                updateConversationArchiveStatus = {
                    if (!it.isArchived) {
                        archivingStatusState(it)
                    } else {
                        eventsHandler.onMoveConversationToArchive(it)
                    }
                },
                clearConversationContent = clearContent,
                blockUser = blockUser,
                unblockUser = unblockUser,
                leaveGroup = { },
                deleteGroup = { },
                deleteGroupLocally = { }
            )
        }

        is BottomSheetContent.ChangeRole ->
            EditGroupRoleBottomSheet(
                groupState = state.groupState,
                changeMemberRole = eventsHandler::onChangeMemberRole,
                closeChangeRoleBottomSheet = closeBottomSheet
            )

        null -> {
            // we don't want to show empty BottomSheet
            closeBottomSheet()
        }
    }
}
