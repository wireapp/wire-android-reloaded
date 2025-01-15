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

package com.wire.android.notification.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.di.ApplicationScope
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class StopAudioMessageReceiver : BroadcastReceiver() {

    @Inject
    lateinit var audioMessagePlayer: ConversationAudioMessagePlayer

    @Inject
    @ApplicationScope
    lateinit var coroutineScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        appLogger.i("StopAudioMessageReceiver: onReceive")
        coroutineScope.launch {
            audioMessagePlayer.forceToStopCurrentAudioMessage()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, StopAudioMessageReceiver::class.java)
    }
}
