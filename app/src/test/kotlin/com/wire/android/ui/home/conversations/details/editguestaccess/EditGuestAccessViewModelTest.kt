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

@file:Suppress("MaximumLineLength")

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestConversationDetails
import com.wire.android.ui.home.conversations.details.participants.model.ConversationParticipantsData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.configuration.GuestRoomLinkStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.CanCreatePasswordProtectedLinksUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import com.wire.kalium.logic.functional.Either
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class)
class EditGuestAccessViewModelTest {

    val dispatcher = TestDispatcherProvider()

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var updateConversationAccessRoleUseCase: UpdateConversationAccessRoleUseCase

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var observeConversationMembers: ObserveParticipantsForConversationUseCase

    @MockK
    lateinit var generateGuestRoomLink: GenerateGuestRoomLinkUseCase

    @MockK
    lateinit var observeGuestRoomLink: ObserveGuestRoomLinkUseCase

    @MockK
    lateinit var revokeGuestRoomLink: RevokeGuestRoomLinkUseCase

    @MockK
    lateinit var observeGuestRoomLinkFeatureFlag: ObserveGuestRoomLinkFeatureFlagUseCase

    @MockK
    lateinit var canCreatePasswordProtectedLinks: CanCreatePasswordProtectedLinksUseCase

    private lateinit var editGuestAccessViewModel: EditGuestAccessViewModel

    private val conversationDetailsChannel = Channel<ObserveConversationDetailsUseCase.Result>(capacity = Channel.UNLIMITED)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        coEvery { savedStateHandle.navArgs<EditGuestAccessNavArgs>() } returns EditGuestAccessNavArgs(
            conversationId = TestConversation.ID,
            editGuessAccessParams = EditGuestAccessParams(
                isGuestAccessAllowed = true,
                isServicesAllowed = true,
                isUpdatingGuestAccessAllowed = true
            )
        )
        coEvery {
            observeConversationDetails(any())
        } returns conversationDetailsChannel.consumeAsFlow()
        coEvery {
            observeConversationMembers(any())
        } returns flowOf(ConversationParticipantsData())
        coEvery {
            observeGuestRoomLink(any())
        } returns flowOf(Either.Right(null))
        coEvery {
            observeGuestRoomLinkFeatureFlag()
        } returns flowOf(GuestRoomLinkStatus(null, null))
        coEvery {
            canCreatePasswordProtectedLinks()
        } returns true

        editGuestAccessViewModel = EditGuestAccessViewModel(
            observeConversationDetails = observeConversationDetails,
            observeConversationMembers = observeConversationMembers,
            updateConversationAccessRole = updateConversationAccessRoleUseCase,
            generateGuestRoomLink = generateGuestRoomLink,
            revokeGuestRoomLink = revokeGuestRoomLink,
            observeGuestRoomLink = observeGuestRoomLink,
            savedStateHandle = savedStateHandle,
            observeGuestRoomLinkFeatureFlag = observeGuestRoomLinkFeatureFlag,
            canCreatePasswordProtectedLinks = canCreatePasswordProtectedLinks,
            dispatcher = dispatcher,
        )
        conversationDetailsChannel.trySend(ObserveConversationDetailsUseCase.Result.Success(TestConversationDetails.GROUP))
    }

    @Test
    fun `given updateConversationAccessRole use case runs successfully, when trying to enable guest access, then enable guest access`() =
        runTest(dispatcher.default()) {
            editGuestAccessViewModel.editGuestAccessState = editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = false)
            coEvery {
                updateConversationAccessRoleUseCase(any(), any(), any())
            } returns UpdateConversationAccessRoleUseCase.Result.Success

            editGuestAccessViewModel.updateGuestAccess(true)

            coVerify(exactly = 1) {
                updateConversationAccessRoleUseCase(any(), any(), any())
            }
            assertEquals(
                true,
                editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed
            )
        }

    @Test
    fun `given a failure when running updateConversationAccessRole, when trying to enable guest access, then do not enable guest access`() {
        editGuestAccessViewModel.editGuestAccessState =
            editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = false)
        coEvery {
            updateConversationAccessRoleUseCase(any(), any(), any())
        } returns UpdateConversationAccessRoleUseCase.Result.Failure(
            CoreFailure.MissingClientRegistration
        )

        editGuestAccessViewModel.updateGuestAccess(true)

        coVerify(exactly = 1) {
            updateConversationAccessRoleUseCase(any(), any(), any())
        }
        assertEquals(
            false,
            editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed
        )
    }

    @Test
    fun `given guest access is activated, when trying to disable guest access, then display dialog before disabling guest access`() {
        editGuestAccessViewModel.editGuestAccessState =
            editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = true)

        editGuestAccessViewModel.updateGuestAccess(false)

        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.shouldShowGuestAccessChangeConfirmationDialog)
        coVerify(inverse = true) {
            updateConversationAccessRoleUseCase(any(), any(), any())
        }
    }

    @Test
    fun `given useCase runs with success, when_generating guest link, then invoke it once`() = runTest(dispatcher.default()) {
        coEvery {
            generateGuestRoomLink.invoke(any(), any())
        } returns GenerateGuestRoomLinkResult.Success

        editGuestAccessViewModel.onRequestGuestRoomLink()
        coVerify(exactly = 1) {
            generateGuestRoomLink.invoke(any(), null)
        }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isGeneratingGuestRoomLink)
    }

    @Test
    fun `given useCase runs with failure, when generating guest link, then show dialog error`() = runTest(dispatcher.default()) {
        coEvery {
            generateGuestRoomLink(any(), any())
        } returns GenerateGuestRoomLinkResult.Failure(NetworkFailure.NoNetworkConnection(null))

        editGuestAccessViewModel.onRequestGuestRoomLink()

        coVerify(exactly = 1) {
            generateGuestRoomLink(any(), null)
        }
        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isFailedToGenerateGuestRoomLink)
    }

    @Test
    fun `given useCase runs with success, when revoking guest link, then invoke it once`() = runTest(dispatcher.default()) {
        coEvery {
            revokeGuestRoomLink(any())
        } returns RevokeGuestRoomLinkResult.Success

        editGuestAccessViewModel.removeGuestLink()

        coVerify(exactly = 1) {
            revokeGuestRoomLink(any())
        }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isRevokingLink)
    }

    @Test
    fun `given useCase runs with failure when revoking guest link then show dialog error`() = runTest(dispatcher.default()) {
        coEvery {
            revokeGuestRoomLink(any())
        } returns RevokeGuestRoomLinkResult.Failure(CoreFailure.MissingClientRegistration)

        editGuestAccessViewModel.removeGuestLink()

        coVerify(exactly = 1) {
            revokeGuestRoomLink(any())
        }
        assertEquals(false, editGuestAccessViewModel.editGuestAccessState.isRevokingLink)
        assertEquals(true, editGuestAccessViewModel.editGuestAccessState.isFailedToRevokeGuestRoomLink)
    }

    @Test
    fun `given updateConversationAccessRole use case runs successfully, when trying to disable guest access, then disable guest access`() =
        runTest(dispatcher.default()) {
            editGuestAccessViewModel.editGuestAccessState = editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = true)
            coEvery {
                updateConversationAccessRoleUseCase(any(), any(), any())
            } coAnswers {
                val accessRoles = secondArg<Set<Conversation.AccessRole>>()
                val newConversationDetails = TestConversationDetails.GROUP.copy(
                    conversation = TestConversationDetails.GROUP.conversation.copy(accessRole = accessRoles.toList())
                )
                // mock emitting updated conversation details with new access roles
                conversationDetailsChannel.send(ObserveConversationDetailsUseCase.Result.Success(newConversationDetails))
                UpdateConversationAccessRoleUseCase.Result.Success
            }

            editGuestAccessViewModel.onGuestDialogConfirm()

            coVerify(exactly = 1) {
                updateConversationAccessRoleUseCase(any(), any(), any())
            }
            assertEquals(
                false,
                editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed
            )
        }

    @Test
    fun `given a failure running updateConversationAccessRole, when trying to disable guest access, then do not disable guest access`() =
        runTest {
            editGuestAccessViewModel.editGuestAccessState = editGuestAccessViewModel.editGuestAccessState.copy(isGuestAccessAllowed = true)
            coEvery {
                updateConversationAccessRoleUseCase(any(), any(), any())
            } returns UpdateConversationAccessRoleUseCase.Result.Failure(CoreFailure.MissingClientRegistration)

            editGuestAccessViewModel.onGuestDialogConfirm()

            coVerify(exactly = 1) {
                updateConversationAccessRoleUseCase(any(), any(), any())
            }
            assertEquals(
                true,
                editGuestAccessViewModel.editGuestAccessState.isGuestAccessAllowed
            )
        }
}
