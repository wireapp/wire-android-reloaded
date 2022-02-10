package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.common.ConversationItemFactory
import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.GeneralConversation
import com.wire.android.ui.home.conversationslist.model.NewActivity

@Composable
fun AllConversationScreen(
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<GeneralConversation>>,
    onOpenConversationClick: (String) -> Unit,
    onScrollPositionChanged: (Int) -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    onScrollPositionChanged(lazyListState.firstVisibleItemIndex)

    AllConversationContent(
        lazyListState = lazyListState,
        newActivities = newActivities,
        conversations = conversations,
        onOpenConversationClick
    )
}

@Composable
private fun AllConversationContent(
    lazyListState: LazyListState,
    newActivities: List<NewActivity>,
    conversations: Map<ConversationFolder, List<GeneralConversation>>,
    onConversationItemClick: (String) -> Unit,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = dimensions().topBarSearchFieldHeight)
    ) {
        folderWithElements(
            header = { stringResource(id = R.string.conversation_label_new_activity) },
            items = newActivities
        ) { newActivity ->
            ConversationItemFactory(
                conversation = newActivity.conversationItem,
                eventType = newActivity.eventType,
                onConversationItemClick = { onConversationItemClick("someId") }
            )
        }

        conversations.forEach { (conversationFolder, conversationList) ->
            folderWithElements(
                header = { conversationFolder.folderName },
                items = conversationList
            ) { generalConversation ->
                GeneralConversationItem(
                    generalConversation = generalConversation,
                    onConversationItemClick = { onConversationItemClick("someId") }
                )
            }
        }
    }
}
