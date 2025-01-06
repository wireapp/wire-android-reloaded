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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.calling.ongoing.incallreactions.InCallReactions.reactionDelayMs
import com.wire.android.ui.calling.ongoing.incallreactions.InCallReactions.defaultReactions
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InCallReactionsPanel(
    onReactionClick: (String) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val scope = rememberCoroutineScope()

    val disabledReactions = remember { mutableStateListOf<String>() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensions().spacing8x)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions().spacing16x)
    ) {

        Spacer(modifier = Modifier.width(dimensions().spacing8x))

        defaultReactions.forEach { reaction ->
            EmojiButton(
                isEnabled = disabledReactions.contains(reaction).not(),
                emoji = reaction,
                onClick = {
                    onReactionClick(reaction)
                    disabledReactions.add(reaction)
                    scope.launch {
                        delay(reactionDelayMs)
                        disabledReactions.remove(reaction)
                    }
                }
            )
        }

        Icon(
            modifier = Modifier.clickable { onMoreClick() },
            painter = painterResource(id = R.drawable.ic_more_emojis),
            contentDescription = stringResource(R.string.content_description_more_emojis)
        )

        Spacer(modifier = Modifier.width(dimensions().spacing8x))
    }
}

@Composable
private fun EmojiButton(
    emoji: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(dimensions().inCallReactionButtonSize)
            .clip(RoundedCornerShape(dimensions().corner10x))
            .clickable(isEnabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = isEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = colorsScheme().secondaryButtonEnabled,
                        shape = RoundedCornerShape(dimensions().corner10x)
                    ),
            )
        }
        Text(
            modifier = Modifier
                .alpha(if (isEnabled) 1f else 0.5f),
            text = emoji,
            fontSize = typography().inCallReactionEmoji.fontSize,
        )
    }
}

@PreviewMultipleThemes
@Composable
private fun PreviewEmojiButton() = WireTheme {
    EmojiButton(
        emoji = defaultReactions[0],
        isEnabled = true,
        onClick = {}
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewEmojiButtonDisabled() = WireTheme {
    EmojiButton(
        emoji = defaultReactions[0],
        isEnabled = false,
        onClick = {}
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewInCallReactionsPanel() = WireTheme {
    InCallReactionsPanel(
        onReactionClick = {},
        onMoreClick = {},
    )
}
