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

package com.wire.android.framework

import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId

object TestConversationItem {
    val PRIVATE = ConversationItem.PrivateConversation(
        userAvatarData = UserAvatarData(),
        conversationId = QualifiedID("value", "domain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        lastMessageContent = null,
        badgeEventType = BadgeEventType.Blocked,
        conversationInfo = ConversationInfo("Name"),
        blockingState = BlockingState.BLOCKED,
        teamId = null,
        userId = UserId("value", "domain"),
        isArchived = false,
        mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        isFavorite = false,
        isUserDeleted = false,
        folder = null,
        playingAudio = null
    )

    val GROUP = ConversationItem.GroupConversation(
        "groupName looooooooooooooooooooooooooooooooooooong",
        conversationId = QualifiedID("value", "domain"),
        mutedStatus = MutedConversationStatus.AllAllowed,
        lastMessageContent = UILastMessageContent.TextMessage(
            MessageBody(UIText.DynamicString("Very looooooooooong messageeeeeeeeeeeeeee"))
        ),
        badgeEventType = BadgeEventType.UnreadMessage(100),
        selfMemberRole = null,
        isFromTheSameTeam = false,
        teamId = null,
        isArchived = false,
        mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
        isFavorite = false,
        folder = null,
        playingAudio = null
    )

    val CONNECTION = ConversationItem.ConnectionConversation(
        userAvatarData = UserAvatarData(),
        conversationId = QualifiedID("value", "domain"),
        mutedStatus = MutedConversationStatus.OnlyMentionsAndRepliesAllowed,
        lastMessageContent = null,
        badgeEventType = BadgeEventType.ReceivedConnectionRequest,
        conversationInfo = ConversationInfo("Name")
    )
}
