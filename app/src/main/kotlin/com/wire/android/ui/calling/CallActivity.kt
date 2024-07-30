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
package com.wire.android.ui.calling

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.wire.android.appLogger
import com.wire.android.navigation.style.TransitionAnimationType
import com.wire.android.notification.CallNotificationManager
import com.wire.android.ui.LocalActivity
import com.wire.android.ui.calling.incoming.IncomingCallScreen
import com.wire.android.ui.calling.outgoing.OutgoingCallScreen
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.theme.WireTheme
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity that handles "new" call screens.
 * New call screens are: Incoming, Outgoing, in other words, one shot disposable screens.
 */
@AndroidEntryPoint
class CallActivity : AppCompatActivity() {
    @Inject
    lateinit var callNotificationManager: CallNotificationManager

    @Inject
    lateinit var proximitySensorManager: ProximitySensorManager

    val callActivityViewModel: CallActivityViewModel by viewModels()

    private val qualifiedIdMapper = QualifiedIdMapperImpl(null)

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appLogger.d("CallActivity: Creating new instance for ${hashCode()}")

        setUpScreenshotPreventionFlag(callActivityViewModel.isScreenshotCensoringConfigEnabled())
        setUpCallingFlags()

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val conversationId = intent.extras?.getString(EXTRA_CONVERSATION_ID)
        val screenType = intent.extras?.getString(EXTRA_SCREEN_TYPE)
        val userId = intent.extras?.getString(EXTRA_USER_ID)

        userId?.let {
            qualifiedIdMapper.fromStringToQualifiedID(it).run {
                callActivityViewModel.switchAccountIfNeeded(this)
            }
        }

        appLogger.i("$TAG Initializing proximity sensor..")
        proximitySensorManager.initialize()

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalActivity provides this
            ) {
                WireTheme {
                    val currentCallScreenType by remember { mutableStateOf(NewCallScreenType.byName(screenType)) }
                    currentCallScreenType?.let { currentScreenType ->
                        AnimatedContent(
                            targetState = currentScreenType,
                            transitionSpec = {
                                TransitionAnimationType.POP_UP.enterTransition.togetherWith(
                                    TransitionAnimationType.POP_UP.exitTransition
                                )
                            },
                            label = currentScreenType.name
                        ) { screenType ->
                            conversationId?.let {
                                when (screenType) {
                                    NewCallScreenType.Outgoing -> {
                                        OutgoingCallScreen(
                                            conversationId =
                                            qualifiedIdMapper.fromStringToQualifiedID(
                                                it
                                            )
                                        ) {
                                            getOngoingCallIntent(this@CallActivity, it).run {
                                                this@CallActivity.startActivity(this)
                                            }
                                            this@CallActivity.finishAndRemoveTask()
                                        }
                                    }

                                    NewCallScreenType.Incoming ->
                                        IncomingCallScreen(
                                            qualifiedIdMapper.fromStringToQualifiedID(it)
                                        ) {
                                            this@CallActivity.startActivity(
                                                getOngoingCallIntent(this@CallActivity, it)
                                            )
                                            this@CallActivity.finishAndRemoveTask()
                                        }
                                }
                            }
                        }
                    } ?: run { finish() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        proximitySensorManager.registerListener()
    }

    override fun onPause() {
        super.onPause()
        proximitySensorManager.unRegisterListener()
    }

    override fun onDestroy() {
        cleanUpCallingFlags()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CallActivity"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_USER_ID = "user_id"
        const val EXTRA_SCREEN_TYPE = "screen_type"
    }
}

fun getOutgoingCallIntent(
    activity: Activity,
    conversationId: String
) = Intent(activity, CallActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
    putExtra(CallActivity.EXTRA_SCREEN_TYPE, NewCallScreenType.Outgoing.name)
}

fun getIncomingCallIntent(
    context: Context,
    conversationId: String,
    userId: String?
) = Intent(context.applicationContext, CallActivity::class.java).apply {
    putExtra(CallActivity.EXTRA_USER_ID, userId)
    putExtra(CallActivity.EXTRA_CONVERSATION_ID, conversationId)
    putExtra(CallActivity.EXTRA_SCREEN_TYPE, NewCallScreenType.Incoming.name)
}
