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
@file:Suppress("TooManyFunctions")

package com.wire.android.ui.calling.ongoing.participantsview

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.atMost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.waz.avs.CameraPreviewBuilder
import com.waz.avs.VideoRenderer
import com.wire.android.R
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.incallreactions.InCallReactions
import com.wire.android.ui.common.avatar.UserProfileAvatar
import com.wire.android.ui.common.avatar.UserProfileAvatarType
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.darkColorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.QualifiedID

@Composable
fun ParticipantTile(
    participantTitleState: UICallParticipant,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    onSelfUserVideoPreviewCreated: (view: View) -> Unit,
    isOnFrontCamera: Boolean,
    flipCamera: () -> Unit,
    modifier: Modifier = Modifier,
    isOnPiPMode: Boolean = false,
    shouldFillSelfUserCameraPreview: Boolean = false,
    shouldFillOthersVideoPreview: Boolean = true,
    isZoomingEnabled: Boolean = false,
    recentReaction: String? = null,
    onClearSelfUserVideoPreview: () -> Unit,
) {
    val alpha =
        if (participantTitleState.hasEstablishedAudio) ContentAlpha.high else ContentAlpha.medium
    Surface(
        modifier = modifier
            .thenIf(participantTitleState.isSpeaking, activeSpeakerBorderModifier),
        color = darkColorsScheme().surfaceContainer,
        shape = RoundedCornerShape(if (participantTitleState.isSpeaking) dimensions().corner8x else dimensions().corner3x),
    ) {

        ConstraintLayout {
            val (avatar, bottomRow, cameraButton) = createRefs()
            val maxAvatarSize = dimensions().onGoingCallUserAvatarSize
            val activeSpeakerBorderPadding = dimensions().spacing6x

            AvatarTile(
                modifier = Modifier
                    .alpha(alpha)
                    .padding(top = activeSpeakerBorderPadding)
                    .constrainAs(avatar) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        if (isOnPiPMode) {
                            bottom.linkTo(parent.bottom)
                        } else {
                            bottom.linkTo(bottomRow.top)
                        }
                        width = Dimension.fillToConstraints.atMost(maxAvatarSize)
                        height =
                            Dimension.fillToConstraints.atMost(maxAvatarSize + activeSpeakerBorderPadding)
                    },
                avatar = UserAvatarData(
                    asset = participantTitleState.avatar,
                    nameBasedAvatar = NameBasedAvatar(participantTitleState.name, participantTitleState.accentId)
                ),
                isOnPiPMode = isOnPiPMode
            )

            if (participantTitleState.isSelfUser) {
                CameraPreview(
                    isCameraOn = isSelfUserCameraOn,
                    shouldFill = shouldFillSelfUserCameraPreview,
                    onSelfUserVideoPreviewCreated = onSelfUserVideoPreviewCreated,
                    onClearSelfUserVideoPreview = onClearSelfUserVideoPreview
                )
            } else {
                OthersVideoRenderer(
                    participantId = participantTitleState.id.toString(),
                    clientId = participantTitleState.clientId,
                    isCameraOn = participantTitleState.isCameraOn,
                    isSharingScreen = participantTitleState.isSharingScreen,
                    shouldFill = shouldFillOthersVideoPreview,
                    isZoomingEnabled = isZoomingEnabled
                )
            }

            if (!isOnPiPMode) {
                BottomRow(
                    participantTitleState = participantTitleState,
                    isSelfUserMuted = isSelfUserMuted,
                    modifier = Modifier
                        .padding(
                            // move by the size of the active speaker border
                            start = dimensions().spacing6x,
                            end = dimensions().spacing6x,
                            bottom = dimensions().spacing6x,
                        )
                        .constrainAs(bottomRow) {
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )
            }

            AnimatedVisibility(
                modifier = Modifier
                    .padding(dimensions().spacing12x),
                visible = recentReaction != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensions().inCallReactionRecentReactionSize)
                        .background(
                            color = colorsScheme().emojiBackgroundColor,
                            shape = RoundedCornerShape(dimensions().corner6x)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = recentReaction ?: "",
                        textAlign = TextAlign.Center,
                        style = typography().inCallReactionRecentEmoji,
                    )
                }
            }

            if (participantTitleState.isSelfUser && isSelfUserCameraOn) {
                FlipCameraButton(
                    modifier = Modifier
                        .constrainAs(cameraButton) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                        },
                    isOnFrontCamera = isOnFrontCamera,
                    flipCamera = flipCamera,
                )
            }
        }
    }
}

private fun Modifier.thenIf(condition: Boolean, other: Modifier): Modifier =
    if (condition) this.then(other) else this

private val activeSpeakerBorderModifier
    @Composable get() = Modifier
        .border(
            width = dimensions().spacing3x,
            shape = RoundedCornerShape(dimensions().corner8x),
            color = colorsScheme().primary
        )
        .border(
            width = dimensions().spacing6x,
            shape = RoundedCornerShape(dimensions().corner9x),
            color = colorsScheme().background
        )

@Composable
private fun BottomRow(
    participantTitleState: UICallParticipant,
    isSelfUserMuted: Boolean,
    modifier: Modifier = Modifier,
) {
    val defaultUserName = stringResource(id = R.string.username_unavailable_label)
    Layout(
        modifier = modifier,
        content = {
            MicrophoneTile(
                modifier = Modifier
                    .padding(end = dimensions().spacing8x)
                    .layoutId("muteIcon"),
                isMuted = if (participantTitleState.isSelfUser) isSelfUserMuted else participantTitleState.isMuted,
                hasEstablishedAudio = participantTitleState.hasEstablishedAudio
            )
            UsernameTile(
                modifier = Modifier
                    .layoutId("username"),
                name = participantTitleState.name ?: defaultUserName,
                isSpeaking = participantTitleState.isSpeaking,
                hasEstablishedAudio = participantTitleState.hasEstablishedAudio
            )
        },
        measurePolicy = { measurables, constraints ->
            val muteIconPlaceable = measurables.firstOrNull { it.layoutId == "muteIcon" }
                ?.measure(constraints.copy(minWidth = 0, minHeight = 0))
            val muteIconWidth = muteIconPlaceable?.width ?: 0
            val maxUsernameWidth = constraints.maxWidth - muteIconWidth
            val usernamePlaceable = measurables.first { it.layoutId == "username" }
                .measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = maxUsernameWidth))

            layout(constraints.maxWidth, usernamePlaceable.height) {
                muteIconPlaceable?.placeRelative(0, 0)
                if (usernamePlaceable.width < constraints.maxWidth - 2 * muteIconWidth) { // can fit in center
                    usernamePlaceable.placeRelative(
                        (constraints.maxWidth - usernamePlaceable.width) / 2,
                        0
                    )
                } else { // needs to take all remaining space
                    usernamePlaceable.placeRelative(muteIconWidth, 0)
                }
            }
        }
    )
}

@Composable
private fun CleanUpRendererIfNeeded(videoRenderer: VideoRenderer) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(videoRenderer, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                videoRenderer.destroyRenderer()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            videoRenderer.destroyRenderer()
        }
    }
}

@Composable
private fun CameraPreview(
    isCameraOn: Boolean,
    onSelfUserVideoPreviewCreated: (view: View) -> Unit,
    shouldFill: Boolean = false,
    onClearSelfUserVideoPreview: () -> Unit
) {
    var isCameraStopped by remember { mutableStateOf(isCameraOn) }

    if (isCameraOn) {
        isCameraStopped = false
        val context = LocalContext.current
        val backgroundColor = darkColorsScheme().surfaceContainer.value.toInt()
        val videoPreview = remember {
            CameraPreviewBuilder(context)
                .setBackgroundColor(backgroundColor)
                .shouldFill(shouldFill)
                .build()
        }
        AndroidView(
            factory = {
                onSelfUserVideoPreviewCreated(videoPreview)
                videoPreview
            }
        )
    } else {
        if (isCameraStopped) return
        isCameraStopped = true
        onClearSelfUserVideoPreview()
    }
}

@Composable
private fun OthersVideoRenderer(
    participantId: String,
    clientId: String,
    isCameraOn: Boolean,
    isSharingScreen: Boolean,
    shouldFill: Boolean,
    isZoomingEnabled: Boolean
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val context = LocalContext.current
    val rendererFillColor = (darkColorsScheme().surfaceContainer.value shr 32).toLong()
    if (isCameraOn || isSharingScreen) {

        val videoRenderer = remember {
            VideoRenderer(
                context,
                participantId,
                clientId,
                false
            ).apply {
                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setFillColor(rendererFillColor)
                setShouldFill(shouldFill)
            }
        }

        CleanUpRendererIfNeeded(videoRenderer)

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    size = it
                }
                .pointerInput(Unit) {
                    // enable zooming on full screen and when video is on
                    if (isZoomingEnabled) {
                        detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 3f)
                            val maxX = (size.width * (zoom - 1)) / 2
                            val minX = -maxX
                            offsetX = maxOf(minX, minOf(maxX, offsetX + gesturePan.x))
                            val maxY = (size.height * (zoom - 1)) / 2
                            val minY = -maxY
                            offsetY = maxOf(minY, minOf(maxY, offsetY + gesturePan.y))
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = offsetX,
                    translationY = offsetY
                ),

            factory = {
                val frameLayout = FrameLayout(it)
                frameLayout.addView(videoRenderer)
                frameLayout
            }
        )
    }
}

@Composable
private fun AvatarTile(
    avatar: UserAvatarData,
    modifier: Modifier = Modifier,
    isOnPiPMode: Boolean = false
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val size = if (isOnPiPMode) 20.dp else min(maxWidth, maxHeight)
        UserProfileAvatar(
            padding = dimensions().spacing0x,
            size = size,
            avatarData = avatar,
            type = UserProfileAvatarType.WithoutIndicators,
        )
    }
}

@Composable
private fun UsernameTile(
    name: String,
    isSpeaking: Boolean,
    hasEstablishedAudio: Boolean,
    modifier: Modifier = Modifier,
) {
    val color =
        if (isSpeaking) colorsScheme().primary else darkColorsScheme().inverseOnSurface
    val nameLabelColor =
        when {
            isSpeaking -> colorsScheme().onPrimary
            hasEstablishedAudio -> darkColorsScheme().onSurface
            else -> darkColorsScheme().secondaryText
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(dimensions().corner3x),
        color = color,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x),
            modifier = Modifier.padding(dimensions().spacing4x)
        ) {
            Text(
                color = nameLabelColor,
                style = MaterialTheme.wireTypography.label01,
                text = name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (!hasEstablishedAudio) {
                Text(
                    color = darkColorsScheme().error,
                    style = MaterialTheme.wireTypography.label01,
                    text = stringResource(id = R.string.participant_tile_call_connecting_label),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun MicrophoneTile(
    isMuted: Boolean,
    hasEstablishedAudio: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isMuted && hasEstablishedAudio) {
        Surface(
            modifier = modifier,
            color = Color.Black,
            shape = RoundedCornerShape(dimensions().corner3x)
        ) {
            Icon(
                modifier = Modifier
                    .padding(dimensions().spacing3x)
                    .size(dimensions().spacing16x),
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_participant_muted),
                tint = darkColorsScheme().error,
                contentDescription = stringResource(R.string.content_description_calling_participant_muted)
            )
        }
    }
}

private enum class PreviewTileShape(val width: Dp, val height: Dp) {
    Regular(width = 175.dp, height = 140.dp),
    Tall(width = 140.dp, height = 175.dp),
    Wide(width = 175.dp, height = 80.dp),
}

@Composable
private fun PreviewParticipantTile(
    longName: Boolean = false,
    isMuted: Boolean = false,
    isSpeaking: Boolean = false,
    hasEstablishedAudio: Boolean = true,
    shape: PreviewTileShape = PreviewTileShape.Wide,
    recentReaction: String? = null,
    isSelfUser: Boolean = false,
    isSelfCameraOn: Boolean = false,
) {
    ParticipantTile(
        modifier = Modifier.size(width = shape.width, height = shape.height),
        participantTitleState = UICallParticipant(
            id = QualifiedID("", ""),
            clientId = "client-id",
            isSelfUser = isSelfUser,
            name = if (longName) "long user name to be displayed in participant tile during a call" else "user name",
            isMuted = isMuted,
            isSpeaking = isSpeaking,
            isCameraOn = false,
            isSharingScreen = false,
            avatar = null,
            membership = Membership.Admin,
            hasEstablishedAudio = hasEstablishedAudio,
            accentId = -1
        ),
        onClearSelfUserVideoPreview = {},
        onSelfUserVideoPreviewCreated = {},
        isSelfUserMuted = false,
        isSelfUserCameraOn = isSelfCameraOn,
        recentReaction = recentReaction,
        isOnFrontCamera = false,
        flipCamera = { },
    )
}

// ------ connecting ------

@PreviewMultipleThemes
@Composable
fun PreviewParticipantConnecting() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Regular, hasEstablishedAudio = false)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantLongNameConnecting() = WireTheme {
    PreviewParticipantTile(
        shape = PreviewTileShape.Regular,
        hasEstablishedAudio = false,
        longName = true
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantTallLongNameConnecting() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Tall, hasEstablishedAudio = false)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantWideLongNameConnecting() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Wide, hasEstablishedAudio = false)
}

// ------ muted ------

@PreviewMultipleThemes
@Composable
fun PreviewParticipantMuted() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Regular, isMuted = true)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantLongNameMuted() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Regular, isMuted = true, longName = true)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantTallLongNameMuted() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Tall, isMuted = true)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantWideLongNameMuted() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Wide, isMuted = true)
}

// ------ talking ------

@PreviewMultipleThemes
@Composable
fun PreviewParticipantTalking() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Regular, isSpeaking = true)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantLongNameTalking() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Regular, isSpeaking = true, longName = true)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantTallLongNameTalking() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Tall, isSpeaking = true)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantWideLongNameTalking() = WireTheme {
    PreviewParticipantTile(shape = PreviewTileShape.Wide, isSpeaking = true)
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantTalkingReaction() = WireTheme {
    PreviewParticipantTile(
        shape = PreviewTileShape.Regular,
        isSpeaking = true,
        recentReaction = InCallReactions.defaultReactions[2],
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewParticipantCameraButton() = WireTheme {
    PreviewParticipantTile(
        shape = PreviewTileShape.Regular,
        recentReaction = InCallReactions.defaultReactions[2],
        isSelfUser = true,
        isSelfCameraOn = true,
    )
}
