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
package com.wire.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun SplashScreen(onStart: () -> Unit) {
    MainBackgroundContent()
}

@Composable
fun MainBackgroundComponent() {
    MainBackgroundContent()
}

@Composable
private fun MainBackgroundContent() {
    val image: Painter = painterResource(id = R.drawable.bg_waves)
    Image(
        painter = image,
        contentDescription = null,
        alignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewSplashScreen() = WireTheme {
    MainBackgroundContent()
}