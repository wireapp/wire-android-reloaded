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
package com.wire.android.ui.calling.ongoing.fullscreen

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.calling.CallState
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.OngoingCallViewModel.Companion.DOUBLE_TAP_TOAST_DISPLAY_TIME
import com.wire.android.ui.calling.ongoing.buildPreviewParticipantsList
import com.wire.android.ui.calling.ongoing.participantsview.ParticipantTile
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.collections.immutable.PersistentList
import kotlinx.coroutines.delay

@Composable
fun FullScreenTile(
    callState: CallState,
    participants: PersistentList<UICallParticipant>,
    selectedParticipant: SelectedParticipant,
    height: Dp,
    closeFullScreen: (offset: Offset) -> Unit,
    onBackButtonClicked: () -> Unit,
    setVideoPreview: (View) -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit,
    clearVideoPreview: () -> Unit,
    isOnFrontCamera: Boolean,
    flipCamera: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = dimensions().spacing4x,
) {
    var shouldShowDoubleTapToast by remember { mutableStateOf(false) }

    BackHandler {
        onBackButtonClicked()
    }

    participants.find {
        it.id == selectedParticipant.userId && it.clientId == selectedParticipant.clientId
    }?.let {
        Box(modifier = modifier) {
            ParticipantTile(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = closeFullScreen
                        )
                    }
                    .height(height)
                    .padding(contentPadding),
                participantTitleState = it,
                isSelfUserCameraOn = if (selectedParticipant.isSelfUser) {
                    callState.isCameraOn
                } else {
                    it.isCameraOn
                },
                isSelfUserMuted = if (selectedParticipant.isSelfUser) {
                    callState.isMuted!!
                } else {
                    it.isMuted
                },
                shouldFillOthersVideoPreview = false,
                isZoomingEnabled = true,
                onSelfUserVideoPreviewCreated = setVideoPreview,
                onClearSelfUserVideoPreview = clearVideoPreview,
                isOnFrontCamera = isOnFrontCamera,
                flipCamera = flipCamera,
            )
            LaunchedEffect(Unit) {
                delay(200)
                shouldShowDoubleTapToast = true

                delay(DOUBLE_TAP_TOAST_DISPLAY_TIME)
                shouldShowDoubleTapToast = false
            }
            DoubleTapToast(
                modifier = Modifier
                    .align(Alignment.TopCenter),
                enabled = shouldShowDoubleTapToast,
                text = stringResource(id = R.string.calling_ongoing_double_tap_to_go_back),
                onTap = {
                    shouldShowDoubleTapToast = false
                }
            )
        }

        LaunchedEffect(selectedParticipant.userId) {
            requestVideoStreams(listOf(it))
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewFullScreenTile() = WireTheme {
    val participants = buildPreviewParticipantsList(1)
    FullScreenTile(
        callState = CallState(
            conversationId = ConversationId("id", "domain"),
        ),
        selectedParticipant = SelectedParticipant(
            userId = participants.first().id,
            clientId = participants.first().clientId,
            isSelfUser = false,
        ),
        height = 800.dp,
        closeFullScreen = {},
        onBackButtonClicked = {},
        setVideoPreview = {},
        requestVideoStreams = {},
        clearVideoPreview = {},
        participants = participants,
        isOnFrontCamera = false,
        flipCamera = {},
    )
}
