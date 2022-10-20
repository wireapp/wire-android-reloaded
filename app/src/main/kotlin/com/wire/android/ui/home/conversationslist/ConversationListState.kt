package com.wire.android.ui.home.conversationslist

import com.wire.android.ui.home.conversationslist.model.ConversationFolder
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

data class ConversationListState(
    val conversations: ImmutableMap<ConversationFolder, List<ConversationItem>> = persistentMapOf(),
    val missedCalls: ImmutableList<ConversationItem> = persistentListOf(),
    val callHistory: ImmutableList<ConversationItem> = persistentListOf(),
    val unreadMentions: ImmutableList<ConversationItem> = persistentListOf(),
    val allMentions: ImmutableList<ConversationItem> = persistentListOf(),
    val newActivityCount: Long = 0,
    val missedCallsCount: Long = 0,
    val unreadMentionsCount: Long = 0,
    val shouldShowEmptyState: Boolean = false
)
