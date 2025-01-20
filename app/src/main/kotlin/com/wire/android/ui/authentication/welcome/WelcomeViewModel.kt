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

package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val getSessions: GetSessionsUseCase,
) : ViewModel() {

    private val welcomeScreenNavArgs: WelcomeScreenNavArgs = savedStateHandle.navArgs()
    var state by mutableStateOf(WelcomeScreenState(ServerConfig.DEFAULT))
        private set

    init {
        observerAuthServer()
        checkNumberOfSessions()
        resolveNavigationToStartLogin()
    }

    private fun observerAuthServer() {
        viewModelScope.launch {
            authServerConfigProvider.authServer.collect {
                state = state.copy(links = it)
            }
        }
    }

    private fun checkNumberOfSessions() {
        viewModelScope.launch {
            getSessions().let {
                when (it) {
                    is GetAllSessionsResult.Success -> {
                        state = state.copy(
                            isThereActiveSession = it.sessions.filterIsInstance<AccountInfo.Valid>().isEmpty().not(),
                            maxAccountsReached = it.sessions.filterIsInstance<AccountInfo.Valid>().size >= BuildConfig.MAX_ACCOUNTS
                        )
                    }

                    is GetAllSessionsResult.Failure.Generic -> {}
                    GetAllSessionsResult.Failure.NoSessionFound -> {
                        state = state.copy(isThereActiveSession = false)
                    }
                }
            }
        }
    }

    private fun resolveNavigationToStartLogin() {
        state = if (welcomeScreenNavArgs.isCustomBackend) {
            state.copy(startLoginDestination = StartLoginDestination.CustomBackend)
        } else {
            state.copy(startLoginDestination = StartLoginDestination.Default)
        }
    }
}

fun ServerConfig.Links.isProxyEnabled() = this.apiProxy != null
