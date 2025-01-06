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
package com.wire.android.ui.home.conversations.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioSpeed
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.AudioMessagesState
import com.wire.android.ui.home.conversations.messages.item.MessageClickActions
import com.wire.android.ui.home.conversations.messages.item.MessageContainerItem
import com.wire.android.ui.home.conversations.messages.item.SwipableMessageConfiguration
import com.wire.android.ui.home.conversations.mock.mockAssetAudioMessage
import com.wire.android.ui.home.conversations.mock.mockAssetMessage
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.UIPagingItem
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessageAssetStatus
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant

@Composable
fun FileAssetsContent(
    groupedAssetMessageList: Flow<PagingData<UIPagingItem>>,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    audioMessagesState: AudioMessagesState = AudioMessagesState(),
    onPlayAudioItemClicked: (messageId: String) -> Unit = {},
    onAudioItemPositionChanged: (String, Int) -> Unit = { _, _ -> },
    onAssetItemClicked: (messageId: String) -> Unit = {},
    onItemLongClicked: (messageId: String, isMyMessage: Boolean) -> Unit = { _, _ -> },
) {
    val lazyPagingMessages = groupedAssetMessageList.collectAsLazyPagingItems()

    if (lazyPagingMessages.itemCount > 0) {
        AssetMessagesListContent(
            groupedAssetMessageList = lazyPagingMessages,
            audioMessagesState = audioMessagesState,
            assetStatuses = assetStatuses,
            onPlayAudioItemClicked = onPlayAudioItemClicked,
            onAudioItemPositionChanged = onAudioItemPositionChanged,
            onAssetItemClicked = onAssetItemClicked,
            onItemLongClicked = onItemLongClicked,
        )
    } else {
        EmptyMediaContentScreen(
            text = stringResource(R.string.label_conversation_files_empty)
        )
    }
}

@Composable
private fun AssetMessagesListContent(
    groupedAssetMessageList: LazyPagingItems<UIPagingItem>,
    audioMessagesState: AudioMessagesState,
    assetStatuses: PersistentMap<String, MessageAssetStatus>,
    onPlayAudioItemClicked: (messageId: String) -> Unit,
    onAudioItemPositionChanged: (String, Int) -> Unit,
    onAssetItemClicked: (messageId: String) -> Unit,
    onItemLongClicked: (messageId: String, isMyMessage: Boolean) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = groupedAssetMessageList.itemCount,
            key = groupedAssetMessageList.itemKey {
                when (it) {
                    is UIPagingItem.Label -> it.date
                    is UIPagingItem.Message -> it.uiMessage.header.messageId
                }
            },
            contentType = groupedAssetMessageList.itemContentType { it }
        ) { index ->
            val uiPagingItem: UIPagingItem = groupedAssetMessageList[index] ?: return@items

            when (uiPagingItem) {
                is UIPagingItem.Label -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = dimensions().spacing6x,
                            // first label should not have top padding
                            top = if (index == 0) dimensions().spacing0x else dimensions().spacing6x,
                        )
                ) {
                    FolderHeader(
                        name = uiPagingItem.date.uppercase(),
                        modifier = Modifier
                            .background(MaterialTheme.wireColorScheme.background)
                            .fillMaxWidth()
                    )
                }

                is UIPagingItem.Message -> {
                    when (val message = uiPagingItem.uiMessage) {
                        is UIMessage.Regular -> {
                            MessageContainerItem(
                                message = message,
                                conversationDetailsData = ConversationDetailsData.None(null),
                                audioState = audioMessagesState.audioStates[message.header.messageId],
                                audioSpeed = audioMessagesState.audioSpeed,
                                assetStatus = assetStatuses[message.header.messageId]?.transferStatus,
                                clickActions = MessageClickActions.Content(
                                    onFullMessageLongClicked = remember { { onItemLongClicked(it.header.messageId, it.isMyMessage) } },
                                    onAssetClicked = onAssetItemClicked,
                                    onPlayAudioClicked = onPlayAudioItemClicked,
                                    onAudioPositionChanged = onAudioItemPositionChanged,
                                ),
                                onSelfDeletingMessageRead = { },
                                shouldDisplayMessageStatus = false,
                                shouldDisplayFooter = false,
                                swipableMessageConfiguration = SwipableMessageConfiguration.NotSwipable,
                                failureInteractionAvailable = false,
                            )
                        }

                        is UIMessage.System -> {}
                    }
                }
            }
        }
        item {
            if (groupedAssetMessageList.loadState.append is LoadState.Loading) {
                WireCircularProgressIndicator(
                    progressColor = MaterialTheme.wireColorScheme.onBackground,
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewFileAssetsEmptyContent() = WireTheme {
    FileAssetsContent(groupedAssetMessageList = emptyFlow(), assetStatuses = persistentMapOf())
}

@PreviewMultipleThemes
@Composable
fun PreviewFileAssetsContent() = WireTheme {
    val (flowOfAssets, assetStatuses, audioStatuses) = mockAssets()
    FileAssetsContent(groupedAssetMessageList = flowOfAssets, assetStatuses = assetStatuses, audioMessagesState = audioStatuses)
}

@Suppress("MagicNumber")
fun mockAssets(): Triple<Flow<PagingData<UIPagingItem>>, PersistentMap<String, MessageAssetStatus>, AudioMessagesState> {
    val msg1 = mockAssetMessage(assetId = "assset1", messageId = "msg1")
    val msg2 = mockAssetMessage(assetId = "assset2", messageId = "msg2")
    val msg3 = mockAssetMessage(assetId = "assset3", messageId = "msg3")
    val msg4 = mockAssetAudioMessage(assetId = "assset4", messageId = "msg4")
    val msg5 = mockAssetAudioMessage(assetId = "assset5", messageId = "msg5")
    val conversationId = ConversationId("value", "domain")
    val flowOfAssets = flowOf(
        PagingData.from(
            listOf(
                UIPagingItem.Label("October"),
                UIPagingItem.Message(msg1, Instant.DISTANT_PAST),
                UIPagingItem.Message(msg2, Instant.DISTANT_PAST),
                UIPagingItem.Message(msg3, Instant.DISTANT_PAST),
                UIPagingItem.Message(msg4, Instant.DISTANT_PAST),
                UIPagingItem.Message(msg5, Instant.DISTANT_PAST),
            )
        )
    )
    val assetsStatuses = persistentMapOf(
        msg1.header.messageId to MessageAssetStatus(msg1.header.messageId, conversationId, AssetTransferStatus.SAVED_EXTERNALLY),
        msg2.header.messageId to MessageAssetStatus(msg2.header.messageId, conversationId, AssetTransferStatus.NOT_DOWNLOADED),
        msg3.header.messageId to MessageAssetStatus(msg3.header.messageId, conversationId, AssetTransferStatus.DOWNLOAD_IN_PROGRESS)
    )
    val audioStatuses = persistentMapOf(
        msg4.header.messageId to AudioState(AudioMediaPlayingState.Fetching, 0, AudioState.TotalTimeInMs.NotKnown, listOf()),
        msg5.header.messageId to AudioState(AudioMediaPlayingState.Playing, 20_000, AudioState.TotalTimeInMs.Known(60_000), listOf()),
    )
    return Triple(flowOfAssets, assetsStatuses, AudioMessagesState(audioStatuses, AudioSpeed.NORMAL))
}
