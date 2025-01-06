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
package com.wire.android.ui.home.messagecomposer.recordaudio

import android.app.Activity
import android.text.format.DateUtils
import android.view.WindowManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.media.audiomessage.AudioMediaPlayingState
import com.wire.android.media.audiomessage.AudioState
import com.wire.android.ui.common.WireCheckbox
import com.wire.android.ui.common.button.IconAlignment
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireTertiaryIconButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.messagetypes.audio.RecordedAudioMessage
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun RecordAudioButtonClose(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireTertiaryIconButton(
        onButtonClicked = onClick,
        iconResource = R.drawable.ic_close,
        contentDescription = R.string.label_close,
        shape = CircleShape,
        minSize = MaterialTheme.wireDimensions.buttonCircleMinSize,
        minClickableSize = MaterialTheme.wireDimensions.buttonMinClickableSize,
        modifier = modifier
    )
}

@Composable
fun RecordAudioButtonEnabled(
    applyAudioFilterState: Boolean,
    applyAudioFilterClick: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RecordAudioButton(
        onClick = onClick,
        modifier = modifier,
        topContent = {},
        iconResId = R.drawable.ic_microphone_on,
        contentDescription = R.string.content_description_record_audio_button_start,
        buttonState = WireButtonState.Default,
        bottomText = R.string.record_audio_start_label,
        applyAudioFilterState = applyAudioFilterState,
        applyAudioFilterClick = applyAudioFilterClick
    )
}

@Composable
fun RecordAudioButtonRecording(
    applyAudioFilterState: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialSeconds = if (LocalInspectionMode.current) 1 else 0
    var seconds by remember {
        mutableStateOf(initialSeconds)
    }
    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay(1000L)
            seconds += 1
        }
    }
    if (!LocalInspectionMode.current) {
        val activity = LocalContext.current as Activity

        DisposableEffect(Unit) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    RecordAudioButton(
        onClick = onClick,
        modifier = modifier,
        topContent = {
            Text(
                text = DateUtils.formatElapsedTime(seconds.toLong()),
                style = MaterialTheme.wireTypography.body01,
                fontSize = 32.sp,
                color = colorsScheme().secondaryText
            )
        },
        iconResId = R.drawable.ic_stop,
        contentDescription = R.string.content_description_record_audio_button_stop,
        bottomText = R.string.record_audio_recording_label,
        buttonState = if (seconds > 0) WireButtonState.Error else WireButtonState.Disabled,
        isAudioFilterEnabled = false,
        applyAudioFilterState = applyAudioFilterState,
        applyAudioFilterClick = { }
    )
}

@Composable
fun RecordAudioButtonEncoding(
    applyAudioFilterState: Boolean,
    modifier: Modifier = Modifier
) {
    if (!LocalInspectionMode.current) {
        val activity = LocalContext.current as Activity

        DisposableEffect(Unit) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    RecordAudioButton(
        onClick = {},
        modifier = modifier,
        topContent = {},
        iconResId = null,
        trailingIconAlignment = IconAlignment.Center,
        contentDescription = -1,
        bottomText = R.string.record_audio_encoding_label,
        buttonState = WireButtonState.Disabled,
        isAudioFilterEnabled = false,
        loading = true,
        applyAudioFilterState = applyAudioFilterState,
        applyAudioFilterClick = { }
    )
}

@Composable
fun RecordAudioButtonSend(
    applyAudioFilterState: Boolean,
    audioState: AudioState,
    onClick: () -> Unit,
    outputFile: File?,
    onPlayAudio: () -> Unit,
    onSliderPositionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    applyAudioFilterClick: (Boolean) -> Unit
) {
    RecordAudioButton(
        onClick = onClick,
        modifier = modifier,
        topContent = {
            outputFile?.let {
                RecordedAudioMessage(
                    audioMediaPlayingState = audioState.audioMediaPlayingState,
                    totalTimeInMs = audioState.totalTimeInMs,
                    currentPositionInMs = audioState.currentPositionInMs,
                    waveMask = audioState.wavesMask,
                    onPlayButtonClick = onPlayAudio,
                    onSliderPositionChange = { position ->
                        onSliderPositionChange(position.toInt())
                    }
                )
            }
        },
        iconResId = R.drawable.ic_send,
        contentDescription = R.string.content_description_record_audio_button_send,
        buttonState = WireButtonState.Default,
        bottomText = R.string.record_audio_send_label,
        applyAudioFilterState = applyAudioFilterState,
        applyAudioFilterClick = applyAudioFilterClick
    )
}

@Composable
private fun RecordAudioButton(
    onClick: () -> Unit,
    topContent: @Composable () -> Unit,
    @DrawableRes iconResId: Int?,
    @StringRes contentDescription: Int,
    @StringRes bottomText: Int,
    applyAudioFilterState: Boolean,
    applyAudioFilterClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonState: WireButtonState = WireButtonState.Default,
    isAudioFilterEnabled: Boolean = true,
    loading: Boolean = false,
    trailingIconAlignment: IconAlignment = IconAlignment.Border,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        topContent()
        Spacer(modifier = Modifier.height(dimensions().spacing40x))

        WirePrimaryButton(
            modifier = Modifier
                .width(dimensions().spacing80x)
                .height(dimensions().spacing80x),
            trailingIconAlignment = trailingIconAlignment,
            onClick = onClick,
            leadingIcon = iconResId?.let {
                {
                    Icon(
                        painter = painterResource(id = it),
                        contentDescription = stringResource(id = contentDescription),
                        modifier = Modifier.size(dimensions().spacing20x),
                        tint = colorsScheme().onPrimary
                    )
                }
            },
            shape = CircleShape,
            state = buttonState,
            loading = loading
        )
        Spacer(modifier = Modifier.height(dimensions().spacing16x))
        Text(
            text = stringResource(id = bottomText),
            style = MaterialTheme.wireTypography.body02
        )

        Spacer(modifier = Modifier.height(dimensions().spacing40x))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            WireCheckbox(
                enabled = isAudioFilterEnabled,
                checked = applyAudioFilterState,
                onCheckedChange = applyAudioFilterClick
            )
            Text(
                text = stringResource(id = R.string.record_audio_apply_filter_label),
                style = MaterialTheme.wireTypography.body01,
                color = if (isAudioFilterEnabled) colorsScheme().onSecondaryButtonEnabled else colorsScheme().onSecondaryButtonDisabled
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewRecordAudioButtonClose() {
    WireTheme {
        RecordAudioButtonClose(onClick = {}, modifier = Modifier)
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewRecordAudioButtonEnabled() {
    WireTheme {
        RecordAudioButtonEnabled(
            onClick = {},
            modifier = Modifier,
            applyAudioFilterState = false,
            applyAudioFilterClick = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewRecordAudioButtonRecording() {
    WireTheme {
        RecordAudioButtonRecording(
            onClick = {},
            modifier = Modifier,
            applyAudioFilterState = false
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewRecordAudioButtonSend() {
    WireTheme {
        RecordAudioButtonSend(
            audioState = AudioState(
                audioMediaPlayingState = AudioMediaPlayingState.Paused,
                totalTimeInMs = AudioState.TotalTimeInMs.Known(1000),
                currentPositionInMs = 0,
                wavesMask = listOf(32, 1, 24, 23, 13, 16, 9, 0, 4, 30, 23)
            ),
            onClick = {},
            modifier = Modifier,
            outputFile = null,
            onPlayAudio = {},
            onSliderPositionChange = {},
            applyAudioFilterState = false,
            applyAudioFilterClick = {}
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewRecordAudioButtonEncoding() = WireTheme {
    RecordAudioButtonEncoding(applyAudioFilterState = false)
}
