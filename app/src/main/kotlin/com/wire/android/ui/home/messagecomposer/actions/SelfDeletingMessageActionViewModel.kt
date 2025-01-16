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
package com.wire.android.ui.home.messagecomposer.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.ViewModelPreview
import com.wire.android.di.scopedArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelPreview
interface SelfDeletingMessageActionViewModel {
    fun state(): SelfDeletionTimer = SelfDeletionTimer.Disabled
}

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class SelfDeletingMessageActionViewModelImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val observeSelfDeletingMessages: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    savedStateHandle: SavedStateHandle
) : SelfDeletingMessageActionViewModel, ViewModel() {

    private val args: SelfDeletingMessageActionArgs = savedStateHandle.scopedArgs()
    private val conversationId: QualifiedID = args.conversationId

    var state: SelfDeletionTimer by mutableStateOf(SelfDeletionTimer.Disabled)

    override fun state(): SelfDeletionTimer = state

    init {
        observeSelfDeletingMessagesStatus()
    }

    private fun observeSelfDeletingMessagesStatus() {
        viewModelScope.launch(dispatchers.io()) {
            observeSelfDeletingMessages(conversationId, considerSelfUserSettings = true)
                .flowOn(dispatchers.io())
                .collectLatest { selfDeletingStatus ->
                    withContext(dispatchers.main()) {
                        state = selfDeletingStatus
                    }
                }
        }
    }
}
