package com.wire.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.R
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun SearchBar(
    placeholderText: String,
    onTextTyped: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    SearchBarTemplate(
        placeholderText = placeholderText,
        leadingIcon =
        {
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search_icon),
                    contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                    tint = MaterialTheme.wireColorScheme.onBackground
                )
            }
        },
        placeholderTextStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
        onTextTyped = onTextTyped,
        modifier = modifier
    )
}

@Composable
fun SearchBarTemplate(
    placeholderText: String,
    leadingIcon: @Composable () -> Unit,
    text: String = "",
    onTextTyped: (String) -> Unit = {},
    placeholderTextStyle: TextStyle = LocalTextStyle.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier
) {
    var showClearButton by remember { mutableStateOf(false) }
    var searchQuery by remember(text) { mutableStateOf(TextFieldValue(text)) }

    WireTextField(
        modifier = modifier
            .padding(bottom = dimensions().spacing16x)
            .padding(horizontal = dimensions().spacing8x),
        value = searchQuery,
        onValueChange = {
            searchQuery = it
            onTextTyped(it.text)
            showClearButton = it.text.isNotEmpty()
        },
        leadingIcon = {
            leadingIcon()
        },
        trailingIcon = {
            Box(modifier = Modifier.size(40.dp)) {
                AnimatedVisibility(
                    visible = showClearButton,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = {
                        searchQuery = TextFieldValue()
                        showClearButton = false
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clear_search),
                            contentDescription = stringResource(R.string.content_description_clear_content)
                        )
                    }
                }
            }
        },
        interactionSource = interactionSource,
        textStyle = textStyle.copy(fontSize = 14.sp),
        placeholderTextStyle = placeholderTextStyle.copy(fontSize = 14.sp),
        placeholderText = placeholderText,
        maxLines = 1,
        singleLine = true,
    )
}


@Preview(showBackground = true)
@Composable
fun SearchBarCollapsedPreview() {
    SearchBar("Search text")
}
