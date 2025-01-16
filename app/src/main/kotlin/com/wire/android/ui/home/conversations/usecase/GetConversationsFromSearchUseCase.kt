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

package com.wire.android.ui.home.conversations.usecase

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.mapper.toConversationItem
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithEvents
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.data.conversation.ConversationQueryConfig
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase
import com.wire.kalium.logic.feature.conversation.folder.GetFavoriteFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveConversationsFromFolderUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetConversationsFromSearchUseCase @Inject constructor(
    private val useCase: GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase,
    private val getFavoriteFolderUseCase: GetFavoriteFolderUseCase,
    private val observeConversationsFromFromFolder: ObserveConversationsFromFolderUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val dispatchers: DispatcherProvider,
    private val observeSelfUser: GetSelfUserUseCase
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        searchQuery: String = "",
        fromArchive: Boolean = false,
        newActivitiesOnTop: Boolean = false,
        onlyInteractionEnabled: Boolean = false,
        conversationFilter: ConversationFilter = ConversationFilter.All,
        playingAudioMessage: PlayingAudioMessage = PlayingAudioMessage.None
    ): Flow<PagingData<ConversationItem>> {
        val pagingConfig = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            initialLoadSize = INITIAL_LOAD_SIZE,
            enablePlaceholders = true,
        )
        return when (conversationFilter) {
            ConversationFilter.All,
            ConversationFilter.Groups,
            ConversationFilter.OneOnOne -> useCase(
                queryConfig = ConversationQueryConfig(
                    searchQuery = searchQuery,
                    fromArchive = fromArchive,
                    newActivitiesOnTop = newActivitiesOnTop,
                    onlyInteractionEnabled = onlyInteractionEnabled,
                    conversationFilter = conversationFilter,
                ),
                pagingConfig = pagingConfig,
                startingOffset = 0L,
            )

            ConversationFilter.Favorites -> {
                when (val result = getFavoriteFolderUseCase.invoke()) {
                    GetFavoriteFolderUseCase.Result.Failure -> flowOf(emptyList())
                    is GetFavoriteFolderUseCase.Result.Success ->
                        observeConversationsFromFromFolder(result.folder.id)
                }
                    .map { staticPagingItems(it) }
            }

            is ConversationFilter.Folder -> {
                observeConversationsFromFromFolder(conversationFilter.folderId)
                    .map { staticPagingItems(it) }
            }
        }
            .map { pagingData ->
                pagingData.map {
                    it.toConversationItem(
                        userTypeMapper = userTypeMapper,
                        searchQuery = searchQuery,
                        selfUserTeamId = observeSelfUser().firstOrNull()?.teamId,
                        playingAudioMessage = playingAudioMessage
                    )
                }
            }.flowOn(dispatchers.io())
    }

    private fun staticPagingItems(conversations: List<ConversationDetailsWithEvents>): PagingData<ConversationDetailsWithEvents> {
        return PagingData.from(
            conversations,
            sourceLoadStates = LoadStates(
                prepend = LoadState.NotLoading(true),
                append = LoadState.NotLoading(true),
                refresh = LoadState.NotLoading(true),
            )
        )
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val INITIAL_LOAD_SIZE = 60
        const val PREFETCH_DISTANCE = 5
    }
}
