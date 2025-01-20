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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.atMost
import com.wire.android.R
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.DefaultText
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.home.conversations.UsersTypingIndicatorForConversation
import com.wire.android.ui.home.conversations.messages.QuotedMessagePreview
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionArgs
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModel
import com.wire.android.ui.home.messagecomposer.actions.SelfDeletingMessageActionViewModelImpl
import com.wire.android.ui.home.messagecomposer.attachments.AdditionalOptionButton
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.InputType
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer

@Composable
fun ActiveMessageComposerInput(
    conversationId: ConversationId,
    messageComposition: MessageComposition,
    messageTextState: TextFieldState,
    isTextExpanded: Boolean,
    inputType: InputType,
    focusRequester: FocusRequester,
    onSendButtonClicked: () -> Unit,
    onEditButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: (currentlySelected: SelfDeletionTimer) -> Unit,
    onToggleInputSize: () -> Unit,
    onTextCollapse: () -> Unit,
    onCancelReply: () -> Unit,
    onCancelEdit: () -> Unit,
    onFocused: () -> Unit,
    onSelectedLineIndexChanged: (Int) -> Unit,
    onLineBottomYCoordinateChanged: (Float) -> Unit,
    showOptions: Boolean,
    optionsSelected: Boolean,
    onPlusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(inputType.backgroundColor())
    ) {
        HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
        if (showOptions) {
            CollapseButton(
                isCollapsed = !isTextExpanded,
                onCollapseClick = onToggleInputSize
            )
        }

        messageComposition.quotedMessage?.let { quotedMessage ->
            VerticalSpace.x4()
            Box(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                QuotedMessagePreview(
                    quotedMessageData = quotedMessage,
                    onCancelReply = onCancelReply
                )
            }
        }

        InputContent(
            conversationId = conversationId,
            messageTextState = messageTextState,
            isTextExpanded = isTextExpanded,
            inputType = inputType,
            focusRequester = focusRequester,
            onSendButtonClicked = onSendButtonClicked,
            onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
            onFocused = onFocused,
            onSelectedLineIndexChanged = onSelectedLineIndexChanged,
            onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
            showOptions = showOptions,
            optionsSelected = optionsSelected,
            onPlusClick = onPlusClick,
            onTextCollapse = onTextCollapse,
            modifier = Modifier
                .fillMaxWidth()
                .let {
                    if (isTextExpanded) it.weight(1F) else it.wrapContentHeight()
                },
        )
        when (inputType) {
            is InputType.Editing -> {
                MessageEditActions(
                    onEditSaveButtonClicked = onEditButtonClicked,
                    onEditCancelButtonClicked = onCancelEdit,
                    editButtonEnabled = inputType.isEditButtonEnabled
                )
            }

            else -> {}
        }
    }
}

// flexible composable to adapt when [MessageComposerTextInput] is expanded or collapsed
@Composable
private fun InputContent(
    conversationId: ConversationId,
    messageTextState: TextFieldState,
    isTextExpanded: Boolean,
    inputType: InputType,
    focusRequester: FocusRequester,
    onSendButtonClicked: () -> Unit,
    onChangeSelfDeletionClicked: (currentlySelected: SelfDeletionTimer) -> Unit,
    onFocused: () -> Unit,
    onSelectedLineIndexChanged: (Int) -> Unit,
    onLineBottomYCoordinateChanged: (Float) -> Unit,
    showOptions: Boolean,
    optionsSelected: Boolean,
    onPlusClick: () -> Unit,
    onTextCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SelfDeletingMessageActionViewModel =
        hiltViewModelScoped<SelfDeletingMessageActionViewModelImpl, SelfDeletingMessageActionViewModel, SelfDeletingMessageActionArgs>(
            SelfDeletingMessageActionArgs(conversationId = conversationId)
        )
) {
    ConstraintLayout(modifier = modifier) {
        val (additionalOptionButton, input, actions) = createRefs()
        val buttonsTopBarrier = createTopBarrier(additionalOptionButton, actions)
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier.constrainAs(additionalOptionButton) {
                start.linkTo(parent.start)
                bottom.linkTo(parent.bottom)
            }
        ) {
            if (!showOptions && inputType is InputType.Composing) {
                AdditionalOptionButton(
                    isSelected = optionsSelected,
                    onClick = onPlusClick,
                    modifier = Modifier.padding(start = dimensions().spacing8x)
                )
            }
        }

        val collapsedMaxHeight = dimensions().messageComposerActiveInputMaxHeight
        MessageComposerTextInput(
            isTextExpanded = isTextExpanded,
            focusRequester = focusRequester,
            colors = inputType.inputTextColor(isSelfDeleting = viewModel.state().duration != null),
            messageTextState = messageTextState,
            placeHolderText = viewModel.state().duration?.let { stringResource(id = R.string.self_deleting_message_label) }
                ?: inputType.labelText(),
            onFocused = onFocused,
            onSelectedLineIndexChanged = onSelectedLineIndexChanged,
            onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
            onTextCollapse = onTextCollapse,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(input) {
                    width = Dimension.fillToConstraints
                    height = if (isTextExpanded) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                        bottom.linkTo(buttonsTopBarrier)
                        Dimension.fillToConstraints
                    } else {
                        start.linkTo(additionalOptionButton.end)
                        end.linkTo(actions.start)
                        bottom.linkTo(parent.bottom)
                        Dimension.preferredWrapContent.atMost(collapsedMaxHeight)
                    }
                }
        )

        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .constrainAs(actions) {
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
        ) {
            if (isTextExpanded) {
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    UsersTypingIndicatorForConversation(conversationId = conversationId)
                }
            }
            if (showOptions) {
                if (inputType is InputType.Composing) {
                    MessageSendActions(
                        onSendButtonClicked = onSendButtonClicked,
                        sendButtonEnabled = inputType.isSendButtonEnabled,
                        selfDeletionTimer = viewModel.state(),
                        onChangeSelfDeletionClicked = onChangeSelfDeletionClicked,
                        modifier = Modifier.padding(end = dimensions().spacing8x)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MessageComposerTextInput(
    messageTextState: TextFieldState,
    isTextExpanded: Boolean,
    focusRequester: FocusRequester,
    colors: WireTextFieldColors,
    placeHolderText: String,
    onTextCollapse: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { }
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onFocused()
        }
    }

    WireTextField(
        textState = messageTextState,
        colors = colors,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add an extra space so that the cursor is placed one space before "Type a message"
        placeholderText = " $placeHolderText",
        state = WireTextFieldState.Default,
        keyboardOptions = KeyboardOptions.DefaultText.copy(imeAction = ImeAction.None),
        modifier = modifier
            .focusable(true)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocused()
                }
            }
            .onPreInterceptKeyBeforeSoftKeyboard { event ->
                if (event.key.nativeKeyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    if (isTextExpanded) {
                        onTextCollapse()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        interactionSource = interactionSource,
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged,
        singleLine = false,
    )
}

@Composable
fun CollapseButton(
    isCollapsed: Boolean,
    onCollapseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val collapseButtonRotationDegree by animateFloatAsState(targetValue = if (isCollapsed) 0F else 180f)

        IconButton(
            onClick = {
                onCollapseClick()
            },
            modifier = Modifier.size(20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_collapse),
                contentDescription = stringResource(R.string.content_description_drop_down_icon),
                tint = colorsScheme().onSecondaryButtonDisabled,
                modifier = Modifier.rotate(collapseButtonRotationDegree)
            )
        }
    }
}

@Composable
private fun PreviewActiveMessageComposerInput(inputType: InputType, isTextExpanded: Boolean) {
    ActiveMessageComposerInput(
        conversationId = ConversationId("conversationId", "domain"),
        messageComposition = MessageComposition(ConversationId("conversationId", "domain")),
        messageTextState = TextFieldState(""),
        isTextExpanded = isTextExpanded,
        inputType = inputType,
        focusRequester = FocusRequester(),
        onSendButtonClicked = {},
        onEditButtonClicked = {},
        onChangeSelfDeletionClicked = {},
        onToggleInputSize = {},
        onTextCollapse = {},
        onCancelReply = {},
        onCancelEdit = {},
        onFocused = {},
        onSelectedLineIndexChanged = {},
        onLineBottomYCoordinateChanged = {},
        showOptions = true,
        optionsSelected = true,
        onPlusClick = {}
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewActiveMessageComposerInputCollapsed() = WireTheme {
    PreviewActiveMessageComposerInput(
        inputType = InputType.Composing(isSendButtonEnabled = true),
        isTextExpanded = false
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewActiveMessageComposerInputCollapsedEdit() = WireTheme {
    PreviewActiveMessageComposerInput(
        inputType = InputType.Editing(isEditButtonEnabled = true),
        isTextExpanded = false
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewActiveMessageComposerInputExpanded() = WireTheme {
    PreviewActiveMessageComposerInput(
        inputType = InputType.Composing(isSendButtonEnabled = true),
        isTextExpanded = true
    )
}
