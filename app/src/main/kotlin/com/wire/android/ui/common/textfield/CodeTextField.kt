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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun CodeTextField(
    textState: TextFieldState,
    modifier: Modifier = Modifier,
    codeLength: Int = integerResource(id = R.integer.code_length),
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.corner4x),
    colors: WireTextFieldColors = wireTextFieldColors(),
    textStyle: TextStyle = MaterialTheme.wireTypography.code01,
    state: WireTextFieldState = WireTextFieldState.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    maxHorizontalSpacing: Dp = MaterialTheme.wireDimensions.spacing16x,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val enabled = state !is WireTextFieldState.Disabled
    CodeTextFieldLayout(
        textState = textState,
        codeLength = codeLength,
        shape = shape,
        colors = colors,
        textStyle = textStyle,
        state = state,
        maxHorizontalSpacing = maxHorizontalSpacing,
        horizontalAlignment = horizontalAlignment,
        modifier = modifier,
        innerBasicTextField = { decorator, textFieldModifier ->
            BasicTextField(
                state = textState,
                textStyle = textStyle,
                enabled = enabled,
                keyboardOptions = KeyboardOptions.DefaultCode,
                onKeyboardAction = { keyboardController?.hide() },
                interactionSource = interactionSource,
                inputTransformation = InputTransformation.maxLengthDigits(codeLength),
                decorator = decorator,
                modifier = textFieldModifier,
            )
        }
    )
}

@Stable
val KeyboardOptions.Companion.DefaultCode: KeyboardOptions
    get() = Default.copy(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done,
        autoCorrectEnabled = false,
    )

@PreviewMultipleThemes
@Composable
fun PreviewCodeTextFieldSuccess() = WireTheme {
    CodeTextField(textState = rememberTextFieldState("123"))
}

@PreviewMultipleThemes
@Composable
fun PreviewCodeTextFieldError() = WireTheme {
    CodeTextField(textState = rememberTextFieldState("123"), state = WireTextFieldState.Error("error text"))
}
