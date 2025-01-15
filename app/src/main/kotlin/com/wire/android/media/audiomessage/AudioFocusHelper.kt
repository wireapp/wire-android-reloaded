/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import javax.inject.Inject

class AudioFocusHelper @Inject constructor(private val audioManager: AudioManager) {

    private var listener: PlayPauseListener? = null

    private val onAudioFocusChangeListener by lazy {
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                    listener?.onPauseCurrentAudio()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    listener?.onPauseCurrentAudio()
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                    listener?.onResumeCurrentAudio()
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    listener?.onPauseCurrentAudio()
                }
            }
        }
    }

    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setOnAudioFocusChangeListener(onAudioFocusChangeListener)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setForceDucking(true)
            }.build()
    }

    /**
     * Requests the audio focus.
     * @return true in case if focus was granted (AudioMessage can be played), false - otherwise
     */
    fun request(): Boolean {
        return audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_FAILED
    }

    /**
     * Abandon the audio focus.
     */
    fun abandon() {
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    fun setListener(onPauseCurrentAudio: () -> Unit, onResumeCurrentAudio: () -> Unit) {
        listener = object : PlayPauseListener {
            override fun onPauseCurrentAudio() {
                onPauseCurrentAudio()
            }

            override fun onResumeCurrentAudio() {
                onResumeCurrentAudio()
            }

        }
    }

    interface PlayPauseListener {
        fun onPauseCurrentAudio()
        fun onResumeCurrentAudio()
    }
}
