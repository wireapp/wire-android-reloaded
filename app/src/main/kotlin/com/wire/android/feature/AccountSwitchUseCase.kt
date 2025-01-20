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

package com.wire.android.feature

import com.wire.android.appLogger
import com.wire.android.di.ApplicationScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.ui.authentication.welcome.WelcomeScreenNavArgs
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.server.ServerConfigForAccountUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.DeleteSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class AccountSwitchUseCase @Inject constructor(
    private val updateCurrentSession: UpdateCurrentSessionUseCase,
    private val getSessions: GetSessionsUseCase,
    private val getCurrentSession: CurrentSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val serverConfigForAccountUseCase: ServerConfigForAccountUseCase,
    @ApplicationScope private val coroutineScope: CoroutineScope
) {
    val currentAccount
        get() = coroutineScope.async {
            when (val result = getCurrentSession()) {
                is CurrentSessionResult.Failure.Generic -> null
                CurrentSessionResult.Failure.SessionNotFound -> null
                is CurrentSessionResult.Success -> result.accountInfo
            }
        }

    suspend operator fun invoke(params: SwitchAccountParam): SwitchAccountResult {
        val current = currentAccount.await()
        appLogger.i("$TAG Switching account invoked: ${params.toLogString()}, current account: ${current?.userId?.toLogString() ?: "-"}")
        return when (params) {
            is SwitchAccountParam.SwitchToAccount -> checkAccountAndSwitchIfPossible(params.userId, current)
            SwitchAccountParam.TryToSwitchToNextAccount -> getNextAccountIfPossibleAndSwitch(current)
            SwitchAccountParam.Clear -> switch(null, current)
        }
    }

    private suspend fun checkAccountAndSwitchIfPossible(userId: UserId, current: AccountInfo?): SwitchAccountResult =
        getSessions().let {
            when (it) {
                is GetAllSessionsResult.Success -> {
                    val isAccountLoggedInAndValid = it.sessions.any {
                        accountInfo ->
                            (accountInfo is AccountInfo.Valid) && (accountInfo.userId == userId)
                    }
                    if (isAccountLoggedInAndValid) {
                        switch(userId, current)
                    } else {
                        appLogger.i("$TAG Given account is not logged in or invalid: ${userId.toLogString()}")
                        return SwitchAccountResult.GivenAccountIsInvalid
                    }
                }

                is GetAllSessionsResult.Failure.Generic -> {
                    appLogger.i("$TAG Failure when switching account to: ${userId.toLogString()}")
                    SwitchAccountResult.Failure
                }

                GetAllSessionsResult.Failure.NoSessionFound -> {
                    appLogger.i("$TAG Given account is not found: ${userId.toLogString()}")
                    SwitchAccountResult.GivenAccountIsInvalid
                }
            }
        }

    private suspend fun getNextAccountIfPossibleAndSwitch(current: AccountInfo?): SwitchAccountResult {
        val nextSessionId: UserId? = getSessions().let {
            when (it) {
                is GetAllSessionsResult.Failure.Generic -> null
                GetAllSessionsResult.Failure.NoSessionFound -> null
                is GetAllSessionsResult.Success ->
                    it.sessions.firstOrNull { accountInfo ->
                        (accountInfo is AccountInfo.Valid) && (accountInfo.userId != current?.userId)
                    }?.userId
            }
        }
        if (nextSessionId == null) appLogger.i("$TAG No next account to switch to")
        else appLogger.i("$TAG Switching to next account: ${nextSessionId.toLogString()}")
        return switch(nextSessionId, current)
    }

    private suspend fun switch(userId: UserId?, current: AccountInfo?): SwitchAccountResult {
        val successResult = (userId?.let { SwitchAccountResult.SwitchedToAnotherAccount }) ?: run {
            // if there are no more accounts, we need to change the auth server config to the one of the current user
            current?.let { updateAuthServer(it.userId) }
            SwitchAccountResult.NoOtherAccountToSwitch
        }
        return when (updateCurrentSession(userId)) {
            is UpdateCurrentSessionUseCase.Result.Success -> {
                current?.also {
                    handleOldSession(it)
                }
                successResult
            }
            is UpdateCurrentSessionUseCase.Result.Failure -> {
                appLogger.i("$TAG Failure when switching account to: ${userId?.toLogString() ?: "-"}")
                SwitchAccountResult.Failure
            }
        }
    }

    private suspend fun updateAuthServer(current: UserId) {
        appLogger.i("$TAG Updating auth server config for account: ${current.toLogString()}")
        serverConfigForAccountUseCase(current).let {
            when (it) {
                is ServerConfigForAccountUseCase.Result.Success -> authServerConfigProvider.updateAuthServer(it.config)
                is ServerConfigForAccountUseCase.Result.Failure -> return
            }
        }
    }

    private fun handleOldSession(oldSession: AccountInfo) {
        when (oldSession) {
            is AccountInfo.Valid -> {
                // do nothing
            }
            is AccountInfo.Invalid -> coroutineScope.launch {
                withTimeout(DELETE_USER_SESSION_TIMEOUT) {
                    handleInvalidSession(oldSession)
                }
            }
        }
    }

    private suspend fun handleInvalidSession(invalidAccount: AccountInfo.Invalid) {
        appLogger.i("$TAG Handling invalid account: ${invalidAccount.userId.toLogString()}")
        when (invalidAccount.logoutReason) {
            LogoutReason.SELF_SOFT_LOGOUT, LogoutReason.SELF_HARD_LOGOUT -> {
                deleteSession(invalidAccount.userId)
            }
            LogoutReason.MIGRATION_TO_CC_FAILED,
            LogoutReason.DELETED_ACCOUNT,
            LogoutReason.REMOVED_CLIENT,
            LogoutReason.SESSION_EXPIRED -> deleteSession(invalidAccount.userId)
        }
    }

    private companion object {
        const val TAG = "AccountSwitch"
        const val DELETE_USER_SESSION_TIMEOUT = 3000L
    }
}

sealed class SwitchAccountParam {
    data object TryToSwitchToNextAccount : SwitchAccountParam()
    data class SwitchToAccount(val userId: UserId) : SwitchAccountParam()
    data object Clear : SwitchAccountParam()
    private fun toLogMap(): Map<String, String> = when (this) {
        is Clear -> mutableMapOf("value" to "CLEAR")
        is SwitchToAccount -> mutableMapOf("value" to "SWITCH_TO_ACCOUNT", "userId" to userId.toLogString())
        is TryToSwitchToNextAccount -> mutableMapOf("value" to "TRY_TO_SWITCH_TO_NEXT_ACCOUNT")
    }
    fun toLogString(): String = Json.encodeToString(toLogMap())
}

sealed class SwitchAccountResult {
    data object Failure : SwitchAccountResult()
    data object SwitchedToAnotherAccount : SwitchAccountResult()
    data object NoOtherAccountToSwitch : SwitchAccountResult()
    data object GivenAccountIsInvalid : SwitchAccountResult()

    fun callAction(actions: SwitchAccountActions) = when (this) {
        NoOtherAccountToSwitch -> actions.noOtherAccountToSwitch()
        SwitchedToAnotherAccount -> actions.switchedToAnotherAccount()
        else -> { /* do nothing */ }
    }
}

interface SwitchAccountActions {
    fun switchedToAnotherAccount()
    fun noOtherAccountToSwitch()
}

class NavigationSwitchAccountActions(val navigate: (NavigationCommand) -> Unit) : SwitchAccountActions {
    override fun switchedToAnotherAccount() = navigate(NavigationCommand(HomeScreenDestination, BackStackMode.CLEAR_WHOLE))
    override fun noOtherAccountToSwitch() = navigate(NavigationCommand(WelcomeScreenDestination(WelcomeScreenNavArgs()), BackStackMode.CLEAR_WHOLE))
}
