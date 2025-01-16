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

package com.wire.android.ui.common.bottomsheet.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.di.ViewModelPreview
import com.wire.android.model.SnackBarMessage
import com.wire.android.model.asSnackBarMessage
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelPreview
interface ChangeConversationFavoriteViewModel {
    val infoMessage: SharedFlow<SnackBarMessage>
        get() = MutableSharedFlow()

    fun changeFavoriteState(dialogState: GroupDialogState, addToFavorite: Boolean) {}
}

@HiltViewModel
class ChangeConversationFavoriteViewModelImpl @Inject constructor(
    private val addConversationToFavorites: AddConversationToFavoritesUseCase,
    private val removeConversationFromFavorites: RemoveConversationFromFavoritesUseCase,
) : ChangeConversationFavoriteViewModel, ViewModel() {

    private val _infoMessage = MutableSharedFlow<SnackBarMessage>()
    override val infoMessage = _infoMessage.asSharedFlow()

    override fun changeFavoriteState(dialogState: GroupDialogState, addToFavorite: Boolean) {
        viewModelScope.launch {
            val messageResource = if (addToFavorite) {
                when (addConversationToFavorites(dialogState.conversationId)) {
                    is AddConversationToFavoritesUseCase.Result.Failure -> R.string.error_adding_to_favorite
                    AddConversationToFavoritesUseCase.Result.Success -> R.string.success_adding_to_favorite
                }
            } else {
                when (removeConversationFromFavorites(dialogState.conversationId)) {
                    is RemoveConversationFromFavoritesUseCase.Result.Failure -> R.string.error_removing_from_favorite
                    RemoveConversationFromFavoritesUseCase.Result.Success -> R.string.success_removing_from_favorite
                }
            }
            _infoMessage.emit(UIText.StringResource(messageResource, dialogState.conversationName).asSnackBarMessage())
        }
    }
}
