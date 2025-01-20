/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.feature.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtTest {

    @Test
    fun `given single word string when converted then return same string`() {
        assertEquals("Username", "username".convertToCamelCase())
    }

    @Test
    fun `given string with multiple underscores when converted then return camel case string`() {
        assertEquals("ThisIsATestCase", ("this_is_a_test_case").convertToCamelCase())
    }

    @Test
    fun `given empty string when converted then return empty string`() {
        assertEquals("", ("").convertToCamelCase())
    }

    @Test
    fun `given string with leading and trailing underscores when converted then return camel case string`() {
        assertEquals("LeadingAndTrailing", ("_leading_and_trailing_").convertToCamelCase())
    }

    @Test
    fun `given string with numbers when converted then return camel case string`() {
        assertEquals("User123Name", ("user_123_name").convertToCamelCase())
    }
}
