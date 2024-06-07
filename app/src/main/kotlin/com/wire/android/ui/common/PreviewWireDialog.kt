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
package com.wire.android.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes

@PreviewMultipleThemes
@Composable
fun PreviewWireDialog() = WireTheme {
    val password = rememberTextFieldState()
    WireTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            WireDialogContent(
                optionButton1Properties = WireDialogButtonProperties(
                    text = "OK",
                    onClick = { },
                    type = WireDialogButtonType.Primary,
                    state = if (password.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Error,
                ),
                dismissButtonProperties = WireDialogButtonProperties(
                    text = "Cancel",
                    onClick = { }
                ),
                title = "title",
                text = buildAnnotatedString {
                    val style = SpanStyle(
                        color = colorsScheme().onBackground,
                        fontWeight = MaterialTheme.wireTypography.body01.fontWeight,
                        fontSize = MaterialTheme.wireTypography.body01.fontSize,
                        fontFamily = MaterialTheme.wireTypography.body01.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.body01.fontStyle
                    )
                    withStyle(style) { append("text\nsecond line\nthirdLine\nfourth line\nfifth line\nsixth line\nseventh line") }
                },
            ) {
                WirePasswordTextField(
                    textState = password,
                    autoFill = false
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewWireDialogWith2OptionButtons() = WireTheme {
    val password = rememberTextFieldState()
    WireTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            WireDialogContent(
                optionButton1Properties = WireDialogButtonProperties(
                    text = "OK",
                    onClick = { },
                    type = WireDialogButtonType.Primary,
                    state = if (password.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Error,
                ),
                optionButton2Properties = WireDialogButtonProperties(
                    text = "Later",
                    onClick = { },
                    type = WireDialogButtonType.Primary,
                    state = if (password.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Error,
                ),
                dismissButtonProperties = WireDialogButtonProperties(
                    text = "Cancel",
                    onClick = { }
                ),
                title = "title",
                text = buildAnnotatedString {
                    val style = SpanStyle(
                        color = colorsScheme().onBackground,
                        fontWeight = MaterialTheme.wireTypography.body01.fontWeight,
                        fontSize = MaterialTheme.wireTypography.body01.fontSize,
                        fontFamily = MaterialTheme.wireTypography.body01.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.body01.fontStyle
                    )
                    withStyle(style) { append("text") }
                },
                buttonsHorizontalAlignment = false
            ) {
                WirePasswordTextField(
                    textState = password,
                    autoFill = false
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewWireDialogCentered() = WireTheme {
    val password = rememberTextFieldState()
    WireTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            WireDialogContent(
                optionButton1Properties = WireDialogButtonProperties(
                    text = "OK",
                    onClick = { },
                    type = WireDialogButtonType.Primary,
                    state = if (password.text.isEmpty()) WireButtonState.Disabled else WireButtonState.Error,
                ),
                dismissButtonProperties = WireDialogButtonProperties(
                    text = "Cancel",
                    onClick = { }
                ),
                centerContent = true,
                title = "title",
                text = buildAnnotatedString {
                    val style = SpanStyle(
                        color = colorsScheme().onBackground,
                        fontWeight = MaterialTheme.wireTypography.body01.fontWeight,
                        fontSize = MaterialTheme.wireTypography.body01.fontSize,
                        fontFamily = MaterialTheme.wireTypography.body01.fontFamily,
                        fontStyle = MaterialTheme.wireTypography.body01.fontStyle
                    )
                    withStyle(style) { append("text\nsecond line\nthirdLine\nfourth line\nfifth line\nsixth line\nseventh line") }
                },
            ) {
                WirePasswordTextField(
                    textState = password,
                    autoFill = false
                )
            }
        }
    }
}
