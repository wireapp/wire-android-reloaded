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
package com.wire.android.ui.calling.ongoing.participantsview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun FlipCameraButton(
    isOnFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    flipCamera: () -> Unit,
) {
    Icon(
        modifier = modifier
            .padding(dimensions().spacing12x)
            .size(32.dp)
            .background(color = colorsScheme().surface, shape = CircleShape)
            .clip(CircleShape)
            .clickable { flipCamera() }
            .padding(dimensions().spacing6x),
        painter = painterResource(R.drawable.ic_flip_camera),
        tint = colorsScheme().onSurface,
        contentDescription = if (isOnFrontCamera) {
            stringResource(R.string.content_description_calling_flip_camera_on)
        } else {
            stringResource(R.string.content_description_calling_flip_camera_on)
        }
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewFlipCameraButton() {
    WireTheme { FlipCameraButton(isOnFrontCamera = true) { } }
}
