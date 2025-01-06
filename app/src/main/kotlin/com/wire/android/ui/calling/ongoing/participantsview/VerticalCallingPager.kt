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

package com.wire.android.ui.calling.ongoing.participantsview

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.BuildConfig
import com.wire.android.ui.calling.model.UICallParticipant
import com.wire.android.ui.calling.ongoing.buildPreviewParticipantsList
import com.wire.android.ui.calling.ongoing.fullscreen.SelectedParticipant
import com.wire.android.ui.calling.ongoing.participantsview.gridview.GroupCallGrid
import com.wire.android.ui.calling.ongoing.participantsview.horizentalview.CallingHorizontalView
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.user.UserId

private const val MAX_TILES_PER_PAGE = 8
private const val MAX_ITEMS_FOR_HORIZONTAL_VIEW = 3

@Composable
fun VerticalCallingPager(
    participants: List<UICallParticipant>,
    isSelfUserMuted: Boolean,
    isSelfUserCameraOn: Boolean,
    isInPictureInPictureMode: Boolean,
    isOnFrontCamera: Boolean,
    contentHeight: Dp,
    recentReactions: Map<UserId, String>,
    onSelfVideoPreviewCreated: (view: View) -> Unit,
    onSelfClearVideoPreview: () -> Unit,
    requestVideoStreams: (participants: List<UICallParticipant>) -> Unit,
    onDoubleTap: (selectedParticipant: SelectedParticipant) -> Unit,
    flipCamera: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(contentHeight)
    ) {
        val pagerState = rememberPagerState(
            pageCount = { pagesCount(participants.size) }
        )
        Box {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                if (participants.isNotEmpty()) {
                    // if PiP is enabled and more than one participant is present,
                    // we need to remove the first participant(self user) from the list
                    val newParticipants =
                        if (BuildConfig.PICTURE_IN_PICTURE_ENABLED && participants.size > 1) {
                            participants.subList(1, participants.size)
                        } else {
                            participants
                        }
                    val participantsChunkedList = remember(newParticipants) {
                        newParticipants.chunked(MAX_TILES_PER_PAGE)
                    }
                    val participantsWithCameraOn by rememberUpdatedState(newParticipants.count { it.isCameraOn })
                    val participantsWithScreenShareOn by rememberUpdatedState(newParticipants.count { it.isSharingScreen })

                    if (participantsChunkedList[pageIndex].size <= MAX_ITEMS_FOR_HORIZONTAL_VIEW) {
                        CallingHorizontalView(
                            participants = participantsChunkedList[pageIndex],
                            isSelfUserMuted = isSelfUserMuted,
                            isSelfUserCameraOn = isSelfUserCameraOn,
                            contentHeight = contentHeight,
                            onSelfVideoPreviewCreated = onSelfVideoPreviewCreated,
                            onSelfClearVideoPreview = onSelfClearVideoPreview,
                            onDoubleTap = onDoubleTap,
                            recentReactions = recentReactions,
                            isOnFrontCamera = isOnFrontCamera,
                            flipCamera = flipCamera,
                        )
                    } else {
                        GroupCallGrid(
                            participants = participantsChunkedList[pageIndex],
                            pageIndex = pageIndex,
                            isSelfUserMuted = isSelfUserMuted,
                            isSelfUserCameraOn = isSelfUserCameraOn,
                            contentHeight = contentHeight,
                            onSelfVideoPreviewCreated = onSelfVideoPreviewCreated,
                            onSelfClearVideoPreview = onSelfClearVideoPreview,
                            onDoubleTap = onDoubleTap,
                            isInPictureInPictureMode = isInPictureInPictureMode,
                            recentReactions = recentReactions,
                            isOnFrontCamera = isOnFrontCamera,
                            flipCamera = flipCamera,
                        )
                    }

                    LaunchedEffect(
                        participantsWithCameraOn, // Request video stream when someone turns camera on/off
                        participantsWithScreenShareOn, // Request video stream when someone starts sharing screen
                        pagerState.currentPage // Request video stream when swiping to a different page on the grid
                    ) {
                        requestVideoStreams(participantsChunkedList[pagerState.currentPage])
                    }
                }
            }
            // we don't need to display the indicator if we have one page and when it's in PiP mode
            if (pagesCount(participants.size) > 1 && !isInPictureInPictureMode) {
                Surface(
                    shape = RoundedCornerShape(dimensions().corner16x),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = MaterialTheme.wireDimensions.spacing12x),
                ) {
                    VerticalPagerIndicator(
                        modifier = Modifier.padding(dimensions().spacing4x),
                        pagerState = pagerState,
                        indicatorHeight = dimensions().spacing12x,
                        indicatorWidth = dimensions().spacing12x,
                        spacing = dimensions().spacing6x,
                        indicatorShape = CircleShape
                    )
                }
            }
        }
    }
}

/**
 * Returns number of pages(with an already defined max number of tiles) needed to display x participants
 */
private fun pagesCount(size: Int): Int {
    val pages = size / MAX_TILES_PER_PAGE
    return if (size % MAX_TILES_PER_PAGE > 0) {
        pages + 1
    } else {
        pages
    }
}

@Composable
private fun PreviewVerticalCallingPager(participants: List<UICallParticipant>) {
    VerticalCallingPager(
        participants = participants,
        isSelfUserMuted = false,
        isSelfUserCameraOn = false,
        contentHeight = 800.dp,
        onSelfVideoPreviewCreated = {},
        onSelfClearVideoPreview = {},
        requestVideoStreams = {},
        onDoubleTap = { },
        flipCamera = { },
        isInPictureInPictureMode = false,
        recentReactions = emptyMap(),
        isOnFrontCamera = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewVerticalCallingPagerHorizontalView() = WireTheme {
    PreviewVerticalCallingPager(
        participants = buildPreviewParticipantsList(
            MAX_ITEMS_FOR_HORIZONTAL_VIEW
        )
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewVerticalCallingPagerGrid() = WireTheme {
    PreviewVerticalCallingPager(participants = buildPreviewParticipantsList(MAX_TILES_PER_PAGE + 1))
}
