/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.settings.about.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.util.author
import com.wire.android.model.Clickable
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireLibraries(
    libraries: List<Library>,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLibraryClick: (Library) -> Unit,
    header: (LazyListScope.() -> Unit)? = null,
) {

    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.Center,
        state = lazyListState,
        contentPadding = contentPadding
    ) {
        header?.invoke(this)
        libraryItems(
            libraries,
            onLibraryClick
        )
    }
}

inline fun LazyListScope.libraryItems(
    libraries: List<Library>,
    crossinline onLibraryClick: ((Library) -> Unit),
) {
    items(libraries) { library ->
        libItem(
            library.name,
            library.author
        ) {
            onLibraryClick.invoke(library)
        }
    }
}

@Composable
fun libItem(
    libName: String,
    libAuthor: String,
    onClick: () -> Unit,
) {
    RowItemTemplate(
        title = {
            androidx.compose.material3.Text(
                style = MaterialTheme.wireTypography.title02,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = libName,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )

            androidx.compose.material3.Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.secondaryText,
                text = libAuthor,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        clickable = Clickable(enabled = true, onClick = onClick)
    )
}
