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

@file:Suppress("StringTemplate")

package com.wire.android.util

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.wire.android.appLogger
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.navigation.getBaseRoute
import com.wire.android.navigation.toDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.CreateAccountDetailsScreenDestination
import com.wire.android.ui.destinations.CreateAccountEmailScreenDestination
import com.wire.android.ui.destinations.CreateAccountSummaryScreenDestination
import com.wire.android.ui.destinations.CreatePersonalAccountOverviewScreenDestination
import com.wire.android.ui.destinations.CreateTeamAccountOverviewScreenDestination
import com.wire.android.ui.destinations.E2EIEnrollmentScreenDestination
import com.wire.android.ui.destinations.E2eiCertificateDetailsScreenDestination
import com.wire.android.ui.destinations.HomeScreenDestination
import com.wire.android.ui.destinations.ImportMediaScreenDestination
import com.wire.android.ui.destinations.InitialSyncScreenDestination
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.android.ui.destinations.MigrationScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.RegisterDeviceScreenDestination
import com.wire.android.ui.destinations.RemoveDeviceScreenDestination
import com.wire.android.ui.destinations.SelfDevicesScreenDestination
import com.wire.android.ui.destinations.WelcomeScreenDestination
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CurrentScreenManager @Inject constructor(
    screenStateObserver: ScreenStateObserver
) : DefaultLifecycleObserver,
    NavController.OnDestinationChangedListener {

    private val currentScreenState = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther())

    /**
     * An integer that counts up when a screen appears, and counts down when
     * the screen goes away.
     * Better than a simple boolean in cases where an activity is re-started,
     * which may result the new instance being shown BEFORE the old instance being hidden.
     */
    private val visibilityCount = AtomicInteger(0)
    private val isApplicationVisibleFlow = MutableStateFlow(false)
    private val isAppVisibleFlow = screenStateObserver.screenStateFlow.combine(
        isApplicationVisibleFlow
    ) { isScreenOn, isOnForeground ->
        isOnForeground && isScreenOn
    }

    suspend fun observeCurrentScreen(scope: CoroutineScope): StateFlow<CurrentScreen> = isAppVisibleFlow
        .flatMapLatest { isAppVisible ->
            if (isAppVisible) currentScreenState
            else flowOf(CurrentScreen.InBackground)
        }
        .distinctUntilChanged()
        .stateIn(scope)

    /**
     * Informs if the UI is visible at the moment.
     * Visibility doesn't necessarily mean being on the foreground. For example,
     * if the device screen is split into multiple activities, and the app is currently not being focused,
     * the app is considered to be on the background, but **still visible and working** fine.
     */
    fun isAppVisibleFlow(): StateFlow<Boolean> = isApplicationVisibleFlow

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        appLogger.i("${TAG}: app onStart called")
        visibilityCount.getAndUpdate { currentValue ->
            val newValue = maxOf(0, currentValue + 1)
            isApplicationVisibleFlow.value = newValue > 0
            newValue
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appLogger.i("${TAG}: app onStop called")
        visibilityCount.getAndUpdate { currentValue ->
            val newValue = maxOf(0, currentValue - 1)
            isApplicationVisibleFlow.value = newValue > 0
            newValue
        }
    }

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val currentScreenName = currentScreenName()
        AnonymousAnalyticsManagerImpl.stopView(currentScreenName)

        val currentItem = destination.toDestination()
        currentScreenState.value = CurrentScreen.fromDestination(
            currentItem,
            arguments,
            isApplicationVisibleFlow.value
        )

        val newScreenName = currentScreenName()
        AnonymousAnalyticsManagerImpl.recordView(newScreenName)
    }

    private fun currentScreenName() = currentScreenState.value.let { currentScreen ->
        when (currentScreen) {
            is CurrentScreen.Home,
            is CurrentScreen.Conversation,
            is CurrentScreen.OtherUserProfile,
            is CurrentScreen.ImportMedia,
            is CurrentScreen.DeviceManager -> return@let currentScreen.toScreenName()

            is CurrentScreen.AuthRelated -> return@let currentScreen.route?.getBaseRoute() ?: currentScreen.toString()
            else -> return@let (currentScreen as? CurrentScreen.SomeOther)?.route?.getBaseRoute() ?: currentScreen.toString()
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        appLogger.i("$TAG app onCreate called")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        appLogger.i("$TAG app onResume called")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        appLogger.i("$TAG app onPause called")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        appLogger.i("$TAG app onDestroy called")
    }

    companion object {
        private const val TAG = "CurrentScreenManager"
    }
}

sealed class CurrentScreen {

    // Home Screen is being displayed
    data object Home : CurrentScreen() {
        override fun toScreenName() = "HomeScreen"
    }

    // Some Conversation is opened
    data class Conversation(val id: ConversationId) : CurrentScreen() {
        override fun toString(): String = "Conversation(${id.toString().obfuscateId()})"
        override fun toScreenName() = "ConversationScreen"
    }

    // Another User Profile Screen is opened
    data class OtherUserProfile(val id: ConversationId) : CurrentScreen() {
        override fun toString(): String = "OtherUserProfile(${id.toString().obfuscateId()})"
        override fun toScreenName() = "OtherUserProfileScreen"
    }

    // Import media screen is opened
    data object ImportMedia : CurrentScreen() {
        override fun toScreenName() = "ImportMediaScreen"
    }

    // SelfDevices screen is opened
    data object DeviceManager : CurrentScreen() {
        override fun toScreenName() = "DeviceManagerScreen"
    }

    // Auth related screen is opened
    data class AuthRelated(val route: String?) : CurrentScreen()

    // Some other screen is opened, kinda "do nothing screen"
    data class SomeOther(val route: String? = null) : CurrentScreen()

    // App is in background (screen is turned off, or covered by another app), non of the screens is visible
    data object InBackground : CurrentScreen()

    open fun toScreenName(): String = "UnknownScreen"

    companion object {
        @SuppressLint("RestrictedApi")
        @Suppress("ComplexMethod")
        fun fromDestination(destination: DestinationSpec<*>?, arguments: Bundle?, isAppVisible: Boolean): CurrentScreen {
            if (!isAppVisible) {
                return InBackground
            }
            return when (destination) {
                is HomeScreenDestination -> Home
                is ConversationScreenDestination ->
                    Conversation(destination.argsFrom(arguments).conversationId)

                is OtherUserProfileScreenDestination ->
                    destination.argsFrom(arguments).conversationId?.let { OtherUserProfile(it) } ?: SomeOther(destination.baseRoute)

                is ImportMediaScreenDestination -> ImportMedia

                is SelfDevicesScreenDestination -> DeviceManager

                is WelcomeScreenDestination,
                is LoginScreenDestination,
                is CreatePersonalAccountOverviewScreenDestination,
                is CreateTeamAccountOverviewScreenDestination,
                is CreateAccountEmailScreenDestination,
                is CreateAccountDetailsScreenDestination,
                is CreateAccountSummaryScreenDestination,
                is MigrationScreenDestination,
                is InitialSyncScreenDestination,
                is E2EIEnrollmentScreenDestination,
                is E2eiCertificateDetailsScreenDestination,
                is RegisterDeviceScreenDestination,
                is RemoveDeviceScreenDestination -> AuthRelated(destination.baseRoute)

                else -> SomeOther(destination?.baseRoute)
            }
        }
    }
}
