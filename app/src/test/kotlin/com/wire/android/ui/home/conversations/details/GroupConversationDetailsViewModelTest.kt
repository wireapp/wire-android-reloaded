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

package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.mapper.testUIParticipant
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationAllParticipantsNavArgs
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateReceiptModeResult
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.GetUpdatedSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class GroupConversationDetailsViewModelTest {
    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup.copy(conversation = testGroup.conversation.copy(name = "group name"))
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        // When - Then
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details1 = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 1"))
        val details2 = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 2"))
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details1)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        // When - Then
        assertEquals(details1.conversation.name, viewModel.groupOptionsState.value.groupName)
        // When - Then
        arrangement.withConversationDetailUpdate(details2)
        assertEquals(details2.conversation.name, viewModel.groupOptionsState.value.groupName)
    }

    @Test
    fun `given some conversation details, when archiving that conversation, then the use case is invoked`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val archivingEventTimestamp = 123456789L
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )
        val conversationDetails = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 1"))
        val dialogState = DialogState(
            conversationId = conversationDetails.conversation.id,
            conversationName = conversationDetails.conversation.name.orEmpty(),
            conversationTypeDetail = ConversationTypeDetail.Group(
                conversationId = conversationDetails.conversation.id,
                isCreator = conversationDetails.isSelfUserCreator
            ),
            isArchived = conversationDetails.conversation.archived,
            isMember = true
        )

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Success)
            .arrange()

        // When
        viewModel.updateConversationArchiveStatus(
            dialogState = dialogState,
            timestamp = archivingEventTimestamp
        ) { }

        // Then
        coVerify(exactly = 1) {
            arrangement.updateConversationArchivedStatus(
                conversationId = viewModel.conversationId,
                shouldArchiveConversation = !conversationDetails.conversation.archived,
                archivedStatusTimestamp = archivingEventTimestamp,
                onlyLocally = false
            )
        }
    }

    @Test
    fun `given some conversation details, when un-archiving that conversation, then the use case is invoked`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val archivingEventTimestamp = 123456789L
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val conversationDetails = testGroup.copy(conversation = testGroup.conversation.copy(name = "Group name 1", archived = true))
        val dialogState = DialogState(
            conversationId = conversationDetails.conversation.id,
            conversationName = conversationDetails.conversation.name.orEmpty(),
            conversationTypeDetail = ConversationTypeDetail.Group(
                conversationId = conversationDetails.conversation.id,
                isCreator = conversationDetails.isSelfUserCreator
            ),
            isArchived = conversationDetails.conversation.archived,
            isMember = true
        )

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(conversationDetails)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withUpdateArchivedStatus(ArchiveStatusUpdateResult.Success)
            .arrange()

        // When
        viewModel.updateConversationArchiveStatus(
            dialogState = dialogState,
            timestamp = archivingEventTimestamp
        ) {}

        // Then
        coVerify(exactly = 1) {
            arrangement.updateConversationArchivedStatus(
                conversationId = viewModel.conversationId,
                shouldArchiveConversation = false,
                archivedStatusTimestamp = archivingEventTimestamp,
                onlyLocally = false
            )
        }
    }

    @Test
    fun `given a group conversation, when solving the state, then the state is correct`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size,
            isSelfAnAdmin = true
        )

        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                name = "group name",
                teamId = TeamId("team_id"),
                accessRole = listOf(
                    Conversation.AccessRole.GUEST,
                    Conversation.AccessRole.TEAM_MEMBER,
                    Conversation.AccessRole.NON_TEAM_MEMBER,
                    Conversation.AccessRole.SERVICE
                )
            )
        )
        val selfTeam = Team("team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
        assertEquals(details.conversation.name, viewModel.groupOptionsState.value.groupName)
        assertEquals(details.conversation.isTeamGroup(), viewModel.groupOptionsState.value.areAccessOptionsAvailable)
        assertEquals(
            (details.conversation.isGuestAllowed() || details.conversation.isNonTeamMemberAllowed()),
            viewModel.groupOptionsState.value.isGuestAllowed
        )
        assertEquals(
            conversationParticipantsData.isSelfAnAdmin && !conversationParticipantsData.isSelfExternalMember,
            viewModel.groupOptionsState.value.isUpdatingNameAllowed
        )
        assertEquals(
            conversationParticipantsData.isSelfAnAdmin && details.conversation.teamId?.value == selfTeam.id,
            viewModel.groupOptionsState.value.isUpdatingGuestAllowed
        )
        assertEquals(conversationParticipantsData.isSelfAnAdmin, viewModel.groupOptionsState.value.isUpdatingServicesAllowed)
        assertEquals(conversationParticipantsData.isSelfAnAdmin, viewModel.groupOptionsState.value.isUpdatingSelfDeletingAllowed)
        assertEquals(
            conversationParticipantsData.isSelfAnAdmin && details.conversation.isTeamGroup(),
            viewModel.groupOptionsState.value.isUpdatingReadReceiptAllowed
        )
    }

    @Test
    fun `when disabling Services , then the dialog must state must be updated`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup

        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServicesUpdate(false)
        assertEquals(true, viewModel.groupOptionsState.value.changeServiceOptionConfirmationRequired)
    }

    @Test
    fun `when no guests allowed and enabling services, use case is called with the correct values`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size,
        )

        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                accessRole = Conversation.defaultGroupAccessRoles.toMutableList().apply {
                    remove(Conversation.AccessRole.NON_TEAM_MEMBER)
                    remove(Conversation.AccessRole.GUEST)
                },
                access = listOf()
            )
        )

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServicesUpdate(true)
        coVerify(exactly = 1) {
            arrangement.updateConversationAccessRoleUseCase(
                conversationId = details.conversation.id,
                accessRoles = Conversation
                    .defaultGroupAccessRoles
                    .toMutableSet()
                    .apply {
                        add(Conversation.AccessRole.SERVICE)
                        remove(Conversation.AccessRole.NON_TEAM_MEMBER)
                    },
                access = Conversation.defaultGroupAccess
            )
        }
    }

    @Test
    fun `when no guests allowed and disable service dialog confirmed, then use case is called with the correct values`() = runTest {
        // Given
        val members = buildList {
            for (i in 1..5) {
                add(testUIParticipant(i))
            }
        }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size
        )

        val details = testGroup.copy(
            conversation = testGroup.conversation.copy(
                accessRole = Conversation.defaultGroupAccessRoles.toMutableList().apply {
                    remove(Conversation.AccessRole.NON_TEAM_MEMBER)
                    remove(Conversation.AccessRole.GUEST)
                },
                access = listOf()
            )
        )

        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationAccessUseCaseReturns(
                UpdateConversationAccessRoleUseCase.Result.Success
            ).withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .arrange()

        viewModel.onServiceDialogConfirm()
        assertEquals(false, viewModel.groupOptionsState.value.changeServiceOptionConfirmationRequired)
        coVerify(exactly = 1) {
            arrangement.updateConversationAccessRoleUseCase(
                conversationId = details.conversation.id,
                accessRoles = Conversation.defaultGroupAccessRoles.toMutableSet().apply { remove(Conversation.AccessRole.NON_TEAM_MEMBER) },
                access = Conversation.defaultGroupAccess
            )
        }
    }

    @Test
    fun `given a group conversation, when self is admin and in owner team, then should be able to edit Guests option`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(true, viewModel.groupOptionsState.value.isUpdatingGuestAllowed)
    }

    @Test
    fun `given a group conversation, when self is admin and not in owner team, then should not be able to edit Guests option`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("other_team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        // When - Then
        assertEquals(false, viewModel.groupOptionsState.value.isUpdatingGuestAllowed)
    }

    @Test
    fun `given a group conversation, then conversationSheetContent is valid`() = runTest {
        // Given
        val conversationParticipantsData = ConversationParticipantsData(isSelfAnAdmin = true)
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = TeamId("team_id")))
        val selfTeam = Team("other_team_id", "team_name", "icon")
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withSelfTeamUseCaseReturns(selfTeam)
            .arrange()

        val expected = ConversationSheetContent(
            title = details.conversation.name.orEmpty(),
            conversationId = details.conversation.id,
            mutingConversationState = details.conversation.mutedStatus,
            conversationTypeDetail = ConversationTypeDetail.Group(details.conversation.id, details.isSelfUserCreator),
            selfRole = Conversation.Member.Role.Member,
            isTeamConversation = details.conversation.isTeamGroup(),
            isArchived = false,
            protocol = Conversation.ProtocolInfo.Proteus,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            isUnderLegalHold = true,
        )
        // When - Then
        assertEquals(expected, viewModel.conversationSheetContent)
    }

    @Test
    fun `given receipt mode value enabled, when updating receipt mode, then value is propagated to screen state`() = runTest {
        // given
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationReceiptModeReturningSuccess()
            .arrange()
        val receiptModeEnabled = true

        // when
        viewModel.onReadReceiptUpdate(enableReadReceipt = receiptModeEnabled)

        // then
        assertEquals(receiptModeEnabled, viewModel.groupOptionsState.value.isReadReceiptAllowed)
        coVerify(exactly = 1) {
            arrangement.updateConversationReceiptMode(
                conversationId = any(),
                receiptMode = Conversation.ReceiptMode.ENABLED
            )
        }
    }

    @Test
    fun `given receipt mode value disabled, when updating receipt mode, then value is propagated to screen state`() = runTest {
        // given
        val (arrangement, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withUpdateConversationReceiptModeReturningSuccess()
            .arrange()
        val receiptModeEnabled = false

        // when
        viewModel.onReadReceiptUpdate(enableReadReceipt = receiptModeEnabled)

        // then
        assertEquals(receiptModeEnabled, viewModel.groupOptionsState.value.isReadReceiptAllowed)
        coVerify(exactly = 1) {
            arrangement.updateConversationReceiptMode(
                conversationId = any(),
                receiptMode = Conversation.ReceiptMode.DISABLED
            )
        }
    }

    private fun testUpdatingAllowedFields(
        isTeamGroup: Boolean = true,
        isSelfAnAdmin: Boolean = true,
        isSelfAMemberOfGroupOwnerTeam: Boolean = true,
        selfUserType: UserType = UserType.INTERNAL,
        assertResult: (GroupConversationOptionsState) -> Unit
    ) = runTest {
        val members = buildList { for (i in 1..5) { add(testUIParticipant(i)) } }
        val conversationParticipantsData = ConversationParticipantsData(
            participants = members.take(GroupConversationDetailsViewModel.MAX_NUMBER_OF_PARTICIPANTS),
            allParticipantsCount = members.size,
            isSelfAnAdmin = isSelfAnAdmin
        )
        val details = testGroup.copy(conversation = testGroup.conversation.copy(teamId = if (isTeamGroup) TeamId("team_id") else null))
        val selfTeamId = if (isTeamGroup && isSelfAMemberOfGroupOwnerTeam) details.conversation.teamId else TeamId("other_team_id")
        val self = TestUser.SELF_USER.copy(userType = selfUserType, teamId = selfTeamId)
        val (_, viewModel) = GroupConversationDetailsViewModelArrangement()
            .withConversationDetailUpdate(details)
            .withConversationMembersUpdate(conversationParticipantsData)
            .withObserveSelfUserReturns(self)
            .withSelfTeamUseCaseReturns(selfTeamId?.let { Team(it.value, "team_name", "icon") })
            .arrange()
        assertResult(viewModel.groupOptionsState.value)
    }

    @Test
    fun `given user is admin and external team member, when init group options, then group name update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = true, selfUserType = UserType.EXTERNAL) {
            assertEquals(false, it.isUpdatingNameAllowed)
        }
    @Test
    fun `given user is admin and internal team member, when init group options, then group name update is allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = true, selfUserType = UserType.INTERNAL) {
            assertEquals(true, it.isUpdatingNameAllowed)
        }
    @Test
    fun `given user is not admin and external team member, when init group options, then group name update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = false, selfUserType = UserType.EXTERNAL) {
            assertEquals(false, it.isUpdatingNameAllowed)
        }
    @Test
    fun `given user is not admin and internal team member, when init group options, then group name update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = false, selfUserType = UserType.INTERNAL) {
            assertEquals(false, it.isUpdatingNameAllowed)
        }
    @Test
    fun `given user is admin and member of group owner team, when init group options, then guests update is allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = true, isSelfAMemberOfGroupOwnerTeam = true) {
            assertEquals(true, it.isUpdatingGuestAllowed)
        }
    @Test
    fun `given user is admin and not member of group owner team, when init group options, then guests update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = true, isSelfAMemberOfGroupOwnerTeam = false) {
            assertEquals(false, it.isUpdatingGuestAllowed)
        }
    @Test
    fun `given user is not admin and member of group owner team, when init group options, then guests update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = false, isSelfAMemberOfGroupOwnerTeam = true) {
            assertEquals(false, it.isUpdatingGuestAllowed)
        }
    @Test
    fun `given user is not admin and not member of group owner team, when init group options, then guests update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = false, isSelfAMemberOfGroupOwnerTeam = false) {
            assertEquals(false, it.isUpdatingGuestAllowed)
        }
    @Test
    fun `given user is admin, when init group options, then services update is allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = true) {
            assertEquals(true, it.isUpdatingServicesAllowed)
        }
    @Test
    fun `given user is not admin, when init group options, then services update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = false) {
            assertEquals(false, it.isUpdatingServicesAllowed)
        }
    @Test
    fun `given user is admin, when init group options, then self deleting update is allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = true) {
            assertEquals(true, it.isUpdatingSelfDeletingAllowed)
        }
    @Test
    fun `given user is not admin, when init group options, then self deleting update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = false) {
            assertEquals(false, it.isUpdatingSelfDeletingAllowed)
        }
    @Test
    fun `given user is admin and team group, when init group options, then read receipts update is allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = true, isTeamGroup = true) {
            assertEquals(true, it.isUpdatingReadReceiptAllowed)
        }
    @Test
    fun `given user is admin and not team group, when init group options, then read receipts update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = true, isTeamGroup = false) {
            assertEquals(false, it.isUpdatingReadReceiptAllowed)
        }
    @Test
    fun `given user is not admin and team group, when init group options, then read receipts update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = false, isTeamGroup = true) {
            assertEquals(false, it.isUpdatingReadReceiptAllowed)
        }
    @Test
    fun `given user is not admin and not team group, when init group options, then read receipts update is not allowed`() =
        testUpdatingAllowedFields(isSelfAnAdmin = false, isTeamGroup = false) {
            assertEquals(false, it.isUpdatingReadReceiptAllowed)
        }

    companion object {
        val dummyConversationId = ConversationId("some-dummy-value", "some.dummy.domain")
        val testGroup = ConversationDetails.Group(
            Conversation(
                id = dummyConversationId,
                name = "Conv Name",
                type = Conversation.Type.ONE_ON_ONE,
                teamId = TeamId("team_id"),
                protocol = Conversation.ProtocolInfo.Proteus,
                mutedStatus = MutedConversationStatus.AllAllowed,
                removedBy = null,
                lastNotificationDate = null,
                lastModifiedDate = null,
                access = listOf(Conversation.Access.CODE, Conversation.Access.INVITE),
                accessRole = Conversation.defaultGroupAccessRoles.toMutableList().apply { add(Conversation.AccessRole.GUEST) },
                lastReadDate = "2022-04-04T16:11:28.388Z",
                creatorId = null,
                receiptMode = Conversation.ReceiptMode.ENABLED,
                messageTimer = null,
                userMessageTimer = null,
                archived = false,
                archivedDateTime = null,
                mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
                proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
                legalHoldStatus = Conversation.LegalHoldStatus.ENABLED
            ),
            hasOngoingCall = false,
            lastMessage = null,
            isSelfUserCreator = false,
            isSelfUserMember = true,
            unreadEventCount = emptyMap(),
            selfRole = Conversation.Member.Role.Member
        )
    }
}

internal class GroupConversationDetailsViewModelArrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var deleteTeamConversation: DeleteTeamConversationUseCase

    @MockK
    lateinit var removeMemberFromConversation: RemoveMemberFromConversationUseCase

    @MockK
    lateinit var observerSelfUser: GetSelfUserUseCase

    @MockK
    lateinit var observeParticipantsForConversationUseCase: ObserveParticipantsForConversationUseCase

    @MockK
    lateinit var updateConversationAccessRoleUseCase: UpdateConversationAccessRoleUseCase

    @MockK
    lateinit var getSelfTeamUseCase: GetUpdatedSelfTeamUseCase

    @MockK
    lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @MockK
    lateinit var clearConversationContentUseCase: ClearConversationContentUseCase

    @MockK
    lateinit var isMLSEnabledUseCase: IsMLSEnabledUseCase

    @MockK
    lateinit var updateConversationReceiptMode: UpdateConversationReceiptModeUseCase

    @MockK
    lateinit var observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase

    @MockK
    lateinit var updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase

    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)

    private val observeParticipantsForConversationChannel = Channel<ConversationParticipantsData>(capacity = Channel.UNLIMITED)

    @MockK
    private lateinit var refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase

    private val viewModel by lazy {
        GroupConversationDetailsViewModel(
            dispatcher = TestDispatcherProvider(),
            observerSelfUser = observerSelfUser,
            observeConversationDetails = observeConversationDetails,
            deleteTeamConversation = deleteTeamConversation,
            removeMemberFromConversation = removeMemberFromConversation,
            observeConversationMembers = observeParticipantsForConversationUseCase,
            updateConversationAccessRole = updateConversationAccessRoleUseCase,
            getSelfTeam = getSelfTeamUseCase,
            savedStateHandle = savedStateHandle,
            updateConversationMutedStatus = updateConversationMutedStatus,
            clearConversationContent = clearConversationContentUseCase,
            updateConversationReceiptMode = updateConversationReceiptMode,
            isMLSEnabled = isMLSEnabledUseCase,
            observeSelfDeletionTimerSettingsForConversation = observeSelfDeletionTimerSettingsForConversation,
            refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
            updateConversationArchivedStatus = updateConversationArchivedStatus
        )
    }

    val conversationId = ConversationId("some-dummy-value", "some.dummy.domain")

    init {

        // Tests setup
        MockKAnnotations.init(this, relaxUnitFun = true)

        every { savedStateHandle.navArgs<GroupConversationAllParticipantsNavArgs>() } returns GroupConversationAllParticipantsNavArgs(
            conversationId = conversationId
        )
        every { savedStateHandle.navArgs<GroupConversationDetailsNavArgs>() } returns GroupConversationDetailsNavArgs(
            conversationId = conversationId
        )

        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { observerSelfUser() } returns flowOf(TestUser.SELF_USER)
        coEvery { observeParticipantsForConversationUseCase(any(), any()) } returns flowOf()
        coEvery { getSelfTeamUseCase() } returns Either.Right(null)
        coEvery { isMLSEnabledUseCase() } returns true
        coEvery { updateConversationMutedStatus(any(), any(), any()) } returns ConversationUpdateStatusResult.Success
        coEvery { observeSelfDeletionTimerSettingsForConversation(any(), any()) } returns flowOf(SelfDeletionTimer.Disabled)
        coEvery { updateConversationArchivedStatus(any(), any(), any()) } returns ArchiveStatusUpdateResult.Success
    }

    suspend fun withObserveSelfUserReturns(user: SelfUser) = apply {
        coEvery { observerSelfUser() } returns flowOf(user)
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails) = apply {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()
            .map { ObserveConversationDetailsUseCase.Result.Success(it) }
        conversationDetailsChannel.send(conversationDetails)
    }

    suspend fun withConversationMembersUpdate(conversationParticipantsData: ConversationParticipantsData) = apply {
        coEvery { observeParticipantsForConversationUseCase(any()) } returns observeParticipantsForConversationChannel.consumeAsFlow()
        observeParticipantsForConversationChannel.send(conversationParticipantsData)
    }

    suspend fun withUpdateConversationAccessUseCaseReturns(result: UpdateConversationAccessRoleUseCase.Result) = apply {
        coEvery { updateConversationAccessRoleUseCase(any(), any(), any()) } returns result
    }

    suspend fun withSelfTeamUseCaseReturns(result: Team?) = apply {
        coEvery { getSelfTeamUseCase() } returns Either.Right(result)
    }

    suspend fun withUpdateConversationReceiptModeReturningSuccess() = apply {
        coEvery { updateConversationReceiptMode(any(), any()) } returns ConversationUpdateReceiptModeResult.Success
    }

    suspend fun withUpdateArchivedStatus(result: ArchiveStatusUpdateResult) = apply {
        coEvery { updateConversationArchivedStatus(any(), any(), any()) } returns result
        coEvery { updateConversationArchivedStatus(any(), any(), any(), any()) } returns result
    }

    fun arrange() = this to viewModel
}
