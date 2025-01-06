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

package com.wire.android.ui.calling.ongoing.participantsview.horizentalview

import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.buildPreviewParticipantsList
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.calling.ongoing.participantsview.ParticipantTile
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId

@Composable
fun CallingHorizontalView(
    participants: List<UICallParticipant>,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    contentHeight: Dp,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    recentReactions: Map<UserId, String>,
    onDoubleTap: (selectedParticipant: SelectedParticipant) -> Unit,
    isOnFrontCamera: Boolean,
    flipCamera: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: Dp = dimensions().spacing4x,
    spacedBy: Dp = dimensions().spacing2x,
) {
    val tileHeight = remember(participants.size, contentHeight, contentPadding, spacedBy) {
        val heightAvailableForItems =
            contentHeight - 2 * contentPadding - (participants.size - 1) * spacedBy
        heightAvailableForItems / participants.size
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(contentPadding),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(spacedBy)
    ) {
        items(items = participants, key = { it.id.toString() + it.clientId }) { participant ->
            ParticipantTile(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                onDoubleTap(
                                    SelectedParticipant(
                                        userId = participant.id,
                                        clientId = participant.clientId,
                                        isSelfUser = participant.isSelfUser,
                                    )
                                )
                            }
                        )
                    }
                    .fillMaxWidth()
                    .height(tileHeight)
                    .animateItem(),
                participantTitleState = participant,
                isSelfUserMuted = isSelfUserMuted,
                isSelfUserCameraOn = isSelfUserCameraOn,
                onSelfUserVideoPreviewCreated = onSelfVideoPreviewCreated,
                onClearSelfUserVideoPreview = onSelfClearVideoPreview,
                recentReaction = recentReactions[participant.id],
                isOnFrontCamera = isOnFrontCamera,
                flipCamera = flipCamera,
            )
        }
    }
}

@Composable
fun PreviewCallingHorizontalView(
    participants: List<UICallParticipant>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.height(800.dp)) {
        CallingHorizontalView(
            participants = participants,
            isSelfUserMuted = true,
            isSelfUserCameraOn = false,
            contentHeight = 800.dp,
            recentReactions = emptyMap(),
            onSelfVideoPreviewCreated = {},
            onSelfClearVideoPreview = {},
            onDoubleTap = { },
            isOnFrontCamera = false,
            flipCamera = { },
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewCallingHorizontalView_1Participant() = WireTheme {
    PreviewCallingHorizontalView(buildPreviewParticipantsList(1))
}

@PreviewMultipleThemes
@Composable
fun PreviewCallingHorizontalView_2Participants() = WireTheme {
    PreviewCallingHorizontalView(buildPreviewParticipantsList(2))
}

@PreviewMultipleThemes
@Composable
fun PreviewCallingHorizontalView_3Participants() = WireTheme {
    PreviewCallingHorizontalView(buildPreviewParticipantsList(3))
}
