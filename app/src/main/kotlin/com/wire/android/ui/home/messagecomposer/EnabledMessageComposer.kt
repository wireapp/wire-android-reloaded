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
package com.wire.android.ui.home.messagecomposer

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.wire.android.ui.common.banner.SecurityClassificationBannerForConversation
import com.wire.android.ui.common.bottombar.bottomNavigationBarHeight
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.ConversationActionPermissionType
import com.wire.android.ui.home.conversations.UsersTypingIndicatorForConversation
import com.wire.android.ui.home.conversations.model.UriAsset
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.InputType
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.util.isImage
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Suppress("ComplexMethod")
@Composable
fun EnabledMessageComposer(
    conversationId: ConversationId,
    bottomSheetVisible: Boolean,
    messageComposerStateHolder: MessageComposerStateHolder,
    messageListContent: @Composable () -> Unit,
    onChangeSelfDeletionClicked: (currentlySelected: SelfDeletionTimer) -> Unit,
    onLocationClicked: () -> Unit,
    onSendButtonClicked: () -> Unit,
    onImagesPicked: (List<Uri>) -> Unit,
    onAttachmentPicked: (UriAsset) -> Unit,
    onAudioRecorded: (UriAsset) -> Unit,
    onPermissionPermanentlyDenied: (type: ConversationActionPermissionType) -> Unit,
    onPingOptionClicked: () -> Unit,
    onClearMentionSearchResult: () -> Unit,
    openDrawingCanvas: () -> Unit,
    tempWritableVideoUri: Uri?,
    tempWritableImageUri: Uri?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val navBarHeight = bottomNavigationBarHeight()
    val isImeVisible = WindowInsets.isImeVisible
    val offsetY = WindowInsets.ime.getBottom(density)
    val imeAnimationSource = WindowInsets.imeAnimationSource.getBottom(density)
    val imeAnimationTarget = WindowInsets.imeAnimationTarget.getBottom(density)
    val rippleProgress = remember { Animatable(0f) }
    var hideRipple by remember { mutableStateOf(true) }

    with(messageComposerStateHolder) {
        val inputStateHolder = messageCompositionInputStateHolder

        LaunchedEffect(offsetY) {
            with(density) {
                inputStateHolder.handleImeOffsetChange(
                    offsetY.toDp(),
                    navBarHeight,
                    imeAnimationSource.toDp(),
                    imeAnimationTarget.toDp()
                )
            }
        }

        LaunchedEffect(inputStateHolder.optionsVisible) {
            if (inputStateHolder.optionsVisible) {
                rippleProgress.snapTo(0f)
                rippleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 400)
                )
            } else {
                hideRipple = true
                rippleProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 400)
                )
                hideRipple = false
            }
        }

        Surface(
            modifier = modifier,
            color = colorsScheme().surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                val expandOrHideMessagesModifier =
                    if (inputStateHolder.isTextExpanded) {
                        Modifier.height(0.dp)
                    } else {
                        Modifier.weight(1f)
                    }
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = expandOrHideMessagesModifier
                        .background(color = colorsScheme().surfaceContainerLow)
                ) {
                    messageListContent()
                    if (!inputStateHolder.isTextExpanded) {
                        UsersTypingIndicatorForConversation(conversationId = conversationId)
                    }
                    if (!inputStateHolder.isTextExpanded && messageComposerViewState.value.mentionSearchResult.isNotEmpty()) {
                        MembersMentionList(
                            membersToMention = messageComposerViewState.value.mentionSearchResult,
                            searchQuery = messageComposerViewState.value.mentionSearchQuery,
                            onMentionPicked = { pickedMention ->
                                messageCompositionHolder.value.addMention(pickedMention)
                                onClearMentionSearchResult()
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
                val fillRemainingSpaceOrWrapContent =
                    if (inputStateHolder.isTextExpanded) {
                        Modifier.weight(1f)
                    } else {
                        Modifier.wrapContentHeight()
                    }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = fillRemainingSpaceOrWrapContent
                        .fillMaxWidth()
                        .background(color = colorsScheme().surfaceContainerLow)
                ) {
                    Box(Modifier.wrapContentSize()) {
                        SecurityClassificationBannerForConversation(
                            conversationId = conversationId
                        )
                    }

                    Box(fillRemainingSpaceOrWrapContent, contentAlignment = Alignment.BottomCenter) {
                        var currentSelectedLineIndex by remember { mutableStateOf(0) }
                        var cursorCoordinateY by remember { mutableStateOf(0F) }

                        ActiveMessageComposerInput(
                            conversationId = conversationId,
                            messageComposition = messageComposition.value,
                            messageTextState = inputStateHolder.messageTextState,
                            isTextExpanded = inputStateHolder.isTextExpanded,
                            inputType = messageCompositionInputStateHolder.inputType,
                            focusRequester = messageCompositionInputStateHolder.focusRequester,
                            onFocused = ::onInputFocused,
                            onToggleInputSize = messageCompositionInputStateHolder::toggleInputSize,
                            onTextCollapse = messageCompositionInputStateHolder::collapseText,
                            onCancelReply = messageCompositionHolder.value::clearReply,
                            onCancelEdit = ::cancelEdit,
                            onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                            onSendButtonClicked = onSendButtonClicked,
                            onEditButtonClicked = {
                                onSendButtonClicked()
                                messageCompositionInputStateHolder.toComposing()
                            },
                            onLineBottomYCoordinateChanged = { yCoordinate ->
                                cursorCoordinateY = yCoordinate
                            },
                            onSelectedLineIndexChanged = { index ->
                                currentSelectedLineIndex = index
                            },
                            showOptions = isImeVisible,
                            optionsSelected = inputStateHolder.optionsVisible,
                            onPlusClick = {
                                if (!hideRipple) {
                                    showAttachments(!inputStateHolder.optionsVisible)
                                }
                            },
                            modifier = fillRemainingSpaceOrWrapContent
                                .onKeyboardDismiss(inputStateHolder.optionsVisible) {
                                    when (additionalOptionStateHolder.additionalOptionsSubMenuState) {
                                        AdditionalOptionSubMenuState.Default -> {
                                            inputStateHolder.showAttachments(false)
                                        }

                                        AdditionalOptionSubMenuState.RecordAudio -> {}
                                    }
                                }
                                .contentReceiver(
                                    receiveContentListener = { transferableContent ->
                                        if (transferableContent.hasMediaType(MediaType.Image)) {
                                            val imageUriList = mutableListOf<Uri>()
                                            transferableContent
                                                .consume { item ->
                                                    // Only use URIs with images
                                                    (item.uri != null && item.uri.isImage(context))
                                                        .also { hasImageUri ->
                                                            if (hasImageUri) imageUriList.add(item.uri)
                                                        }
                                                }
                                                .also {
                                                    onImagesPicked(imageUriList)
                                                }
                                        } else {
                                            transferableContent
                                        }
                                    }
                                ),
                        )

                        val mentionSearchResult = messageComposerViewState.value.mentionSearchResult
                        if (mentionSearchResult.isNotEmpty() && inputStateHolder.isTextExpanded
                        ) {
                            DropDownMentionsSuggestions(
                                currentSelectedLineIndex = currentSelectedLineIndex,
                                cursorCoordinateY = cursorCoordinateY,
                                membersToMention = mentionSearchResult,
                                searchQuery = messageComposerViewState.value.mentionSearchQuery,
                                onMentionPicked = {
                                    messageCompositionHolder.value.addMention(it)
                                    onClearMentionSearchResult()
                                }
                            )
                        }
                    }

                    if (isImeVisible) {
                        AdditionalOptionsMenu(
                            conversationId = conversationId,
                            additionalOptionsState = additionalOptionStateHolder.additionalOptionState,
                            selectedOption = additionalOptionStateHolder.selectedOption,
                            attachmentsVisible = inputStateHolder.optionsVisible,
                            isEditing = messageCompositionInputStateHolder.inputType is InputType.Editing,
                            isMentionActive = messageComposerViewState.value.mentionSearchResult.isNotEmpty(),
                            onMentionButtonClicked = messageCompositionHolder.value::startMention,
                            onOnSelfDeletingOptionClicked = onChangeSelfDeletionClicked,
                            onRichOptionButtonClicked = messageCompositionHolder.value::addOrRemoveMessageMarkdown,
                            onPingOptionClicked = onPingOptionClicked,
                            onAdditionalOptionsMenuClicked = {
                                if (!hideRipple) {
                                    showAttachments(!inputStateHolder.optionsVisible)
                                }
                            },
                            onRichEditingButtonClicked = {
                                messageCompositionInputStateHolder.requestFocus()
                                additionalOptionStateHolder.toRichTextEditing()
                            },
                            onCloseRichEditingButtonClicked = additionalOptionStateHolder::toAttachmentAndAdditionalOptionsMenu,
                            onDrawingModeClicked = openDrawingCanvas,
                            isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled
                        )
                    }
                }
            }
            if ((inputStateHolder.optionsVisible || rippleProgress.value > 0f) && !bottomSheetVisible) {
                Popup(
                    alignment = Alignment.BottomCenter,
                    properties = PopupProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    ),
                    offset = if (isImeVisible) {
                        IntOffset(0, 0)
                    } else {
                        with(density) { IntOffset(0, -dimensions().spacing64x.toPx().roundToInt()) }
                    },
                    onDismissRequest = {
                        hideRipple = true
                        showAttachments(false)
                    }
                ) {
                    val rippleColor = colorsScheme().surfaceContainerLowest
                    val borderColor = colorsScheme().divider
                    val borderWidthPx = if (isImeVisible) 0f else dimensions().spacing1x.toPx(density)
                    val cornerRadiusPx = if (isImeVisible) 0f else dimensions().corner14x.toPx(density)
                    val shape = GenericShape { size, _ ->
                        addPath(calculateOptionsPath(cornerRadiusPx, rippleProgress.value, isImeVisible, size))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(inputStateHolder.calculateOptionsMenuHeight(additionalOptionStateHolder.additionalOptionsSubMenuState))
                            .padding(
                                horizontal = if (isImeVisible) {
                                    dimensions().spacing0x
                                } else {
                                    dimensions().spacing8x
                                }
                            )
                            .background(
                                color = Color.Transparent,
                                shape = shape
                            )
                            .clip(shape)
                            .drawBehind {
                                if (!hideRipple || rippleProgress.value > 0f) {
                                    calculateOptionsPath(cornerRadiusPx, rippleProgress.value, isImeVisible, size).let {
                                        drawPath(
                                            path = it,
                                            color = rippleColor,
                                            style = Fill
                                        )
                                        if (borderWidthPx > 0f) {
                                            drawPath(
                                                path = it,
                                                color = borderColor,
                                                style = Stroke(
                                                    width = borderWidthPx * 2f // double to make inner stroke, outer half is clipped anyway
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                    ) {
                        AdditionalOptionSubMenu(
                            optionsVisible = inputStateHolder.optionsVisible,
                            isFileSharingEnabled = messageComposerViewState.value.isFileSharingEnabled,
                            additionalOptionsState = additionalOptionStateHolder.additionalOptionsSubMenuState,
                            onRecordAudioMessageClicked = ::toAudioRecording,
                            onCloseAdditionalAttachment = ::toInitialAttachmentOptions,
                            onLocationPickerClicked = onLocationClicked,
                            onImagesPicked = onImagesPicked,
                            onAttachmentPicked = onAttachmentPicked,
                            onAudioRecorded = onAudioRecorded,
                            onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
                            tempWritableImageUri = tempWritableImageUri,
                            tempWritableVideoUri = tempWritableVideoUri,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                BackHandler(inputStateHolder.optionsVisible) {
                    inputStateHolder.showAttachments(false)
                }
                BackHandler(inputStateHolder.inputType is InputType.Editing) {
                    cancelEdit()
                }
                BackHandler(isImeVisible || inputStateHolder.inputFocused) {
                    inputStateHolder.collapseComposer(additionalOptionStateHolder.additionalOptionsSubMenuState)
                }
            }
        }
    }
}

private fun Size.getDistanceToCorner(corner: Offset): Float {
    val cornerOffset = Offset(width - corner.x, height - corner.y)
    return cornerOffset.getDistance()
}

private fun calculateOptionsPath(cornerRadiusPx: Float, rippleProgress: Float, isImeVisible: Boolean, size: Size): Path {
    val ripplePath = Path()
    ripplePath.addOval(
        oval = Rect(
            center = Offset(
                x = 0f,
                y = if (isImeVisible) 0f else size.height
            ),
            radius = rippleProgress * size.getDistanceToCorner(Offset(0f, 0f))
        )
    )
    val shapePath = Path()
    shapePath.addRoundRect(
        roundRect = RoundRect(
            rect = size.toRect(),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
        )
    )
    return ripplePath.and(shapePath)
}

private fun Dp.toPx(density: Density) = with(density) { toPx() }
