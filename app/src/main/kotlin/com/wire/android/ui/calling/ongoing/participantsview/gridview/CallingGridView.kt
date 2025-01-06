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

package com.wire.android.ui.calling.ongoing.participantsview.gridview

import android.view.View
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
fun GroupCallGrid(
    participants: List<UICallParticipant>,
    pageIndex: Int,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    contentHeight: Dp,
    isOnFrontCamera: Boolean,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    onDoubleTap: (selectedParticipant: SelectedParticipant) -> Unit,
    flipCamera: () -> Unit,
    isInPictureInPictureMode: Boolean,
    recentReactions: Map<UserId, String>,
    modifier: Modifier = Modifier,
    contentPadding: Dp = dimensions().spacing4x,
    spacedBy: Dp = dimensions().spacing2x,
) {
    // We need the number of tiles rows needed to calculate their height
    val numberOfTilesRows = remember(participants.size) {
        tilesRowsCount(participants.size)
    }
    val tileHeight = remember(participants.size, contentHeight, contentPadding, spacedBy) {
        val heightAvailableForItems = contentHeight - 2 * contentPadding - (numberOfTilesRows - 1) * spacedBy
        heightAvailableForItems / numberOfTilesRows
    }
    LazyVerticalGrid(
        modifier = modifier,
        userScrollEnabled = false,
        contentPadding = PaddingValues(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(spacedBy),
        verticalArrangement = Arrangement.spacedBy(spacedBy),
        columns = GridCells.Fixed(NUMBER_OF_GRID_CELLS)
    ) {

        items(
            items = participants,
            key = { it.id.toString() + it.clientId + pageIndex },
            contentType = { getContentType(it.isCameraOn, it.isSharingScreen) }
        ) { participant ->
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
                    .height(tileHeight)
                    .animateItem(),
                participantTitleState = participant,
                isOnPiPMode = isInPictureInPictureMode,
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

/**
 * Returns the number of lines needed to display x participants in a page
 */
private fun tilesRowsCount(participantsSize: Int): Int = with(participantsSize) {
    return@with if (this % 2 == 0) (this / 2) else ((this / 2) + 1)
}

private fun getContentType(
    isCameraOn: Boolean,
    isSharingScreen: Boolean
) = if (isCameraOn || isSharingScreen) "videoRender" else null

private const val NUMBER_OF_GRID_CELLS = 2

@Composable
private fun PreviewGroupCallGrid(participants: List<UICallParticipant>, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(800.dp)) {
        GroupCallGrid(
            participants = participants,
            pageIndex = 0,
            isSelfUserMuted = false,
            isSelfUserCameraOn = false,
            contentHeight = 800.dp,
            onSelfVideoPreviewCreated = {},
            onSelfClearVideoPreview = {},
            onDoubleTap = { },
            isInPictureInPictureMode = false,
            recentReactions = emptyMap(),
            isOnFrontCamera = false,
            flipCamera = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupCallGrid_4Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(4))
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupCallGrid_6Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(6))
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupCallGrid_8Participants() = WireTheme {
    PreviewGroupCallGrid(buildPreviewParticipantsList(8))
}
