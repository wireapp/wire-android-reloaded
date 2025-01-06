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

import android.view.View
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.OngoingCallActivity
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions

private const val DAMPING_RATIO_MEDIUM_BOUNCY = 0.6f
private const val STIFFNESS_MEDIUM_LOW = 300f
private const val DEFAULT_OFFSETX_SELF_USER_TILE = -50f
private const val DEFAULT_OFFSETY_SELF_USER_TILE = 80F
private val SELF_VIDEO_TILE_HEIGHT_IN_PIP = 50.dp
private val SELF_VIDEO_TILE_WIDTH_IN_PIP = 30.dp

@Composable
fun FloatingSelfUserTile(
    contentHeight: Dp,
    contentWidth: Dp,
    participant: UICallParticipant,
    isOnFrontCamera: Boolean,
    onSelfUserVideoPreviewCreated: (view: View) -> Unit,
    onClearSelfUserVideoPreview: () -> Unit,
    flipCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selfVideoTileHeight by remember {
        mutableStateOf(contentHeight / 4)
    }
    var selfVideoTileWidth by remember {
        mutableStateOf(contentWidth / 4)
    }
    val activity = LocalContext.current

    val density = LocalDensity.current
    val contentHeightPx = density.run { (contentHeight).toPx() }
    val contentWidthPx = density.run { (contentWidth).toPx() }

    var isOnPiPMode by remember {
        mutableStateOf(false)
    }

    var selfUserTileOffsetX by remember {
        mutableStateOf(DEFAULT_OFFSETX_SELF_USER_TILE)
    }
    var selfUserTileOffsetY by remember {
        mutableStateOf(DEFAULT_OFFSETY_SELF_USER_TILE)
    }
    val selfUserTileOffset by animateOffsetAsState(
        targetValue = Offset(selfUserTileOffsetX, selfUserTileOffsetY),
        animationSpec = spring(
            DAMPING_RATIO_MEDIUM_BOUNCY,
            stiffness = STIFFNESS_MEDIUM_LOW
        ),
        label = "selfUserTileOffset"
    )

    DisposableEffect(activity) {
        val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
            if (info.isInPictureInPictureMode) {
                selfVideoTileHeight = SELF_VIDEO_TILE_HEIGHT_IN_PIP
                selfVideoTileWidth = SELF_VIDEO_TILE_WIDTH_IN_PIP
                selfUserTileOffsetX = -10f
                selfUserTileOffsetY = 10f
                isOnPiPMode = true
            } else {
                selfVideoTileHeight = contentHeight / 4
                selfVideoTileWidth = contentWidth / 4
                selfUserTileOffsetX = DEFAULT_OFFSETX_SELF_USER_TILE
                selfUserTileOffsetY = DEFAULT_OFFSETY_SELF_USER_TILE
                isOnPiPMode = false
            }
        }
        (activity as OngoingCallActivity).addOnPictureInPictureModeChangedListener(
            observer
        )
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }

    Card(
        border = BorderStroke(1.dp, colorsScheme().onSecondaryButtonDisabled),
        shape = RoundedCornerShape(dimensions().corner6x),
        modifier = modifier
            .height(selfVideoTileHeight)
            .width(selfVideoTileWidth)
            .offset { IntOffset(selfUserTileOffset.x.toInt(), selfUserTileOffset.y.toInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val tileWidthPx = density.run { (selfVideoTileWidth).toPx() }
                        val tileHeightPx = density.run { (selfVideoTileHeight).toPx() }
                        selfUserTileOffsetX =
                            if (selfUserTileOffsetX - (tileWidthPx / 2) > -(contentWidthPx / 2)) {
                                DEFAULT_OFFSETX_SELF_USER_TILE
                            } else {
                                -contentWidthPx + tileWidthPx - DEFAULT_OFFSETX_SELF_USER_TILE
                            }
                        selfUserTileOffsetY =
                            if (selfUserTileOffsetY + (tileHeightPx / 2) > (contentHeightPx / 2)) {
                                contentHeightPx - tileHeightPx - DEFAULT_OFFSETY_SELF_USER_TILE
                            } else {
                                DEFAULT_OFFSETY_SELF_USER_TILE
                            }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val newOffsetX = (selfUserTileOffsetX + dragAmount.x)
                        .coerceAtLeast(
                            -contentHeightPx - selfVideoTileWidth.toPx()
                        )
                        .coerceAtMost(-50f)

                    val newOffsetY = (selfUserTileOffsetY + dragAmount.y)
                        .coerceAtLeast(50f)
                        .coerceAtMost(
                            contentHeightPx - selfVideoTileHeight.toPx()
                        )

                    selfUserTileOffsetX = newOffsetX
                    selfUserTileOffsetY = newOffsetY
                }
            }
    ) {
        ParticipantTile(
            participantTitleState = participant,
            isOnPiPMode = isOnPiPMode,
            shouldFillSelfUserCameraPreview = true,
            isSelfUserMuted = participant.isMuted,
            isSelfUserCameraOn = participant.isCameraOn,
            onSelfUserVideoPreviewCreated = onSelfUserVideoPreviewCreated,
            onClearSelfUserVideoPreview = onClearSelfUserVideoPreview,
            isOnFrontCamera = isOnFrontCamera,
            flipCamera = flipCamera,
        )
    }
}
