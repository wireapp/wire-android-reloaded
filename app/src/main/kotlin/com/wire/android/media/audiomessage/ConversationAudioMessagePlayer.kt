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
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
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
) {
    private var player: ConversationAudioMessagePlayer? = null
    private var usageCount: Int = 0

    @Synchronized
    fun provide(): ConversationAudioMessagePlayer {
        val player = player ?: ConversationAudioMessagePlayer(context, audioMediaPlayer, wavesMaskHelper, coreLogic).also {
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
) {
    private companion object {
        const val UPDATE_POSITION_INTERVAL_IN_MS = 100L
    }

    init {
        audioMediaPlayer.setOnCompletionListener {
            if (currentAudioMessageId != null) {
                audioMessageStateUpdate.tryEmit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                        currentAudioMessageId!!,
                        AudioMediaPlayingState.Completed
                    )
                )
                seekToAudioPosition.tryEmit(currentAudioMessageId!! to 0)
            }
        }
    }

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

    // MediaPlayer API does not have any mechanism that would inform as about the currentPosition,
    // in a callback manner, therefore we need to create a timer manually that ticks every 1 second
    // and emits the current position
    private val mediaPlayerPosition = flow {
        delay(UPDATE_POSITION_INTERVAL_IN_MS)
        while (true) {
            if (audioMediaPlayer.isPlaying) {
                emit(currentAudioMessageId to audioMediaPlayer.currentPosition)
            }
            delay(UPDATE_POSITION_INTERVAL_IN_MS)
        }
    }.distinctUntilChanged()

    private val seekToAudioPosition =
        MutableSharedFlow<Pair<String, Int>>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 1
        )

    private val positionChangedUpdate = merge(mediaPlayerPosition, seekToAudioPosition)
        .map { (messageId, position) ->
            messageId?.let {
                AudioMediaPlayerStateUpdate.PositionChangeUpdate(it, position)
            }
        }.filterNotNull()

    private var audioMessageStateHistory: Map<String, AudioState> = emptyMap()

    // Flow collecting the audio message state updates as well as the audio message position
    // updates, the collected values are then put into the map holding the state for each individual audio message.
    // The audio message position can be either updated by the user manually by for example a Slider component or by the player itself.
    val observableAudioMessagesState: Flow<Map<String, AudioState>> =
        merge(positionChangedUpdate, audioMessageStateUpdate).map { audioMessageStateUpdate ->
            val currentState = audioMessageStateHistory.getOrDefault(
                audioMessageStateUpdate.messageId,
                AudioState.DEFAULT
            )

            when (audioMessageStateUpdate) {
                is AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            audioMessageStateUpdate.messageId,
                            currentState.copy(audioMediaPlayingState = audioMessageStateUpdate.audioMediaPlayingState)
                        )
                    }
                }

                is AudioMediaPlayerStateUpdate.PositionChangeUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            audioMessageStateUpdate.messageId,
                            currentState.copy(currentPositionInMs = audioMessageStateUpdate.position)
                        )
                    }
                }

                is AudioMediaPlayerStateUpdate.TotalTimeUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            audioMessageStateUpdate.messageId,
                            currentState.copy(
                                totalTimeInMs = AudioState.TotalTimeInMs.Known(audioMessageStateUpdate.totalTimeInMs)
                            )
                        )
                    }
                }

                is AudioMediaPlayerStateUpdate.WaveMaskUpdate -> {
                    audioMessageStateHistory = audioMessageStateHistory.toMutableMap().apply {
                        put(
                            audioMessageStateUpdate.messageId,
                            currentState.copy(wavesMask = audioMessageStateUpdate.waveMask)
                        )
                    }
                }
            }

            audioMessageStateHistory
        }.onStart { emit(audioMessageStateHistory) }

    val audioSpeed: Flow<AudioSpeed> = _audioSpeed.onStart { emit(AudioSpeed.NORMAL) }

    private var currentAudioMessageId: String? = null

    suspend fun playAudio(
        conversationId: ConversationId,
        requestedAudioMessageId: String
    ) {
        val isRequestedAudioMessageCurrentlyPlaying = currentAudioMessageId == requestedAudioMessageId
        if (isRequestedAudioMessageCurrentlyPlaying) {
            resumeOrPauseCurrentlyPlayingAudioMessage(requestedAudioMessageId)
        } else {
            stopCurrentlyPlayingAudioMessage()
            playAudioMessage(
                conversationId = conversationId,
                messageId = requestedAudioMessageId,
                position = previouslyResumedPosition(requestedAudioMessageId)
            )
        }
    }

    suspend fun setSpeed(speed: AudioSpeed) {
        val currentParams = audioMediaPlayer.playbackParams
        audioMediaPlayer.playbackParams = currentParams.setSpeed(speed.value)
        updateSpeedFlow()
    }

    private fun previouslyResumedPosition(requestedAudioMessageId: String): Int? {
        return audioMessageStateHistory[requestedAudioMessageId]?.run {
            if (audioMediaPlayingState == AudioMediaPlayingState.Completed) {
                0
            } else {
                currentPositionInMs
            }
        }
    }

    private suspend fun stopCurrentlyPlayingAudioMessage() {
        currentAudioMessageId?.let {
            val currentAudioState = audioMessageStateHistory[it]
            if (currentAudioState?.audioMediaPlayingState != AudioMediaPlayingState.Fetching) {
                stop(it)
            }
        }
    }

    private suspend fun resumeOrPauseCurrentlyPlayingAudioMessage(messageId: String) {
        if (audioMediaPlayer.isPlaying) {
            pause(messageId)
        } else {
            resumeAudio(messageId)
        }
    }

    private suspend fun playAudioMessage(
        conversationId: ConversationId,
        messageId: String,
        position: Int? = null
    ) {
        currentAudioMessageId = messageId

        coroutineScope {
            launch {
                val currentAccountResult = coreLogic.getGlobalScope().session.currentSession()
                if (currentAccountResult is CurrentSessionResult.Failure) return@launch

                audioMessageStateUpdate.emit(
                    AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Fetching)
                )

                val assetMessage = getAssetMessage(currentAccountResult, conversationId, messageId)

                when (val result = assetMessage.await()) {
                    is MessageAssetResult.Success -> {
                        audioMessageStateUpdate.emit(
                            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(
                                messageId,
                                AudioMediaPlayingState.SuccessfulFetching
                            )
                        )

                        val isFetchedAudioCurrentlyQueuedToPlay = messageId == currentAudioMessageId

                        if (isFetchedAudioCurrentlyQueuedToPlay) {
                            audioMediaPlayer.setDataSource(context, Uri.parse(result.decodedAssetPath.toString()))
                            audioMediaPlayer.prepare()

                            audioMessageStateUpdate.emit(
                                AudioMediaPlayerStateUpdate.WaveMaskUpdate(
                                    messageId,
                                    wavesMaskHelper.getWaveMask(result.decodedAssetPath)
                                )
                            )

                            if (position != null) audioMediaPlayer.seekTo(position)

                            audioMediaPlayer.start()

                            updateSpeedFlow()

                            audioMessageStateUpdate.emit(
                                AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Playing)
                            )

                            audioMessageStateUpdate.emit(
                                AudioMediaPlayerStateUpdate.TotalTimeUpdate(messageId, audioMediaPlayer.duration)
                            )
                        }
                    }

                    is MessageAssetResult.Failure -> {
                        audioMessageStateUpdate.emit(
                            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Failed)
                        )
                    }
                }
            }
        }
    }

    suspend fun setPosition(messageId: String, position: Int) {
        val currentAudioState = audioMessageStateHistory[messageId]

        if (currentAudioState != null) {
            val isAudioMessageCurrentlyPlaying = currentAudioMessageId == messageId

            if (isAudioMessageCurrentlyPlaying) {
                audioMediaPlayer.seekTo(position.toLong(), SEEK_CLOSEST_SYNC)
            }
        }

        seekToAudioPosition.emit(messageId to position)
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

    private suspend fun resumeAudio(messageId: String) {
        audioMediaPlayer.start()
        updateSpeedFlow()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Playing)
        )
    }

    private suspend fun pause(messageId: String) {
        audioMediaPlayer.pause()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Paused)
        )
    }

    private suspend fun stop(messageId: String) {
        audioMediaPlayer.reset()

        audioMessageStateUpdate.emit(
            AudioMediaPlayerStateUpdate.AudioMediaPlayingStateUpdate(messageId, AudioMediaPlayingState.Stopped)
        )
    }

    private suspend fun updateSpeedFlow() {
        val currentSpeed = AudioSpeed.fromFloat(audioMediaPlayer.playbackParams.speed)
        _audioSpeed.emit(currentSpeed)
    }

    internal fun close() {
        audioMediaPlayer.reset()
        wavesMaskHelper.clear()
    }
}
