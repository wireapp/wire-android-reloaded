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

package com.wire.android.ui.common.bottomsheet.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId

class ConversationSheetState(
    conversationSheetContent: ConversationSheetContent? = null,
    conversationOptionNavigation: ConversationOptionNavigation = ConversationOptionNavigation.Home
) {
    val startOptionNavigation = conversationOptionNavigation

    var conversationSheetContent: ConversationSheetContent? by mutableStateOf(conversationSheetContent)

    var currentOptionNavigation: ConversationOptionNavigation by mutableStateOf(conversationOptionNavigation)

    val conversationId: ConversationId?
        get() = conversationSheetContent?.conversationId

    fun muteConversation(mutedConversationStatus: MutedConversationStatus) {
        conversationSheetContent = conversationSheetContent?.copy(mutingConversationState = mutedConversationStatus)
    }

    fun toMutingNotificationOption() {
        currentOptionNavigation = ConversationOptionNavigation.MutingNotificationOption
    }

    fun toHome() {
        currentOptionNavigation = ConversationOptionNavigation.Home
    }
}

@Composable
fun rememberConversationSheetState(
    conversationItem: ConversationItem,
    conversationOptionNavigation: ConversationOptionNavigation,
    isConversationDeletionLocallyRunning: Boolean
): ConversationSheetState {
    val conversationSheetContent: ConversationSheetContent = when (conversationItem) {
        is ConversationItem.GroupConversation -> {
            with(conversationItem) {
                ConversationSheetContent(
                    conversationId = conversationId,
                    title = groupName.ifEmpty { stringResource(id = R.string.member_name_deleted_label) },
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Group(
                        conversationId = conversationId,
                        isFromTheSameTeam = isFromTheSameTeam
                    ),
                    isTeamConversation = teamId != null,
                    selfRole = selfMemberRole,
                    isArchived = conversationItem.isArchived,
                    protocol = Conversation.ProtocolInfo.Proteus,
                    mlsVerificationStatus = Conversation.VerificationStatus.VERIFIED,
                    proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED,
                    isUnderLegalHold = showLegalHoldIndicator,
                    isFavorite = isFavorite,
                    isDeletingConversationLocallyRunning = isConversationDeletionLocallyRunning
                )
            }
        }

        is ConversationItem.PrivateConversation -> {
            with(conversationItem) {
                ConversationSheetContent(
                    conversationId = conversationId,
                    title = if (conversationInfo.isSenderUnavailable) {
                        stringResource(id = R.string.username_unavailable_label)
                    } else conversationInfo.name,
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Private(
                        avatarAsset = userAvatarData.asset,
                        userId = userId,
                        blockingState = blockingState,
                        isUserDeleted = isUserDeleted
                    ),
                    isTeamConversation = isTeamConversation,
                    selfRole = Conversation.Member.Role.Member,
                    isArchived = conversationItem.isArchived,
                    protocol = Conversation.ProtocolInfo.Proteus,
                    mlsVerificationStatus = Conversation.VerificationStatus.VERIFIED,
                    proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED,
                    isUnderLegalHold = showLegalHoldIndicator,
                    isFavorite = isFavorite,
                    isDeletingConversationLocallyRunning = false
                )
            }
        }

        is ConversationItem.ConnectionConversation -> {
            with(conversationItem) {
                ConversationSheetContent(
                    conversationId = conversationId,
                    title = conversationInfo.name,
                    mutingConversationState = mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Connection(
                        userAvatarData.asset
                    ),
                    isTeamConversation = isTeamConversation,
                    selfRole = Conversation.Member.Role.Member,
                    isArchived = conversationItem.isArchived,
                    protocol = Conversation.ProtocolInfo.Proteus,
                    mlsVerificationStatus = Conversation.VerificationStatus.VERIFIED,
                    proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED,
                    isUnderLegalHold = showLegalHoldIndicator,
                    isFavorite = null,
                    isDeletingConversationLocallyRunning = false
                )
            }
        }
    }

    return remember(conversationItem, conversationOptionNavigation, isConversationDeletionLocallyRunning) {
        ConversationSheetState(
            conversationSheetContent = conversationSheetContent,
            conversationOptionNavigation = conversationOptionNavigation
        )
    }
}

@Composable
fun rememberConversationSheetState(
    conversationSheetContent: ConversationSheetContent?,
    conversationOptionNavigation: ConversationOptionNavigation = ConversationOptionNavigation.Home
): ConversationSheetState {
    return remember(conversationSheetContent) {
        ConversationSheetState(
            conversationSheetContent = conversationSheetContent,
            conversationOptionNavigation = conversationOptionNavigation
        )
    }
}
