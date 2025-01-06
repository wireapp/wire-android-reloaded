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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toSize
import com.wire.android.R
import com.wire.android.ui.calling.model.ReactionSender
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import kotlin.math.max

@Composable
fun Modifier.drawInCallReactions(
    state: InCallReactionsState,
    labelTextColor: Color = Color.White,
    labelColor: Color = Color.Black,
    emojiBackgroundColor: Color = colorsScheme().emojiBackgroundColor,
    emojiBackgroundSize: Dp = dimensions().inCallReactionButtonSize,
    emojiTextStyle: TextStyle = typography().inCallReactionEmoji,
    labelTextStyle: TextStyle = typography().label01,
): Modifier {

    val textMeasurer = rememberTextMeasurer()

    val emojiBackgroundSizePx = with(LocalDensity.current) { emojiBackgroundSize.toPx() }
    val labelTopMarginPx = with(LocalDensity.current) { dimensions().spacing12x.toPx() }
    val labelTextPaddingPx = with(LocalDensity.current) { dimensions().spacing4x.toPx() }
    val reactionSenderSelf = stringResource(R.string.reaction_sender_self)

    return this then Modifier.drawWithContent {

        drawContent()

        clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {

            state.getReactions().forEach { reaction ->

                val senderText = when (reaction.inCallReaction.sender) {
                    is ReactionSender.You -> reactionSenderSelf
                    is ReactionSender.Other -> reaction.inCallReaction.sender.name
                    is ReactionSender.Unknown -> ""
                }

                val emojiLayoutResult = textMeasurer.measure(reaction.inCallReaction.emoji, emojiTextStyle)
                val labelLayoutResult = textMeasurer.measure(senderText, labelTextStyle)

                val emojiSize = emojiLayoutResult.size.toSize()
                val labelSize = Size(
                    width = labelLayoutResult.size.width + labelTextPaddingPx * 2,
                    height = labelLayoutResult.size.height + labelTextPaddingPx * 2
                )

                val offsetVertical = size.height - size.height * reaction.verticalOffset.value
                val offsetHorizontal = (size.width - max(emojiSize.width, labelSize.width)) * reaction.horizontalOffset

                translate(
                    top = offsetVertical + emojiSize.height,
                    left = offsetHorizontal + (labelLayoutResult.size.width - emojiSize.width).coerceAtLeast(0f) / 2f
                ) {

                    // Draw emoji background
                    drawRoundRect(
                        color = emojiBackgroundColor.copy(alpha = reaction.alpha.value),
                        topLeft = Offset(
                            x = emojiSize.center.x - emojiBackgroundSizePx / 2,
                            y = emojiSize.center.y - emojiBackgroundSizePx / 2,
                        ),
                        size = Size(emojiBackgroundSizePx, emojiBackgroundSizePx),
                        cornerRadius = CornerRadius(20f),
                    )

                    // Draw emoji
                    drawText(
                        textLayoutResult = emojiLayoutResult,
                        color = Color.Black.copy(alpha = reaction.alpha.value),
                    )

                    if (reaction.inCallReaction.sender != ReactionSender.Unknown) {
                        // Draw label background
                        drawRoundRect(
                            color = labelColor.copy(alpha = reaction.alpha.value),
                            topLeft = Offset(
                                x = emojiSize.center.x - labelSize.center.x,
                                y = emojiSize.height + labelTopMarginPx,
                            ),
                            size = labelSize,
                            cornerRadius = CornerRadius(10f),
                        )

                        // Draw label text
                        drawText(
                            textLayoutResult = labelLayoutResult,
                            color = labelTextColor.copy(alpha = reaction.alpha.value),
                            topLeft = Offset(
                                x = emojiSize.center.x - labelLayoutResult.size.center.x,
                                y = emojiSize.height + labelTopMarginPx + labelTextPaddingPx
                            )
                        )
                    }
                }
            }
        }
    }
}
