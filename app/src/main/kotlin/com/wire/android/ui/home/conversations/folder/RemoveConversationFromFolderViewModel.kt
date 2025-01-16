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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.di.ViewModelPreview
import com.wire.android.model.DefaultSnackBarMessage
import com.wire.android.model.SnackBarMessage
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelPreview
interface RemoveConversationFromFolderViewModel {
    val infoMessage: SharedFlow<SnackBarMessage>
        get() = MutableSharedFlow()

    fun removeFromFolder(conversationId: ConversationId, conversationName: String, folder: ConversationFolder) {}
}

@HiltViewModel
class RemoveConversationFromFolderViewModelImpl @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val removeConversationFromFolder: RemoveConversationFromFolderUseCase,
) : ViewModel(), RemoveConversationFromFolderViewModel {

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    override val infoMessage = _infoMessage.asSharedFlow()

    override fun removeFromFolder(conversationId: ConversationId, conversationName: String, folder: ConversationFolder) {
        viewModelScope.launch {
            val result = withContext(dispatchers.io()) {
                removeConversationFromFolder.invoke(
                    conversationId,
                    folder.id
                )
            }
            when (result) {
                is RemoveConversationFromFolderUseCase.Result.Failure -> _infoMessage.emit(
                    DefaultSnackBarMessage(
                        UIText.StringResource(
                            R.string.remove_from_folder_failed,
                            conversationName,
                        )
                    )
                )

                RemoveConversationFromFolderUseCase.Result.Success -> _infoMessage.emit(
                    DefaultSnackBarMessage(
                        UIText.StringResource(
                            R.string.remove_from_folder_success,
                            conversationName,
                            folder.name
                        )
                    )
                )
            }
        }
    }
}
