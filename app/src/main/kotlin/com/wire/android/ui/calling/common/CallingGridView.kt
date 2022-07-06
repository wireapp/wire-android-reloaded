package com.wire.android.ui.calling.common

import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.wire.android.ui.calling.ParticipantTile
import com.wire.android.ui.calling.getConversationName
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.theme.wireDimensions

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCallGrid(
    participants: List<UICallParticipant>,
    pageIndex: Int,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit
) {
    val config = LocalConfiguration.current

    LazyVerticalGrid(
        userScrollEnabled = false,
        contentPadding = PaddingValues(MaterialTheme.wireDimensions.spacing6x),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing6x),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing6x),
        columns = GridCells.Fixed(NUMBER_OF_GRID_CELLS)
    ) {

        items(participants) { participant ->
            // We need the number of tiles rows needed to calculate their height
            val numberOfTilesRows = tilesRowsCount(participants.size)
            // For now we are handling only self user camera state
            val isCameraOn = if (pageIndex == 0 && participants.first() == participant)
                isSelfUserCameraOn else false
            // for self user we don't need to get the muted value from participants list
            // if we do, this will show visuals with some delay
            // since we are getting participants by chunk of 8 items,
            // we need to check that we are on first page for sel user
            val isMuted = if (pageIndex == 0 && participants.first() == participant) isSelfUserMuted
                else participant.isMuted

            ParticipantTile(
                modifier = Modifier
                    .height(((config.screenHeightDp - TOP_APP_BAR_AND_BOTTOM_SHEET_HEIGHT) / numberOfTilesRows).dp)
                    .animateItemPlacement(),
                conversationName = getConversationName(participant.name),
                participantAvatar = participant.avatar,
                isMuted = isMuted,
                isCameraOn = isCameraOn,
                onSelfUserVideoPreviewCreated = {
                    if (pageIndex == 0 && participants.first() == participant) onSelfVideoPreviewCreated(it)
                },
                onClearSelfUserVideoPreview = {
                    if (pageIndex == 0 && participants.first() == participant)
                        onSelfClearVideoPreview()
                }
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

private const val NUMBER_OF_GRID_CELLS = 2
private const val TOP_APP_BAR_AND_BOTTOM_SHEET_HEIGHT = 178
