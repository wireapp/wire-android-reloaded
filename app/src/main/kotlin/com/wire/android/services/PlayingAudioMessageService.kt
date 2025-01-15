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

package com.wire.android.services

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.notification.NotificationConstants.PLAYING_AUDIO_CHANNEL_ID
import com.wire.android.notification.NotificationIds
import com.wire.android.notification.openAppPendingIntent
import com.wire.android.notification.playPauseAudioPendingIntent
import com.wire.android.notification.stopAudioPendingIntent
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PlayingAudioMessageService : Service() {

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcherProvider.io())
    }

    @Inject
    lateinit var audioMessagePlayer: ConversationAudioMessagePlayer

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        appLogger.i("$TAG: starting foreground")
        isServiceStarted = true
        generateForegroundNotification(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /**
         * When service is restarted by system onCreate lifecycle method is not guaranteed to be called
         * so we need to check if service is already started and if not generate notification and call startForeground()
         * https://issuetracker.google.com/issues/307329994#comment100
         */
        if (!isServiceStarted) {
            appLogger.i("$TAG: already started")
            isServiceStarted = true
            generateForegroundNotification(null)
        }
        scope.launch {
            audioMessagePlayer.playingAudioMessageFlow
                .distinctUntilChanged { old, new -> old.isSameAs(new) }
                .collectLatest { playingAudioMessage ->
                    if (playingAudioMessage is PlayingAudioMessage.Some) {
                        generateForegroundNotification(playingAudioMessage)
                    } else {
                        generateForegroundNotification(null)
                    }
                }
        }
        return START_STICKY
    }

    private fun generateForegroundNotification(audio: PlayingAudioMessage.Some?) {
        val notification = getNotification(audio)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                ServiceCompat.startForeground(
                    this,
                    NotificationIds.PLAYING_AUDIO_MESSAGE_ID.ordinal,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } catch (e: ForegroundServiceStartNotAllowedException) {
                // ForegroundServiceStartNotAllowedException may be thrown on restarting service from the background.
                // this is the only suggested workaround from google for now.
                // https://issuetracker.google.com/issues/307329994#comment86
                appLogger.e("$TAG: Failure while starting foreground: $e")
                stopSelf()
            }
        } else {
            ServiceCompat.startForeground(
                this,
                NotificationIds.PLAYING_AUDIO_MESSAGE_ID.ordinal,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        }
    }

    private fun getNotification(audio: PlayingAudioMessage.Some?): Notification {
        val playPauseVisibility = if (audio == null) View.GONE else View.VISIBLE
        val playPauseIconRes = if (audio?.state?.isPlaying() == true) R.drawable.ic_pause else R.drawable.ic_play
        val btnColor = this.getColor(R.color.default_icon_color)
        val playPauseIcon = Icon.createWithResource(this, playPauseIconRes).setTint(btnColor)
        val stopIcon = Icon.createWithResource(this, R.drawable.ic_stop).setTint(btnColor)
        val senderName = audio?.authorName?.asString(this.resources)
        val stopAudioPendingIntent = stopAudioPendingIntent(this)
        val playPauseAudioPendingIntent = playPauseAudioPendingIntent(this)

        val notificationLayout = RemoteViews(packageName, R.layout.notification_playing_audio_message).apply {
            setTextViewText(R.id.title, senderName)
            setImageViewIcon(R.id.play_pause, playPauseIcon)
            setViewVisibility(R.id.play_pause, playPauseVisibility)
            setImageViewIcon(R.id.stop, stopIcon)
            setOnClickPendingIntent(R.id.stop, stopAudioPendingIntent)
            setOnClickPendingIntent(R.id.play_pause, playPauseAudioPendingIntent)
        }

        return NotificationCompat.Builder(this, PLAYING_AUDIO_CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openAppPendingIntent(this))
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel("$TAG was destroyed")
        isServiceStarted = false
        appLogger.i("$TAG: was destroyed")
    }

    companion object {
        private const val TAG = "PlayingAudioMessageService"
        fun newIntent(context: Context?): Intent =
            Intent(context, PlayingAudioMessageService::class.java)

        var isServiceStarted = false
    }
}
