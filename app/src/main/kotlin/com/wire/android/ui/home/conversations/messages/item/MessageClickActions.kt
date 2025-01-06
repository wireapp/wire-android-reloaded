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

import com.wire.android.media.audiomessage.AudioSpeed
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

sealed class MessageClickActions {
    open val onFullMessageClicked: ((messageId: String) -> Unit)? = null
    open val onFullMessageLongClicked: ((UIMessage.Regular) -> Unit)? = null
    open val onProfileClicked: (String) -> Unit = {}
    open val onReactionClicked: (String, String) -> Unit = { _, _ -> }
    open val onAssetClicked: (String) -> Unit = {}
    open val onPlayAudioClicked: (String) -> Unit = {}
    open val onAudioPositionChanged: (String, Int) -> Unit = { _, _ -> }
    open val onAudioSpeedChange: (AudioSpeed) -> Unit = { _ -> }
    open val onImageClicked: (UIMessage.Regular, Boolean) -> Unit = { _, _ -> }
    open val onLinkClicked: (String) -> Unit = {}
    open val onReplyClicked: (UIMessage.Regular) -> Unit = {}
    open val onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit = { _, _ -> }
    open val onFailedMessageRetryClicked: (String, ConversationId) -> Unit = { _, _ -> }
    open val onFailedMessageCancelClicked: (String) -> Unit = {}

    data class FullItem(
        override val onFullMessageLongClicked: ((UIMessage.Regular) -> Unit)? = null,
        override val onFullMessageClicked: (messageId: String) -> Unit = {},
    ) : MessageClickActions()

    data class Content(
        override val onFullMessageLongClicked: ((UIMessage.Regular) -> Unit)? = null,
        override val onProfileClicked: (String) -> Unit = {},
        override val onReactionClicked: (String, String) -> Unit = { _, _ -> },
        override val onAssetClicked: (String) -> Unit = {},
        override val onPlayAudioClicked: (String) -> Unit = {},
        override val onAudioPositionChanged: (String, Int) -> Unit = { _, _ -> },
        override val onAudioSpeedChange: (AudioSpeed) -> Unit = { _ -> },
        override val onImageClicked: (UIMessage.Regular, Boolean) -> Unit = { _, _ -> },
        override val onLinkClicked: (String) -> Unit = {},
        override val onReplyClicked: (UIMessage.Regular) -> Unit = {},
        override val onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit = { _, _ -> },
        override val onFailedMessageRetryClicked: (String, ConversationId) -> Unit = { _, _ -> },
        override val onFailedMessageCancelClicked: (String) -> Unit = {},
    ) : MessageClickActions()
}
