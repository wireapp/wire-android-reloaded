package com.wire.android.ui.home.conversationslist.all

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.folderWithElements
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun AllConversationScreen(
    conversations: Map<ConversationFolder, List<ConversationItem>>,
    hasNoConversations: Boolean,
    onOpenConversation: (ConversationId) -> Unit,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onOpenConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val lazyListState = rememberLazyListState()

    if (hasNoConversations) {
        ConversationListEmptyStateScreen()
    } else {
        AllConversationContent(
            lazyListState = lazyListState,
            conversations = conversations,
            onOpenConversation = onOpenConversation,
            onEditConversation = onEditConversation,
            onOpenUserProfile = onOpenUserProfile,
            onOpenConversationNotificationsSettings = onOpenConversationNotificationsSettings,
            onJoinCall = onJoinCall
        )
    }
}

@Composable
private fun AllConversationContent(
    lazyListState: LazyListState,
    conversations: Map<ConversationFolder, List<ConversationItem>>,
    onOpenConversation: (ConversationId) -> Unit,
    onEditConversation: (ConversationItem) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    onOpenConversationNotificationsSettings: (ConversationItem) -> Unit,
    onJoinCall: (ConversationId) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        /*
         * When the list is scrolled to top and new items (e.g. new activity section) should appear on top of the list, it appears above
         * all current items, scroll is preserved so the list still shows the same item as the first one on list so it scrolls
         * automatically to that item and the newly added section on top is hidden above this previously top item, so for such situation
         * when the list is scrolled to the top and we want the new section to appear at the top we need a dummy top item which will make
         *  it so it wants to keep this dummy top item as the first one on list and show all other items below it.
         */
        item("empty-top-header") {
            Divider(
                thickness = Dp.Hairline,
                color = Color.Transparent
            )
        }
        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = when (conversationFolder) {
                    is ConversationFolder.Predefined -> context.getString(conversationFolder.folderNameResId)
                    is ConversationFolder.Custom -> conversationFolder.folderName
                },
                items = conversationList.associateBy {
                    it.conversationId.toString()
                }
            ) { generalConversation ->
                ConversationItemFactory(
                    conversation = generalConversation,
                    openConversation = onOpenConversation,
                    openMenu = onEditConversation,
                    openUserProfile = onOpenUserProfile,
                    openNotificationsOptions = onOpenConversationNotificationsSettings,
                    joinCall = onJoinCall
                )
            }
        }
    }
}

@Composable
fun ConversationListEmptyStateScreen() {
    Column(
        modifier = Modifier
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


//@Preview
//@Composable
//fun ComposablePreview() {
//    AllConversationScreen(mapOf(), {}, {}, {}, {}, {}, false, emptySearchResult)
//}
//
//@Preview
//@Composable
//fun ConversationListEmptyStateScreenPreview() {
//    AllConversationScreen(mapOf(), {}, {}, {}, {}, {}, true, emptySearchResult)
//}
