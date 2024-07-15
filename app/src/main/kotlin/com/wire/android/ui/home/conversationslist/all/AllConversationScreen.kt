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

package com.wire.android.ui.home.conversationslist.all

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
<<<<<<< HEAD
import com.ramcosta.composedestinations.annotation.Destination
=======
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
>>>>>>> 50f6423ec (fix: interaction during screen transitions [WPB-6533] 🍒 (#3180))
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.archive.ArchivedConversationsEmptyStateScreen
import com.wire.android.ui.home.conversationslist.ConversationItemType
import com.wire.android.ui.home.conversationslist.ConversationListCallState
import com.wire.android.ui.home.conversationslist.ConversationRouterHomeBridge
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@HomeNavGraph(start = true)
@WireDestination
@Composable
fun AllConversationScreen(homeStateHolder: HomeStateHolder) {
    with(homeStateHolder) {
        ConversationRouterHomeBridge(
            navigator = navigator,
            conversationItemType = ConversationItemType.ALL_CONVERSATIONS,
            searchBarState = searchBarState,
        )
    }
}

@Composable
fun AllConversationScreenContent(
    conversations: ImmutableMap<ConversationFolder, List<ConversationItem>>,
    conversationListCallState: ConversationListCallState,
    hasNoConversations: Boolean,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenConversation: (ConversationId) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onJoinedCall: (ConversationId) -> Unit,
    onAudioPermissionPermanentlyDenied: () -> Unit,
    dismissJoinCallAnywayDialog: () -> Unit,
    joinCallAnyway: (conversationId: ConversationId, onJoinedCall: (ConversationId) -> Unit) -> Unit,
    isFromArchive: Boolean = false,
    joinOngoingCall: (conversationId: ConversationId, onJoinedCall: (ConversationId) -> Unit) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val callConversationIdToJoin = remember { mutableStateOf(ConversationId("", "")) }

    if (conversationListCallState.shouldShowJoinAnywayDialog) {
        appLogger.i("$TAG showing showJoinAnywayDialog..")
        JoinAnywayDialog(
            onDismiss = dismissJoinCallAnywayDialog,
            onConfirm = { joinCallAnyway(callConversationIdToJoin.value, onJoinedCall) }
        )
    }
    if (hasNoConversations) {
        if (isFromArchive) {
            ArchivedConversationsEmptyStateScreen()
        } else {
            ConversationListEmptyStateScreen()
        }
    } else {
        ConversationList(
            lazyListState = lazyListState,
            conversationListItems = conversations,
            searchQuery = "",
            onOpenConversation = onOpenConversation,
            onEditConversation = onEditConversation,
            onOpenUserProfile = onOpenUserProfile,
            onJoinCall = {
                callConversationIdToJoin.value = it
                joinOngoingCall(it, onJoinedCall)
            },
            onAudioPermissionPermanentlyDenied = onAudioPermissionPermanentlyDenied,
        )
    }
}

@Composable
fun ConversationListEmptyStateScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                dimensions().spacing40x
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.padding(
                bottom = dimensions().spacing24x,
                top = dimensions().spacing100x
            ),
            text = stringResource(R.string.conversation_empty_list_title),
            style = MaterialTheme.wireTypography.title01,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        Text(
            modifier = Modifier.padding(bottom = dimensions().spacing8x),
            text = stringResource(R.string.conversation_empty_list_description),
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        Image(
            modifier = Modifier.padding(start = dimensions().spacing100x),
            painter = painterResource(
                id = R.drawable.ic_empty_conversation_arrow
            ),
            contentDescription = ""
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationScreen() = WireTheme {
    AllConversationScreenContent(
        conversations = persistentMapOf(),
        hasNoConversations = false,
        onEditConversation = {},
        onOpenConversation = {},
        onOpenUserProfile = {},
        onJoinedCall = {},
        onAudioPermissionPermanentlyDenied = {},
        conversationListCallState = ConversationListCallState(),
        isFromArchive = false,
        dismissJoinCallAnywayDialog = {},
        joinCallAnyway = { _, _ -> },
        joinOngoingCall = { _, _ -> }
    )
}

@PreviewMultipleThemes
@Composable
fun ConversationListEmptyStateScreenPreview() = WireTheme {
    AllConversationScreenContent(
        conversations = persistentMapOf(),
        hasNoConversations = true,
        onEditConversation = {},
        onOpenConversation = {},
        onOpenUserProfile = {},
        onJoinedCall = {},
        onAudioPermissionPermanentlyDenied = {},
        conversationListCallState = ConversationListCallState(),
        isFromArchive = false,
        dismissJoinCallAnywayDialog = {},
        joinCallAnyway = { _, _ -> },
        joinOngoingCall = { _, _ -> }
    )
}

private const val TAG = "AllConversationScreen"
