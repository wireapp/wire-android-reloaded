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
package com.wire.android.feature.sketch

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wire.android.feature.sketch.model.DrawingState
import com.wire.android.model.ClickBlockParams
import com.wire.android.ui.common.Icon
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryIconButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.button.WireTertiaryIconButton
import com.wire.android.ui.common.button.wireSendPrimaryButtonColors
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasBottomSheet(
    onDismissSketch: () -> Unit,
    onSendSketch: (Uri) -> Unit,
    tempWritableImageUri: Uri?,
    conversationTitle: String = "",
    viewModel: DrawingCanvasViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { false })
    ModalBottomSheet(
        shape = CutCornerShape(dimensions().spacing0x),
        containerColor = colorsScheme().background,
        dragHandle = {
            DrawingTopBar(scope, sheetState, conversationTitle, onDismissSketch, viewModel::onUndoLastStroke, viewModel.state)
        },
        sheetState = sheetState,
        onDismissRequest = onDismissSketch
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(dimensions().spacing80x)
                .weight(weight = 1f, fill = true)
        ) {
            DrawingCanvasComponent(
                state = viewModel.state,
                onStartDrawingEvent = viewModel::onStartDrawingEvent,
                onDrawEvent = viewModel::onDrawEvent,
                onStopDrawingEvent = viewModel::onStopDrawingEvent,
                onSizeChanged = viewModel::onSizeChanged,
                onStartDrawing = viewModel::onStartDrawing,
                onDraw = viewModel::onDraw,
                onStopDrawing = viewModel::onStopDrawing
            )
        }
        DrawingToolbar(
            state = viewModel.state,
            onSendSketch = {
                scope.launch { onSendSketch(viewModel.saveImage(context, tempWritableImageUri)) }
                    .invokeOnCompletion { scope.launch { sheetState.hide() } }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawingTopBar(
    scope: CoroutineScope = rememberCoroutineScope(),
    sheetState: SheetState,
    conversationTitle: String,
    onDismissSketch: () -> Unit,
    onUndoStroke: () -> Unit,
    state: DrawingState
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensions().spacing8x),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        WireTertiaryIconButton(
            onButtonClicked = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissSketch() } },
            iconResource = R.drawable.ic_close,
            contentDescription = R.string.content_description_close_button,
            minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        )
        Text(
            text = conversationTitle,
            style = MaterialTheme.wireTypography.title01,
            modifier = Modifier.align(Alignment.CenterVertically),
            maxLines = MAX_LINES_TOPBAR,
            overflow = TextOverflow.Ellipsis
        )
        WireSecondaryIconButton(
            onButtonClicked = onUndoStroke,
            iconResource = R.drawable.ic_undo,
            contentDescription = R.string.content_description_undo_button,
            state = if (state.paths.isNotEmpty()) WireButtonState.Default else WireButtonState.Disabled,
            minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        )
    }
}

@Composable
private fun DrawingToolbar(
    state: DrawingState,
    onSendSketch: () -> Unit = {},
) {
    var showToolSelection by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxHeight()
            .padding(horizontal = dimensions().spacing8x)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val colorPickerEnabled = false // enable when implemented
        if (colorPickerEnabled) {
            WireSecondaryButton(
                onClick = { showToolSelection = !showToolSelection },
                leadingIcon = Icons.Default.Circle.Icon(),
                leadingIconAlignment = IconAlignment.Center,
                fillMaxWidth = false,
                minSize = dimensions().buttonSmallMinSize,
                minClickableSize = dimensions().buttonMinClickableSize,
                shape = RoundedCornerShape(dimensions().spacing12x),
                contentPadding = PaddingValues(horizontal = dimensions().spacing8x, vertical = dimensions().spacing4x)
            )
        }
        Spacer(Modifier.size(dimensions().spacing2x))
        WirePrimaryIconButton(
            onButtonClicked = onSendSketch,
            iconResource = R.drawable.ic_send,
            contentDescription = R.string.content_description_send_button,
            state = if (state.paths.isNotEmpty()) WireButtonState.Default else WireButtonState.Disabled,
            shape = RoundedCornerShape(dimensions().spacing20x),
            colors = wireSendPrimaryButtonColors(),
            clickBlockParams = ClickBlockParams(blockWhenSyncing = true, blockWhenConnecting = true),
            minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
            minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolPicker() {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        shape = CutCornerShape(dimensions().spacing0x),
        containerColor = colorsScheme().background,
        sheetState = sheetState,
        onDismissRequest = { scope.launch { sheetState.hide() } }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = "Tool Picker here")
        }
    }
}

private const val MAX_LINES_TOPBAR = 1
