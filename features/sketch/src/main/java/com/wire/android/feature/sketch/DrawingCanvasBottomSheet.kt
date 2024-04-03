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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wire.android.feature.sketch.config.DrawingViewModelFactory
import com.wire.android.feature.sketch.tools.DrawingToolsConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasBottomSheet(
    drawingToolsConfig: DrawingToolsConfig,
    onDismissSketch: () -> Unit,
    onSendSketch: (Uri) -> Unit,
    tempWritableImageUri: Uri?,
    viewModel: DrawingCanvasViewModel = viewModel(factory = DrawingViewModelFactory(drawingToolsConfig)),
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        dragHandle = {
            Row(
                Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val scope = rememberCoroutineScope()
                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissSketch() }
                    },
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(
                            com.google.android.material.R.string.mtrl_picker_cancel
                        )
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch { onSendSketch(viewModel.saveImage(context, tempWritableImageUri)) }
                    },
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = stringResource(
                            com.google.android.material.R.string.mtrl_picker_cancel
                        )
                    )
                }

            }
        },
        sheetState = sheetState,
        onDismissRequest = onDismissSketch
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
}
