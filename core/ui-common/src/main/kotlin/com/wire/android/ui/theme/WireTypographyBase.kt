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

package com.wire.android.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import io.github.esentsov.PackagePrivate

@PackagePrivate
object WireTypographyBase {

    val Title01 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        textAlign = TextAlign.Center
    )
    val Title02 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )
    val Title03 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
    val Title04 = Title01.copy(
        fontWeight = FontWeight.W400,
    )
    val Title05 = Title01.copy(
        fontWeight = FontWeight.W900
    )
    val Body01 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.05.sp
    )
    val Body02 = Body01.copy(
        fontWeight = FontWeight.W500,
    )
    val Body03 = Body01.copy(
        fontWeight = FontWeight.W700,
    )
    val Body04 = TextStyle(
        fontWeight = FontWeight.W700,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.35.sp
    )
    val Body05 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontStyle = FontStyle.Italic,
        color = WireColorPalette.Gray70
    )
    val SubLine01 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 14.sp,
    )
    val Button01 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 18.sp
    )
    val Button02 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 15.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.3.sp,
        textAlign = TextAlign.Center
    )
    val Button03 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
        textAlign = TextAlign.Center
    )
    val Button04 = Button03.copy(
        textDecoration = TextDecoration.Underline
    )
    val Button05 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
        textAlign = TextAlign.Center
    )
    val Label01 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.25.sp
    )
    val Label02 = Label01.copy(
        fontWeight = FontWeight.W700,
    )
    val Label03 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 11.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.5.sp
    )
    val Label04 = TextStyle(
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        textAlign = TextAlign.Center
    )
    val Label05 = Label04.copy(
        fontWeight = FontWeight.W700
    )
    val Badge01 = TextStyle(
        fontWeight = FontWeight.W700,
        fontSize = 10.sp,
        lineHeight = 11.72.sp,
        textAlign = TextAlign.Center
    )
    val Code01 = TextStyle(
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        lineHeight = 28.13.sp,
        textAlign = TextAlign.Center
    )
    val InCallEmoji = TextStyle(
        fontSize = 32.sp,
    )
    val InCallEmojiRecent = TextStyle(
        fontSize = 20.sp,
    )
}
