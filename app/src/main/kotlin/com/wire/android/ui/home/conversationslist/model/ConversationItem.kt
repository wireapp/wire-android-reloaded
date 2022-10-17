package com.wire.android.ui.home.conversationslist.model

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.common.UserInfoLabel
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType

sealed class ConversationItem {
    abstract val conversationId: ConversationId
    abstract val mutedStatus: MutedConversationStatus
    abstract val isLegalHold: Boolean
    abstract val lastEvent: ConversationLastEvent
    abstract val badgeEventType: BadgeEventType

    data class GroupConversation(
        val groupName: String,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastEvent: ConversationLastEvent,
        override val badgeEventType: BadgeEventType,
        val hasOnGoingCall: Boolean = false,
        val isSelfUserCreator: Boolean = false,
        val isSelfUserMember: Boolean = true
    ) : ConversationItem()

    data class PrivateConversation(
        val userAvatarData: UserAvatarData,
        val conversationInfo: ConversationInfo,
        val userId: UserId,
        val blockingState: BlockingState,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastEvent: ConversationLastEvent,
        override val badgeEventType: BadgeEventType,
    ) : ConversationItem()

    data class ConnectionConversation(
        val userAvatarData: UserAvatarData,
        val conversationInfo: ConversationInfo,
        override val conversationId: ConversationId,
        override val mutedStatus: MutedConversationStatus,
        override val isLegalHold: Boolean = false,
        override val lastEvent: ConversationLastEvent,
        override val badgeEventType: BadgeEventType
    ) : ConversationItem()
}

data class ConversationInfo(
    val name: String,
    val membership: Membership = Membership.None,
    val unavailable: Boolean = false
)

enum class BlockingState {
    CAN_NOT_BE_BLOCKED, // we should not be able to block our own team-members
    BLOCKED,
    NOT_BLOCKED
}

val OtherUser.BlockState: BlockingState
    get() =
        when {
            connectionStatus == ConnectionState.BLOCKED -> BlockingState.BLOCKED
            userType == UserType.INTERNAL -> BlockingState.CAN_NOT_BE_BLOCKED
            else -> BlockingState.NOT_BLOCKED
        }

fun ConversationItem.PrivateConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = isLegalHold,
        membership = conversationInfo.membership,
        unavailable = conversationInfo.unavailable
    )

fun ConversationItem.ConnectionConversation.toUserInfoLabel() =
    UserInfoLabel(
        labelName = conversationInfo.name,
        isLegalHold = isLegalHold,
        membership = conversationInfo.membership
    )
