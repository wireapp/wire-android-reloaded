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

package com.wire.android.services

import android.content.Context
import android.content.Intent
import android.os.Build
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This is helper class that should be used for starting/stopping any services.
 * The idea is that we don't want to inject, or provide any context into ViewModel,
 * but to have an ability start Service from it.
 */
@Singleton
class ServicesManager @Inject constructor(
    private val context: Context,
    dispatcherProvider: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.default())
    private val callServiceEvents = MutableStateFlow(false)

    init {
        scope.launch {
            callServiceEvents
                .debounce { if (it) 0L else DEBOUNCE_TIME } // debounce to avoid starting and stopping service too fast
                .distinctUntilChanged()
                .collectLatest { shouldBeStarted ->
                    if (!shouldBeStarted) {
                        appLogger.i("$TAG: stopping CallService because there are no calls")
                        when (CallService.serviceState.get()) {
                            CallService.ServiceState.STARTED -> {
                                // Instead of simply calling stopService(CallService::class), which can end up with a crash if it
                                // happens before the service calls startForeground, we call the startService command with an empty data
                                // or some specific argument that tells the service that it should stop itself right after startForeground.
                                // This way, when this service is killed and recreated by the system, it will stop itself right after
                                // recreating so it won't cause any problems.
                                startService(CallService.newIntentToStop(context))
                                appLogger.i("$TAG: CallService stopped by passing stop argument")
                            }

                            CallService.ServiceState.FOREGROUND -> {
                                // we can just stop the service, because it's already in foreground
                                context.stopService(CallService.newIntent(context))
                                appLogger.i("$TAG: CallService stopped by calling stopService")
                            }

                            else -> {
                                appLogger.i("$TAG: CallService not running, nothing to stop")
                            }
                        }
                    } else {
                        appLogger.i("$TAG: starting CallService")
                        startService(CallService.newIntent(context))
                    }
                }
        }
    }

    fun startCallService() {
        appLogger.i("$TAG: start CallService event")
        scope.launch {
            callServiceEvents.emit(true)
        }
    }

    fun stopCallService() {
        appLogger.i("$TAG: stop CallService event")
        scope.launch {
            callServiceEvents.emit(false)
        }
    }

    // Persistent WebSocket
    fun startPersistentWebSocketService() {
        if (PersistentWebSocketService.isServiceStarted) {
            appLogger.i("$TAG: PersistentWebsocketService already started, not starting again")
        } else {
            startService(PersistentWebSocketService.newIntent(context))
        }
    }

    fun stopPersistentWebSocketService() {
        stopService(PersistentWebSocketService.newIntent(context))
    }

    fun isPersistentWebSocketServiceRunning(): Boolean =
        PersistentWebSocketService.isServiceStarted

    // Playing AudioMessage service
    fun startPlayingAudioMessageService() {
        if (PlayingAudioMessageService.isServiceStarted) {
            appLogger.i("$TAG: PlayingAudioMessageService already started, not starting again")
        } else {
            startService(PlayingAudioMessageService.newIntent(context))
        }
    }

    fun stopPlayingAudioMessageService() {
        stopService(PlayingAudioMessageService.newIntent(context))
    }

    private fun startService(intent: Intent) {
        appLogger.i("$TAG: starting service for $intent")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService(intent: Intent) {
        appLogger.i("$TAG: stopping service for $intent")
        context.stopService(intent)
    }

    companion object {
        private const val TAG = "ServicesManager"

        @VisibleForTesting
        const val DEBOUNCE_TIME = 500L
    }
}
