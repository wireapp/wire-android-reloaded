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
package com.wire.android.ui.common.textfield

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import io.github.esentsov.PackagePrivate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@PackagePrivate
@Composable
internal fun CodeTextFieldLayout(
    textState: TextFieldState,
    codeLength: Int,
    innerBasicTextField: InnerBasicTextFieldBuilder,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x),
    colors: WireTextFieldColors = wireTextFieldColors(),
    textStyle: TextStyle = MaterialTheme.wireTypography.code01,
    state: WireTextFieldState = WireTextFieldState.Default,
    maxHorizontalSpacing: Dp = MaterialTheme.wireDimensions.spacing16x,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = modifier.width(IntrinsicSize.Min),
    ) {
        innerBasicTextField.Build(
            decorator = TextFieldDecorator { innerTextField ->
                // hide the inner text field as we are dwelling the text field ourselves
                CompositionLocalProvider(LocalTextSelectionColors.provides(TextSelectionColors(Color.Transparent, Color.Transparent))) {
                    Box(modifier = Modifier.drawWithContent { }) {
                        innerTextField()
                    }
                }

                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(codeLength) { index ->
                        if (index != 0) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .width(maxHorizontalSpacing)
                            )
                        }
                        Digit(
                            char = textState.text.getOrNull(index),
                            shape = shape,
                            colors = colors,
                            textStyle = textStyle,
                            selected = index == textState.text.length,
                            state = state
                        )
                    }
                }
            },
            textFieldModifier = Modifier
        )
        val bottomText = when {
            state is WireTextFieldState.Error && state.errorText != null -> state.errorText
            else -> String.EMPTY
        }
        AnimatedVisibility(visible = bottomText.isNotEmpty()) {
            Text(
                text = bottomText,
                style = MaterialTheme.wireTypography.label04,
                color = colors.descriptionColor(state).value,
                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing4x)
            )
        }
    }
}

@Composable
private fun Digit(
    char: Char? = null,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    state: WireTextFieldState = WireTextFieldState.Default,
    selected: Boolean = false
) {
    val interactionSource = object : InteractionSource {
        private val focusInteraction: FocusInteraction.Focus = FocusInteraction.Focus()
        override val interactions: Flow<Interaction> = flow {
            emit(if (selected) focusInteraction else FocusInteraction.Unfocus(focusInteraction))
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(color = colors.backgroundColor(state).value, shape = shape)
            .border(width = dimensions().spacing1x, color = colors.borderColor(state, interactionSource).value, shape = shape)
            .size(width = MaterialTheme.wireDimensions.codeFieldItemWidth, height = MaterialTheme.wireDimensions.codeFieldItemHeight)
    ) {
        Text(
            text = char?.toString() ?: "",
            color = colors.textColor(state = state).value,
            style = textStyle,
            textAlign = TextAlign.Center,
        )
    }
}
