/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations.details.participants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.spec.Direction
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.destinations.ServiceDetailsScreenDestination
import com.wire.android.ui.home.conversations.details.GroupDetailsBaseViewModel
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.id.QualifiedID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class GroupConversationParticipantsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase
) : GroupDetailsBaseViewModel(savedStateHandle) {

    open val maxNumberOfItems get() = -1 // -1 means return whole list

    var groupParticipantsState: GroupConversationParticipantsState by mutableStateOf(GroupConversationParticipantsState())

    private val groupConversationAllParticipantsNavArgs: GroupConversationAllParticipantsNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = groupConversationAllParticipantsNavArgs.conversationId

    init {
        observeConversationMembers()
    }

    private fun observeConversationMembers() {
        viewModelScope.launch {
            observeConversationMembers(conversationId, maxNumberOfItems)
                .collect {
                    groupParticipantsState = groupParticipantsState.copy(data = it)
                }
        }
    }

    fun navigateBack() = viewModelScope.launch {
        navigationManager.navigateBack()
    }

    fun openProfile(participant: UIParticipant, direction: Direction? = null) = viewModelScope.launch {
        if (participant.isSelf) {
            navigateToSelfProfile()
        } else if (participant.isService && participant.botService != null) {
            navigateToServiceProfile(direction ?: ServiceDetailsScreenDestination(participant.botService, conversationId))
        } else {
            navigateToOtherProfile(direction ?: OtherUserProfileScreenDestination(participant.id, conversationId))
        }
    }

    private suspend fun navigateToSelfProfile() {
        navigationManager.navigate(NavigationCommand(SelfUserProfileScreenDestination))
    }

    private suspend fun navigateToOtherProfile(direction: Direction) {
        navigationManager.navigate(NavigationCommand(direction))
    }

    private suspend fun navigateToServiceProfile(direction: Direction) {
        navigationManager.navigate(NavigationCommand(direction))
    }
}
