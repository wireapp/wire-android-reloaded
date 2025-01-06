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

package com.wire.android.ui.calling.controlbuttons

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.wire.android.R
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun HangUpButton(
    onHangUpButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = dimensions().defaultCallingControlsHeight,
    iconSize: Dp = dimensions().bigCallingHangUpButtonIconSize,
) {
    WirePrimaryIconButton(
        iconResource = R.drawable.ic_call_reject,
        contentDescription = R.string.content_description_calling_hang_up_call,
        state = WireButtonState.Error,
        shape = CircleShape,
        minSize = DpSize(size, size),
        minClickableSize = DpSize(size, size),
        iconSize = iconSize,
        onButtonClicked = onHangUpButtonClicked,
        modifier = modifier,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewComposableHangUpButton() = WireTheme {
    HangUpButton(
        modifier = Modifier
            .width(MaterialTheme.wireDimensions.bigCallingControlsSize)
            .height(MaterialTheme.wireDimensions.bigCallingControlsSize),
        onHangUpButtonClicked = { }
    )
}
