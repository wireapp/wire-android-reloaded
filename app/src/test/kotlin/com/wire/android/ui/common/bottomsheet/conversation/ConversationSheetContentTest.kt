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
package com.wire.android.ui.common.bottomsheet.conversation

import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModelTest.Companion.testGroup
import com.wire.android.ui.home.conversationslist.model.BlockingState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.TeamId
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConversationSheetContentTest {

    @Test
    fun givenTitleIsEmptyAndTheGroupSizeIsOne_whenCallingIsTheGroupAbandoned_returnsTrue() = runTest {
        val givenConversationSheetContent = createGroupSheetContent("")
        val givenParticipantsCount = 1

        assertEquals(true, givenConversationSheetContent.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }

    @Test
    fun givenTitleIsEmptyAndTheGroupSizeIsGtOne_whenCallingIsTheGroupAbandoned_returnsFalse() = runTest {
        val givenConversationSheetContent = createGroupSheetContent("")
        val givenParticipantsCount = 3

        assertEquals(false, givenConversationSheetContent.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }

    @Test
    fun givenTitleIsNotEmptyAndTheGroupSizeIsOne_whenCallingIsTheGroupAbandoned_returnsFalse() = runTest {
        val givenConversationSheetContent = createGroupSheetContent("notEmpty")
        val givenParticipantsCount = 3

        assertEquals(false, givenConversationSheetContent.isAbandonedOneOnOneConversation(givenParticipantsCount))
    }

    @Test
    fun givenPrivateConversationWithoutBlockedAndNotDeletedUser_whenCanDeleteUserIsInvoked_thenReturnsTrue() = runTest {
        // given
        val conversationSheetContent =
            createPrivateSheetContent(blockingState = BlockingState.NOT_BLOCKED, isUserDeleted = false)

        // when
        val canBlockUser = conversationSheetContent.canBlockUser()

        // then
        assertTrue(canBlockUser)
    }

    @Test
    fun givenGroupConversation_whenCanDeleteUserIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent = createGroupSheetContent("")

        // when
        val canBlockUser = conversationSheetContent.canBlockUser()

        // then
        assertFalse(canBlockUser)
    }

    @Test
    fun givenPrivateConversationWithBlockedAndNotDeletedUser_whenCanDeleteUserIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent =
            createPrivateSheetContent(blockingState = BlockingState.BLOCKED, isUserDeleted = false)

        // when
        val canBlockUser = conversationSheetContent.canBlockUser()

        // then
        assertFalse(canBlockUser)
    }

    @Test
    fun givenPrivateConversationWithoutBlockedAndDeletedUser_whenCanDeleteUserIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent =
            createPrivateSheetContent(blockingState = BlockingState.NOT_BLOCKED, isUserDeleted = true)

        // when
        val canBlockUser = conversationSheetContent.canBlockUser()

        // then
        assertFalse(canBlockUser)
    }

    @Test
    fun givenPrivateConversationWithoutBlockedAndNotDeletedUser_whenCanEditNotificationsIsInvoked_thenReturnsTrue() = runTest {
        // given
        val conversationSheetContent =
            createPrivateSheetContent(blockingState = BlockingState.NOT_BLOCKED, isUserDeleted = false)

        // when
        val canEditNotifications = conversationSheetContent.canEditNotifications()

        // then
        assertTrue(canEditNotifications)
    }

    @Test
    fun givenGroupConversation_whenCanEditNotificationsIsInvoked_thenReturnsTrue() = runTest {
        // given
        val conversationSheetContent = createGroupSheetContent("")

        // when
        val canEditNotifications = conversationSheetContent.canEditNotifications()

        // then
        assertTrue(canEditNotifications)
    }

    @Test
    fun givenPrivateConversationWithBlockedAndNotDeletedUser_whenCanEditNotificationsIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent =
            createPrivateSheetContent(blockingState = BlockingState.BLOCKED, isUserDeleted = false)

        // when
        val canEditNotifications = conversationSheetContent.canEditNotifications()

        // then
        assertFalse(canEditNotifications)
    }

    @Test
    fun givenPrivateConversationWithoutBlockedAndDeletedUser_whenCanEditNotificationsIsInvoked_thenReturnsFalse() = runTest {
        // given
        val conversationSheetContent =
            createPrivateSheetContent(blockingState = BlockingState.BLOCKED, isUserDeleted = false)

        // when
        val canEditNotifications = conversationSheetContent.canEditNotifications()

        // then
        assertFalse(canEditNotifications)
    }

    @Test
    fun givenGroupConversation_whenMemberOfTheConversation_thenDeleteConversationLocallyIsNotVisible() = runTest {
        // given
        val conversationSheetContent = createGroupSheetContent("Title")

        // when
        val canDeleteGroupLocally = conversationSheetContent.canDeleteGroupLocally()

        // then
        assertFalse(canDeleteGroupLocally)
    }

    @Test
    fun givenGroupConversation_whenNotMemberOfTheConversation_thenDeleteConversationLocallyIsVisible() = runTest {
        // given
        val conversationSheetContent = createGroupSheetContent(title = "Title", selfRole = null)

        // when
        val canDeleteGroupLocally = conversationSheetContent.canDeleteGroupLocally()

        // then
        assertTrue(canDeleteGroupLocally)
    }

    @Test
    fun givenGroupConversation_whenNotMemberOfTheConversationAndDeletionRunning_thenDeleteConversationLocallyIsNotVisible() = runTest {
        // given
        val conversationSheetContent =
            createGroupSheetContent(title = "Title", selfRole = null, conversationDeletionLocallyRunning = true)

        // when
        val canDeleteGroupLocally = conversationSheetContent.canDeleteGroupLocally()

        // then
        assertFalse(canDeleteGroupLocally)
    }

    private fun createPrivateSheetContent(
        blockingState: BlockingState,
        isUserDeleted: Boolean
    ): ConversationSheetContent {
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        return ConversationSheetContent(
            title = "notEmpty",
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Private(
                avatarAsset = null,
                userId = TestUser.USER_ID,
                blockingState = blockingState,
                isUserDeleted = isUserDeleted
            ),
            selfRole = Conversation.Member.Role.Member,
            isTeamConversation = details.conversation.isTeamGroup(),
            isArchived = false,
            protocol = Conversation.ProtocolInfo.Proteus,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            isUnderLegalHold = false,
            isFavorite = false,
            isDeletingConversationLocallyRunning = false
        )
    }

    private fun createGroupSheetContent(
        title: String,
        selfRole: Conversation.Member.Role? = Conversation.Member.Role.Member,
        conversationDeletionLocallyRunning: Boolean = false,
    ): ConversationSheetContent {
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))

        return ConversationSheetContent(
            title = title,
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Group(details.conversation.id, false),
            selfRole = selfRole,
            isTeamConversation = details.conversation.isTeamGroup(),
            isArchived = false,
            protocol = Conversation.ProtocolInfo.Proteus,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            isUnderLegalHold = false,
            isFavorite = false,
            isDeletingConversationLocallyRunning = conversationDeletionLocallyRunning
        )
    }
}
