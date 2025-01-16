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
package com.wire.android.mapper

import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.UILastMessageContent
import com.wire.android.ui.home.conversationslist.model.BadgeEventType
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.ConversationInfo
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.PlayingAudioInConversation
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.ConversationDetails.Connection
import com.wire.kalium.logic.data.conversation.ConversationDetails.Group
import com.wire.kalium.logic.data.conversation.ConversationDetails.OneOne
import com.wire.kalium.logic.data.conversation.ConversationDetails.Self
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithEvents
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.conversation.UnreadEventCount
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.message.UnreadEventType
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus

@Suppress("LongMethod")
fun ConversationDetailsWithEvents.toConversationItem(
    userTypeMapper: UserTypeMapper,
    searchQuery: String,
    selfUserTeamId: TeamId?,
    playingAudioMessage: PlayingAudioMessage
): ConversationItem = when (val conversationDetails = this.conversationDetails) {
    is Group -> {
        ConversationItem.GroupConversation(
            groupName = conversationDetails.conversation.name.orEmpty(),
            conversationId = conversationDetails.conversation.id,
            mutedStatus = conversationDetails.conversation.mutedStatus,
            showLegalHoldIndicator = conversationDetails.conversation.legalHoldStatus.showLegalHoldIndicator(),
            lastMessageContent = lastMessage.toUIPreview(unreadEventCount),
            badgeEventType = parseConversationEventType(
                mutedStatus = conversationDetails.conversation.mutedStatus,
                unreadEventCount = unreadEventCount
            ),
            hasOnGoingCall = conversationDetails.hasOngoingCall && conversationDetails.isSelfUserMember,
            isFromTheSameTeam = conversationDetails.conversation.teamId == selfUserTeamId,
            isSelfUserMember = conversationDetails.isSelfUserMember,
            teamId = conversationDetails.conversation.teamId,
            selfMemberRole = conversationDetails.selfRole,
            isArchived = conversationDetails.conversation.archived,
            mlsVerificationStatus = conversationDetails.conversation.mlsVerificationStatus,
            proteusVerificationStatus = conversationDetails.conversation.proteusVerificationStatus,
            hasNewActivitiesToShow = hasNewActivitiesToShow,
            searchQuery = searchQuery,
            isFavorite = conversationDetails.isFavorite,
            folder = conversationDetails.folder,
            playingAudio = getPlayingAudioInConversation(playingAudioMessage, conversationDetails)
        )
    }

    is OneOne -> {
        ConversationItem.PrivateConversation(
            userAvatarData = UserAvatarData(
                asset = conversationDetails.otherUser.previewPicture?.let { UserAvatarAsset(it) },
                availabilityStatus = conversationDetails.otherUser.availabilityStatus,
                connectionState = conversationDetails.otherUser.connectionStatus,
                nameBasedAvatar = NameBasedAvatar(conversationDetails.otherUser.name, conversationDetails.otherUser.accentId)
            ),
            conversationInfo = ConversationInfo(
                name = conversationDetails.otherUser.name.orEmpty(),
                membership = userTypeMapper.toMembership(conversationDetails.userType),
                isSenderUnavailable = conversationDetails.otherUser.isUnavailableUser
            ),
            conversationId = conversationDetails.conversation.id,
            mutedStatus = conversationDetails.conversation.mutedStatus,
            showLegalHoldIndicator = conversationDetails.conversation.legalHoldStatus.showLegalHoldIndicator(),
            lastMessageContent = lastMessage.toUIPreview(unreadEventCount),
            badgeEventType = parsePrivateConversationEventType(
                conversationDetails.otherUser.connectionStatus,
                conversationDetails.otherUser.deleted,
                parseConversationEventType(
                    mutedStatus = conversationDetails.conversation.mutedStatus,
                    unreadEventCount = unreadEventCount
                )
            ),
            userId = conversationDetails.otherUser.id,
            blockingState = conversationDetails.otherUser.BlockState,
            isUserDeleted = conversationDetails.otherUser.deleted,
            teamId = conversationDetails.otherUser.teamId,
            isArchived = conversationDetails.conversation.archived,
            mlsVerificationStatus = conversationDetails.conversation.mlsVerificationStatus,
            proteusVerificationStatus = conversationDetails.conversation.proteusVerificationStatus,
            hasNewActivitiesToShow = hasNewActivitiesToShow,
            searchQuery = searchQuery,
            isFavorite = conversationDetails.isFavorite,
            folder = conversationDetails.folder,
            playingAudio = getPlayingAudioInConversation(playingAudioMessage, conversationDetails)
        )
    }

    is Connection -> {
        ConversationItem.ConnectionConversation(
            userAvatarData = UserAvatarData(
                asset = conversationDetails.otherUser?.previewPicture?.let { UserAvatarAsset(it) },
                availabilityStatus = conversationDetails.otherUser?.availabilityStatus ?: UserAvailabilityStatus.NONE,
                nameBasedAvatar = NameBasedAvatar(conversationDetails.otherUser?.name, conversationDetails.otherUser?.accentId ?: -1)
            ),
            conversationInfo = ConversationInfo(
                name = conversationDetails.otherUser?.name.orEmpty(),
                membership = userTypeMapper.toMembership(conversationDetails.userType),
                isSenderUnavailable = conversationDetails.otherUser?.isUnavailableUser ?: true
            ),
            lastMessageContent = UILastMessageContent.Connection(
                connectionState = conversationDetails.connection.status,
                userId = conversationDetails.connection.qualifiedToId
            ),
            badgeEventType = parseConnectionEventType(conversationDetails.connection.status),
            conversationId = conversationDetails.conversation.id,
            mutedStatus = conversationDetails.conversation.mutedStatus,
            hasNewActivitiesToShow = hasNewActivitiesToShow,
            searchQuery = searchQuery,
        )
    }

    is Self -> {
        throw IllegalArgumentException("Self conversations should not be visible to the user.")
    }

    else -> {
        throw IllegalArgumentException("$this conversations should not be visible to the user.")
    }
}

private fun getPlayingAudioInConversation(
    playingAudioMessage: PlayingAudioMessage,
    conversationDetails: ConversationDetails
): PlayingAudioInConversation? =
    if (playingAudioMessage is PlayingAudioMessage.Some
        && playingAudioMessage.conversationId == conversationDetails.conversation.id
    ) {
        if (playingAudioMessage.state.isPlaying()) {
            PlayingAudioInConversation(playingAudioMessage.messageId, false)
        } else if (playingAudioMessage.state.audioMediaPlayingState is AudioMediaPlayingState.Paused) {
            PlayingAudioInConversation(playingAudioMessage.messageId, true)
        } else {
            // states Fetching, Completed, Stopped, etc. should not be shown in ConversationItem
            null
        }
    } else {
        null
    }

private fun parseConnectionEventType(connectionState: ConnectionState) =
    if (connectionState == ConnectionState.SENT) {
        BadgeEventType.SentConnectRequest
    } else {
        BadgeEventType.ReceivedConnectionRequest
    }

private fun parsePrivateConversationEventType(
    connectionState: ConnectionState,
    isDeleted: Boolean,
    eventType: BadgeEventType
) =
    if (connectionState == ConnectionState.BLOCKED) {
        BadgeEventType.Blocked
    } else if (isDeleted) {
        BadgeEventType.Deleted
    } else {
        eventType
    }

private fun parseConversationEventType(
    mutedStatus: MutedConversationStatus,
    unreadEventCount: UnreadEventCount
): BadgeEventType = when (mutedStatus) {
    MutedConversationStatus.AllMuted -> BadgeEventType.None
    MutedConversationStatus.OnlyMentionsAndRepliesAllowed ->
        when {
            unreadEventCount.containsKey(UnreadEventType.MENTION) -> BadgeEventType.UnreadMention
            unreadEventCount.containsKey(UnreadEventType.REPLY) -> BadgeEventType.UnreadReply
            unreadEventCount.containsKey(UnreadEventType.MISSED_CALL) -> BadgeEventType.MissedCall
            else -> BadgeEventType.None
        }

    else -> {
        val unreadMessagesCount = unreadEventCount.values.sum()
        when {
            unreadEventCount.containsKey(UnreadEventType.KNOCK) -> BadgeEventType.Knock
            unreadEventCount.containsKey(UnreadEventType.MISSED_CALL) -> BadgeEventType.MissedCall
            unreadEventCount.containsKey(UnreadEventType.MENTION) -> BadgeEventType.UnreadMention
            unreadEventCount.containsKey(UnreadEventType.REPLY) -> BadgeEventType.UnreadReply
            unreadMessagesCount > 0 -> BadgeEventType.UnreadMessage(unreadMessagesCount)
            else -> BadgeEventType.None
        }
    }
}
