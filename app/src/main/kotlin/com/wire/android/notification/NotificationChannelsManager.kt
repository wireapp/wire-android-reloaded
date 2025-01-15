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

package com.wire.android.notification

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import com.wire.android.appLogger
import com.wire.android.media.PingRinger
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelsManager @Inject constructor(
    private val context: Context,
    private val notificationManagerCompat: NotificationManagerCompat
) {
    private val incomingCallSoundUri by lazy {
        Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://" +
                    "${context.packageName}/raw/ringing_from_them"
        )
    }

    private val knockSoundUri by lazy {
        Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://" +
                    "${context.packageName}/raw/ping_from_them"
        )
    }

    /**
     *  Creating user-specific NotificationChannels for each user, they will be grouped by User in App Settings.
     *  And removing the ChannelGroups (with all the channels in it) that are not belongs to any user in a list (user logged out e.x.)
     */
    fun createUserNotificationChannels(allUsers: List<SelfUser>) {
        appLogger.i("$TAG: creating all the notification channels for ${allUsers.size} users")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        allUsers.forEach { user ->
            val groupId = createNotificationChannelGroup(user.id, user.handle ?: user.name ?: user.id.value)

            createIncomingCallsChannel(groupId, user.id)
            createOutgoingCallChannel(groupId, user.id)
            createMessagesNotificationChannel(user.id, groupId)
            createPingNotificationChannel(user.id, groupId)
        }

        // OngoingCall is not user specific channel, but common for all users.
        createOngoingNotificationChannel()

        createPlayingAudioMessageNotificationChannel()

        deleteRedundantChannelGroups(allUsers)
    }

    /**
     * Deletes NotificationChanelGroup (and all NotificationChannels that belongs to it)
     * for the users that are not in [activeUsers] list.
     */
    private fun deleteRedundantChannelGroups(activeUsers: List<SelfUser>) {
        val groupsToKeep = activeUsers.map { NotificationConstants.getChanelGroupIdForUser(it.id) }

        notificationManagerCompat.notificationChannelGroups
            .filter { group -> groupsToKeep.none { it == group.id } }
            .forEach { group ->
                appLogger.i("$TAG: deleting notification channels for ${group.name} group")
                notificationManagerCompat.deleteNotificationChannelGroup(group.id)
            }
    }

    /**
     * @return created groupId.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelGroup(userId: UserId, userName: String): String {
        val chanelGroupId = NotificationConstants.getChanelGroupIdForUser(userId)
        val channelGroup = NotificationChannelGroupCompat.Builder(chanelGroupId)
            .setName(getChanelGroupNameForUser(userName))
            .build()
        notificationManagerCompat.createNotificationChannelGroup(channelGroup)
        return chanelGroupId
    }

    private fun createIncomingCallsChannel(groupId: String, userId: UserId) {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(getAudioAttributeUsageByOsLevel())
            .build()

        val channelId = NotificationConstants.getIncomingChannelId(userId)
        val notificationChannel = NotificationChannelCompat
            .Builder(channelId, NotificationManagerCompat.IMPORTANCE_MAX)
            .setName(NotificationConstants.INCOMING_CALL_CHANNEL_NAME)
            .setImportance(NotificationManagerCompat.IMPORTANCE_MAX)
            .setSound(incomingCallSoundUri, audioAttributes)
            .setShowBadge(false)
            .setVibrationPattern(VIBRATE_PATTERN)
            .setGroup(groupId)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    private fun createOutgoingCallChannel(groupId: String, userId: UserId) {
        val channelId = NotificationConstants.getOutgoingChannelId(userId)
        val notificationChannel = NotificationChannelCompat
            .Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(NotificationConstants.OUTGOING_CALL_CHANNEL_NAME)
            .setImportance(NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setShowBadge(false)
            .setGroup(groupId)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    private fun createOngoingNotificationChannel() {
        val channelId = NotificationConstants.ONGOING_CALL_CHANNEL_ID
        val notificationChannel = NotificationChannelCompat
            .Builder(channelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(NotificationConstants.ONGOING_CALL_CHANNEL_NAME)
            .setVibrationEnabled(false)
            .setImportance(NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setSound(null, null)
            .setShowBadge(false)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    private fun createPlayingAudioMessageNotificationChannel() {
        val channelId = NotificationConstants.PLAYING_AUDIO_CHANNEL_ID
        val notificationChannel = NotificationChannelCompat
            .Builder(channelId, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(NotificationConstants.PLAYING_AUDIO_CHANNEL_NAME)
            .setVibrationEnabled(false)
            .setImportance(NotificationManagerCompat.IMPORTANCE_LOW)
            .setSound(null, null)
            .setShowBadge(false)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    private fun createMessagesNotificationChannel(userId: UserId, channelGroupId: String) {
        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.getMessagesChannelId(userId), NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(NotificationConstants.MESSAGE_CHANNEL_NAME)
            .setGroup(channelGroupId)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    private fun createPingNotificationChannel(userId: UserId, channelGroupId: String) {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()

        val notificationChannel = NotificationChannelCompat
            .Builder(NotificationConstants.getPingsChannelId(userId), NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(NotificationConstants.PING_CHANNEL_NAME)
            .setGroup(channelGroupId)
            .setVibrationPattern(PingRinger.VIBRATE_PATTERN)
            .setSound(knockSoundUri, audioAttributes)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    fun createRegularChannel(channelId: String, channelName: String) {
        val notificationChannel = NotificationChannelCompat
            .Builder(channelId, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(channelName)
            .setShowBadge(false)
            .build()

        notificationManagerCompat.createNotificationChannel(notificationChannel)
    }

    /**
     * Tricky bug: No documentation whatsoever, but these values affect how the system cancels or not the vibration of the notification
     * on different Android OS levels, probably channel creation related.
     */
    private fun getAudioAttributeUsageByOsLevel() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AudioAttributes.USAGE_NOTIFICATION_RINGTONE else AudioAttributes.USAGE_MEDIA

    companion object {
        private fun getChanelGroupNameForUser(userName: String): String = userName
        private val VIBRATE_PATTERN = longArrayOf(0, 1000, 1000)

        const val TAG = "NotificationChannelsManager"
    }
}
