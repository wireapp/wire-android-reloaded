package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.extension.rememberLazyListState
import com.wire.android.ui.home.conversationslist.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.model.ConversationType
import com.wire.android.ui.home.conversationslist.model.ConversationUnreadMention
import com.wire.android.ui.home.conversationslist.model.EventType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

@Composable
fun MentionScreen(
    unreadMentions: List<ConversationUnreadMention> = emptyList(),
    allMentions: List<ConversationUnreadMention> = emptyList(),
    onMentionItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationType) -> Unit,
    onScrollPositionChanged: (Int) -> Unit = {},
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationType) -> Unit,
) {
    val lazyListState = rememberLazyListState { firstVisibleItemIndex ->
        onScrollPositionChanged(firstVisibleItemIndex)
    }

    MentionContent(
        lazyListState = lazyListState,
        unreadMentions = unreadMentions,
        allMentions = allMentions,
        onMentionItemClick = onMentionItemClick,
        onEditConversationItem = onEditConversationItem,
        onOpenUserProfile = onOpenUserProfile,
        openConversationNotificationsSettings = openConversationNotificationsSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MentionContent(
    lazyListState: LazyListState,
    unreadMentions: List<ConversationUnreadMention>,
    allMentions: List<ConversationUnreadMention>,
    onMentionItemClick: (ConversationId) -> Unit,
    onEditConversationItem: (ConversationType) -> Unit,
    onOpenUserProfile: (UserId) -> Unit,
    openConversationNotificationsSettings: (ConversationType) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.mention_label_unread_mentions) },
            items = unreadMentions
        ) { unreadMention ->
            ConversationItemFactory(
                conversation = unreadMention,
                eventType = EventType.UnreadMention,
                openConversation = { onMentionItemClick(unreadMention.id) },
                onConversationItemLongClick = { onEditConversationItem(unreadMention.conversationType) },
                openUserProfile = onOpenUserProfile,
                onMutedIconClick = { openConversationNotificationsSettings(unreadMention.conversationType) },
            )
        }

        folderWithElements(
            header = { stringResource(R.string.mention_label_all_mentions) },
            items = allMentions
        ) { mention ->
            ConversationItemFactory(
                conversation = mention,
                openConversation = { onMentionItemClick(mention.id) },
                onConversationItemLongClick = { onEditConversationItem(mention.conversationType) },
                openUserProfile = onOpenUserProfile,
                onMutedIconClick = { openConversationNotificationsSettings(mention.conversationType) },
            )
        }
    }
}


