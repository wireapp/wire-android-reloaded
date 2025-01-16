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

package com.wire.android.ui.home.archive

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.conversationslist.ConversationListViewModelPaginatedPreview
import com.wire.android.ui.home.conversationslist.ConversationsScreenContent
import com.wire.android.ui.home.conversationslist.common.previewConversationFoldersFlow
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@HomeNavGraph
@WireDestination
@Composable
fun ArchiveScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationsScreenContent(
            navigator = navigator,
            searchBarState = searchBarState,
            conversationsSource = ConversationsSource.ARCHIVE,
            lazyListState = lazyListStateFor(HomeDestination.Archive),
            emptyListContent = { ArchiveEmptyContent() }
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewArchiveEmptyScreen() = WireTheme {
    ConversationsScreenContent(
        navigator = rememberNavigator {},
        searchBarState = rememberSearchbarState(),
        conversationsSource = ConversationsSource.ARCHIVE,
        emptyListContent = { ArchiveEmptyContent() },
        conversationListViewModel = ConversationListViewModelPaginatedPreview(previewConversationFoldersFlow(list = listOf())),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewArchiveEmptySearchScreen() = WireTheme {
    ConversationsScreenContent(
        navigator = rememberNavigator {},
        searchBarState = rememberSearchbarState(initialIsSearchActive = true, searchQueryTextState = TextFieldState(initialText = "er")),
        conversationsSource = ConversationsSource.ARCHIVE,
        emptyListContent = { ArchiveEmptyContent() },
        conversationListViewModel = ConversationListViewModelPaginatedPreview(
            previewConversationFoldersFlow(searchQuery = "er", list = listOf())
        ),
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewArchiveScreen() = WireTheme {
    ConversationsScreenContent(
        navigator = rememberNavigator {},
        searchBarState = rememberSearchbarState(initialIsSearchActive = true, searchQueryTextState = TextFieldState(initialText = "er")),
        conversationsSource = ConversationsSource.ARCHIVE,
        emptyListContent = { ArchiveEmptyContent() },
        conversationListViewModel = ConversationListViewModelPaginatedPreview(previewConversationFoldersFlow(searchQuery = "er")),
    )
}
