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

package com.wire.android.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.userDatabase.ShouldTriggerMigrationForUserUserCase
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class HomeViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val globalDataStore: GlobalDataStore,
    private val getSelf: GetSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val shouldTriggerMigrationForUser: ShouldTriggerMigrationForUserUserCase,
) : SavedStateViewModel(savedStateHandle) {

    var homeState by mutableStateOf(HomeState())
        private set

    init {
        loadUserAvatar()
    }

    fun checkRequirements(onRequirement: (HomeRequirement) -> Unit) {
        viewModelScope.launch {
            val userId = getSelf().first().id
            when {
                shouldTriggerMigrationForUser(userId) ->
                    onRequirement(HomeRequirement.Migration(userId))
                needsToRegisterClient() -> // check if the client has been registered and open the proper screen if not
                    onRequirement(HomeRequirement.RegisterDevice)
                getSelf().first().handle.isNullOrEmpty() -> // check if the user handle has been set and open the proper screen if not
                    onRequirement(HomeRequirement.CreateAccountUsername)
                shouldDisplayWelcomeToARScreen() -> {
                    homeState = homeState.copy(shouldDisplayWelcomeMessage = true)
                }
            }
        }
    }

    private suspend fun shouldDisplayWelcomeToARScreen() =
        globalDataStore.isMigrationCompleted() && !globalDataStore.isWelcomeScreenPresented()

    private fun loadUserAvatar() {
        viewModelScope.launch {
            getSelf().collect { selfUser ->
                homeState = HomeState(
                    selfUser.previewPicture?.let { UserAvatarAsset(wireSessionImageLoader, it) },
                    selfUser.availabilityStatus
                )
            }
        }
    }

    fun dismissWelcomeMessage() {
        viewModelScope.launch {
            globalDataStore.setWelcomeScreenPresented()
            homeState = homeState.copy(shouldDisplayWelcomeMessage = false)
        }
    }
}
