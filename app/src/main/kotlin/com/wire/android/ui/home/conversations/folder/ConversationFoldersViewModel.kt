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
import com.wire.android.di.ScopedArgs
import com.wire.android.di.ViewModelPreview
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@ViewModelPreview
interface ConversationFoldersViewModel {
    fun state(): ConversationFoldersState = ConversationFoldersState(persistentListOf())
    fun onFolderSelected(folderId: String) {}
}

@HiltViewModel(assistedFactory = ConversationFoldersViewModelImpl.Factory::class)
class ConversationFoldersViewModelImpl @AssistedInject constructor(
    @Assisted val args: ConversationFoldersStateArgs,
    private val observeUserFoldersUseCase: ObserveUserFoldersUseCase,
) : ConversationFoldersViewModel, ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: ConversationFoldersStateArgs): ConversationFoldersViewModelImpl
    }

    private var state by mutableStateOf(ConversationFoldersState(persistentListOf(), args.selectedFolderId))

    override fun state(): ConversationFoldersState = state

    init {
        observeUserFolders()
    }

    private fun observeUserFolders() = viewModelScope.launch {
        observeUserFoldersUseCase()
            .collect { folders ->
                state = state.copy(folders = folders.toPersistentList())
            }
    }

    override fun onFolderSelected(folderId: String) {
        state = state.copy(selectedFolderId = folderId)
    }
}

data class ConversationFoldersState(val folders: PersistentList<ConversationFolder>, val selectedFolderId: String? = null)

@Serializable
data class ConversationFoldersStateArgs(val selectedFolderId: String?) : ScopedArgs {

    override val key = "$ARGS_KEY:$selectedFolderId"

    companion object {
        const val ARGS_KEY = "ConversationFoldersStateArgsKey"
    }
}
