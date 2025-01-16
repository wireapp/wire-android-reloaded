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
package com.wire.android.ui.common.bottomsheet.folder

import app.cash.turbine.test
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestConversation
import com.wire.android.model.asSnackBarMessage
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class ChangeConversationFavoriteViewModelTest {

    @Test
    fun `given conversation is added to favorites successfully, then infoMessage should emit success`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withAddToFavoritesResult(AddConversationToFavoritesUseCase.Result.Success)
        }

        viewModel.infoMessage.test {

            viewModel.changeFavoriteState(dialogState, addToFavorite = true)

            assertEquals(
                UIText.StringResource(R.string.success_adding_to_favorite, conversationName).asSnackBarMessage(),
                awaitItem()
            )
            coVerify(exactly = 1) {
                arrangement.addConversationToFavorites(dialogState.conversationId)
            }
        }
    }

    @Test
    fun `given conversation fails to add to favorites, then infoMessage should emit error`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withAddToFavoritesResult(AddConversationToFavoritesUseCase.Result.Failure(CoreFailure.Unknown(null)))
        }
        viewModel.infoMessage.test {
            viewModel.changeFavoriteState(dialogState, addToFavorite = true)

            assertEquals(
                UIText.StringResource(R.string.error_adding_to_favorite, conversationName).asSnackBarMessage(),
                awaitItem()
            )
            coVerify(exactly = 1) {
                arrangement.addConversationToFavorites(dialogState.conversationId)
            }
        }
    }

    @Test
    fun `given conversation is removed from favorites successfully, then infoMessage should emit success`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withRemoveFromFavoritesResult(RemoveConversationFromFavoritesUseCase.Result.Success)
        }
        viewModel.infoMessage.test {
            viewModel.changeFavoriteState(dialogState, addToFavorite = false)

            assertEquals(
                UIText.StringResource(R.string.success_removing_from_favorite, conversationName).asSnackBarMessage(),
                awaitItem()
            )
            coVerify(exactly = 1) {
                arrangement.removeConversationFromFavorites(dialogState.conversationId)
            }
        }
    }

    @Test
    fun `given conversation fails to remove from favorites, then infoMessage should emit error`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withRemoveFromFavoritesResult(RemoveConversationFromFavoritesUseCase.Result.Failure(CoreFailure.Unknown(null)))
        }
        viewModel.infoMessage.test {
            viewModel.changeFavoriteState(dialogState, addToFavorite = false)

            assertEquals(
                UIText.StringResource(R.string.error_removing_from_favorite, conversationName).asSnackBarMessage(),
                awaitItem()
            )
            coVerify(exactly = 1) {
                arrangement.removeConversationFromFavorites(dialogState.conversationId)
            }
        }
    }

    companion object {
        val dialogState = GroupDialogState(conversationId = TestConversation.ID, conversationName = "Test Conversation")
        val conversationName = dialogState.conversationName
    }

    private class Arrangement {

        @MockK
        lateinit var addConversationToFavorites: AddConversationToFavoritesUseCase

        @MockK
        lateinit var removeConversationFromFavorites: RemoveConversationFromFavoritesUseCase

        private lateinit var viewModel: ChangeConversationFavoriteViewModel

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withAddToFavoritesResult(result: AddConversationToFavoritesUseCase.Result) = apply {
            coEvery { addConversationToFavorites(any()) } returns result
        }

        fun withRemoveFromFavoritesResult(result: RemoveConversationFromFavoritesUseCase.Result) = apply {
            coEvery { removeConversationFromFavorites(any()) } returns result
        }

        fun arrange(block: Arrangement.() -> Unit) = apply(block).let {
            viewModel = ChangeConversationFavoriteViewModelImpl(
                addConversationToFavorites,
                removeConversationFromFavorites
            )
            this to viewModel
        }
    }
}
