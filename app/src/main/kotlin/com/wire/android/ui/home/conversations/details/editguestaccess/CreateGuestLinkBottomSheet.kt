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
package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState

@Composable
fun CreateGuestLinkBottomSheet(
    sheetState: WireModalSheetState<Unit>,
    onItemClick: (passwordProtected: Boolean) -> Unit,
    isPasswordInviteLinksAllowed: Boolean,
) {
    WireModalSheetLayout(sheetState = sheetState) {
        WireMenuModalSheetContent(
            header = MenuModalSheetHeader.Visible(title = stringResource(R.string.create_guest_link)),
            menuItems = buildList {
                if (isPasswordInviteLinksAllowed) {
                    add {
                        CreateInviteLinkSheetItem(
                            title = stringResource(R.string.create_guest_link_with_password),
                            onClicked = { onItemClick(true) },
                            enabled = isPasswordInviteLinksAllowed
                        )
                    }
                }
                add {
                    CreateInviteLinkSheetItem(
                        title = stringResource(R.string.create_guest_link_without_password_title),
                        onClicked = { onItemClick(false) },
                        enabled = true
                    )
                }
            }
        )
    }
}

@Composable
private fun CreateInviteLinkSheetItem(
    title: String,
    onClicked: () -> Unit,
    enabled: Boolean = true,
) {
    MenuBottomSheetItem(
        title = title,
        onItemClick = onClicked,
        trailing = { ArrowRightIcon(contentDescription = R.string.content_description_empty) },
        enabled = enabled
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PreviewCreateGuestLinkBottomSheet() {
    CreateGuestLinkBottomSheet(
        sheetState = rememberWireModalSheetState(),
        onItemClick = {},
        isPasswordInviteLinksAllowed = true,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PreviewCreateGuestLinkBottomSheetDisabled() {
    CreateGuestLinkBottomSheet(
        sheetState = rememberWireModalSheetState(),
        onItemClick = {},
        isPasswordInviteLinksAllowed = false,
    )
}
