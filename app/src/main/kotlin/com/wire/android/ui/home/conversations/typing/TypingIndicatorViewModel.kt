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
package com.wire.android.ui.home.conversations.typing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelPreview
import com.wire.android.di.scopedArgs
import com.wire.android.ui.home.conversations.usecase.ObserveUsersTypingInConversationUseCase
import com.wire.kalium.logic.data.id.QualifiedID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@ViewModelPreview
interface TypingIndicatorViewModel {
    fun state(): UsersTypingViewState = UsersTypingViewState()
}

@HiltViewModel
class TypingIndicatorViewModelImpl @Inject constructor(
    private val observeUsersTypingInConversation: ObserveUsersTypingInConversationUseCase,
    savedStateHandle: SavedStateHandle,
) : TypingIndicatorViewModel, ViewModel() {

    private val args: TypingIndicatorArgs = savedStateHandle.scopedArgs()
    val conversationId: QualifiedID = args.conversationId
    private var usersTypingViewState by mutableStateOf(UsersTypingViewState())
    override fun state(): UsersTypingViewState = usersTypingViewState

    init {
        observeUsersTypingState()
    }

    private fun observeUsersTypingState() {
        viewModelScope.launch {
            observeUsersTypingInConversation(conversationId).collect {
                usersTypingViewState = usersTypingViewState.copy(usersTyping = it)
            }
        }
    }
}

@Serializable
data class TypingIndicatorArgs(val conversationId: QualifiedID) : ScopedArgs {
    override val key = "$ARGS_KEY:$conversationId"
    companion object { const val ARGS_KEY = "TypingIndicatorArgsKey" }
}
