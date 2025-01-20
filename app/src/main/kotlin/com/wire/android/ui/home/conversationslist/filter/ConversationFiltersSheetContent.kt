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
package com.wire.android.ui.home.conversationslist.filter

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.android.ui.common.bottomsheet.SelectableMenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.dimensions
import com.wire.kalium.logic.data.conversation.ConversationFilter

@Composable
fun ConversationFiltersSheetContent(
    sheetData: ConversationFilterSheetData,
    onChangeFilter: (ConversationFilter) -> Unit,
    showFoldersBottomSheet: (selectedFolderId: String?) -> Unit
) {
    WireMenuModalSheetContent(
        header = MenuModalSheetHeader.Visible(
            title = stringResource(R.string.label_filter_conversations),
            customVerticalPadding = dimensions().spacing8x
        ),
        menuItems = buildList<@Composable () -> Unit> {
            add {
                val state = if (ConversationFilter.All == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = ConversationFilter.All.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(ConversationFilter.All) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (ConversationFilter.Favorites == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = ConversationFilter.Favorites.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(ConversationFilter.Favorites) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (ConversationFilter.Groups == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = ConversationFilter.Groups.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(ConversationFilter.Groups) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (ConversationFilter.OneOnOne == sheetData.currentFilter) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = ConversationFilter.OneOnOne.toSheetItemLabel().asString(),
                    onItemClick = Clickable(
                        enabled = state == RichMenuItemState.DEFAULT,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { onChangeFilter(ConversationFilter.OneOnOne) },
                    ),
                    state = state
                )
            }
            add {
                val state = if (sheetData.currentFilter is ConversationFilter.Folder) {
                    RichMenuItemState.SELECTED
                } else {
                    RichMenuItemState.DEFAULT
                }
                SelectableMenuBottomSheetItem(
                    title = stringResource(R.string.label_filter_folders),
                    description = (sheetData.currentFilter as? ConversationFilter.Folder)?.folderName,
                    onItemClick = Clickable(
                        enabled = true,
                        onClickDescription = stringResource(id = R.string.content_description_select_label),
                        onClick = { showFoldersBottomSheet((sheetData.currentFilter as? ConversationFilter.Folder)?.folderId) },
                    ),
                    state = state
                )
            }
        }
    )
}
