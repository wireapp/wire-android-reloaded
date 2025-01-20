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

package com.wire.android.ui.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.HomeDestination.Conversations
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberTrackingAnimatedNavController
import com.wire.android.ui.common.topappbar.ConversationFilterState
import com.wire.android.ui.common.topappbar.rememberConversationFilterState
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.conversationslist.filter.toTopBarTitle
import com.wire.kalium.logic.data.conversation.ConversationFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class HomeStateHolder(
    val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    val drawerState: DrawerState,
    val searchBarState: SearchBarState,
    val navigator: Navigator,
    private val currentNavigationItemState: State<HomeDestination>,
    private val conversationFilterState: ConversationFilterState,
) {
    val currentNavigationItem
        get() = currentNavigationItemState.value

    val currentConversationFilter
        get() = conversationFilterState.filter

    val currentTitle
        get() = when (currentNavigationItemState.value) {
            Conversations -> conversationFilterState.filter.toTopBarTitle()
            else -> currentNavigationItemState.value.title
        }

    private val lazyListStatesMap = mutableMapOf<String, LazyListState>()

    fun lazyListStateFor(
        destination: HomeDestination,
        conversationFilter: ConversationFilter = ConversationFilter.All,
    ): LazyListState =
        lazyListStatesMap.getOrPut(
            key = destination.itemName + when (destination) {
                Conversations -> ":$conversationFilter" // each filter has its own scroll state
                else -> "" // other destinations shouldn't care about the conversation filter
            }
        ) {
            LazyListState()
        }

    fun closeDrawer() {
        coroutineScope.launch {
            drawerState.close()
        }
    }

    fun openDrawer() {
        coroutineScope.launch {
            drawerState.open()
        }
    }

    fun changeFilter(filter: ConversationFilter) = conversationFilterState.changeFilter(filter)
}

@Composable
fun rememberHomeScreenState(
    navigator: Navigator,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberTrackingAnimatedNavController {
        HomeDestination.fromRoute(it)?.itemName
    },
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
): HomeStateHolder {

    val searchBarState = rememberSearchbarState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentNavigationItemState = remember {
        derivedStateOf {
            navBackStackEntry?.destination?.route?.let { HomeDestination.fromRoute(it) } ?: Conversations
        }
    }
    val conversationFilterState = rememberConversationFilterState()

    return remember {
        HomeStateHolder(
            coroutineScope = coroutineScope,
            navController = navController,
            drawerState = drawerState,
            searchBarState = searchBarState,
            navigator = navigator,
            currentNavigationItemState = currentNavigationItemState,
            conversationFilterState = conversationFilterState,
        )
    }
}
