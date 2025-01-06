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
import androidx.core.net.toUri
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.io.File
import javax.inject.Inject

@ViewModelScoped
class RecordAudioMessagePlayer @Inject constructor(
    private val context: Context,
    private val audioMediaPlayer: MediaPlayer,
    private val wavesMaskHelper: AudioWavesMaskHelper
) {
    private var currentAudioFile: File? = null
    private var audioState: AudioState = AudioState.DEFAULT

    init {
        audioMediaPlayer.setOnCompletionListener {
            if (currentAudioFile != null) {
                audioMessageStateUpdate.tryEmit(
                    RecordAudioMediaPlayerStateUpdate.RecordAudioMediaPlayingStateUpdate(
                        AudioMediaPlayingState.Completed
                    )
                )
                seekToAudioPosition.tryEmit(0)
            }
        }
    }

    private val audioMessageStateUpdate =
        MutableSharedFlow<RecordAudioMediaPlayerStateUpdate>(
            extraBufferCapacity = 1
        )

    // MediaPlayer API does not have any mechanism that would inform us about the currentPosition,
    // in a callback manner, therefore we need to create a timer manually that ticks every 1 second
    // and emits the current position
    private val mediaPlayerPosition = flow {
        while (true) {
            delay(UPDATE_POSITION_INTERVAL_IN_MS)
            if (currentAudioFile != null && audioMediaPlayer.isPlaying) {
                emit(audioMediaPlayer.currentPosition)
            }
        }
    }.distinctUntilChanged()

    private val seekToAudioPosition =
        MutableSharedFlow<Int>(
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            extraBufferCapacity = 1
        )

    private val positionChangedUpdate = merge(mediaPlayerPosition, seekToAudioPosition)
        .map { position ->
            currentAudioFile?.let {
                RecordAudioMediaPlayerStateUpdate.PositionChangeUpdate(position)
            }
        }.filterNotNull()

    // Flow collecting the audio message state updates as well as the audio position updates.
    // The collected value is updated for our single recorded audio.
    // The audio position can be either updated manually by the user or from the Slider component or
    // from the player itself.
    val audioMessageStateFlow: Flow<AudioState> =
        merge(positionChangedUpdate, audioMessageStateUpdate).map { audioStateUpdate ->
            when (audioStateUpdate) {
                is RecordAudioMediaPlayerStateUpdate.RecordAudioMediaPlayingStateUpdate -> {
                    audioState = audioState.copy(
                        audioMediaPlayingState = audioStateUpdate.audioMediaPlayingState
                    )
                }

                is RecordAudioMediaPlayerStateUpdate.PositionChangeUpdate -> {
                    audioState = audioState.copy(
                        currentPositionInMs = audioStateUpdate.position
                    )
                }

                is RecordAudioMediaPlayerStateUpdate.TotalTimeUpdate -> {
                    audioState = audioState.copy(
                        totalTimeInMs = AudioState.TotalTimeInMs.Known(
                            value = audioStateUpdate.totalTimeInMs
                        )
                    )
                }

                is RecordAudioMediaPlayerStateUpdate.WaveMaskUpdate -> {
                    audioState = audioState.copy(
                        wavesMask = audioStateUpdate.waveMask
                    )
                }
            }

            audioState
        }

    suspend fun playAudio(
        audioFile: File
    ) {
        if (currentAudioFile != null && audioFile.name == currentAudioFile?.name) {
            resumeOrPauseAudio()
        } else {
            stopCurrentlyPlayingAudioMessage()
            playAudioMessage(
                audioFile = audioFile,
                position = previouslyResumedPosition()
            )
        }
    }

    private fun previouslyResumedPosition(): Int =
        if (audioState.audioMediaPlayingState == AudioMediaPlayingState.Completed) {
            0
        } else {
            audioState.currentPositionInMs
        }

    private suspend fun stopCurrentlyPlayingAudioMessage() {
        if (currentAudioFile != null) {
            stop()
        }
    }

    private suspend fun resumeOrPauseAudio() {
        if (audioMediaPlayer.isPlaying) {
            pause()
        } else {
            resumeAudio()
        }
    }

    private suspend fun playAudioMessage(
        audioFile: File,
        position: Int
    ) {
        currentAudioFile = audioFile

        audioMediaPlayer.setDataSource(
            context,
            audioFile.toUri()
        )
        audioMediaPlayer.prepare()
        audioMediaPlayer.seekTo(position)
        audioMediaPlayer.start()

        audioMessageStateUpdate.emit(
            RecordAudioMediaPlayerStateUpdate.WaveMaskUpdate(
                wavesMaskHelper.getWaveMask(audioFile)
            )
        )

        audioMessageStateUpdate.emit(
            RecordAudioMediaPlayerStateUpdate.RecordAudioMediaPlayingStateUpdate(
                audioMediaPlayingState = AudioMediaPlayingState.Playing
            )
        )

        audioMessageStateUpdate.emit(
            RecordAudioMediaPlayerStateUpdate.TotalTimeUpdate(
                totalTimeInMs = audioMediaPlayer.duration
            )
        )
    }

    suspend fun setPosition(position: Int) {
        if (currentAudioFile != null) {
            audioMediaPlayer.seekTo(position)
            seekToAudioPosition.emit(position)
        }
    }

    private suspend fun resumeAudio() {
        audioMediaPlayer.start()
        audioMessageStateUpdate.emit(
            RecordAudioMediaPlayerStateUpdate.RecordAudioMediaPlayingStateUpdate(
                audioMediaPlayingState = AudioMediaPlayingState.Playing
            )
        )
    }

    private suspend fun pause() {
        audioMediaPlayer.pause()
        audioMessageStateUpdate.emit(
            RecordAudioMediaPlayerStateUpdate.RecordAudioMediaPlayingStateUpdate(
                AudioMediaPlayingState.Paused
            )
        )
    }

    suspend fun stop() {
        currentAudioFile = null
        audioMediaPlayer.reset()
        audioMessageStateUpdate.emit(
            RecordAudioMediaPlayerStateUpdate.RecordAudioMediaPlayingStateUpdate(
                audioMediaPlayingState = AudioMediaPlayingState.Stopped
            )
        )
    }

    fun close() {
        audioMediaPlayer.release()
    }

    private companion object {
        const val UPDATE_POSITION_INTERVAL_IN_MS = 100L
    }
}
