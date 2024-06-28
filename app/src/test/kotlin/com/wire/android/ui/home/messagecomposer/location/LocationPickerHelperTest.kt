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
package com.wire.android.ui.home.messagecomposer.location

import android.app.Application
import android.content.Context
<<<<<<< HEAD
import android.location.LocationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
=======
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
>>>>>>> 395395269 (fix: location sharing without gms when not moving [WPB-9724] (#3136))
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
<<<<<<< HEAD

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class LocationPickerHelperTest {

    @Test
    fun `given user has device location disabled, when sharing location, then error lambda is called`() = runTest {
        // given
        val (arrangement, locationHelper) = Arrangement()
            .withLocationEnabled(false)
            .arrange()

        // when - then
        locationHelper.getLocation(
            onSuccess = {
                assertTrue(false) // this should not be called, so it will fail the test otherwise.
            },
            onError = { assertTrue(true) }
        )
    }

    @Test
    fun `given user has device location enabled, when sharing location, then on success lambda is called`() = runTest {
        // given
        val (arrangement, locationHelper) = Arrangement()
            .withLocationEnabled(true)
            .arrange()

        // when - then
        locationHelper.getLocation(
            onSuccess = {
                assertTrue(true)
            },
            onError = {
                assertTrue(false) // this should not be called, so it will fail the test otherwise.
            }
        )
    }

    private class Arrangement {
        val context: Context = ApplicationProvider.getApplicationContext()
        val locationManager: LocationManager = context.getSystemService(Application.LOCATION_SERVICE) as LocationManager

        init {
            shadowOf(locationManager).apply {
                setProviderEnabled(LocationManager.GPS_PROVIDER, true)
                setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)
            }
        }

        fun withLocationEnabled(enabled: Boolean) = apply {
            locationManager.apply {
                shadowOf(this).apply {
                    setLocationEnabled(enabled)
                }
            }
        }

        fun arrange() = this to LocationPickerHelperFlavor(context)
=======
import org.robolectric.shadows.ShadowSystemClock
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class LocationPickerHelperTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    @Config(sdk = [Build.VERSION_CODES.P, Build.VERSION_CODES.R])
    fun `given last location not too old, then emit last location`() = runTest(dispatcher) {
        // given
        val resultHandler = ResultHandler()
        val (arrangement, locationPickerHelper) = Arrangement().arrange()
        val location = Location(LocationManager.FUSED_PROVIDER).apply {
            latitude = 1.0
            longitude = 1.0
            elapsedRealtimeNanos = arrangement.lastLocationTimeLimit.inWholeNanoseconds - 1.seconds.inWholeNanoseconds
            time = dispatcher.scheduler.currentTime - elapsedRealtimeNanos.nanoseconds.inWholeMilliseconds
            bearing = 0f
        }
        arrangement.updateLocation(location)
        shadowOf(Looper.getMainLooper()).idle()

        // when
        locationPickerHelper.getLocationWithoutGms(resultHandler::onSuccess, resultHandler::onError)

        // then
        resultHandler.assert(expectedErrorCount = 0, expectedLocations = listOf(GeoLocatedAddress(arrangement.address, location)))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P, Build.VERSION_CODES.R])
    fun `given last location too old, when new location comes before timeout, then emit new location`() = runTest(dispatcher) {
        // given
        val resultHandler = ResultHandler()
        val (arrangement, locationPickerHelper) = Arrangement().arrange()
        val lastLocation = Location(LocationManager.FUSED_PROVIDER).apply {
            latitude = 1.0
            longitude = 1.0
            elapsedRealtimeNanos = arrangement.lastLocationTimeLimit.inWholeNanoseconds + 1.seconds.inWholeNanoseconds
            time = dispatcher.scheduler.currentTime - elapsedRealtimeNanos.nanoseconds.inWholeMilliseconds
        }
        arrangement.updateLocation(lastLocation)
        shadowOf(Looper.getMainLooper()).idle()

        // when
        locationPickerHelper.getLocationWithoutGms(resultHandler::onSuccess, resultHandler::onError)
        advanceTimeBy(arrangement.requestLocationTimeout - 1.seconds)

        val newLocation = Location(LocationManager.FUSED_PROVIDER).apply {
            latitude = 2.0
            longitude = 2.0
            elapsedRealtimeNanos = 0
            time = dispatcher.scheduler.currentTime
        }
        arrangement.updateLocation(newLocation)
        shadowOf(Looper.getMainLooper()).idle()

        // then
        resultHandler.assert(expectedErrorCount = 0, expectedLocations = listOf(GeoLocatedAddress(arrangement.address, newLocation)))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P, Build.VERSION_CODES.R])
    fun `given last location too old, when new location times out, then emit error`() = runTest(dispatcher) {
        // given
        val resultHandler = ResultHandler()
        val (arrangement, locationPickerHelper) = Arrangement().arrange()
        val lastLocation = Location(LocationManager.FUSED_PROVIDER).apply {
            latitude = 1.0
            longitude = 1.0
            elapsedRealtimeNanos = arrangement.lastLocationTimeLimit.inWholeNanoseconds + 1.seconds.inWholeNanoseconds
            time = dispatcher.scheduler.currentTime - elapsedRealtimeNanos.nanoseconds.inWholeMilliseconds
        }
        arrangement.updateLocation(lastLocation)
        shadowOf(Looper.getMainLooper()).idle()

        // when
        locationPickerHelper.getLocationWithoutGms(resultHandler::onSuccess, resultHandler::onError)
        advanceTimeBy(arrangement.requestLocationTimeout + 1.seconds)

        // then
        resultHandler.assert(expectedErrorCount = 1, expectedLocations = emptyList())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R]) // null location can happen only for R and above after some timeout
    fun `given no last location, when new location is null, then emit error`() = runTest(dispatcher) {
        // given
        val resultHandler = ResultHandler()
        val (arrangement, locationPickerHelper) = Arrangement(
            requestLocationTimeout = 1.minutes
        )
            .arrange()

        // when
        locationPickerHelper.getLocationWithoutGms(resultHandler::onSuccess, resultHandler::onError)

        shadowOf(Looper.getMainLooper())
            .idleFor(shadowOf(Looper.getMainLooper()).lastScheduledTaskTime) // this is how the timeout is simulated

        // then
        resultHandler.assert(expectedErrorCount = 1, expectedLocations = emptyList())
    }

    inner class Arrangement(
        val lastLocationTimeLimit: Duration = 1.minutes,
        val requestLocationTimeout: Duration = 10.seconds
    ) {
        private val context: Context = ApplicationProvider.getApplicationContext()
        private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
        private val geocoder: Geocoder = Geocoder(context)
        private val geocoderHelper: GeocoderHelper = GeocoderHelper(geocoder)
        private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val address = Address(Locale.getDefault()).apply {
            setAddressLine(0, "address")
        }

        private val locationPickerHelper by lazy {
            LocationPickerHelper(
                context = context,
                scope = scope,
                currentTimestampProvider = dispatcher.scheduler::currentTime,
                geocoderHelper = geocoderHelper,
                parameters = LocationPickerParameters(
                    lastLocationTimeLimit = lastLocationTimeLimit,
                    requestLocationTimeout = requestLocationTimeout
                ),
            )
        }

        init {
            shadowOf(geocoder).setFromLocation(listOf(address))
            shadowOf(locationManager).setProviderEnabled(LocationManager.FUSED_PROVIDER, true)

            // update the system clock to not start with 0 and prevent from having negative time for some locations
            dispatcher.scheduler.advanceTimeBy(1.hours)
            ShadowSystemClock.advanceBy(1.hours.toJavaDuration())
        }

        fun updateLocation(location: Location) = apply {
            shadowOf(locationManager).simulateLocation(location)
        }

        fun arrange() = this to locationPickerHelper
    }

    class ResultHandler {
        private val locations = ConcurrentLinkedQueue<GeoLocatedAddress>()
        private val errorCount = AtomicInteger(0)

        fun onSuccess(geoLocatedAddress: GeoLocatedAddress) {
            locations.add(geoLocatedAddress)
        }

        fun onError() {
            errorCount.incrementAndGet()
        }

        fun assert(expectedErrorCount: Int = 0, expectedLocations: List<GeoLocatedAddress> = emptyList()) {
            assertEquals(expectedErrorCount, errorCount.get())
            assertEquals(expectedLocations.size, locations.size)
            locations.forEachIndexed { index, geoLocatedAddress ->
                assertEquals(expectedLocations[index].address, geoLocatedAddress.address)
                assertEquals(expectedLocations[index].location.latitude, geoLocatedAddress.location.latitude)
                assertEquals(expectedLocations[index].location.longitude, geoLocatedAddress.location.longitude)
                assertEquals(expectedLocations[index].location.time, geoLocatedAddress.location.time)
            }
        }
>>>>>>> 395395269 (fix: location sharing without gms when not moving [WPB-9724] (#3136))
    }
}
