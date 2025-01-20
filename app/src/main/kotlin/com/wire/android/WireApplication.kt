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

package com.wire.android

import android.app.Activity
import android.content.ComponentCallbacks2
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import co.touchlab.kermit.platformLogWriter
import com.wire.android.analytics.ObserveCurrentSessionAnalyticsUseCase
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.AnonymousAnalyticsRecorderImpl
import com.wire.android.feature.analytics.globalAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.feature.analytics.model.AnalyticsSettings
import com.wire.android.util.AppNameUtil
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.DataDogLogger
import com.wire.android.util.LogFileWriter
import com.wire.android.util.getGitBuildId
import com.wire.android.util.lifecycle.ConnectionPolicyManager
import com.wire.android.workmanager.WireWorkerFactory
import com.wire.kalium.logger.KaliumLogLevel
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreLogger
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class WireApplication : BaseApp() {

    @Inject
    @KaliumCoreLogic
    lateinit var coreLogic: Lazy<CoreLogic>

    @Inject
    lateinit var logFileWriter: Lazy<LogFileWriter>

    @Inject
    lateinit var connectionPolicyManager: Lazy<ConnectionPolicyManager>

    @Inject
    lateinit var wireWorkerFactory: Lazy<WireWorkerFactory>

    @Inject
    lateinit var globalObserversManager: Lazy<GlobalObserversManager>

    @Inject
    lateinit var globalDataStore: Lazy<GlobalDataStore>

    @Inject
    lateinit var userDataStoreProvider: Lazy<UserDataStoreProvider>

    @Inject
    @ApplicationScope
    lateinit var globalAppScope: CoroutineScope

    @Inject
    lateinit var currentScreenManager: CurrentScreenManager

    @Inject
    lateinit var analyticsManager: Lazy<AnonymousAnalyticsManager>

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(wireWorkerFactory.get())
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        enableStrictMode()

        startActivityLifecycleCallback()

        globalAppScope.launch {
            initializeApplicationLoggingFrameworks()

            appLogger.i("$TAG app lifecycle")
            withContext(Dispatchers.Main) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(currentScreenManager)
            }
            connectionPolicyManager.get().startObservingAppLifecycle()

            appLogger.i("$TAG api version update")
            // TODO: Can be handled in one of Sync steps
            coreLogic.get().updateApiVersionsScheduler.schedulePeriodicApiVersionUpdate()

            appLogger.i("$TAG global observers")
            globalObserversManager.get().observe()

            observeRecentlyEndedCall()
        }
    }

    private suspend fun observeRecentlyEndedCall() {
        coreLogic.get().getGlobalScope().session.currentSessionFlow().filterIsInstance(CurrentSessionResult.Success::class)
            .filter { session -> session.accountInfo.isValid() }
            .flatMapLatest { session ->
                coreLogic.get().getSessionScope(session.accountInfo.userId).calls.observeRecentlyEndedCallMetadata()
            }
            .collect { metadata ->
                analyticsManager.get().sendEvent(AnalyticsEvent.RecentlyEndedCallEvent(metadata))
            }
    }

    private fun enableStrictMode() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog()
//                    .penaltyDeath() // Disabled as some devices and libraries are not compliant
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    // .penaltyDeath() TODO: add it later after fixing reported violations
                    .build()
            )
        }
    }

    @Suppress("EmptyFunctionBlock")
    private fun startActivityLifecycleCallback() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                globalAnalyticsManager.onStart(activity)
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                globalAnalyticsManager.onStop(activity)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private suspend fun initializeApplicationLoggingFrameworks() {
        // 1. Datadog should be initialized first
        ExternalLoggerManager.initDatadogLogger(applicationContext)
        // 2. Initialize our internal logging framework
        val isLoggingEnabled = globalDataStore.get().isLoggingEnabled().first()
        val config = if (isLoggingEnabled) {
            KaliumLogger.Config(
                KaliumLogLevel.VERBOSE,
                listOf(DataDogLogger, platformLogWriter())
            )
        } else {
            KaliumLogger.Config.DISABLED
        }
        // 2. Initialize our internal logging framework
        AppLogger.init(config)
        CoreLogger.init(config)
        // 3. Initialize our internal FILE logging framework
        logFileWriter.get().start()
        // 4. Everything ready, now we can log device info
        appLogger.i("Logger enabled")
        logDeviceInformation()
        // 5. Verify if we can initialize Anonymous Analytics
        initializeAnonymousAnalytics()
    }

    private fun initializeAnonymousAnalytics() {
        if (!BuildConfig.ANALYTICS_ENABLED) return

        val anonymousAnalyticsRecorder = AnonymousAnalyticsRecorderImpl()
        val analyticsSettings = AnalyticsSettings(
            countlyAppKey = BuildConfig.ANALYTICS_APP_KEY,
            countlyServerUrl = BuildConfig.ANALYTICS_SERVER_URL,
            enableDebugLogging = BuildConfig.DEBUG
        )

        val analyticsResultFlow = ObserveCurrentSessionAnalyticsUseCase(
            currentSessionFlow = coreLogic.get().getGlobalScope().session.currentSessionFlow(),
            isUserTeamMember = {
                coreLogic.get().getSessionScope(it).team.isSelfATeamMember()
            },
            observeAnalyticsTrackingIdentifierStatusFlow = {
                coreLogic.get().getSessionScope(it).observeAnalyticsTrackingIdentifierStatus()
            },
            analyticsIdentifierManagerProvider = {
                coreLogic.get().getSessionScope(it).analyticsIdentifierManager
            },
            userDataStoreProvider = userDataStoreProvider.get(),
            currentBackend = {
                coreLogic.get().getSessionScope(it).users.serverLinks()
            }
        ).invoke()

        AnonymousAnalyticsManagerImpl.init(
            context = this,
            analyticsSettings = analyticsSettings,
            analyticsResultFlow = analyticsResultFlow,
            anonymousAnalyticsRecorder = anonymousAnalyticsRecorder,
            propagationHandler = { manager, identifier ->
                manager.propagateTrackingIdentifier(identifier)
            },
            migrationHandler = { manager ->
                manager.onMigrationComplete()
            },
            dispatcher = Dispatchers.IO
        )

        AnonymousAnalyticsManagerImpl.applicationOnCreate()

        // observe the app visibility state and send AppOpen event if the app goes from the background to the foreground
        globalAppScope.launch {
            currentScreenManager
                .isAppVisibleFlow()
                .filter { isVisible -> isVisible }
                .collect {
                    AnonymousAnalyticsManagerImpl.sendEvent(AnalyticsEvent.AppOpen)
                }
        }
    }

    private fun logDeviceInformation() {
        appLogger.d(
            """
            > Device info: 
                App version=${AppNameUtil.createAppName()} 
                OS version=${Build.VERSION.SDK_INT}
                Phone model=${Build.BRAND}/${Build.MODEL}
                Commit hash=${applicationContext.getGitBuildId()}
        """.trimIndent()
        )
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        appLogger.w(
            "onTrimMemory called - App info: Memory trim level=${MemoryLevel.byLevel(level)}. " +
                    "See more at https://developer.android.com/reference/kotlin/android/content/ComponentCallbacks2"
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        appLogger.w("onLowMemory called - Stopping logging, buckling the seatbelt and hoping for the best!")
        logFileWriter.get().stop()
    }

    private companion object {
        enum class MemoryLevel(val level: Int) {
            TRIM_MEMORY_BACKGROUND(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND),
            TRIM_MEMORY_COMPLETE(ComponentCallbacks2.TRIM_MEMORY_COMPLETE),
            TRIM_MEMORY_MODERATE(ComponentCallbacks2.TRIM_MEMORY_MODERATE),
            TRIM_MEMORY_RUNNING_CRITICAL(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL),
            TRIM_MEMORY_RUNNING_LOW(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW),
            TRIM_MEMORY_RUNNING_MODERATE(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE),
            TRIM_MEMORY_UI_HIDDEN(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN),

            @Suppress("MagicNumber")
            TRIM_MEMORY_UNKNOWN(-1);

            companion object {
                fun byLevel(value: Int) =
                    entries.firstOrNull { it.level == value } ?: TRIM_MEMORY_UNKNOWN
            }
        }

        private const val TAG = "WireApplication"
    }
}
