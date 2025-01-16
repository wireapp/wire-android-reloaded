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

package com.wire.android.ui.home.conversations.messages

import androidx.paging.PagingData
import com.wire.android.media.audiomessage.AudioSpeed
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.message.MessageAssetStatus
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant

data class ConversationMessagesViewState(
    val messages: Flow<PagingData<UIMessage>> = emptyFlow(),
    val firstUnreadInstant: Instant? = null,
    val firstUnreadEventIndex: Int = 0,
    val downloadedAssetDialogState: DownloadedAssetDialogVisibilityState = DownloadedAssetDialogVisibilityState.Hidden,
    val audioMessagesState: AudioMessagesState = AudioMessagesState(),
    val assetStatuses: PersistentMap<String, MessageAssetStatus> = persistentMapOf(),
    val searchedMessageId: String? = null
)

data class AudioMessagesState(
    val audioStates: PersistentMap<String, AudioState> = persistentMapOf(),
    val audioSpeed: AudioSpeed = AudioSpeed.NORMAL,
    val playingAudiMessage: PlayingAudioMessage = PlayingAudioMessage.None
)

sealed class DownloadedAssetDialogVisibilityState {
    object Hidden : DownloadedAssetDialogVisibilityState()
    data class Displayed(val assetData: AssetBundle, val messageId: String) : DownloadedAssetDialogVisibilityState()
}
