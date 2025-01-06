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
package com.wire.android.util

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test

class ExpiringMapTest {

    @Test
    fun `check new item can be added map `() = runTest {
        // given
        val map = withTestExpiringMap()

        // when
        map.put("testKey", "testValue")

        // then
        assertEquals("testValue", map["testKey"])
    }

    @Test
    fun `check item can be removed from map before expiration`() = runTest {
        // given
        val map = withTestExpiringMap()

        // when
        map.put("testKey", "testValue")
        map.remove("testKey")

        // then
        assertEquals(null, map["testKey"])
    }

    @Test
    fun `check item can not be obtained before expiration`() = runTest {
        // given
        val map = withTestExpiringMap()

        // when
        map.put("testKey", "testValue")
        advanceTimeBy(300)

        // then
        assertEquals("testValue", map["testKey"])
    }

    @Test
    fun `check item can not be obtained after expiration`() = runTest {
        // given
        val map = withTestExpiringMap()

        // when
        map.put("testKey", "testValue")
        advanceTimeBy(301)

        // then
        assertEquals(null, map["testKey"])
    }

    @Test
    fun `check adding item with existing key resets expiration`() = runTest {
        // given
        val map = withTestExpiringMap()

        // when
        map.put("testKey", "testValue")
        advanceTimeBy(300)
        map.put("testKey", "testValue2")
        advanceTimeBy(300)

        // then
        assertEquals("testValue2", map["testKey"])
    }

    @Test
    fun `check adding item with non-existing key keeps expiration for other keys`() = runTest {
        // given
        val map = withTestExpiringMap()

        // when
        map.put("testKey", "testValue")
        advanceTimeBy(200)
        map.put("testKey2", "testValue2")
        advanceTimeBy(200)

        // then
        assertEquals(null, map["testKey"])
    }

    private fun TestScope.withTestExpiringMap(): MutableMap<String, String> = ExpiringMap<String, String>(
        scope = this.backgroundScope,
        expiration = 300,
        delegate = mutableMapOf(),
        currentTime = { currentTime }
    )
}
