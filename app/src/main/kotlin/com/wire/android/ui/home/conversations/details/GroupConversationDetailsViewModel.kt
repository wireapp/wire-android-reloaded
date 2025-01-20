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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.home.conversations.details.menu.GroupConversationDetailsBottomSheetEventsHandler
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.model.LeaveGroupDialogState
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.util.uiText
import com.wire.android.workmanager.worker.ConversationDeletionLocallyStatus
import com.wire.android.workmanager.worker.enqueueConversationDeletionLocally
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.SupportedProtocol
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
import com.wire.kalium.logic.feature.team.Result
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.functional.getOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class GroupConversationDetailsViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val updateConversationAccessRole: UpdateConversationAccessRoleUseCase,
    private val getSelfTeam: GetUpdatedSelfTeamUseCase,
    private val observerSelfUser: ObserveSelfUserUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val clearConversationContent: ClearConversationContentUseCase,
    private val updateConversationReceiptMode: UpdateConversationReceiptModeUseCase,
    private val observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase,
    override val savedStateHandle: SavedStateHandle,
    private val isMLSEnabled: IsMLSEnabledUseCase,
    private val getDefaultProtocol: GetDefaultProtocolUseCase,
    private val workManager: WorkManager,
    refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
) : GroupConversationParticipantsViewModel(
    savedStateHandle, observeConversationMembers, refreshUsersWithoutMetadata
), GroupConversationDetailsBottomSheetEventsHandler {

    private val groupConversationDetailsNavArgs: GroupConversationDetailsNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = groupConversationDetailsNavArgs.conversationId

    var conversationSheetContent: ConversationSheetContent? by mutableStateOf(null)
        private set

    private val _groupOptionsState = MutableStateFlow(GroupConversationOptionsState(conversationId))
    val groupOptionsState: StateFlow<GroupConversationOptionsState> = _groupOptionsState

    var requestInProgress: Boolean by mutableStateOf(false)
        private set

    init {
        observeConversationDetails()
    }

    private suspend fun groupDetailsFlow(): Flow<ConversationDetails.Group> = observeConversationDetails(conversationId)
        .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
        .map { it.conversationDetails }
        .filterIsInstance<ConversationDetails.Group>()
        .distinctUntilChanged()
        .flowOn(dispatcher.io())

    private fun observeConversationDetails() {
        viewModelScope.launch {
            val groupDetailsFlow = groupDetailsFlow()
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)

            val selfTeam = getSelfTeam().getOrNull()
            val selfUser = observerSelfUser().first()
            val isMLSTeam = getDefaultProtocol() == SupportedProtocol.MLS

            combine(
                groupDetailsFlow,
                observeSelfDeletionTimerSettingsForConversation(conversationId, considerSelfUserSettings = false),
            ) { groupDetails, selfDeletionTimer ->
                val isSelfInOwnerTeam = selfTeam?.id != null && selfTeam.id == groupDetails.conversation.teamId?.value
                val isSelfExternalMember = selfUser.userType == UserType.EXTERNAL
                val isSelfAnAdmin = groupDetails.selfRole == Conversation.Member.Role.Admin
                val isMLSConversation = groupDetails.conversation.protocol is Conversation.ProtocolInfo.MLS

                conversationSheetContent = ConversationSheetContent(
                    title = groupDetails.conversation.name.orEmpty(),
                    conversationId = conversationId,
                    mutingConversationState = groupDetails.conversation.mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Group(
                        conversationId = conversationId,
                        isFromTheSameTeam = groupDetails.conversation.teamId == observerSelfUser().firstOrNull()?.teamId
                    ),
                    isTeamConversation = groupDetails.conversation.teamId?.value != null,
                    selfRole = groupDetails.selfRole,
                    isArchived = groupDetails.conversation.archived,
                    protocol = groupDetails.conversation.protocol,
                    mlsVerificationStatus = groupDetails.conversation.mlsVerificationStatus,
                    proteusVerificationStatus = groupDetails.conversation.proteusVerificationStatus,
                    isUnderLegalHold = groupDetails.conversation.legalHoldStatus.showLegalHoldIndicator(),
                    isFavorite = groupDetails.isFavorite,
                    folder = groupDetails.folder,
                    isDeletingConversationLocallyRunning = false
                )

                updateState(
                    groupOptionsState.value.copy(
                        groupName = groupDetails.conversation.name.orEmpty(),
                        protocolInfo = groupDetails.conversation.protocol,
                        areAccessOptionsAvailable = groupDetails.conversation.isTeamGroup(),
                        isGuestAllowed = groupDetails.conversation.isGuestAllowed() || groupDetails.conversation.isNonTeamMemberAllowed(),
                        isServicesAllowed = groupDetails.conversation.isServicesAllowed() && !isMLSTeam && !isMLSConversation,
                        isUpdatingNameAllowed = isSelfAnAdmin && !isSelfExternalMember,
                        isUpdatingGuestAllowed = isSelfAnAdmin && isSelfInOwnerTeam,
                        isUpdatingServicesAllowed = isSelfAnAdmin && !isMLSTeam && !isMLSConversation,
                        isUpdatingReadReceiptAllowed = isSelfAnAdmin && groupDetails.conversation.isTeamGroup(),
                        isUpdatingSelfDeletingAllowed = isSelfAnAdmin,
                        mlsEnabled = isMLSEnabled(),
                        isReadReceiptAllowed = groupDetails.conversation.receiptMode == Conversation.ReceiptMode.ENABLED,
                        selfDeletionTimer = selfDeletionTimer
                    )
                )
            }.collect {}
        }
    }

    fun leaveGroup(
        leaveGroupState: LeaveGroupDialogState,
        onSuccess: () -> Unit,
        onFailure: (UIText) -> Unit
    ) {
        viewModelScope.launch {
            requestInProgress = true
            val response = withContext(dispatcher.io()) {
                val selfUser = observerSelfUser().first()
                removeMemberFromConversation(
                    leaveGroupState.conversationId, selfUser.id
                )
            }
            when (response) {
                is RemoveMemberFromConversationUseCase.Result.Failure -> onFailure(response.cause.uiText())
                RemoveMemberFromConversationUseCase.Result.Success -> {
                    if (leaveGroupState.shouldDelete) {
                        deleteGroupLocally(leaveGroupState, onSuccess, onFailure)
                    } else {
                        onSuccess()
                    }
                }
            }
            requestInProgress = false
        }
    }

    fun deleteGroup(
        groupState: GroupDialogState,
        onSuccess: () -> Unit,
        onFailure: (UIText) -> Unit
    ) {
        viewModelScope.launch {
            requestInProgress = true
            when (val response = withContext(dispatcher.io()) { deleteTeamConversation(groupState.conversationId) }) {
                is Result.Failure.GenericFailure -> onFailure(response.coreFailure.uiText())
                Result.Failure.NoTeamFailure -> onFailure(CoreFailure.Unknown(null).uiText())
                Result.Success -> onSuccess()
            }
            requestInProgress = false
        }
    }

    fun deleteGroupLocally(
        groupDialogState: GroupDialogState,
        onSuccess: () -> Unit,
        onFailure: (UIText) -> Unit
    ) {
        viewModelScope.launch {
            workManager.enqueueConversationDeletionLocally(groupDialogState.conversationId)
                .collect { status ->
                    when (status) {
                        ConversationDeletionLocallyStatus.SUCCEEDED -> onSuccess()

                        ConversationDeletionLocallyStatus.FAILED ->
                            onFailure(UIText.StringResource(R.string.delete_group_conversation_error))

                        ConversationDeletionLocallyStatus.RUNNING,
                        ConversationDeletionLocallyStatus.IDLE -> {
                            // nop
                        }
                    }
                }
        }
    }

    fun onServicesUpdate(enableServices: Boolean) {
        updateState(groupOptionsState.value.copy(loadingServicesOption = true, isServicesAllowed = enableServices))
        when (enableServices) {
            true -> updateServicesRemoteRequest(enableServices)
            false -> updateState(groupOptionsState.value.copy(changeServiceOptionConfirmationRequired = true))
        }
    }

    fun onReadReceiptUpdate(enableReadReceipt: Boolean) {
        appLogger.i("[$TAG][onReadReceiptUpdate] - enableReadReceipt: $enableReadReceipt")
        updateState(groupOptionsState.value.copy(loadingReadReceiptOption = true, isReadReceiptAllowed = enableReadReceipt))
        updateReadReceiptRemoteRequest(enableReadReceipt)
    }

    fun onServiceDialogDismiss() {
        updateState(
            groupOptionsState.value.copy(
                loadingServicesOption = false,
                changeServiceOptionConfirmationRequired = false,
                isServicesAllowed = !groupOptionsState.value.isServicesAllowed
            )
        )
    }

    fun onServiceDialogConfirm() {
        updateState(groupOptionsState.value.copy(changeServiceOptionConfirmationRequired = false, loadingServicesOption = true))
        updateServicesRemoteRequest(false)
    }

    private fun updateServicesRemoteRequest(enableServices: Boolean) {
        viewModelScope.launch {
            val result = withContext(dispatcher.io()) {
                updateConversationAccess(
                    enableGuestAndNonTeamMember = groupOptionsState.value.isGuestAllowed,
                    enableServices = enableServices,
                    conversationId = conversationId
                )
            }

            when (result) {
                is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                    groupOptionsState.value.copy(
                        isServicesAllowed = !enableServices, error = GroupConversationOptionsState.Error.UpdateServicesError(result.cause)
                    )
                )

                is UpdateConversationAccessRoleUseCase.Result.Success -> Unit
            }

            updateState(groupOptionsState.value.copy(loadingServicesOption = false))
        }
    }

    private fun updateReadReceiptRemoteRequest(enableReadReceipt: Boolean) {
        viewModelScope.launch {
            val result = withContext(dispatcher.io()) {
                updateConversationReceiptMode(
                    conversationId = conversationId,
                    receiptMode = when (enableReadReceipt) {
                        true -> Conversation.ReceiptMode.ENABLED
                        else -> Conversation.ReceiptMode.DISABLED
                    }
                )
            }

            when (result) {
                is ConversationUpdateReceiptModeResult.Failure -> updateState(
                    groupOptionsState.value.copy(
                        isReadReceiptAllowed = !enableReadReceipt,
                        error = GroupConversationOptionsState.Error.UpdateReadReceiptError(result.cause)
                    )
                )

                ConversationUpdateReceiptModeResult.Success -> Unit
            }

            updateState(groupOptionsState.value.copy(loadingReadReceiptOption = false))
        }
    }

    private suspend fun updateConversationAccess(
        enableGuestAndNonTeamMember: Boolean,
        enableServices: Boolean,
        conversationId: ConversationId
    ): UpdateConversationAccessRoleUseCase.Result {

        val accessRoles = Conversation
            .accessRolesFor(
                guestAllowed = enableGuestAndNonTeamMember,
                servicesAllowed = enableServices,
                nonTeamMembersAllowed = enableGuestAndNonTeamMember
            )

        val access = Conversation
            .accessFor(
                guestsAllowed = enableGuestAndNonTeamMember
            )

        return updateConversationAccessRole(
            conversationId = conversationId,
            accessRoles = accessRoles,
            access = access
        )
    }

    private fun updateState(newState: GroupConversationOptionsState) {
        _groupOptionsState.value = newState
    }

    override fun onMutingConversationStatusChange(
        conversationId: ConversationId?,
        status: MutedConversationStatus,
        onMessage: (UIText) -> Unit
    ) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, status, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> {
                        onMessage(UIText.StringResource(R.string.error_updating_muting_setting))
                    }

                    ConversationUpdateStatusResult.Success -> {
                        appLogger.i("MutedStatus changed for conversation: $conversationId to $status")
                    }
                }
            }
        }
    }

    override fun onClearConversationContent(dialogState: DialogState, onMessage: (UIText) -> Unit) {
        viewModelScope.launch {
            requestInProgress = true
            with(dialogState) {
                val result = withContext(dispatcher.io()) { clearConversationContent(conversationId) }
                requestInProgress = false
                handleClearContentResult(result, conversationTypeDetail, onMessage)
            }
        }
    }

    private fun handleClearContentResult(
        clearContentResult: ClearConversationContentUseCase.Result,
        conversationTypeDetail: ConversationTypeDetail,
        onMessage: (UIText) -> Unit
    ) {
        if (conversationTypeDetail is ConversationTypeDetail.Connection) throw IllegalStateException(
            "Unsupported conversation type to clear content, something went wrong?"
        )

        if (clearContentResult is ClearConversationContentUseCase.Result.Failure) {
            onMessage(UIText.StringResource(R.string.group_content_delete_failure))
        } else {
            onMessage(UIText.StringResource(R.string.group_content_deleted))
        }
    }

    override fun updateConversationArchiveStatus(
        dialogState: DialogState,
        timestamp: Long,
        onMessage: (UIText) -> Unit
    ) {
        viewModelScope.launch {
            val shouldArchive = dialogState.isArchived.not()
            requestInProgress = true
            val result = withContext(dispatcher.io()) {
                updateConversationArchivedStatus(
                    conversationId = conversationId,
                    shouldArchiveConversation = shouldArchive,
                    onlyLocally = !dialogState.isMember,
                    archivedStatusTimestamp = timestamp
                )
            }
            requestInProgress = false
            when (result) {
                ArchiveStatusUpdateResult.Failure -> onMessage(
                    UIText.StringResource(
                        if (shouldArchive) R.string.error_archiving_conversation else R.string.error_unarchiving_conversation
                    )
                )

                ArchiveStatusUpdateResult.Success -> onMessage(
                    UIText.StringResource(
                        if (shouldArchive) R.string.success_archiving_conversation else R.string.success_unarchiving_conversation
                    )
                )
            }
        }
    }

    companion object {
        const val TAG = "GroupConversationDetailsViewModel"
    }
}
