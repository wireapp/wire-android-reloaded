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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.KeyboardHelper
import com.wire.android.ui.common.SecurityClassificationBanner
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.home.conversations.mention.MemberItemToMention
import com.wire.android.ui.home.conversations.messages.QuotedMessagePreview
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.messagecomposer.AttachmentOptionsComponent
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionMenuState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.MessageComposerState
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputState

import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputType
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.KeyboardHeight
import com.wire.kalium.logic.feature.conversation.InteractionAvailability
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType

@Composable
fun MessageComposer(
    messageComposerState: MessageComposerState,
    messageListContent: @Composable () -> Unit
) {
    with(messageComposerState) {
        when (messageComposerState.interactionAvailability) {
            InteractionAvailability.BLOCKED_USER -> BlockedUserComposerInput(securityClassificationType)
            InteractionAvailability.DELETED_USER -> DeletedUserComposerInput(securityClassificationType)
            InteractionAvailability.NOT_MEMBER, InteractionAvailability.DISABLED ->
                MessageComposerClassifiedBanner(securityClassificationType, PaddingValues(vertical = dimensions().spacing16x))

            InteractionAvailability.ENABLED -> {
                EnabledMessageComposerInput(messageComposerState, messageListContent)
            }
        }
    }
}

@Composable
private fun EnabledMessageComposerInput(
    messageComposerState: MessageComposerState,
    messageListContent: @Composable () -> Unit
) {
    with(messageComposerState) {
        Row {
            val isClassifiedConversation = securityClassificationType != SecurityClassificationType.NONE
            if (isClassifiedConversation) {
                Box(Modifier.wrapContentSize()) {
                    VerticalSpace.x8()
                    SecurityClassificationBanner(securityClassificationType = securityClassificationType)
                }
            }
            when (messageComposerState.inputState) {
                MessageCompositionInputState.ACTIVE -> {
                    ActiveMessageComposer(
                        messageComposerState = messageComposerState,
                        messageListContent = messageListContent,
                        onTransitionToInActive = messageComposerState::toInActive
                    )
                }

                MessageCompositionInputState.INACTIVE -> {
                    InActiveMessageComposer(
                        messageComposerState = messageComposerState,
                        messageListContent = messageListContent,
                        onTransitionToActive = messageComposerState::toActive
                    )
                }
            }
        }
    }
}

@Composable
private fun InActiveMessageComposer(
    messageComposerState: MessageComposerState,
    messageListContent: @Composable () -> Unit,
    onTransitionToActive: (Boolean) -> Unit
) {
    with(messageComposerState) {
        Surface(color = colorsScheme().messageComposerBackgroundColor) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                val fillRemainingSpaceBetweenMessageListContentAndMessageComposer = Modifier
                    .fillMaxWidth()
                    .weight(1f)

                Box(
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { onTransitionToActive(false) },
                                onDoubleTap = { /* Called on Double Tap */ },
                                onLongPress = { /* Called on Long Press */ },
                                onTap = { /* Called on Tap */ }
                            )
                        }
                        .background(color = colorsScheme().backgroundVariant)
                        .then(fillRemainingSpaceBetweenMessageListContentAndMessageComposer)
                ) {
                    messageListContent()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Box(modifier = Modifier.padding(start = dimensions().spacing8x)) {
                        AdditionalOptionButton(
                            isSelected = false,
                            isEnabled = isFileSharingEnabled,
                            onClick = { onTransitionToActive(true) }
                        )
                    }

                    MessageComposerTextInput(
                        inputFocused = false,
                        colors = inputType.inputTextColor(),
                        messageText = inputType.messageCompositionState.value.messageTextFieldValue,
                        onMessageTextChanged = { },
                        singleLine = false,
                        onFocusChanged = { isFocused ->
                            if (isFocused) {
                                onTransitionToActive(false)
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun ActiveMessageComposer(
    messageComposerState: MessageComposerState,
    messageListContent: @Composable () -> Unit,
    onTransitionToInActive: () -> Unit
) {
    with(messageComposerState) {
        Surface(color = colorsScheme().messageComposerBackgroundColor) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val currentScreenHeight: Dp = with(LocalDensity.current) { constraints.maxHeight.toDp() }

                // when MessageComposer is composed for the first time we do not know the height until users opens the keyboard
                var keyboardHeight: KeyboardHeight by remember { mutableStateOf(KeyboardHeight.NotKnown) }

                if (KeyboardHelper.isKeyboardVisible()) {
                    val calculatedKeyboardHeight = KeyboardHelper.getCalculatedKeyboardHeight()
                    val notKnownAndCalculated =
                        keyboardHeight is KeyboardHeight.NotKnown && calculatedKeyboardHeight > 0.dp
                    val knownAndDifferent =
                        keyboardHeight is KeyboardHeight.Known && keyboardHeight.height != calculatedKeyboardHeight
                    if (notKnownAndCalculated || knownAndDifferent) {
                        keyboardHeight = KeyboardHeight.Known(calculatedKeyboardHeight)
                    }
                }

                val makeTheContentAsBigAsScreenHeightWithoutKeyboard = Modifier
                    .fillMaxWidth()
                    .height(currentScreenHeight)

                Column(
                    makeTheContentAsBigAsScreenHeightWithoutKeyboard
                ) {
                    val fillRemainingSpaceBetweenThisAndAdditionalSubMenu = Modifier
                        .weight(1f)
                        .fillMaxWidth()

                    Column(fillRemainingSpaceBetweenThisAndAdditionalSubMenu) {
                        val fillRemainingSpaceBetweenMessageListContentAndMessageComposer = Modifier
                            .fillMaxWidth()
                            .weight(1f)

                        Box(
                            Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = { onTransitionToInActive() },
                                        onDoubleTap = { /* Called on Double Tap */ },
                                        onLongPress = { /* Called on Long Press */ },
                                        onTap = { /* Called on Tap */ }
                                    )
                                }
                                .background(color = colorsScheme().backgroundVariant)
                                .then(fillRemainingSpaceBetweenMessageListContentAndMessageComposer)

                        ) {
                            messageListContent()
                            if (messageComposition.value.mentions.isNotEmpty()) {
                                MembersMentionList(
                                    membersToMention = messageComposition.value.mentions,
                                    onMentionPicked = { }
                                )
                            }
                        }
                        Column(
                            Modifier.wrapContentSize()
                        ) {
                            val fillRemainingSpaceOrWrapContent =
                                if (inputSize == MessageCompositionInputSize.COLLAPSED)
                                    Modifier.wrapContentHeight()
                                else Modifier.weight(1f)

                            MessageComposerInput(
                                inputFocused = inputFocused,
                                messageCompositionInputState = inputType,
                                messageCompositionInputSize = inputSize,
                                securityClassificationType = SecurityClassificationType.CLASSIFIED,
                                interactionAvailability = InteractionAvailability.BLOCKED_USER,
                                onMessageTextChanged = ::onMessageTextChanged,
                                onSendButtonClicked = { },
                                onFocused = ::onInputFocused,
                                onCollapseButtonClicked = ::toggleFullScreenInput,
                                modifier = fillRemainingSpaceOrWrapContent
                            )
                            AdditionalOptionsMenu(
                                onOnSelfDeletingOptionClicked = ::toSelfDeleting,
                                onAttachmentOptionClicked = {},
                                onGifOptionClicked = { },
                                onPingOptionClicked = { },
                            )
                        }
                    }

                    val additionalOptionSubMenuVisible =
                        additionalOptionsSubMenuState != AdditionalOptionSubMenuState.Hidden
                                && !KeyboardHelper.isKeyboardVisible()

                    val isTransitionToOpenKeyboardOngoing =
                        additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Hidden
                                && !KeyboardHelper.isKeyboardVisible()

                    if (additionalOptionSubMenuVisible) {
                        AdditionalOptionSubMenu(
                            additionalOptionsState = additionalOptionsSubMenuState,
                            modifier = Modifier
                                .height(keyboardHeight.height)
                                .fillMaxWidth()
                                .background(
                                    colorsScheme().messageComposerBackgroundColor
                                )
                        )
                    }
                    // This covers the situation when the user switches from attachment options to the input keyboard - there is a moment when
                    // both attachmentOptionsDisplayed and isKeyboardVisible are false, but right after that keyboard shows, so if we know that
                    // the input already has a focus, we can show an empty Box which has a height of the keyboard to prevent flickering.
                    else if (isTransitionToOpenKeyboardOngoing) {
                        Box(
                            modifier = Modifier
                                .height(keyboardHeight.height)
                                .fillMaxWidth()
                        )
                    }
                }
            }
            BackHandler(additionalOptionsSubMenuState != AdditionalOptionSubMenuState.Hidden) {
                onTransitionToInActive()
            }
        }
    }
}

@Composable
private fun AdditionalOptionsMenu(
    onOnSelfDeletingOptionClicked: () -> Unit,
    onAttachmentOptionClicked: () -> Unit,
    onGifOptionClicked: () -> Unit,
    onPingOptionClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var additionalOptionState: AdditionalOptionMenuState by remember { mutableStateOf(AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu) }

    Box(modifier) {
        when (additionalOptionState) {
            is AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu -> {
                AttachmentAndAdditionalOptionsMenuItems(
                    isMentionActive = true,
                    isFileSharingEnabled = true,
                    onMentionButtonClicked = onOnSelfDeletingOptionClicked,
                    onAttachmentOptionClicked = onAttachmentOptionClicked,
                    onGifButtonClicked = onGifOptionClicked,
                    onSelfDeletionOptionButtonClicked = onOnSelfDeletingOptionClicked,
                    onRichEditingButtonClicked = { additionalOptionState = AdditionalOptionMenuState.RichTextEditing },
                    onPingClicked = onPingOptionClicked,
                    showSelfDeletingOption = true,
                    modifier = Modifier.background(Color.Black)
                )
            }

            is AdditionalOptionMenuState.RichTextEditing -> {
                RichTextOptions(
                    onRichTextHeaderButtonClicked = {},
                    onRichTextBoldButtonClicked = {},
                    onRichTextItalicButtonClicked = {},
                    onCloseRichTextEditingButtonClicked = {
                        additionalOptionState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu
                    }
                )
            }
        }
    }
}

@Composable
private fun AdditionalOptionSubMenu(
    additionalOptionsState: AdditionalOptionSubMenuState,
    modifier: Modifier
) {
    when (additionalOptionsState) {
        AdditionalOptionSubMenuState.AttachFile -> {
            AttachmentOptionsComponent(
                onAttachmentPicked = {},
                tempWritableImageUri = null,
                tempWritableVideoUri = null,
                isFileSharingEnabled = true,
                modifier = modifier
            )
        }

        AdditionalOptionSubMenuState.Emoji -> {}
        AdditionalOptionSubMenuState.Gif -> {}
        AdditionalOptionSubMenuState.RecordAudio -> {}
        AdditionalOptionSubMenuState.AttachImage -> {}
        AdditionalOptionSubMenuState.Hidden -> {}
    }
}

@Composable
private fun AttachmentAndAdditionalOptionsMenuItems(
    isMentionActive: Boolean,
    isFileSharingEnabled: Boolean,
    onMentionButtonClicked: () -> Unit,
    onAttachmentOptionClicked: () -> Unit = {},
    onPingClicked: () -> Unit = {},
    onSelfDeletionOptionButtonClicked: () -> Unit,
    showSelfDeletingOption: Boolean,
    onGifButtonClicked: () -> Unit = {},
    onRichEditingButtonClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier.wrapContentSize()) {
        Divider(color = MaterialTheme.wireColorScheme.outline)
        MessageComposeActions(
            false,
            isMentionActive,
            false,
            isEditMessage = false,
            isFileSharingEnabled,
            onMentionButtonClicked = onMentionButtonClicked,
            onAdditionalOptionButtonClicked = onAttachmentOptionClicked,
            onPingButtonClicked = onPingClicked,
            onSelfDeletionOptionButtonClicked = onSelfDeletionOptionButtonClicked,
            showSelfDeletingOption = showSelfDeletingOption,
            onGifButtonClicked = onGifButtonClicked,
            onRichEditingButtonClicked = onRichEditingButtonClicked
        )
    }
}

@Composable
private fun MessageComposerInput(
    inputFocused: Boolean,
    securityClassificationType: SecurityClassificationType,
    interactionAvailability: InteractionAvailability,
    messageCompositionInputState: MessageCompositionInputType,
    messageCompositionInputSize: MessageCompositionInputSize,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onCollapseButtonClicked: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(messageCompositionInputState) {
        Column(
            modifier = modifier
        ) {
            CollapseButton(
                onCollapseClick = {
                    onCollapseButtonClicked()
                }
            )

            val quotedMessage = messageCompositionState.value.quotedMessage
            if (quotedMessage != null) {
                Row(modifier = Modifier.padding(horizontal = dimensions().spacing8x)) {
                    QuotedMessagePreview(
                        quotedMessageData = quotedMessage,
                        onCancelReply = {}
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalAlignment = Alignment.Bottom
            ) {
                val stretchToMaxParentConstraintHeightOrWithInBoundary = when (messageCompositionInputSize) {
                    MessageCompositionInputSize.COLLAPSED -> Modifier.heightIn(max = dimensions().messageComposerActiveInputMaxHeight)
                    MessageCompositionInputSize.EXPANDED -> Modifier.fillMaxHeight()
                }.weight(1f)

                MessageComposerTextInput(
                    inputFocused = inputFocused,
                    colors = messageCompositionInputState.inputTextColor(),
                    messageText = messageCompositionInputState.messageCompositionState.value.messageTextFieldValue,
                    onMessageTextChanged = onMessageTextChanged,
                    singleLine = false,
                    onFocusChanged = { isFocused ->
                        if (isFocused) onFocused()
                    },
                    modifier = stretchToMaxParentConstraintHeightOrWithInBoundary
                )
                Row(Modifier.wrapContentSize()) {
                    when (messageCompositionInputState) {
                        is MessageCompositionInputType.Composing -> MessageSendActions(
                            onSendButtonClicked = onSendButtonClicked,
                            sendButtonEnabled = messageCompositionInputState.isSendButtonEnabled
                        )

                        is MessageCompositionInputType.SelfDeleting -> SelfDeletingActions(
                            selfDeletionTimer = messageCompositionInputState.messageCompositionState.value.selfDeletionTimer,
                            sendButtonEnabled = messageCompositionInputState.isSendButtonEnabled,
                            onSendButtonClicked = onSendButtonClicked,
                            onChangeSelfDeletionClicked = messageCompositionInputState::showSelfDeletingTimeOption
                        )

                        else -> {}
                    }
                }
            }
            when (messageCompositionInputState) {
                is MessageCompositionInputType.Editing -> {
                    MessageEditActions(
                        onEditSaveButtonClicked = { },
                        onEditCancelButtonClicked = {},
                        editButtonEnabled = messageCompositionInputState.isEditButtonEnabled
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun MessageComposerTextInput(
    inputFocused: Boolean,
    colors: WireTextFieldColors,
    singleLine: Boolean,
    messageText: TextFieldValue,
    onMessageTextChanged: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(inputFocused) {
        if (inputFocused) focusRequester.requestFocus()
        else focusManager.clearFocus()
    }


    WireTextField(
        value = messageText,
        onValueChange = onMessageTextChanged,
        colors = colors,
        singleLine = singleLine,
        maxLines = Int.MAX_VALUE,
        textStyle = MaterialTheme.wireTypography.body01,
        // Add an extra space so that the cursor is placed one space before "Type a message"
        placeholderText = " " + stringResource(R.string.label_type_a_message),
        modifier = modifier.then(
            Modifier
                .onFocusChanged { focusState ->
                    onFocusChanged(focusState.isFocused)
                }
                .focusRequester(focusRequester)
        ),
        onSelectedLineIndexChanged = onSelectedLineIndexChanged,
        onLineBottomYCoordinateChanged = onLineBottomYCoordinateChanged
    )
}

@Composable
private fun MembersMentionList(
    membersToMention: List<Contact>,
    onMentionPicked: (Contact) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (membersToMention.isNotEmpty()) Divider()
        LazyColumn(
            modifier = Modifier.background(colorsScheme().background),
            reverseLayout = true
        ) {
            membersToMention.forEach {
                if (it.membership != Membership.Service) {
                    item {
                        MemberItemToMention(
                            avatarData = it.avatarData,
                            name = it.name,
                            label = it.label,
                            membership = it.membership,
                            clickable = Clickable { onMentionPicked(it) },
                            modifier = Modifier
                        )
                        Divider(
                            color = MaterialTheme.wireColorScheme.divider,
                            thickness = Dp.Hairline
                        )
                    }
                }
            }
        }
    }
}
