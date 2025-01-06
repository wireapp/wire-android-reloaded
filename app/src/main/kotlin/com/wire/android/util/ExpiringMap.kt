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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Map implementation that removes entries after a delay. Not thread-safe.
 */
class ExpiringMap<K, V>(
    private val scope: CoroutineScope,
    private val expiration: Long,
    private val delegate: MutableMap<K, V>,
    private val currentTime: () -> Long = { System.currentTimeMillis() },
) : MutableMap<K, V> by delegate {

    private val timestamps: MutableMap<K, Long> = ConcurrentHashMap<K, Long>()
    private var cleanupJob: Job? = null

    override fun put(key: K, value: V): V? {
        return delegate.put(key, value).also {
            timestamps.put(key, currentTime() + expiration)
            scheduleCleanup()
        }
    }

    override fun remove(key: K): V? {
        return delegate.remove(key).also {
            timestamps.remove(key)
            scheduleCleanup()
        }
    }

    private fun scheduleCleanup() {
        cleanupJob?.cancel()
        timestamps.values.sorted().firstOrNull()?.let { nextExpiration ->
            val delayToNext = nextExpiration - currentTime()
            cleanupJob = scope.launch {
                delay(delayToNext)
                removeAllExpired()
            }
        }
    }

    private fun removeAllExpired() {
        val now = currentTime()
        timestamps.entries.onEach { (key, expiration) ->
            if (expiration <= now) {
                delegate.remove(key)
            }
        }
        timestamps.entries.removeAll { it.value <= now }
        scheduleCleanup()
    }
}
