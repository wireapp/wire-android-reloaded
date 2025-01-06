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

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import com.wire.android.config.SnapshotExtension
import com.wire.android.framework.TestConversation
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SnapshotExtension::class)
class MessageCompositionHolderTest {

    @MockK
    lateinit var context: Context

    private lateinit var state: MessageCompositionHolder

    private lateinit var messageComposition: MutableState<MessageComposition>
    private lateinit var messageTextState: TextFieldState
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(dispatcher)

        messageComposition = mutableStateOf(MessageComposition(TestConversation.ID))
        messageTextState = TextFieldState()
        state = MessageCompositionHolder(
            messageComposition = messageComposition,
            messageTextState = messageTextState,
            onClearDraft = {},
            onSaveDraft = {},
            onSearchMentionQueryChanged = {},
            onClearMentionSearchResult = {},
            onTypingEvent = {},
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given empty text, when adding header markdown, then # is added to the text`() = runTest {
        // given
        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Header)

        // then
        assertEquals(
            "# ",
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given empty text, when adding bold markdown, then 2x star char is added to the text`() = runTest {
        // given
        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Bold)

        // then
        assertEquals(
            "****",
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given empty text, when adding italic markdown, then 2x _ is added to the text`() = runTest {
        // given
        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Italic)

        // then
        assertEquals(
            "__",
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given non empty text, when adding header markdown on selection, then # is added to the text`() = runTest {
        // given
        state.messageTextState.edit {
            replace(0, length, "header")
            selection = TextRange(
                start = 0,
                end = 6
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Header)

        // then
        assertEquals(
            "# header",
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given non empty text, when adding bold markdown on selection, then 2x star char is added to the text`() = runTest {
        // given
        state.messageTextState.edit {
            replace(0, length, "bold")
            selection = TextRange(
                start = 0,
                end = 4
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Bold)

        // then
        assertEquals(
            "**bold**",
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given non empty text, when adding italic markdown on selection, then 2x _ is added to the text`() = runTest {
        // given
        state.messageTextState.edit {
            replace(0, length, "italic")
            selection = TextRange(
                start = 0,
                end = 6
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Italic)

        // then
        assertEquals(
            "_italic_",
            state.messageTextState.text.toString()
        )
    }
}
