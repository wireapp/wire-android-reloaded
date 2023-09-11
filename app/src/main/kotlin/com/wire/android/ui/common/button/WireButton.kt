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

package com.wire.android.ui.common.button

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.Tint
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.rememberClickBlockAction
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import java.lang.Integer.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireButton(
    onClick: () -> Unit,
    loading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    leadingIconAlignment: IconAlignment = IconAlignment.Center,
    trailingIcon: @Composable (() -> Unit)? = null,
    trailingIconAlignment: IconAlignment = IconAlignment.Border,
    text: String? = null,
    fillMaxWidth: Boolean = true,
    textStyle: TextStyle = if (fillMaxWidth) MaterialTheme.wireTypography.button02 else MaterialTheme.wireTypography.button03,
    state: WireButtonState = WireButtonState.Default,
    clickBlockParams: ClickBlockParams = ClickBlockParams(),
    minSize: DpSize = MaterialTheme.wireDimensions.buttonMinSize,
    minClickableSize: DpSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
    shape: Shape = RoundedCornerShape(MaterialTheme.wireDimensions.buttonCornerSize),
    colors: WireButtonColors = wirePrimaryButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    borderWidth: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = MaterialTheme.wireDimensions.buttonHorizontalContentPadding,
        vertical = MaterialTheme.wireDimensions.buttonVerticalContentPadding
    ),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    modifier: Modifier = Modifier,
) {
    val border =
        if (borderWidth > 0.dp) BorderStroke(
            width = borderWidth,
            color = colors.outlineColor(state, interactionSource).value
        )
        else null
    val baseColors = ButtonDefaults.buttonColors(
        containerColor = colors.containerColor(state, interactionSource).value,
        contentColor = colors.rippleColor(), // actual content color is set directly for the children, here it's only used for the ripple
        disabledContainerColor = colors.containerColor(state, interactionSource).value,
        disabledContentColor = colors.rippleColor(),
    )
    val onClickWithSyncObserver = rememberClickBlockAction(clickBlockParams, onClick)
    var currentSize by remember { mutableStateOf(minSize) }
    val currentPadding by remember {
        derivedStateOf {
            PaddingValues(
                horizontal = max(0.dp, (minClickableSize.width - currentSize.width) / 2),
                vertical = max(0.dp, (minClickableSize.height - currentSize.height) / 2),
            )
        }
    }
    val density = LocalDensity.current
    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
        Button(
            onClick = onClickWithSyncObserver,
            modifier = modifier
                .let { if (fillMaxWidth) it.fillMaxWidth() else it.wrapContentWidth() }
                .padding(currentPadding)
                .sizeIn(minHeight = minSize.height, minWidth = minSize.width)
                .onGloballyPositioned { with(density) { currentSize = DpSize(it.size.width.toDp(), it.size.height.toDp()) } },
            enabled = state != WireButtonState.Disabled,
            interactionSource = interactionSource,
            elevation = elevation,
            shape = shape,
            border = border,
            colors = baseColors,
            contentPadding = contentPadding
        ) {
            InnerButtonBox(
                fillMaxWidth = fillMaxWidth,
                loading = loading,
                leadingIcon = leadingIcon,
                leadingIconAlignment = leadingIconAlignment,
                trailingIcon = trailingIcon,
                trailingIconAlignment = trailingIconAlignment,
                text = text,
                textStyle = textStyle,
                state = state,
                colors = colors,
                interactionSource = interactionSource
            )
        }
    }
}

@Composable
private fun InnerButtonBox(
    fillMaxWidth: Boolean = true,
    loading: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    leadingIconAlignment: IconAlignment = IconAlignment.Center,
    trailingIcon: @Composable (() -> Unit)? = null,
    trailingIconAlignment: IconAlignment = IconAlignment.Border,
    text: String? = null,
    textStyle: TextStyle = MaterialTheme.wireTypography.button03,
    state: WireButtonState = WireButtonState.Default,
    colors: WireButtonColors = wirePrimaryButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val contentColor = colors.contentColor(state, interactionSource).value
    val leadingItem: (@Composable () -> Unit) = { leadingIcon?.let { Tint(contentColor = contentColor, content = it) } }
    val trailingItem: (@Composable () -> Unit) = {
        Crossfade(targetState = trailingIcon to loading) { (trailingIcon, loading) ->
            when {
                loading -> WireCircularProgressIndicator(progressColor = contentColor)
                trailingIcon != null -> Tint(contentColor = contentColor, content = trailingIcon)
            }
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth(),
    ) {
        var startItemWidth by remember { mutableStateOf(0) }
        var endItemWidth by remember { mutableStateOf(0) }
        val borderItemsMaxWidth = with(LocalDensity.current) { max(startItemWidth, endItemWidth).toDp() }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .onGloballyPositioned { startItemWidth = it.size.width },
        ) { if (leadingIconAlignment == IconAlignment.Border) leadingItem() }

        Row(
            modifier = Modifier
                .padding(horizontal = borderItemsMaxWidth)
                .let {
                    if (fillMaxWidth) it.fillMaxWidth()
                    else it.wrapContentWidth()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIconAlignment == IconAlignment.Center) leadingItem()
            if (!text.isNullOrEmpty()) {
                Text(
                    text = text,
                    style = textStyle,
                    color = contentColor
                )
            }
            if (trailingIconAlignment == IconAlignment.Center) trailingItem()
        }

        Box(modifier = Modifier
            .align(Alignment.CenterEnd)
            .onGloballyPositioned { endItemWidth = it.size.width }
        ) { if (trailingIconAlignment == IconAlignment.Border) trailingItem() }
    }
}

@Composable
fun getMinTouchMargins(minSize: DpSize) = PaddingValues(
    horizontal = (LocalViewConfiguration.current.minimumTouchTargetSize.width - minSize.width) / 2,
    vertical = (LocalViewConfiguration.current.minimumTouchTargetSize.height - minSize.height) / 2
)

enum class IconAlignment { Border, Center }
