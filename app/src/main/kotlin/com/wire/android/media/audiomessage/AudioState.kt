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

import androidx.annotation.StringRes
import com.wire.android.R

data class AudioState(
    val audioMediaPlayingState: AudioMediaPlayingState,
    val currentPositionInMs: Int,
    val totalTimeInMs: TotalTimeInMs,
    val wavesMask: List<Int>
) {
    companion object {
        val DEFAULT = AudioState(AudioMediaPlayingState.Stopped, 0, TotalTimeInMs.NotKnown, listOf())
    }

    // if the back-end returned the total time, we use that, in case it didn't we use what we get from
    // the [ConversationAudioMessagePlayer.kt] which will emit the time once the users play the audio.
    fun sanitizeTotalTime(otherClientTotalTime: Int): TotalTimeInMs {
        if (otherClientTotalTime != 0) {
            return TotalTimeInMs.Known(otherClientTotalTime)
        }

        return totalTimeInMs
    }

    sealed class TotalTimeInMs {
        object NotKnown : TotalTimeInMs()

        data class Known(val value: Int) : TotalTimeInMs()
    }
}

@Suppress("MagicNumber")
enum class AudioSpeed(val value: Float, @StringRes val titleRes: Int) {
    NORMAL(1f, R.string.audio_speed_1),
    FAST(1.5f, R.string.audio_speed_1_5),
    MAX(2f, R.string.audio_speed_2);

    fun toggle(): AudioSpeed = when (this) {
        NORMAL -> FAST
        FAST -> MAX
        MAX -> NORMAL
    }

    companion object {
        fun fromFloat(speed: Float): AudioSpeed = when {
            (speed < FAST.value) -> NORMAL
            (speed < MAX.value) -> FAST
            else -> MAX
        }
    }
}

sealed class AudioMediaPlayingState {
    object Playing : AudioMediaPlayingState()
    object Stopped : AudioMediaPlayingState()

    object Completed : AudioMediaPlayingState()

    object Paused : AudioMediaPlayingState()

    object Fetching : AudioMediaPlayingState()

    object SuccessfulFetching : AudioMediaPlayingState()

    object Failed : AudioMediaPlayingState()
}

sealed class AudioMediaPlayerStateUpdate(
    open val messageId: String
) {
    data class AudioMediaPlayingStateUpdate(
        override val messageId: String,
        val audioMediaPlayingState: AudioMediaPlayingState
    ) : AudioMediaPlayerStateUpdate(messageId)

    data class PositionChangeUpdate(
        override val messageId: String,
        val position: Int
    ) : AudioMediaPlayerStateUpdate(messageId)

    data class TotalTimeUpdate(
        override val messageId: String,
        val totalTimeInMs: Int
    ) : AudioMediaPlayerStateUpdate(messageId)

    data class WaveMaskUpdate(
        override val messageId: String,
        val waveMask: List<Int>
    ) : AudioMediaPlayerStateUpdate(messageId)
}

sealed class RecordAudioMediaPlayerStateUpdate {
    data class RecordAudioMediaPlayingStateUpdate(
        val audioMediaPlayingState: AudioMediaPlayingState
    ) : RecordAudioMediaPlayerStateUpdate()

    data class PositionChangeUpdate(
        val position: Int
    ) : RecordAudioMediaPlayerStateUpdate()

    data class TotalTimeUpdate(
        val totalTimeInMs: Int
    ) : RecordAudioMediaPlayerStateUpdate()

    data class WaveMaskUpdate(
        val waveMask: List<Int>
    ) : RecordAudioMediaPlayerStateUpdate()
}
