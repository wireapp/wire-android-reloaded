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

package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography

@Composable
fun MessageSendActions(
    sendButtonEnabled: Boolean,
    onSendButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Row(Modifier.padding(end = dimensions().spacing8x)) {
            SendButton(
                isEnabled = sendButtonEnabled,
                onSendButtonClicked = onSendButtonClicked
            )
        }
    }
}


@Composable
fun SelfDeletingActions(
    sendButtonEnabled: Boolean,
    onSendButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "test",
            style = typography().label02,
            color = colorsScheme().primary,
            modifier = Modifier
                .padding(horizontal = dimensions().spacing16x)
                .clickable(enabled = true) {
                    // Don't allow clicking the duration picker if the self-deleting duration is enforced from TM Settings
                    onChangeSelfDeletionClicked()
                }
        )
        WirePrimaryIconButton(
            onButtonClicked = onSendButtonClicked,
            iconResource = R.drawable.ic_timer,
            contentDescription = R.string.content_description_send_button,
            state = if (sendButtonEnabled) WireButtonState.Default else WireButtonState.Disabled,
            shape = RoundedCornerShape(dimensions().spacing20x),
            colors = wireSendPrimaryButtonColors(),
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = false),
            minHeight = dimensions().spacing40x,
            minWidth = dimensions().spacing40x
        )
    }
}

@Composable
private fun SendButton(
    isEnabled: Boolean,
    onSendButtonClicked: () -> Unit
) {
    WirePrimaryIconButton(
        onButtonClicked = onSendButtonClicked,
        iconResource = R.drawable.ic_send,
        contentDescription = R.string.content_description_send_button,
        state = if (isEnabled) WireButtonState.Default else WireButtonState.Disabled,
        shape = RoundedCornerShape(dimensions().spacing20x),
        colors = wireSendPrimaryButtonColors(),
        clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = false),
        minHeight = dimensions().spacing40x,
        minWidth = dimensions().spacing40x
    )
}

@Preview
@Composable
fun PreviewMessageSendActionsEnabled() {
    MessageSendActions(true, {})
}

@Preview
@Composable
fun PreviewMessageSendActionsDisabled() {
    MessageSendActions(false, {})
}
