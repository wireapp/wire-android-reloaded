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

package com.wire.android.ui.authentication.start

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.ui.authentication.login.LoginState
import com.wire.android.ui.authentication.login.email.LoginEmailState
import com.wire.android.ui.authentication.login.email.LoginEmailViewModel.Companion.USER_IDENTIFIER_SAVED_STATE_KEY
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.logic.configuration.server.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartLoginViewModel @Inject constructor(
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    var state by mutableStateOf(StartLoginScreenState(ServerConfig.DEFAULT))
        private set
    val userIdentifierTextState: TextFieldState = TextFieldState()
    var loginState by mutableStateOf(LoginEmailState())

    init {
        observerAuthServer()
        viewModelScope.launch {
            userIdentifierTextState.textAsFlow().distinctUntilChanged().onEach {
                savedStateHandle[USER_IDENTIFIER_SAVED_STATE_KEY] = it.toString()
            }.collectLatest {
                updateEmailFlowState(loginState.flowState)
            }
        }
    }

    private fun updateEmailFlowState(flowState: LoginState) {
        loginState = loginState.copy(
            flowState = flowState,
            loginEnabled = userIdentifierTextState.text.isNotEmpty() && flowState !is LoginState.Loading
        )
    }

    private fun observerAuthServer() {
        viewModelScope.launch {
            authServerConfigProvider.authServer.collect {
                state = state.copy(links = it)
            }
        }
    }
}
