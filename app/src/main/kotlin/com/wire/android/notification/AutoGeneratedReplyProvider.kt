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
package com.wire.android.notification

import com.wire.android.datastore.GlobalDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoGeneratedReplyProvider @Inject constructor(
    private val globalDataStore: GlobalDataStore
) {

    private var cachedValue: Boolean? = null
    private val lock = Mutex()

    suspend fun setAutoGeneratedReply(
        value: Boolean
    ) = lock.withLock {
        globalDataStore.setMessageNotificationAutoGeneratedReply(value)
        cachedValue = value
    }

    suspend fun getAutoGeneratedReply() = lock.withLock {
        cachedValue ?: readAndSetCache()
    }

    private suspend fun readAndSetCache(): Boolean =
        globalDataStore.getMessageNotificationAutoGeneratedReply(true).first().also {
            cachedValue = it
        }

}
