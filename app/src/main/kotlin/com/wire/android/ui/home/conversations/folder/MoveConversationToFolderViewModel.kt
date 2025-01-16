/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.folder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelPreview
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.folder.MoveConversationToFolderUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@ViewModelPreview
interface MoveConversationToFolderViewModel {
    val infoMessage: SharedFlow<UIText>
        get() = MutableSharedFlow()

    fun actionableState(): MoveConversationToFolderState = MoveConversationToFolderState()
    fun moveConversationToFolder(folder: ConversationFolder) {}
}

@HiltViewModel(assistedFactory = MoveConversationToFolderViewModelImpl.Factory::class)
class MoveConversationToFolderViewModelImpl @AssistedInject constructor(
    private val dispatchers: DispatcherProvider,
    @Assisted val args: MoveConversationToFolderArgs,
    private val moveConversationToFolder: MoveConversationToFolderUseCase,
) : MoveConversationToFolderViewModel, ViewModel() {

    private var state: MoveConversationToFolderState by mutableStateOf(MoveConversationToFolderState())

    @AssistedFactory
    interface Factory {
        fun create(args: MoveConversationToFolderArgs): MoveConversationToFolderViewModelImpl
    }

    private val _infoMessage = MutableSharedFlow<UIText>()
    override val infoMessage = _infoMessage.asSharedFlow()

    override fun actionableState(): MoveConversationToFolderState = state
    override fun moveConversationToFolder(folder: ConversationFolder) {
        viewModelScope.launch {
            state = state.copy(isPerformingAction = true)
            val result = withContext(dispatchers.io()) {
                moveConversationToFolder.invoke(
                    args.conversationId,
                    folder.id,
                    args.currentFolderId
                )
            }
            when (result) {
                is MoveConversationToFolderUseCase.Result.Failure -> _infoMessage.emit(
                    UIText.StringResource(
                        R.string.move_to_folder_failed,
                        args.conversationName,
                    )
                )

                MoveConversationToFolderUseCase.Result.Success -> _infoMessage.emit(
                    UIText.StringResource(
                        R.string.move_to_folder_success,
                        args.conversationName,
                        folder.name
                    )
                )
            }
            state = state.copy(isPerformingAction = false)
        }
    }
}

data class MoveConversationToFolderState(
    val isPerformingAction: Boolean = false,
)

@Serializable
data class MoveConversationToFolderArgs(
    val conversationId: ConversationId,
    val conversationName: String,
    val currentFolderId: String?,
) : ScopedArgs {
    override val key = "$ARGS_KEY:$conversationId$currentFolderId"

    companion object {
        const val ARGS_KEY = "MoveConversationToFolderArgsKey"
    }
}
