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

import androidx.test.platform.app.InstrumentationRegistry
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.kalium.logic.data.message.Message
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SelfDeletionTimerTest {

    private val selfDeletionTimer by lazy {
        SelfDeletionTimerHelper(context = InstrumentationRegistry.getInstrumentation().targetContext)
    }
    private val dispatcher = StandardTestDispatcher()
    private fun currentTime(): Instant = Instant.fromEpochMilliseconds(dispatcher.scheduler.currentTime)

    @Before
    fun setUp() {
        mockkObject(SelfDeletionTimerHelper.Companion)
        every { SelfDeletionTimerHelper.Companion.currentTime() } answers { currentTime() }
    }

    @After
    fun cleanUp() {
        unmockkObject(SelfDeletionTimerHelper.Companion)
    }

    @Test
    fun givenTimeLeftIsAboveOneHour_whenGettingTheUpdateInterval_ThenIsEqualToMinutesLeftTillWholeHour() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours + 30.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 30.minutes)
    }

    @Test
    fun givenTimeLeftIsEqualToWholeHour_whenGettingTheUpdateInterval_ThenIsEqualToOneMinute() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 1.hours)
    }

    @Test
    fun givenTimeLeftIsEqualToOneHour_whenGettingTheUpdateInterval_ThenIsEqualToOneMinute() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 1.minutes)
    }

    @Test
    fun givenTimeLeftIsEqualToOneMinute_whenGettingTheUpdateInterval_ThenIsEqualToOneSeconds() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 1.seconds)
    }

    @Test
    fun givenTimeLeftIsEqualToOneMinuteAndTenPointNineSeconds_whenGettingTheUpdateInterval_ThenIsEqualToTenPointNineSeconds() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.minutes + 10.seconds + 900.milliseconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 10.seconds + 900.milliseconds)
    }

    @Test
    fun givenTimeLeftIsEqualToThirtySeconds_whenGettingTheUpdateInterval_ThenIsEqualToOneSeconds() {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 30.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val interval = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).updateInterval()
        assert(interval == 1.seconds)
    }

    @Test
    fun givenTimeLeftIsEqualToFiftyDays_whenGettingThTimeLeftFormatted_ThenIsEqualToFourWeeksLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 50.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "4 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualToTwentySevenDays_whenGettingThTimeLeftFormatted_ThenIsEqualToFourWeeksLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 27.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "4 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualTo27DaysAnd12Hours_whenGettingThTimeLeftFormatted_ThenIsEqualToFourWeeksLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 27.days + 12.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "4 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualTo27DaysAnd1Second_whenGettingThTimeLeftFormatted_ThenIsEqualToFourWeeksLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 27.days + 1.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "4 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualTo28Days_whenGettingThTimeLeftFormatted_ThenIsEqualToFourWeeksLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 28.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "4 weeks left")
    }

    @Test
    fun givenTimeLeftIsEqualTo21Days_whenGettingThTimeLeftFormatted_ThenIsEqualToTwentyOneLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 21.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "21 days left")
    }

    @Test
    fun givenTimeLeftIsEqualTo14Days_whenGettingThTimeLeftFormatted_ThenIsEqualToFourTeenDaysLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 14.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "14 days left")
    }

    @Test
    fun givenTimeLeftIsEqualTo20Days_whenGettingThTimeLeftFormatted_ThenIsEqualToTwentyDaysLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 20.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "20 days left")
    }

    @Test
    fun givenTimeLeftIsEqualToSevenDays_whenGettingThTimeLeftFormatted_ThenIsEqualToOneWeekLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 7.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 week left")
    }

    @Test
    fun givenTimeLeftIsEqualToSixDays_whenGettingThTimeLeftFormatted_ThenIsEqualToOneWeekLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 6.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 week left")
    }

    @Test
    fun givenTimeLeftIsEqualToSixDaysAnd12Hours_whenGettingThTimeLeftFormatted_ThenIsEqualToOneWeekLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 6.days + 12.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 week left")
    }

    @Test
    fun givenTimeLeftIsEqualToSixDaysAndOneSecond_whenGettingThTimeLeftFormatted_ThenIsEqualToOneWeekLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 6.days + 1.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 week left")
    }

    @Test
    fun givenTimeLeftIsEqualToThirteenDays_whenGettingThTimeLeftFormatted_ThenIsEqualToThirteenDays() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 13.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "13 days left")
    }

    @Test
    fun givenTimeLeftIsEqualToOneDay_whenGettingThTimeLeftFormatted_ThenIsEqualToOneDayLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.days,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 day left")
    }

    @Test
    fun givenTimeLeftIsEqualToTwentyFourHours_whenGettingThTimeLeftFormatted_ThenIsEqualToOneDayLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 24.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 day left")
    }

    @Test
    fun givenTimeLeftIsEqualToTwentyThreeHours_whenGettingThTimeLeftFormatted_ThenIsEqualToTwentyThreeHourLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "23 hours left")
    }

    @Test
    fun givenTimeLeftIsEqualToSixtyMinutes_whenGettingThTimeLeftFormatted_ThenIsEqualToOneHourLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 60.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 hour left")
    }

    @Test
    fun givenTimeLeftIsEqualToOneMinute_whenGettingThTimeLeftFormatted_ThenIsEqualToOneMinuteLeft() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 minute left")
    }

    @Test
    fun givenTimeLeftIsEqualToOFiftyNineMinutes_whenGettingThTimeLeftFormatted_ThenIsEqualToFiftyNineMinutes() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 59.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "59 minutes left")
    }

    @Test
    fun givenTimeLeftIsEqualToSixtySeconds_whenGettingThTimeLeftFormatted_ThenIsEqualToOneMinute() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 60.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        val timeLeftLabel = (selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable).timeLeftFormatted
        assert(timeLeftLabel == "1 minute left")
    }

    @Test
    fun givenTimeLeftIs1DayAnd12Hours_whenRecalculatingTimeAfterIntervals_thenTimeLeftIsEqualToExpected() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.days + 12.hours,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        with(selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
            advanceTimeBy(updateInterval())
            recalculateTimeLeft()
            assert(selfDeletionTimer.timeLeftFormatted == "1 day left")

            advanceTimeBy(updateInterval())
            recalculateTimeLeft()
            assert(selfDeletionTimer.timeLeftFormatted == "23 hours left")
        }
    }

    @Test
    fun givenTimeLeftIs23HoursAnd23Minutes_whenRecalculatingTimeAfterIntervals_thenTimeLeftIsEqualToExpected() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 23.hours + 23.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        with(selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
            advanceTimeBy(updateInterval())
            recalculateTimeLeft()
            assert(selfDeletionTimer.timeLeftFormatted == "23 hours left")
        }
    }

    @Test
    fun givenTimeLeftIs1HourAnd12Minutes_whenRecalculatingTimeAfterIntervals_thenTimeLeftIsEqualToExpected() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.hours + 12.minutes,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        with(selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
            advanceTimeBy(updateInterval())
            recalculateTimeLeft()
            assert(selfDeletionTimer.timeLeftFormatted == "1 hour left")

            advanceTimeBy(updateInterval())
            recalculateTimeLeft()
            assert(selfDeletionTimer.timeLeftFormatted == "59 minutes left")
        }
    }

    @Test
    fun givenTimeLeftIs1HourAnd23Seconds_whenRecalculatingTimeAfterIntervals_thenTimeLeftIsEqualToExpected() = runTest(dispatcher) {
        val selfDeletionTimer = selfDeletionTimer.fromExpirationStatus(
            ExpirationStatus.Expirable(
                expireAfter = 1.minutes + 23.seconds,
                selfDeletionStatus = Message.ExpirationData.SelfDeletionStatus.NotStarted
            )
        )
        assert(selfDeletionTimer is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable)
        with(selfDeletionTimer as SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable) {
            advanceTimeBy(updateInterval())
            recalculateTimeLeft()
            assert(selfDeletionTimer.timeLeftFormatted == "1 minute left")

            advanceTimeBy(updateInterval())
            recalculateTimeLeft()
            assert(selfDeletionTimer.timeLeftFormatted == "59 seconds left")
        }
    }
}
