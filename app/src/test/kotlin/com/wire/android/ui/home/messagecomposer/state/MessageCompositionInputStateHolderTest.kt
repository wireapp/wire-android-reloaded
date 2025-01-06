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

@file:Suppress("MaxLineLength")

package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class MessageCompositionInputStateHolderTest {

    @Test
    fun `given source and target differ and target is zero when IME offset changes then options height resets and input focus is lost`() =
        runTest {
            // Given
            val (state, _) = Arrangement().arrange()
            val initialHeight = 30.dp
            state.updateValuesForTesting(keyboardHeight = initialHeight)
            val sourceHeight = 50.dp
            val targetHeight = 0.dp

            // When
            state.handleImeOffsetChange(40.dp, 0.dp, sourceHeight, targetHeight)
            advanceUntilIdle()

            // Then
            state.optionsHeight shouldBeEqualTo initialHeight
            state.inputFocused shouldBeEqualTo false
            state.isTextExpanded shouldBeEqualTo false
        }

    @Test
    fun `given initial keyboard height is non-zero when IME offset increases then keyboard height does not update but options height updates`() =
        runTest {
            // Given
            val (state, _) = Arrangement().arrange()
            val initialHeight = 30.dp
            val newHeight = 50.dp
            state.updateValuesForTesting(keyboardHeight = initialHeight)

            // When
            state.handleImeOffsetChange(newHeight, 0.dp, newHeight, newHeight)

            // Then
            state.keyboardHeight shouldBeEqualTo initialHeight
            state.optionsHeight shouldBeEqualTo newHeight
        }

    @Test
    fun `given initial keyboard height when keyboard height changes then both keyboardHeight and optionsHeight update correctly`() =
        runTest {
            // Given
            val (state, _) = Arrangement().arrange()
            val newOffset = 100.dp

            // When
            state.handleImeOffsetChange(newOffset, 0.dp, newOffset, newOffset)

            // Then
            state.keyboardHeight shouldBeEqualTo newOffset
            state.optionsHeight shouldBeEqualTo newOffset
        }

    @Test
    fun `given text is not blank when transitioning to composing state then send button is enabled`() = runTest {
        // Given
        val (state, _) = Arrangement().withText("Hello World").arrange()

        // When
        state.toComposing()

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Composing>().isSendButtonEnabled shouldBeEqualTo true
    }

    @Test
    fun `given text has changed and is not blank when transitioning to editing state then edit button is enabled`() = runTest {
        // Given
        val initialText = "Hello"
        val newText = "Hello World"
        val (state, _) = Arrangement().withText(initialText).arrange()
        state.toEdit(newText)

        // When
        val result = state.inputType as InputType.Editing

        // Then
        result.isEditButtonEnabled shouldBeEqualTo true
    }

    @Test
    fun `given text size toggle is activated when toggling text size then isTextExpanded state changes correctly`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()

        // When & Then
        state.toggleInputSize()
        state.isTextExpanded shouldBeEqualTo true
        state.toggleInputSize()
        state.isTextExpanded shouldBeEqualTo false
    }

    @Test
    fun `given text is expanded when collapsing text then isTextExpanded resets to false`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.toggleInputSize()

        // When
        state.collapseText()

        // Then
        state.isTextExpanded shouldBeEqualTo false
    }

    @Test
    fun `given keyboard is focused when setting focus then inputFocused is true and keyboard shows`() = runTest {
        // Given
        val (state, arrangement) = Arrangement().arrange()

        // When
        state.setFocused()

        // Then
        state.inputFocused shouldBeEqualTo true
        verify(exactly = 1) {
            arrangement.softwareKeyboardController.show()
        }
    }

    @Test
    fun `given options are visible when focus is requested then options remain visible and inputFocused is true`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.showAttachments(true)

        // When
        state.requestFocus()

        // Then
        state.inputFocused shouldBeEqualTo true
        state.optionsVisible shouldBeEqualTo true
    }

    @Test
    fun `given text is initially blank when transitioning to composing state then send button is disabled`() = runTest {
        // Given
        val (state, _) = Arrangement().withText("").arrange()

        // When
        state.toComposing()

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Composing>().isSendButtonEnabled shouldBeEqualTo false
    }

    @Test
    fun `given unchanged text when editing then edit button is disabled`() = runTest {
        // Given
        val messageText = "Hello"
        val (state, _) = Arrangement().withText(messageText).arrange()

        // When
        state.toEdit(messageText)

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Editing>().isEditButtonEnabled shouldBeEqualTo false
    }

    @Test
    fun `given additional space is added to the keyboard when handling IME offset change then options height adjusts but keyboard height remains`() =
        runTest {
            // Given
            val (state, _) = Arrangement().arrange()
            state.updateValuesForTesting(keyboardHeight = 20.dp)

            // When
            state.handleImeOffsetChange(50.dp, 0.dp, 50.dp, 50.dp)

            // Then
            state.keyboardHeight shouldBeEqualTo 20.dp
            state.optionsHeight shouldBeEqualTo 50.dp
        }

    @Test
    fun `given keyboard is visible when keyboard is hidden then reset keyboard and options height`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.updateValuesForTesting(keyboardHeight = 30.dp, optionsHeight = 30.dp)

        // When
        state.handleImeOffsetChange(0.dp, 0.dp, 30.dp, 0.dp)

        // Then
        state.keyboardHeight shouldBeEqualTo 30.dp
        state.optionsHeight shouldBeEqualTo 30.dp
        state.inputFocused shouldBeEqualTo false
    }

    class Arrangement {

        private val textFieldState = TextFieldState()

        val softwareKeyboardController = mockk<SoftwareKeyboardController>()

        private val focusRequester = mockk<FocusRequester>()

        private val state by lazy {
            MessageCompositionInputStateHolder(textFieldState, softwareKeyboardController, focusRequester)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { focusRequester.requestFocus() } returns Unit
            every { focusRequester.captureFocus() } returns true
            every { softwareKeyboardController.show() } returns Unit
        }

        fun withText(text: String) = apply {
            textFieldState.setTextAndPlaceCursorAtEnd(text)
        }

        fun arrange() = state to this
    }
}
