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

package com.wire.android.ui.common.bottomsheet.conversation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.conversationColor
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.model.getMutedStatusTextResource
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.user.ConnectionState

// items cannot be simplified
@Suppress("CyclomaticComplexMethod")
@Composable
internal fun ConversationMainSheetContent(
    conversationSheetContent: ConversationSheetContent,
    changeFavoriteState: (dialogState: GroupDialogState, addToFavorite: Boolean) -> Unit,
    // TODO(profile): enable when implemented
    // moveConversationToFolder: () -> Unit,
    updateConversationArchiveStatus: (DialogState) -> Unit,
    clearConversationContent: (DialogState) -> Unit,
    blockUserClick: (BlockUserDialogState) -> Unit,
    unblockUserClick: (UnblockUserDialogState) -> Unit,
    leaveGroup: (GroupDialogState) -> Unit,
    deleteGroup: (GroupDialogState) -> Unit,
    deleteGroupLocally: (GroupDialogState) -> Unit,
    navigateToNotification: () -> Unit
) {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = conversationSheetContent.title,
            leadingIcon = {
                if (conversationSheetContent.conversationTypeDetail is ConversationTypeDetail.Group) {
                    GroupConversationAvatar(
                        color = colorsScheme()
                            .conversationColor(id = conversationSheetContent.conversationTypeDetail.conversationId)
                    )
                } else if (conversationSheetContent.conversationTypeDetail is ConversationTypeDetail.Private) {
                    val connectionState: ConnectionState? = conversationSheetContent.conversationTypeDetail.blockingState.let {
                        if (it == BlockingState.BLOCKED) ConnectionState.BLOCKED else null
                    }
                    UserProfileAvatar(
                        avatarData = UserAvatarData(
                            asset = conversationSheetContent.conversationTypeDetail.avatarAsset,
                            connectionState = connectionState,
                            nameBasedAvatar = NameBasedAvatar(conversationSheetContent.title, accentColor = -1)
                        )
                    )
                }
            },
            customVerticalPadding = dimensions().spacing8x
        ),
        menuItems = buildList<@Composable () -> Unit> {
            if (conversationSheetContent.canEditNotifications() && !conversationSheetContent.isArchived) {
                add {
                    MenuBottomSheetItem(
                        title = stringResource(R.string.label_notifications),
                        leading = {
                            MenuItemIcon(
                                id = R.drawable.ic_mute,
                                contentDescription = null,
                            )
                        },
                        trailing = { NotificationsOptionsItemAction(conversationSheetContent.mutingConversationState) },
                        onItemClick = navigateToNotification,
                        onItemClickDescription = stringResource(id = R.string.content_description_open_notification_settings_label)
                    )
                }
            }

            if (conversationSheetContent.canAddToFavourite() && !conversationSheetContent.isArchived) {
                conversationSheetContent.isFavorite?.let { isFavorite ->
                    add {
                        MenuBottomSheetItem(
                            title = stringResource(
                                if (isFavorite) {
                                    R.string.label_remove_from_favourites
                                } else {
                                    R.string.label_add_to_favourites
                                }
                            ),
                            leading = {
                                MenuItemIcon(
                                    id = R.drawable.ic_favourite,
                                    contentDescription = null
                                )
                            },
                            onItemClick = {
                                changeFavoriteState(
                                    GroupDialogState(
                                        conversationSheetContent.conversationId,
                                        conversationSheetContent.title
                                    ),
                                    !isFavorite
                                )
                            }
                        )
                    }
                }
            }
// TODO(profile): enable when implemented
//            add {
//                MenuBottomSheetItem(
//                    icon = {
//                        MenuItemIcon(
//                            id = R.drawable.ic_folder,
//                            contentDescription = stringResource(R.string.content_description_move_to_folder),
//                        )
//                    },
//                    title = stringResource(R.string.label_move_to_folder),
//                    onItemClick = moveConversationToFolder
//                )
//            }
            add {
                MenuBottomSheetItem(
                    leading = {
                        MenuItemIcon(
                            id = R.drawable.ic_archive,
                            contentDescription = null,
                        )
                    },
                    title = stringResource(
                        if (!conversationSheetContent.isArchived) R.string.label_move_to_archive
                        else R.string.label_unarchive
                    ),
                    onItemClick = {
                        with(conversationSheetContent) {
                            updateConversationArchiveStatus(
                                DialogState(
                                    conversationId = conversationId,
                                    conversationName = title,
                                    conversationTypeDetail = conversationTypeDetail,
                                    isArchived = isArchived,
                                    isMember = conversationSheetContent.selfRole != null
                                )
                            )
                        }
                    }
                )
            }
            add {
                MenuBottomSheetItem(
                    leading = {
                        MenuItemIcon(
                            id = R.drawable.ic_erase,
                            contentDescription = null,
                        )
                    },
                    title = stringResource(R.string.label_clear_content),
                    onItemClick = {
                        clearConversationContent(
                            DialogState(
                                conversationId = conversationSheetContent.conversationId,
                                conversationName = conversationSheetContent.title,
                                conversationTypeDetail = conversationSheetContent.conversationTypeDetail,
                                isArchived = conversationSheetContent.isArchived,
                                isMember = conversationSheetContent.selfRole != null
                            )
                        )
                    }
                )
            }
            if (conversationSheetContent.canBlockUser()) {
                add {
                    MenuBottomSheetItem(
                        leading = {
                            MenuItemIcon(
                                id = R.drawable.ic_block,
                                contentDescription = null,
                            )
                        },
                        itemProvidedColor = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.label_block),
                        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
                        onItemClick = {
                            blockUserClick(
                                BlockUserDialogState(
                                    userName = conversationSheetContent.title,
                                    userId = (conversationSheetContent.conversationTypeDetail as ConversationTypeDetail.Private).userId
                                )
                            )
                        }
                    )
                }
            }
            if (conversationSheetContent.canUnblockUser()) {
                add {
                    MenuBottomSheetItem(
                        leading = {
                            MenuItemIcon(
                                id = R.drawable.ic_block,
                                contentDescription = null
                            )
                        },
                        itemProvidedColor = MaterialTheme.colorScheme.onBackground,
                        title = stringResource(R.string.label_unblock),
                        onItemClick = {
                            unblockUserClick(
                                UnblockUserDialogState(
                                    userName = conversationSheetContent.title,
                                    userId = (conversationSheetContent.conversationTypeDetail as ConversationTypeDetail.Private).userId
                                )
                            )
                        }
                    )
                }
            }
            if (conversationSheetContent.canLeaveTheGroup()) {
                add {
                    MenuBottomSheetItem(
                        leading = {
                            MenuItemIcon(
                                id = R.drawable.ic_leave,
                                contentDescription = null,
                            )
                        },
                        itemProvidedColor = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.label_leave_group),
                        onItemClick = {
                            leaveGroup(
                                GroupDialogState(
                                    conversationSheetContent.conversationId,
                                    conversationSheetContent.title
                                )
                            )
                        }
                    )
                }
            }
            if (conversationSheetContent.canDeleteGroupLocally()) {
                add {
                    MenuBottomSheetItem(
                        leading = {
                            MenuItemIcon(
                                id = R.drawable.ic_close,
                                contentDescription = null
                            )
                        },
                        title = stringResource(R.string.label_delete_group_locally),
                        itemProvidedColor = MaterialTheme.colorScheme.error,
                        onItemClick = {
                            deleteGroupLocally(
                                GroupDialogState(
                                    conversationSheetContent.conversationId,
                                    conversationSheetContent.title
                                )
                            )
                        }
                    )
                }
            }
            if (conversationSheetContent.canDeleteGroup()) {
                add {
                    MenuBottomSheetItem(
                        leading = {
                            MenuItemIcon(
                                id = R.drawable.ic_remove,
                                contentDescription = null,
                            )
                        },
                        title = stringResource(R.string.label_delete_group),
                        itemProvidedColor = MaterialTheme.colorScheme.error,
                        onItemClick = {
                            deleteGroup(
                                GroupDialogState(
                                    conversationSheetContent.conversationId,
                                    conversationSheetContent.title
                                )
                            )
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun NotificationsOptionsItemAction(
    mutedStatus: MutedConversationStatus,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = mutedStatus.getMutedStatusTextResource(),
            style = MaterialTheme.wireTypography.body01,
            color = MaterialTheme.wireColorScheme.secondaryText,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(weight = 1f, fill = false)
        )
        Spacer(modifier = Modifier.size(dimensions().spacing16x))
        ArrowRightIcon(contentDescription = R.string.content_description_empty)
    }
}
