package com.wire.android.ui.home.conversations.details.menu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.bottomsheet.MenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.RichMenuBottomSheetItem
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar

@Composable
fun ConversationGroupDetailsBottomSheet(
    conversationOptionsState: GroupConversationOptionsState,
    onDeleteGroup: () -> Unit,
    onLeaveGroup: () -> Unit,
    closeBottomSheet: () -> Unit
) {
    MenuModalSheetContent(
        headerTitle = conversationOptionsState.groupName,
        headerIcon = {
            GroupConversationAvatar(
                color = colorsScheme().conversationColor(
                    id =
                    conversationOptionsState.conversationId
                )
            )
        },
        menuItems = listOf {
            LeaveGroupItem(
                onLeaveGroup = onLeaveGroup,
                closeBottomSheet = closeBottomSheet
            )
            if (conversationOptionsState.isAbleToRemoveGroup)
                DeleteGroupItem(
                    onDeleteGroup = onDeleteGroup,
                    closeBottomSheet = closeBottomSheet
                )
        },
    )
}

@Composable
private fun LeaveGroupItem(
    onLeaveGroup: () -> Unit,
    closeBottomSheet: () -> Unit
) {
    RichMenuBottomSheetItem(
        title = stringResource(id = R.string.leave_group_conversation_menu_item),
        titleColor = MaterialTheme.colorScheme.error,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_leave),
                contentDescription = stringResource(R.string.content_description_leave_the_group),
                modifier = Modifier.padding(dimensions().spacing8x),
                tint = MaterialTheme.colorScheme.error
            )
        },
        onItemClick = Clickable {
            onLeaveGroup()
            closeBottomSheet()
        }
    )
}

@Composable
private fun DeleteGroupItem(
    onDeleteGroup: () -> Unit,
    closeBottomSheet: () -> Unit
) {
    RichMenuBottomSheetItem(
        title = stringResource(id = R.string.delete_group_conversation_menu_item),
        titleColor = MaterialTheme.colorScheme.error,
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_remove),
                contentDescription = stringResource(R.string.content_description_delete_the_group),
                modifier = Modifier.padding(dimensions().spacing8x),
                tint = MaterialTheme.colorScheme.error
            )
        },
        onItemClick = Clickable {
            onDeleteGroup()
            closeBottomSheet()
        }
    )
}
