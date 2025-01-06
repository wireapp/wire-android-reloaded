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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun WirePasswordTextField(
    textState: TextFieldState,
    modifier: Modifier = Modifier,
    placeholderText: String? = stringResource(R.string.login_password_placeholder),
    labelText: String? = stringResource(R.string.login_password_label),
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    semanticDescription: String? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    autoFill: Boolean = false,
    inputTransformation: InputTransformation = InputTransformation.maxLength(8000),
    keyboardOptions: KeyboardOptions = KeyboardOptions.DefaultPassword,
    onKeyboardAction: KeyboardActionHandler? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = MaterialTheme.wireTypography.body01.copy(textAlign = TextAlign.Start),
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01.copy(textAlign = TextAlign.Start),
    placeholderAlignment: Alignment.Horizontal = Alignment.Start,
    inputMinHeight: Dp = MaterialTheme.wireDimensions.textFieldMinHeight,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    onTap: ((Offset) -> Unit)? = null,
    testTag: String = String.EMPTY
) {
    val autoFillType = if (autoFill) WireAutoFillType.Password else WireAutoFillType.None
    var passwordVisibility by remember { mutableStateOf(false) }
    WireTextFieldLayout(
        shouldShowPlaceholder = textState.text.isEmpty(),
        placeholderText = placeholderText,
        labelText = labelText,
        labelMandatoryIcon = labelMandatoryIcon,
        descriptionText = descriptionText,
        semanticDescription = semanticDescription,
        state = state,
        interactionSource = interactionSource,
        placeholderTextStyle = placeholderTextStyle,
        placeholderAlignment = placeholderAlignment,
        inputMinHeight = inputMinHeight,
        shape = shape,
        colors = colors,
        trailingIcon = { VisibilityIconButton(passwordVisibility) { passwordVisibility = it } },
        modifier = modifier.then(autoFillModifier(autoFillType, textState::setTextAndPlaceCursorAtEnd)),
        testTag = testTag,
        onTap = onTap,
        innerBasicTextField = { decorator, textFieldModifier ->
            BasicSecureTextField(
                state = textState,
                textStyle = textStyle.copy(color = colors.textColor(state = state).value, textDirection = TextDirection.ContentOrLtr),
                keyboardOptions = keyboardOptions,
                onKeyboardAction = onKeyboardAction,
                inputTransformation = inputTransformation,
                textObfuscationMode = if (passwordVisibility) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                enabled = state !is WireTextFieldState.Disabled,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
                modifier = textFieldModifier,
                decorator = decorator,
            )
        }
    )
}

@Composable
private fun VisibilityIconButton(isVisible: Boolean, onVisibleChange: (Boolean) -> Unit) {
    IconButton(onClick = { onVisibleChange(!isVisible) }) {
        Icon(
            imageVector = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
            contentDescription = stringResource(
                if (isVisible) R.string.content_description_hide_password
                else R.string.content_description_reveal_password
            ),
            modifier = Modifier
                .size(dimensions().spacing20x)
                .testTag("hidePassword")
        )
    }
}

@Stable
val KeyboardOptions.Companion.DefaultPassword: KeyboardOptions
    get() = Default.copy(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        autoCorrectEnabled = false,
        capitalization = KeyboardCapitalization.None
    )

@PreviewMultipleThemes
@Composable
fun PreviewWirePasswordTextField() = WireTheme {
    WirePasswordTextField(
        textState = rememberTextFieldState(),
        modifier = Modifier.padding(16.dp)
    )
}
