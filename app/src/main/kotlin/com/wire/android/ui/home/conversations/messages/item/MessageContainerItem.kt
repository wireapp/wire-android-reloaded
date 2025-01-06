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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wire.android.media.audiomessage.AudioSpeed
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.info.ConversationDetailsData
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.rememberSelfDeletionTimer
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.asset.AssetTransferStatus

@Suppress("ComplexMethod")
@Composable
fun MessageContainerItem(
    message: UIMessage,
    conversationDetailsData: ConversationDetailsData,
    clickActions: MessageClickActions,
    swipableMessageConfiguration: SwipableMessageConfiguration,
    onSelfDeletingMessageRead: (UIMessage) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    showAuthor: Boolean = true,
    useSmallBottomPadding: Boolean = false,
    audioState: AudioState? = null,
    audioSpeed: AudioSpeed = AudioSpeed.NORMAL,
    assetStatus: AssetTransferStatus? = null,
    shouldDisplayMessageStatus: Boolean = true,
    shouldDisplayFooter: Boolean = true,
    isSelectedMessage: Boolean = false,
    failureInteractionAvailable: Boolean = true,
    defaultBackgroundColor: Color = colorsScheme().surfaceContainerLow,
) {
    val selfDeletionTimerState = rememberSelfDeletionTimer(message.header.messageStatus.expirationStatus)
    if (
        selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable &&
        !message.isPending &&
        !message.sendingFailed
    ) {
        selfDeletionTimerState.StartDeletionTimer(
            message = message,
            onSelfDeletingMessageRead = onSelfDeletingMessageRead
        )
    }
    Row(
        modifier
            .customizeMessageBackground(
                sendingFailed = message.sendingFailed,
                receivingFailed = message.decryptionFailed,
                isDeleted = message.header.messageStatus.isDeleted,
                isSelectedMessage = isSelectedMessage,
                selfDeletionTimerState = selfDeletionTimerState,
                defaultBackgroundColor = defaultBackgroundColor,
            )
    ) {
        when (message) {
            is UIMessage.System -> SystemMessageItem(
                message = message,
                onFailedMessageCancelClicked = clickActions.onFailedMessageCancelClicked,
                onFailedMessageRetryClicked = clickActions.onFailedMessageRetryClicked,
                failureInteractionAvailable = failureInteractionAvailable,
            )

            is UIMessage.Regular -> RegularMessageItem(
                message = message,
                conversationDetailsData = conversationDetailsData,
                clickActions = clickActions,
                showAuthor = showAuthor,
                audioState = audioState,
                audioSpeed = audioSpeed,
                assetStatus = assetStatus,
                swipableMessageConfiguration = swipableMessageConfiguration,
                failureInteractionAvailable = failureInteractionAvailable,
                searchQuery = searchQuery,
                shouldDisplayMessageStatus = shouldDisplayMessageStatus,
                shouldDisplayFooter = shouldDisplayFooter,
                selfDeletionTimerState = selfDeletionTimerState,
                useSmallBottomPadding = useSmallBottomPadding,
            )
        }
    }
    if (message.messageContent is UIMessageContent.SystemMessage.ConversationMessageCreated) {
        Row(
            Modifier
                .background(colorsScheme().background)
                .height(dimensions().spacing24x)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .padding(start = dimensions().spacing56x)
                    .align(Alignment.CenterVertically),
                style = MaterialTheme.wireTypography.title03,
                text = (message.messageContent as UIMessageContent.SystemMessage.ConversationMessageCreated).date
            )
        }
    }
}
