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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun MicrophoneButton(
    isMuted: Boolean,
    onMicrophoneButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireCallControlButton(
        isSelected = !isMuted,
        iconResId = when (isMuted) {
            true -> R.drawable.ic_microphone_off
            false -> R.drawable.ic_microphone_on
        },
        contentDescription = when (isMuted) {
            true -> R.string.content_description_calling_unmute_call
            false -> R.string.content_description_calling_mute_call
        },
        onClick = onMicrophoneButtonClicked,
        modifier = modifier
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewComposableMicrophoneButtonOn() = WireTheme {
    MicrophoneButton(isMuted = true, onMicrophoneButtonClicked = { })
}

@PreviewMultipleThemes
@Composable
fun PreviewComposableMicrophoneButtonOff() = WireTheme {
    MicrophoneButton(isMuted = false, onMicrophoneButtonClicked = { })
}
