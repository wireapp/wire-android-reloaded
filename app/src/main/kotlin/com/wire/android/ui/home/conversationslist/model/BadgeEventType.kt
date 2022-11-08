package com.wire.android.ui.home.conversationslist.model

sealed class BadgeEventType {
    data class UnreadMessage(val unreadMessageCount: Int) : BadgeEventType()
    object UnreadMention : BadgeEventType()
    object UnreadReply : BadgeEventType()
    object MissedCall : BadgeEventType()
    object ReceivedConnectionRequest : BadgeEventType()
    object SentConnectRequest : BadgeEventType()
    object Blocked : BadgeEventType()
    object Deleted : BadgeEventType()
    object None : BadgeEventType()
}
