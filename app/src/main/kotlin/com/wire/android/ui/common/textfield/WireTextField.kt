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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
internal fun WireTextField(
    textState: TextFieldState,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    labelText: String? = null,
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    semanticDescription: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    autoFillType: WireAutoFillType = WireAutoFillType.None,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    inputTransformation: InputTransformation = InputTransformation.maxLength(8000),
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.DefaultText,
    onKeyboardAction: KeyboardActionHandler? = null,
    scrollState: ScrollState = rememberScrollState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderAlignment: Alignment.Horizontal = Alignment.Start,
    inputMinHeight: Dp = MaterialTheme.wireDimensions.textFieldMinHeight,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
    onTap: ((Offset) -> Unit)? = null,
    testTag: String = String.EMPTY,
    validateKeyboardOptions: Boolean = true,
) {
    if (validateKeyboardOptions) {
        assert(
            keyboardOptions.keyboardType != KeyboardType.Email ||
                    keyboardOptions == KeyboardOptions.DefaultEmailDone ||
                    keyboardOptions == KeyboardOptions.DefaultEmailNext
        ) {
            "For email text fields use KeyboardOptions.DefaultEmailDone or KeyboardOptions.DefaultEmailNext. " +
                    "If you want to use a custom KeyboardOptions, set validateKeyboardOptions to false."
        }

        assert(keyboardOptions.keyboardType != KeyboardType.Password) {
            "Use WirePasswordTextField for passwords. If you want to use a custom KeyboardOptions, set validateKeyboardOptions to false."
        }
    }

    WireTextFieldLayout(
        shouldShowPlaceholder = textState.text.isEmpty(),
        placeholderText = placeholderText,
        labelText = labelText,
        labelMandatoryIcon = labelMandatoryIcon,
        descriptionText = descriptionText,
        semanticDescription = semanticDescription,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        state = state,
        interactionSource = interactionSource,
        placeholderTextStyle = placeholderTextStyle,
        placeholderAlignment = placeholderAlignment,
        inputMinHeight = inputMinHeight,
        shape = shape,
        colors = colors,
        modifier = modifier.then(
            autoFillModifier(
                autoFillType,
                textState::setTextAndPlaceCursorAtEnd
            )
        ),
        onTap = onTap,
        testTag = testTag,
        innerBasicTextField = { decorator, textFieldModifier ->
            BasicTextField(
                state = textState,
                textStyle = textStyle.copy(
                    color = colors.textColor(state = state).value,
                    textDirection = TextDirection.ContentOrLtr
                ),
                keyboardOptions = keyboardOptions,
                onKeyboardAction = onKeyboardAction,
                lineLimits = lineLimits,
                inputTransformation = inputTransformation,
                outputTransformation = outputTransformation,
                scrollState = scrollState,
                readOnly = state is WireTextFieldState.ReadOnly,
                enabled = state !is WireTextFieldState.Disabled,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
                modifier = textFieldModifier,
                decorator = decorator,
                onTextLayout = onTextLayout(
                    textState,
                    onSelectedLineIndexChanged,
                    onLineBottomYCoordinateChanged
                )
            )
        }
    )
}

private fun onTextLayout(
    state: TextFieldState,
    onSelectedLineIndexChanged: (Int) -> Unit = { },
    onLineBottomYCoordinateChanged: (Float) -> Unit = { },
): (Density.(getResult: () -> TextLayoutResult?) -> Unit) = {
    it()?.let {
        val lineOfText = it.getLineForOffset(state.selection.end)
        val bottomYCoordinate = it.getLineBottom(lineOfText)
        onSelectedLineIndexChanged(lineOfText)
        onLineBottomYCoordinateChanged(bottomYCoordinate)
    }
}

@Stable
val KeyboardOptions.Companion.DefaultText: KeyboardOptions
    get() = Default.copy(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Done,
        autoCorrectEnabled = true,
        capitalization = KeyboardCapitalization.Sentences,
    )

@Stable
val KeyboardOptions.Companion.DefaultEmailDone: KeyboardOptions
    get() = defaultEmail(ImeAction.Done)

@Stable
val KeyboardOptions.Companion.DefaultEmailNext: KeyboardOptions
    get() = defaultEmail(ImeAction.Next)

@Stable
private fun KeyboardOptions.Companion.defaultEmail(imeAction: ImeAction): KeyboardOptions {
    return Default.copy(
        keyboardType = KeyboardType.Email,
        imeAction = imeAction,
        autoCorrectEnabled = false,
        capitalization = KeyboardCapitalization.None,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireTextField() = WireTheme {
    WireTextField(
        textState = rememberTextFieldState("text"),
        modifier = Modifier.padding(16.dp)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireTextFieldLabels() = WireTheme {
    WireTextField(
        textState = rememberTextFieldState("text"),
        labelText = "label",
        labelMandatoryIcon = true,
        descriptionText = "description",
        modifier = Modifier.padding(16.dp)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireTextFieldDenseSearch() = WireTheme {
    WireTextField(
        textState = rememberTextFieldState("text"),
        placeholderText = "Search",
        leadingIcon = {
            IconButton(
                modifier = Modifier.height(40.dp),
                onClick = {}
            ) { Icon(Icons.Filled.Search, "") }
        },
        trailingIcon = {
            IconButton(
                modifier = Modifier.height(40.dp),
                onClick = {}
            ) { Icon(Icons.Filled.Close, "") }
        },
        inputMinHeight = 40.dp,
        modifier = Modifier.padding(16.dp)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireTextFieldDisabled() = WireTheme {
    WireTextField(
        textState = rememberTextFieldState("text"),
        state = WireTextFieldState.Disabled,
        modifier = Modifier.padding(16.dp)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireTextFieldError() = WireTheme {
    WireTextField(
        textState = rememberTextFieldState("text"),
        state = WireTextFieldState.Error("error"),
        modifier = Modifier.padding(16.dp)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewWireTextFieldSuccess() = WireTheme {
    WireTextField(
        textState = rememberTextFieldState("text"),
        state = WireTextFieldState.Success,
        modifier = Modifier.padding(16.dp)
    )
}
