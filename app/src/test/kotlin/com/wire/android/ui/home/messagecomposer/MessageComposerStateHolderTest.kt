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

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.framework.TestConversation
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionStateHolder
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.InputType
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputStateHolder
import com.wire.android.util.EMPTY
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class MessageComposerStateHolderTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var focusRequester: FocusRequester

    private lateinit var messageComposerViewState: MutableState<MessageComposerViewState>
    private lateinit var messageComposition: MutableState<MessageComposition>
    private lateinit var messageCompositionInputStateHolder: MessageCompositionInputStateHolder
    private lateinit var messageCompositionHolder: State<MessageCompositionHolder>
    private lateinit var additionalOptionStateHolder: AdditionalOptionStateHolder
    private lateinit var state: MessageComposerStateHolder
    private lateinit var messageTextState: TextFieldState

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { focusRequester.requestFocus() } returns Unit
        every { focusRequester.captureFocus() } returns true
        messageComposerViewState = mutableStateOf(MessageComposerViewState())
        messageComposition = mutableStateOf(MessageComposition(TestConversation.ID))
        messageTextState = TextFieldState()
        messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
            messageTextState = messageTextState,
            keyboardController = null,
            focusRequester = focusRequester
        )
        messageCompositionHolder = mutableStateOf(
            MessageCompositionHolder(
                messageComposition = messageComposition,
                messageTextState = messageTextState,
                onClearDraft = {},
                onSaveDraft = {},
                onSearchMentionQueryChanged = {},
                onClearMentionSearchResult = {},
                onTypingEvent = {},
            )
        )
        additionalOptionStateHolder = AdditionalOptionStateHolder()

        state = MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            messageCompositionInputStateHolder = messageCompositionInputStateHolder,
            messageCompositionHolder = messageCompositionHolder,
            additionalOptionStateHolder = additionalOptionStateHolder,
        )
    }

    @Test
    fun `given state, when setting toEdit, then input state toEdit is called`() = runTest {
        // given
        // when
        state.toEdit(
            messageId = "messageId",
            editMessageText = "edit_message_text",
            mentions = listOf()
        )

        // then
        assertInstanceOf(InputType.Editing::class.java, messageCompositionInputStateHolder.inputType)
    }

    @Test
    fun `given message edit, when not making any changes to message, then edit button should be disabled`() = runTest {
        state.toEdit(
            messageId = "messageId",
            editMessageText = "edit_message_text",
            mentions = listOf()
        )
        assertInstanceOf(InputType.Editing::class.java, messageCompositionInputStateHolder.inputType).also {
            assertEquals(false, it.isEditButtonEnabled)
        }
    }

    @Test
    fun `given message edit, when making some changes to message, then edit button should be enabled`() = runTest {
        state.toEdit(
            messageId = "messageId",
            editMessageText = "edit_message_text",
            mentions = listOf()
        )
        state.messageCompositionHolder.value.messageTextState.edit {
            append("some text")
        }
        assertInstanceOf(InputType.Editing::class.java, messageCompositionInputStateHolder.inputType).also {
            assertEquals(true, it.isEditButtonEnabled)
        }
    }

    @Test
    fun `given state, when setting toReply, then composition holder is correctly set up`() =
        runTest {
            // given
            // when
            state.toReply(mockMessageWithText)

            // then
            assertEquals(String.EMPTY, messageCompositionHolder.value.messageTextState.text.toString())
            assertInstanceOf(InputType.Composing::class.java, messageCompositionInputStateHolder.inputType)
        }

    @Test
    fun `given some message was being composed, when setting toReply, then input continues with the current text`() = runTest {
        // given
        val currentText = "Potato"
        messageCompositionHolder.value.messageTextState.setTextAndPlaceCursorAtEnd(currentText)

        // when
        state.toReply(mockMessageWithText)

        // then
        assertEquals(currentText, messageCompositionHolder.value.messageTextState.text.toString())
    }

    @Test
    fun `given state, when requesting to show additional options menu, then additional options menu is shown`() =
        runTest {
            // given
            // when
            state.showAttachments(true)

            // then
            assertEquals(
                AdditionalOptionSubMenuState.Default,
                additionalOptionStateHolder.additionalOptionsSubMenuState
            )
            assertEquals(false, messageCompositionInputStateHolder.inputFocused)
        }

    @Test
    fun `given state, when message is sent, then message is cleared`() = runTest {
        // given
        // when
        state.clearMessage()

        // then
        assertEquals(
            String.EMPTY,
            messageCompositionHolder.value.messageTextState.text.toString()
        )
        assertEquals(
            null,
            messageCompositionHolder.value.messageComposition.value.quotedMessage
        )
        assertEquals(
            null,
            messageCompositionHolder.value.messageComposition.value.quotedMessageId
        )
    }
}
