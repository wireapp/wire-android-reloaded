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

package com.wire.android.ui.common.banner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.scopedArgs
import com.wire.android.di.ViewModelPreview
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveOtherUserSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.ObserveSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelPreview
interface SecurityClassificationViewModel {
    fun state(): SecurityClassificationType = SecurityClassificationType.NONE
}

@HiltViewModel
class SecurityClassificationViewModelImpl @Inject constructor(
    private val observeSecurityClassificationLabel: ObserveSecurityClassificationLabelUseCase,
    private val observeOtherUserSecurityClassificationLabel: ObserveOtherUserSecurityClassificationLabelUseCase,
    savedStateHandle: SavedStateHandle
) : SecurityClassificationViewModel, ViewModel() {

    private val args: SecurityClassificationArgs = savedStateHandle.scopedArgs()

    private var state by mutableStateOf(SecurityClassificationType.NONE)

    override fun state(): SecurityClassificationType = state

    init {
        when (args) {
            is SecurityClassificationArgs.Conversation -> fetchConversationClassificationType(args.id)
            is SecurityClassificationArgs.User -> fetchUserClassificationType(args.id)
        }
    }

    private fun fetchConversationClassificationType(conversationId: ConversationId) = viewModelScope.launch {
        observeSecurityClassificationLabel.invoke(conversationId)
            .collect { classificationType ->
                state = classificationType
            }
    }

    private fun fetchUserClassificationType(userId: UserId) = viewModelScope.launch {
        observeOtherUserSecurityClassificationLabel(userId).collect { classificationType ->
            state = classificationType
        }
    }
}
