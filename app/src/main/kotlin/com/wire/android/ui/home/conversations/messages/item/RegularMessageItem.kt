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

package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioSpeed
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.ui.common.LegalHoldIndicator
import com.wire.android.ui.common.StatusBox
import com.wire.android.ui.common.UserBadge
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.messages.ReactionPill
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.theme.Accent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.asset.AssetTransferStatus

// TODO: a definite candidate for a refactor and cleanup WPB-14390
@Suppress("ComplexMethod")
@Composable
fun RegularMessageItem(
    clickActions: MessageClickActions,
    message: UIMessage.Regular,
    conversationDetailsData: ConversationDetailsData,
    audioState: AudioState?,
    audioSpeed: AudioSpeed,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    assetStatus: AssetTransferStatus? = null,
    swipableMessageConfiguration: SwipableMessageConfiguration = SwipableMessageConfiguration.NotSwipable,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    failureInteractionAvailable: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState = SelfDeletionTimerHelper.SelfDeletionTimerState.NotExpirable,
): Unit = with(message) {
    @Composable
    fun messageContent() {
        MessageItemTemplate(
            modifier = modifier
                .interceptCombinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = clickActions.onFullMessageClicked?.let { onFullMessageClicked ->
                        {
                            onFullMessageClicked(message.header.messageId)
                        }
                    },
                    onLongPress = when {
                        message.header.messageStatus.isDeleted -> null // do not allow long press on deleted messages
                        else -> clickActions.onFullMessageLongClicked?.let {
                            {
                                it(message)
                            }
                        }
                    },
                ),
            showAuthor = showAuthor,
            useSmallBottomPadding = useSmallBottomPadding,
            fullAvatarOuterPadding = dimensions().avatarClickablePadding,
            leading = {
                RegularMessageItemLeading(
                    header = header,
                    showAuthor = showAuthor,
                    userAvatarData = message.userAvatarData,
                    onOpenProfile = clickActions.onProfileClicked
                )
            },
            content = {
                Column {
                    if (showAuthor) {
                        Spacer(modifier = Modifier.height(dimensions().avatarClickablePadding))
                        MessageAuthorRow(messageHeader = message.header)
                        Spacer(modifier = Modifier.height(dimensions().spacing4x))
                    }
                    if (selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
                        MessageExpireLabel(messageContent, selfDeletionTimerState.timeLeftFormatted)

                        // if the message is marked as deleted and is [SelfDeletionTimer.SelfDeletionTimerState.Expirable]
                        // the deletion responsibility belongs to the receiver, therefore we need to wait for the receiver
                        // timer to expire to permanently delete the message, in the meantime we show the EphemeralMessageExpiredLabel
                        if (isDeleted) {
                            EphemeralMessageExpiredLabel(
                                message.isMyMessage,
                                conversationDetailsData
                            )
                        }
                    } else {
                        MessageStatusLabel(messageStatus = message.header.messageStatus)
                    }

                    if (isDeleted) return@Column

                    if (!decryptionFailed) {
                        MessageContentAndStatus(
                            message = message,
                            assetStatus = assetStatus,
                            onAssetClicked = clickActions.onAssetClicked,
                            onImageClicked = clickActions.onImageClicked,
                            searchQuery = searchQuery,
                            audioState = audioState,
                            audioSpeed = audioSpeed,
                            onAudioClicked = clickActions.onPlayAudioClicked,
                            onAudioPositionChanged = clickActions.onAudioPositionChanged,
                            onProfileClicked = clickActions.onProfileClicked,
                            onLinkClicked = clickActions.onLinkClicked,
                            shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                            conversationDetailsData = conversationDetailsData,
                            onReplyClicked = clickActions.onReplyClicked,
                            onAudioSpeedChange = clickActions.onAudioSpeedChange
                        )
                        if (shouldDisplayFooter) {
                            VerticalSpace.x4()
                            MessageFooter(
                                messageFooter = messageFooter,
                                onReactionClicked = clickActions.onReactionClicked
                            )
                        }
                    } else {
                        MessageDecryptionFailure(
                            messageHeader = header,
                            decryptionStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Decryption,
                            onResetSessionClicked = clickActions.onResetSessionClicked,
                            conversationProtocol = conversationDetailsData.conversationProtocol
                        )
                    }
                    if (message.sendingFailed) {
                        MessageSendFailureWarning(
                            messageStatus = header.messageStatus.flowStatus as MessageFlowStatus.Failure.Send,
                            isInteractionAvailable = failureInteractionAvailable,
                            onRetryClick = remember(message) {
                                {
                                    clickActions.onFailedMessageRetryClicked(
                                        header.messageId,
                                        message.conversationId
                                    )
                                }
                            },
                            onCancelClick = remember(message) {
                                {
                                    clickActions.onFailedMessageCancelClicked(header.messageId)
                                }
                            }
                        )
                    }
                }
            }
        )
    }
    if (swipableMessageConfiguration is SwipableMessageConfiguration.SwipableToReply && isReplyable) {
        val onSwipe =
            remember(message) { { swipableMessageConfiguration.onSwipedToReply(message) } }
        SwipableToReplyBox(onSwipedToReply = onSwipe) {
            messageContent()
        }
    } else {
        messageContent()
    }
}

@Composable
fun EphemeralMessageExpiredLabel(
    isSelfMessage: Boolean,
    conversationDetailsData: ConversationDetailsData,
    modifier: Modifier = Modifier,
) {

    val stringResource = if (!isSelfMessage) {
        stringResource(id = R.string.label_information_waiting_for_deleation_when_self_not_sender)
    } else if (conversationDetailsData is ConversationDetailsData.OneOne) {
        conversationDetailsData.otherUserName?.let {
            stringResource(
                R.string.label_information_waiting_for_recipient_timer_to_expire_one_to_one,
                conversationDetailsData.otherUserName
            )
        } ?: stringResource(id = R.string.unknown_user_name)
    } else {
        stringResource(R.string.label_information_waiting_for_recipient_timer_to_expire_group)
    }

    Text(
        modifier = modifier,
        text = stringResource,
        style = typography().body05
    )
}

@Composable
fun MessageExpireLabel(messageContent: UIMessageContent?, timeLeft: String) {
    when (messageContent) {
        is UIMessageContent.Location,
        is UIMessageContent.AssetMessage,
        is UIMessageContent.AudioAssetMessage,
        is UIMessageContent.ImageMessage,
        is UIMessageContent.TextMessage -> {
            StatusBox(
                statusText = stringResource(
                    R.string.self_deleting_message_time_left,
                    timeLeft
                )
            )
        }

        is UIMessageContent.Deleted -> {
            val context = LocalContext.current

            StatusBox(
                statusText = stringResource(
                    R.string.self_deleting_message_time_left,
                    context.resources.getQuantityString(
                        R.plurals.seconds_left,
                        0,
                        0
                    )
                )
            )
        }

        else -> {}
    }
}

@Composable
private fun MessageAuthorRow(messageHeader: MessageHeader) {
    with(messageHeader) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(weight = 1f, fill = true),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Username(
                    username.asString(),
                    accent,
                    modifier = Modifier.weight(weight = 1f, fill = false)
                )
                UserBadge(
                    membership = membership,
                    connectionState = connectionState,
                    startPadding = dimensions().spacing6x,
                    isDeleted = isSenderDeleted
                )
                if (showLegalHoldIndicator) {
                    LegalHoldIndicator(modifier = Modifier.padding(start = dimensions().spacing6x))
                }
            }
            MessageTimeLabel(
                messageTime = messageHeader.messageTime.formattedDate,
                modifier = Modifier.padding(start = dimensions().spacing6x)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MessageFooter(
    messageFooter: MessageFooter,
    onReactionClicked: (String, String) -> Unit
) {
    // to eliminate adding unnecessary paddings when the list is empty
    if (messageFooter.reactions.entries.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(dimensions().spacing4x, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing6x, Alignment.Top),
        ) {
            messageFooter.reactions.entries
                .sortedBy { it.key }
                .forEach {
                    val reaction = it.key
                    val count = it.value
                    ReactionPill(
                        emoji = reaction,
                        count = count,
                        isOwn = messageFooter.ownReactions.contains(reaction),
                        onTap = {
                            onReactionClicked(messageFooter.messageId, reaction)
                        },
                    )
                }
        }
    }
}

@Composable
private fun MessageTimeLabel(
    messageTime: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = messageTime,
        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.secondaryText),
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun Username(username: String, accent: Accent, modifier: Modifier = Modifier) {
    Text(
        text = username,
        style = MaterialTheme.wireTypography.body02,
        color = MaterialTheme.wireColorScheme.wireAccentColors.getOrDefault(
            accent,
            MaterialTheme.wireColorScheme.onBackground
        ),
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun MessageStatusLabel(messageStatus: MessageStatus) {
    messageStatus.badgeText?.let {
        StatusBox(it.asString())
    }
}

@PreviewMultipleThemes
@Composable
fun LongMessageFooterPreview() = WireTheme {
    Box(modifier = Modifier.width(200.dp)) {
        MessageFooter(
            messageFooter = MessageFooter(
                messageId = "messageId",
                reactions = mapOf(
                    "👍" to 1,
                    "👎" to 2,
                    "👏" to 3,
                    "🤔" to 4,
                    "🤷" to 5,
                    "🤦" to 6,
                    "🤢" to 7
                ),
                ownReactions = setOf("👍"),
            ),
            onReactionClicked = { _, _ -> }
        )
    }
}

internal val DeliveryStatusContent.expandable
    get() = this is DeliveryStatusContent.PartialDelivery && !this.isSingleUserFailure
