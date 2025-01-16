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
package com.wire.android.ui.home.conversations

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.scopedArgs
import com.wire.android.ui.home.conversations.model.CompositeMessageArgs
import com.wire.android.ui.navArgs
import com.wire.android.di.ViewModelPreview
import com.wire.kalium.logic.data.id.MessageButtonId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.message.composite.SendButtonActionMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelPreview
interface CompositeMessageViewModel {
    val pendingButtonId: MessageButtonId?
        get() = null
    fun sendButtonActionMessage(buttonId: String) {}
}

@HiltViewModel
class CompositeMessageViewModelImpl @Inject constructor(
    private val sendButtonActionMessageUseCase: SendButtonActionMessageUseCase,
    savedStateHandle: SavedStateHandle,
) : CompositeMessageViewModel, ViewModel() {

    private val conversationNavArgs: ConversationNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = conversationNavArgs.conversationId

    private val scopedArgs: CompositeMessageArgs = savedStateHandle.scopedArgs()
    private val messageId: String = scopedArgs.messageId

    override var pendingButtonId: MessageButtonId? by mutableStateOf(null)
        @VisibleForTesting
        set

    override fun sendButtonActionMessage(buttonId: String) {
        if (pendingButtonId != null) return

        pendingButtonId = buttonId
        viewModelScope.launch {
            sendButtonActionMessageUseCase(conversationId, messageId, buttonId)
        }.invokeOnCompletion {
            pendingButtonId = null
        }
    }
}
