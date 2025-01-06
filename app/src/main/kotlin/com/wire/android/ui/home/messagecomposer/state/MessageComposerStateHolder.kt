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

package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.message.draft.MessageDraft
import com.wire.kalium.logic.data.message.mention.MessageMention

@Suppress("LongParameterList")
@Composable
fun rememberMessageComposerStateHolder(
    messageComposerViewState: State<MessageComposerViewState>,
    draftMessageComposition: MessageComposition,
    onClearDraft: () -> Unit,
    onSaveDraft: (MessageDraft) -> Unit,
    onSearchMentionQueryChanged: (String) -> Unit,
    onClearMentionSearchResult: () -> Unit,
    onTypingEvent: (Conversation.TypingIndicatorMode) -> Unit,
): MessageComposerStateHolder {
    val density = LocalDensity.current

    val messageComposition = remember(draftMessageComposition) {
        mutableStateOf(draftMessageComposition)
    }

    val messageTextState = rememberTextFieldState()

    val messageCompositionHolder = remember {
        mutableStateOf(
            MessageCompositionHolder(
                messageComposition = messageComposition,
                messageTextState = messageTextState,
                onClearDraft = onClearDraft,
                onSaveDraft = onSaveDraft,
                onSearchMentionQueryChanged = onSearchMentionQueryChanged,
                onClearMentionSearchResult = onClearMentionSearchResult,
                onTypingEvent = onTypingEvent,
            )
        )
    }

    LaunchedEffect(draftMessageComposition.draftText) {
        if (draftMessageComposition.draftText.isNotBlank()) {
            messageTextState.setTextAndPlaceCursorAtEnd(draftMessageComposition.draftText)
        }

        if (draftMessageComposition.selectedMentions.isNotEmpty()) {
            messageCompositionHolder.value.setMentions(
                draftMessageComposition.draftText,
                draftMessageComposition.selectedMentions.map { it.intoMessageMention() }
            )
        }
    }

    LaunchedEffect(Unit) {
        messageCompositionHolder.value.handleMessageTextUpdates()
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember {
        FocusRequester()
    }

    val messageCompositionInputStateHolder = rememberSaveable(
        saver = MessageCompositionInputStateHolder.saver(
            messageTextState = messageTextState,
            keyboardController = keyboardController,
            focusRequester = focusRequester,
            density = density
        )
    ) {
        MessageCompositionInputStateHolder(
            messageTextState = messageTextState,
            keyboardController = keyboardController,
            focusRequester = focusRequester
        )
    }

    val additionalOptionStateHolder = rememberSaveable(
        saver = AdditionalOptionStateHolder.saver()
    ) {
        AdditionalOptionStateHolder()
    }

    return remember {
        MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            messageCompositionInputStateHolder = messageCompositionInputStateHolder,
            messageCompositionHolder = messageCompositionHolder,
            additionalOptionStateHolder = additionalOptionStateHolder,
        )
    }
}

/**
 * Class holding the whole UI state for the MessageComposer, this is the class that is used by the out-side world to give the control
 * of the state to the parent Composables
 */
class MessageComposerStateHolder(
    val messageComposerViewState: State<MessageComposerViewState>,
    val messageCompositionInputStateHolder: MessageCompositionInputStateHolder,
    val messageCompositionHolder: State<MessageCompositionHolder>,
    val additionalOptionStateHolder: AdditionalOptionStateHolder,
) {
    val messageComposition = messageCompositionHolder.value.messageComposition

    fun toEdit(messageId: String, editMessageText: String, mentions: List<MessageMention>) {
        messageCompositionHolder.value.setEditText(messageId, editMessageText, mentions)
        messageCompositionInputStateHolder.toEdit(editMessageText)
    }

    fun toReply(message: UIMessage.Regular) {
        messageCompositionHolder.value.setReply(message)
        messageCompositionInputStateHolder.toComposing()
    }

    fun onInputFocused() {
        additionalOptionStateHolder.unselectAdditionalOptionsMenu()
        messageCompositionInputStateHolder.setFocused()
    }

    fun toAudioRecording() {
        additionalOptionStateHolder.toAudioRecording()
    }

    fun toInitialAttachmentOptions() {
        additionalOptionStateHolder.toInitialAttachmentOptionsMenu()
    }

    fun cancelEdit() {
        messageCompositionInputStateHolder.toComposing()
        messageCompositionHolder.value.clearMessage()
    }

    fun showAttachments(showOptions: Boolean) {
        messageCompositionInputStateHolder.showAttachments(showOptions)
    }

    fun clearMessage() {
        messageCompositionHolder.value.clearMessage()
    }
}
