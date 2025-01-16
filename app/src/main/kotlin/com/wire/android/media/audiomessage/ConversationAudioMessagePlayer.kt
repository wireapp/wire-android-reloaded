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
package com.wire.android.media.audiomessage

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.SEEK_CLOSEST_SYNC
import android.net.Uri
import com.wire.android.R
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.util.extension.intervalFlow
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.message.GetNextAudioMessageInConversationUseCase
import com.wire.kalium.logic.feature.message.GetSenderNameByMessageIdUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationAudioMessagePlayerProvider
@Inject constructor(
    private val context: Context,
    private val audioMediaPlayer: MediaPlayer,
    private val wavesMaskHelper: AudioWavesMaskHelper,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @ApplicationScope private val scope: CoroutineScope
) {
    private var player: ConversationAudioMessagePlayer? = null
    private var usageCount: Int = 0

    @Synchronized
    fun provide(): ConversationAudioMessagePlayer {
        val player = player ?: ConversationAudioMessagePlayer(context, audioMediaPlayer, wavesMaskHelper, coreLogic, scope).also {
            player = it
        }
        usageCount++

        return player
    }

    @Synchronized
    fun onCleared() {
        usageCount--
        if (usageCount <= 0) {
            player?.close()
            player = null
        }
    }
}

@Suppress("TooManyFunctions")
class ConversationAudioMessagePlayer
internal constructor(
    private val context: Context,
    private val audioMediaPlayer: MediaPlayer,
    private val wavesMaskHelper: AudioWavesMaskHelper,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @ApplicationScope private val scope: CoroutineScope
) {
    private companion object {
        const val UPDATE_POSITION_INTERVAL_IN_MS = 100L
    }

    init {
        audioMediaPlayer.setOnCompletionListener {
            if (currentAudioMessageId != null) {
                audioMessageStateUpdate.tryEmit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                        currentAudioMessageId!!.conversationId,
                        currentAudioMessageId!!.messageId,
                        AudioMediaPlayingState.Completed
                    )
                )
                seekToAudioPosition.tryEmit(currentAudioMessageId!! to 0)

                tryToPlayNextAudio()
            }
        }
    }

    private var audioMessageStateHistory: Map<MessageIdWrapper, AudioState> = emptyMap()
    private var currentAudioMessageId: MessageIdWrapper? = null

    private val audioMessageStateUpdate =
        MutableSharedFlow<AudioMediaPlayerStateUpdate>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 1
        )

    private val _audioSpeed = MutableSharedFlow<AudioSpeed>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        extraBufferCapacity = 1,
        replay = 1
    )

    private val seekToAudioPosition =
        MutableSharedFlow<Pair<MessageIdWrapper, Int>>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 1
        )

    private val positionCheckTrigger = MutableSharedFlow<Unit>()

    // MediaPlayer API does not have any mechanism that would inform as about the currentPosition,
    // in a callback manner, therefore we need to create a timer manually that ticks every UPDATE_POSITION_INTERVAL_IN_MS
    // and emits the current position
    private val mediaPlayerPosition = positionCheckTrigger
        .map {
            currentAudioMessageId?.let {
                audioMessageStateHistory[it]?.let { state -> state.isPlaying() }
            } ?: false
        }
        .distinctUntilChanged()
        .flatMapLatest { isAnythingPlaying ->
            if (isAnythingPlaying) {
                intervalFlow(UPDATE_POSITION_INTERVAL_IN_MS)
                    .map {
                        if (audioMediaPlayer.isPlaying) {
                            currentAudioMessageId to audioMediaPlayer.currentPosition
                        } else {
                            null
                        }
                    }.filterNotNull()
            } else {
                // no need for tick-tack checking if there on playing message
                emptyFlow<Pair<MessageIdWrapper, Int>>()
            }
        }

    private val positionChangedUpdate = merge(mediaPlayerPosition, seekToAudioPosition)
        .map { (messageId, position) ->
            messageId?.let {
                AudioMediaPlayerStateUpdate.PositionChangeUpdate(it.conversationId, it.messageId, position)
            }
        }.filterNotNull()

    // Flow collecting the audio message state updates as well as the audio message position
    // updates, the collected values are then put into the map holding the state for each individual audio message.
    // The audio message position can be either updated by the user manually by for example a Slider component or by the player itself.
    val observableAudioMessagesState: Flow<Map<MessageIdWrapper, AudioState>> =
        merge(positionChangedUpdate, audioMessageStateUpdate).map { audioMessageStateUpdate ->
            val messageIdKey =
                MessageIdWrapper(audioMessageStateUpdate.conversationId, audioMessageStateUpdate.messageId)
            val currentState = audioMessageStateHistory.getOrDefault(
                messageIdKey,
                AudioState.DEFAULT
            )

            when (audioMessageStateUpdate) {
                is AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            messageIdKey,
                            currentState.copy(audioMediaPlayingState = audioMessageStateUpdate.audioMediaPlayingState)
                        )
                    }
                    positionCheckTrigger.emit(Unit)
                }

                is AudioMediaPlayerStateUpdate.PositionChangeUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            messageIdKey,
                            currentState.copy(currentPositionInMs = audioMessageStateUpdate.position)
                        )
                    }
                }

                is AudioMediaPlayerStateUpdate.TotalTimeUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            messageIdKey,
                            currentState.copy(
                                totalTimeInMs = AudioState.TotalTimeInMs.Known(audioMessageStateUpdate.totalTimeInMs)
                            )
                        )
                    }
                }

                is AudioMediaPlayerStateUpdate.WaveMaskUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            messageIdKey,
                            currentState.copy(wavesMask = audioMessageStateUpdate.waveMask)
                        )
                    }
                }
            }

            audioMessageStateHistory
        }.shareIn(scope, SharingStarted.WhileSubscribed(), 1)
            .onStart { emit(audioMessageStateHistory) }

    // Flow contains currently playing or last paused Audio message date.
    // If there is such a message state is PlayingAudioMessageState.Some,
    // PlayingAudioMessageState.None otherwise
    val playingAudioMessageFlow: Flow<PlayingAudioMessage> = observableAudioMessagesState
        .scan(PlayingAudioMessage.None as PlayingAudioMessage) { prevState, statesHistory ->
            val currentMessageId = currentAudioMessageId
            val state = currentMessageId?.let { statesHistory[it] }

            when {
                (state?.isPlayingOrPaused() != true) -> PlayingAudioMessage.None

                (prevState is PlayingAudioMessage.Some && prevState.messageId == currentMessageId.messageId) ->
                    // no need to request Sender name if we already have it
                    PlayingAudioMessage.Some(
                        conversationId = currentMessageId.conversationId,
                        messageId = currentMessageId.messageId,
                        authorName = prevState.authorName,
                        state = state
                    )

                else -> {
                    val authorName = getSenderNameByMessageId(currentMessageId.conversationId, currentMessageId.messageId)
                        ?.let { UIText.DynamicString(it) }
                        ?: UIText.StringResource(R.string.username_unavailable_label)

                    PlayingAudioMessage.Some(
                        conversationId = currentMessageId.conversationId,
                        messageId = currentMessageId.messageId,
                        authorName = authorName,
                        state = state,
                    )
                }
            }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(), 1)

    val audioSpeed: Flow<AudioSpeed> = _audioSpeed.onStart { emit(AudioSpeed.NORMAL) }

    suspend fun playAudio(
        conversationId: ConversationId,
        requestedAudioMessageId: String
    ) {
        val isRequestedAudioMessageCurrentlyPlaying = currentAudioMessageId == MessageIdWrapper(conversationId, requestedAudioMessageId)
        if (isRequestedAudioMessageCurrentlyPlaying) {
            resumeOrPauseCurrentlyPlayingAudioMessage(conversationId, requestedAudioMessageId)
        } else {
            stopCurrentlyPlayingAudioMessage()
            playAudioMessage(
                conversationId = conversationId,
                messageId = requestedAudioMessageId,
                position = previouslyResumedPosition(conversationId, requestedAudioMessageId)
            )
        }
    }

    suspend fun setSpeed(speed: AudioSpeed) {
        val currentParams = audioMediaPlayer.playbackParams
        audioMediaPlayer.playbackParams = currentParams.setSpeed(speed.value)
        updateSpeedFlow()
    }

    private fun previouslyResumedPosition(conversationId: ConversationId, requestedAudioMessageId: String): Int? {
        return audioMessageStateHistory[MessageIdWrapper(conversationId, requestedAudioMessageId)]?.run {
            if (audioMediaPlayingState == AudioMediaPlayingState.Completed) {
                0
            } else {
                currentPositionInMs
            }
        }
    }

    suspend fun stopCurrentlyPlayingAudioMessage() {
        currentAudioMessageId?.let {
            val currentAudioState = audioMessageStateHistory[it]
            if (currentAudioState?.audioMediaPlayingState != AudioMediaPlayingState.Fetching) {
                stop(it.conversationId, it.messageId)
            }
        }
    }

    suspend fun resumeOrPauseCurrentlyPlayingAudioMessage(conversationId: ConversationId, messageId: String) {
        if (audioMediaPlayer.isPlaying) {
            pause(conversationId, messageId)
        } else {
            resumeAudio(conversationId, messageId)
        }
    }

    private suspend fun playAudioMessage(
        conversationId: ConversationId,
        messageId: String,
        position: Int? = null
    ) {
        currentAudioMessageId = MessageIdWrapper(conversationId, messageId)

        val currentAccountResult = coreLogic.getGlobalScope().session.currentSession()
        if (currentAccountResult is CurrentSessionResult.Failure) return

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(conversationId, messageId, AudioMediaPlayingState.Fetching)
        )

        val assetMessage = getAssetMessage(currentAccountResult, conversationId, messageId)

        when (val result = assetMessage.await()) {
            is MessageAssetResult.Success -> {
                audioMessageStateUpdate.emit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                        conversationId,
                        messageId,
                        AudioMediaPlayingState.SuccessfulFetching
                    )
                )

                val isFetchedAudioCurrentlyQueuedToPlay = MessageIdWrapper(conversationId, messageId) == currentAudioMessageId

                if (isFetchedAudioCurrentlyQueuedToPlay) {
                    audioMediaPlayer.setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                    audioMediaPlayer.prepare()

                    audioMessageStateUpdate.emit(
                        AudioMediaPlayerStateUpdate.WaveMaskUpdate(
                            conversationId,
                            messageId,
                            wavesMaskHelper.getWaveMask(result.decodedAssetPath)
                        )
                    )

                    if (position != null) audioMediaPlayer.seekTo(position)

                    audioMediaPlayer.start()

                    updateSpeedFlow()

                    audioMessageStateUpdate.emit(
                        AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                            conversationId,
                            messageId,
                            AudioMediaPlayingState.Playing
                        )
                    )

                    audioMessageStateUpdate.emit(
                        AudioMediaPlayerStateUpdate.TotalTimeUpdate(conversationId, messageId, audioMediaPlayer.duration)
                    )
                }
            }

            is MessageAssetResult.Failure -> {
                audioMessageStateUpdate.emit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                        conversationId,
                        messageId,
                        AudioMediaPlayingState.Failed
                    )
                )
            }
        }
    }

    suspend fun setPosition(conversationId: ConversationId, messageId: String, position: Int) {
        val currentAudioState = audioMessageStateHistory[MessageIdWrapper(conversationId, messageId)]

        if (currentAudioState != null) {
            val isAudioMessageCurrentlyPlaying = currentAudioMessageId == MessageIdWrapper(conversationId, messageId)

            if (isAudioMessageCurrentlyPlaying) {
                audioMediaPlayer.seekTo(position.toLong(), SEEK_CLOSEST_SYNC)
            }
        }

        seekToAudioPosition.emit(MessageIdWrapper(conversationId, messageId) to position)
    }

    suspend fun fetchWavesMask(conversationId: ConversationId, messageId: String) {
        val currentAccountResult = coreLogic.getGlobalScope().session.currentSession()
        if (currentAccountResult is CurrentSessionResult.Failure) return

        val result = coreLogic
            .getSessionScope((currentAccountResult as CurrentSessionResult.Success).accountInfo.userId)
            .messages
            .getAssetMessage(conversationId, messageId)
            .await()

        if (result is MessageAssetResult.Success) {
            audioMessageStateUpdate.emit(
                AudioMediaPlayerStateUpdate.WaveMaskUpdate(
                    conversationId,
                    messageId,
                    wavesMaskHelper.getWaveMask(result.decodedAssetPath)
                )
            )
        }
    }

    private suspend fun getAssetMessage(
        currentAccountResult: CurrentSessionResult,
        conversationId: ConversationId,
        messageId: String
    ) = coreLogic
        .getSessionScope((currentAccountResult as CurrentSessionResult.Success).accountInfo.userId)
        .messages
        .getAssetMessage(conversationId, messageId)

    private suspend fun resumeAudio(conversationId: ConversationId, messageId: String) {
        audioMediaPlayer.start()
        updateSpeedFlow()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(conversationId, messageId, AudioMediaPlayingState.Playing)
        )
    }

    private suspend fun pause(conversationId: ConversationId, messageId: String) {
        audioMediaPlayer.pause()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(conversationId, messageId, AudioMediaPlayingState.Paused)
        )
    }

    private suspend fun stop(conversationId: ConversationId, messageId: String) {
        audioMediaPlayer.reset()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(conversationId, messageId, AudioMediaPlayingState.Stopped)
        )
    }

    private suspend fun updateSpeedFlow() {
        val currentSpeed = AudioSpeed.fromFloat(audioMediaPlayer.playbackParams.speed)
        _audioSpeed.emit(currentSpeed)
    }

    private fun tryToPlayNextAudio() {
        scope.launch {
            currentAudioMessageId?.let { (conversationId, currentMessageId) ->
                val currentAccountResult = coreLogic.getGlobalScope().session.currentSession()
                if (currentAccountResult is CurrentSessionResult.Failure) return@launch

                val nextAudio = coreLogic
                    .getSessionScope((currentAccountResult as CurrentSessionResult.Success).accountInfo.userId)
                    .messages
                    .getNextAudioMessageInConversation(conversationId, currentMessageId)

                if (nextAudio is GetNextAudioMessageInConversationUseCase.Result.Success) {
                    stop(conversationId, currentMessageId)
                    playAudioMessage(conversationId, nextAudio.messageId)
                }
            }
        }
    }

    private suspend fun getSenderNameByMessageId(conversationId: ConversationId, messageId: String): String? {
        val currentAccountResult = coreLogic.getGlobalScope().session.currentSession()
        if (currentAccountResult is CurrentSessionResult.Failure) return null

        val senderNameResult = coreLogic
            .getSessionScope((currentAccountResult as CurrentSessionResult.Success).accountInfo.userId)
            .messages
            .getSenderNameByMessageId(conversationId, messageId)

        return if (senderNameResult is GetSenderNameByMessageIdUseCase.Result.Success) senderNameResult.name else null
    }

    internal fun close() {
        audioMediaPlayer.reset()
        wavesMaskHelper.clear()
    }

    data class MessageIdWrapper(val conversationId: ConversationId, val messageId: String)
}
