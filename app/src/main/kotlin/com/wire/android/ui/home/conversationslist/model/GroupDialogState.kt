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

package com.wire.android.ui.home.conversationslist.model

import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.kalium.logic.data.id.ConversationId

open class GroupDialogState(
    open val conversationId: ConversationId,
    open val conversationName: String
)

data class DialogState(
    val conversationId: ConversationId,
    val conversationName: String,
    val conversationTypeDetail: ConversationTypeDetail,
    val isArchived: Boolean,
    val isMember: Boolean
)

data class LeaveGroupDialogState(
    override val conversationId: ConversationId,
    override val conversationName: String,
    val shouldDelete: Boolean = false
) : GroupDialogState(conversationId, conversationName)
