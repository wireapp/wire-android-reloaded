package com.wire.android.notification

import com.wire.kalium.logic.data.id.ConversationId

//TODO: The names need to be localisable
object NotificationConstants {

    const val INCOMING_CALL_CHANNEL_ID = "com.wire.android.notification_incoming_call_channel"
    const val INCOMING_CALL_CHANNEL_NAME = "Incoming calls"
    const val ONGOING_CALL_CHANNEL_ID = "com.wire.android.notification_ongoing_call_channel"
    const val ONGOING_CALL_CHANNEL_NAME = "Ongoing calls"

    const val WEB_SOCKET_CHANNEL_ID = "com.wire.android.persistent_web_socket_channel"
    const val WEB_SOCKET_CHANNEL_NAME = "Persistent WebSocket"

    const val MESSAGE_CHANNEL_ID = "com.wire.android.notification_channel"
    const val MESSAGE_CHANNEL_NAME = "Messages"
    const val MESSAGE_GROUP_KEY = "wire_reloaded_notification_group"
    const val KEY_TEXT_REPLY = "key_text_notification_reply"

    const val MESSAGE_SYNC_CHANNEL_ID = "com.wire.android.message_synchronization"
    const val MESSAGE_SYNC_CHANNEL_NAME = "Message synchronization"

    const val OTHER_CHANNEL_ID = "com.wire.android.other"
    const val OTHER_CHANNEL_NAME = "Other essential actions"

    const val CHANNEL_GROUP_ID = "com.wire.notification_channel_group"
    const val CHANNEL_GROUP_NAME = "Notifications for"

    //Notification IDs (has to be unique!)
    val MESSAGE_SUMMARY_ID = "wire_messages_summary_notification".hashCode()
    val CALL_INCOMING_NOTIFICATION_ID = "wire_incoming_call_notification".hashCode()
    val CALL_ONGOING_NOTIFICATION_ID = "wire_ongoing_call_notification".hashCode()
    val PERSISTENT_NOTIFICATION_ID = "wire_persistent_web_socket_notification".hashCode()
    val MESSAGE_SYNC_NOTIFICATION_ID = "wire_notification_fetch_notification".hashCode()
    val MIGRATION_NOTIFICATION_ID = "wire_migration_notification".hashCode()

    fun getConversationNotificationId(conversationId: ConversationId) = getConversationNotificationId(conversationId.toString())
    fun getConversationNotificationId(conversationIdString: String) = conversationIdString.hashCode()
}
