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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.Tint
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.EMPTY
import io.github.esentsov.PackagePrivate

/**
 * Priority in which fields are used for SemanticContentDescription:
 * [semanticDescription] -> [labelText] -> [placeholderText] -> [descriptionText].
 * If you need to make empty SemanticContentDescription (which is definitely bad idea for TextView)
 * set [semanticDescription] = ""
 */
@PackagePrivate
@Composable
internal fun WireTextFieldLayout(
    shouldShowPlaceholder: Boolean,
    innerBasicTextField: InnerBasicTextFieldBuilder,
    modifier: Modifier = Modifier,
    placeholderText: String? = null,
    labelText: String? = null,
    labelMandatoryIcon: Boolean = false,
    descriptionText: String? = null,
    semanticDescription: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    state: WireTextFieldState = WireTextFieldState.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderAlignment: Alignment.Horizontal = Alignment.Start,
    inputMinHeight: Dp = MaterialTheme.wireDimensions.textFieldMinHeight,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.textFieldCornerSize),
    colors: WireTextFieldColors = wireTextFieldColors(),
    onTap: ((Offset) -> Unit)? = null,
    testTag: String = String.EMPTY
) {
    Column(modifier = modifier) {
        if (labelText != null) {
            WireLabel(
                labelText = labelText,
                labelMandatoryIcon = labelMandatoryIcon,
                state = state,
                interactionSource = interactionSource,
                colors = colors
            )
        }
        innerBasicTextField.Build(
            decorator = { innerTextField ->
                InnerTextLayout(
                    innerTextField = innerTextField,
                    shouldShowPlaceholder = shouldShowPlaceholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    placeholderText = placeholderText,
                    style = state,
                    placeholderTextStyle = placeholderTextStyle,
                    placeholderAlignment = placeholderAlignment,
                    inputMinHeight = inputMinHeight,
                    colors = colors,
                    onTap = onTap,
                )
            },
            textFieldModifier = Modifier
                .fillMaxWidth()
                .background(color = colors.backgroundColor(state).value, shape = shape)
                .border(width = dimensions().spacing1x, color = colors.borderColor(state, interactionSource).value, shape = shape)
                .semantics(mergeDescendants = true) {
                    (semanticDescription ?: labelText ?: placeholderText ?: descriptionText)?.let {
                        contentDescription = it
                    }
                }
                .testTag(testTag)
        )

        val bottomText = when {
            state is WireTextFieldState.Error && state.errorText != null -> state.errorText
            !descriptionText.isNullOrEmpty() -> descriptionText
            else -> String.EMPTY
        }
        AnimatedVisibility(visible = bottomText.isNotEmpty()) {
            Text(
                text = bottomText,
                style = MaterialTheme.wireTypography.label04,
                textAlign = TextAlign.Start,
                color = colors.descriptionColor(state).value,
                modifier = Modifier.padding(top = dimensions().spacing4x)
            )
        }
    }
}

@Composable
private fun InnerTextLayout(
    innerTextField: @Composable () -> Unit,
    shouldShowPlaceholder: Boolean,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholderText: String? = null,
    style: WireTextFieldState = WireTextFieldState.Default,
    placeholderTextStyle: TextStyle = MaterialTheme.wireTypography.body01,
    placeholderAlignment: Alignment.Horizontal = Alignment.Start,
    inputMinHeight: Dp = dimensions().spacing48x,
    colors: WireTextFieldColors = wireTextFieldColors(),
    onTap: ((Offset) -> Unit)? = null
) {
    val modifier: Modifier = Modifier.apply {
        if (onTap != null) {
            pointerInput(Unit) {
                detectTapGestures(onTap = onTap)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .heightIn(min = inputMinHeight)
    ) {
        val trailingOrStateIcon: @Composable (() -> Unit)? = when {
            trailingIcon != null -> trailingIcon
            else -> style.icon()?.Icon(Modifier.padding(horizontal = dimensions().spacing16x))
        }
        if (leadingIcon != null) {
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(style).value, content = leadingIcon)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = if (leadingIcon == null) dimensions().spacing16x else dimensions().spacing0x,
                    end = if (trailingOrStateIcon == null) dimensions().spacing16x else dimensions().spacing0x,
                    top = dimensions().spacing2x,
                    bottom = dimensions().spacing2x,
                )
        ) {
            if (shouldShowPlaceholder && placeholderText != null) {
                Text(
                    text = placeholderText,
                    style = placeholderTextStyle,
                    color = colors.placeholderColor(style).value,
                    modifier = Modifier
                        .align(placeholderAlignment.toAlignment())
                        .clearAndSetSemantics {}
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth(),
                propagateMinConstraints = true
            ) {
                innerTextField()
            }
        }
        if (trailingOrStateIcon != null) {
            Box(contentAlignment = Alignment.Center) {
                Tint(contentColor = colors.iconColor(style).value, content = trailingOrStateIcon)
            }
        }
    }
}

private fun Alignment.Horizontal.toAlignment(): Alignment = Alignment { size, space, layoutDirection ->
    IntOffset(this@toAlignment.align(size.width, space.width, layoutDirection), 0)
}

fun interface InnerBasicTextFieldBuilder {
    @Composable
    fun Build(decorator: TextFieldDecorator, textFieldModifier: Modifier)
}
