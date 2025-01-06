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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.wire.android.ui.calling.model.InCallReaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableList
import kotlin.random.Random

/**
 * Keeps state for currently animated reactions
 */
interface InCallReactionsState {
    fun runAnimation(inCallReaction: InCallReaction)
    fun getReactions(): List<AnimatableReaction>
}

@Composable
internal fun rememberInCallReactionsState(): InCallReactionsState {

    val scope = rememberCoroutineScope()

    return remember {
        InCallReactionsStateImpl(
            scope = scope,
            mutableReactions = mutableStateListOf(),
        )
    }
}

private class InCallReactionsStateImpl(
    private val scope: CoroutineScope,
    private val mutableReactions: MutableList<AnimatableReaction>,
) : InCallReactionsState {

    /**
     * Used by modifier to draw each animated emoji with current animation state
     */
    override fun getReactions(): List<AnimatableReaction> = mutableReactions.toImmutableList()

    /**
     * Adds new emoji to the list of animated emojis.
     * Runs  animations
     * Removes emoji from the list once animations are complete
     */
    override fun runAnimation(inCallReaction: InCallReaction) {
        scope.launch(Dispatchers.Main) {
            val animatable = AnimatableReaction(
                inCallReaction = inCallReaction,
                horizontalOffset = Random.nextFloat(),
            )

            mutableReactions.add(animatable)
            runAnimations(animatable)
            mutableReactions.remove(animatable)
        }
    }

    /**
     * Start transition and fade-out animations, wait for complete and return
     */
    private suspend fun CoroutineScope.runAnimations(reaction: AnimatableReaction) {
        listOf(
            launch {
                reaction.verticalOffset.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = InCallReactions.animationDurationMs, easing = LinearEasing)
                )
            },
            launch {
                reaction.alpha.animateTo(
                    targetValue = 0.0f,
                    animationSpec = tween(
                        durationMillis = InCallReactions.fadeOutAnimationDuarationMs,
                        delayMillis = InCallReactions.animationDurationMs - InCallReactions.fadeOutAnimationDuarationMs
                    )
                )
            }
        ).joinAll()
    }
}

data class AnimatableReaction(
    val inCallReaction: InCallReaction,
    val verticalOffset: Animatable<Float, AnimationVector1D> = Animatable(0f),
    val alpha: Animatable<Float, AnimationVector1D> = Animatable(1f),
    val horizontalOffset: Float,
)
