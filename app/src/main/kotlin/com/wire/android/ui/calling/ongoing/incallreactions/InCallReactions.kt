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
package com.wire.android.ui.calling.ongoing.incallreactions

@Suppress("MagicNumber")
object InCallReactions {

    /**
     * Default in call reaction emojis
     */
    val defaultReactions = listOf("👍", "🎉", "❤️", "😂", "😮", "👏", "🤔", "😢", "👎")

    /**
     * Next reaction click is disabled until delay expires
     */
    const val reactionDelayMs = 3000L

    /**
     * Total duration for reaction animation
     */
    const val animationDurationMs = 3000

    /**
     * Duration for reaction fade out animation
     */
    const val fadeOutAnimationDuarationMs = 500

    /**
     * Delay between displaying reactions on screen
     */
    const val reactionsThrottleDelayMs: Long = 200

    /**
     * Duration for showing recent reaction next to user image
     */
    const val recentReactionShowDurationMs: Long = 6000
}
