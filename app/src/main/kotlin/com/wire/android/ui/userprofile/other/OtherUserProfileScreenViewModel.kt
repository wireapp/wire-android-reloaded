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

package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAsset
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.showLegalHoldIndicator
import com.wire.android.ui.navArgs
import com.wire.android.ui.userprofile.common.UsernameMapper.fromOtherUser
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.BlockingUserOperationError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.BlockingUserOperationSuccess
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.ChangeGroupRoleError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.MutingOperationError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.RemoveConversationMemberError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.UnblockingUserOperationError
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserE2eiCertificatesUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val blockUser: BlockUserUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val observeOneToOneConversation: GetOneToOneConversationUseCase,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val updateMemberRole: UpdateConversationMemberRoleUseCase,
    private val observeClientList: ObserveClientsByUserIdUseCase,
    private val fetchUsersClients: FetchUsersClientsFromRemoteUseCase,
    private val clearConversationContentUseCase: ClearConversationContentUseCase,
    private val updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase,
    private val getUserE2eiCertificateStatus: IsOtherUserE2EIVerifiedUseCase,
    private val getUserE2eiCertificates: GetUserE2eiCertificatesUseCase,
    private val isOneToOneConversationCreated: IsOneToOneConversationCreatedUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel(), OtherUserProfileEventsHandler, OtherUserProfileBottomSheetEventsHandler {

    private val otherUserProfileNavArgs: OtherUserProfileNavArgs = savedStateHandle.navArgs()
    private val userId: QualifiedID = otherUserProfileNavArgs.userId
    private val conversationId: QualifiedID? = otherUserProfileNavArgs.conversationId

    var state: OtherUserProfileState by mutableStateOf(
        OtherUserProfileState(
            userId = userId,
            conversationId = conversationId,
            isDataLoading = true,
            isAvatarLoading = true
        )
    )
    var requestInProgress: Boolean by mutableStateOf(false)

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    private val _closeBottomSheet = MutableSharedFlow<Unit>()
    val closeBottomSheet = _closeBottomSheet.asSharedFlow()

    init {
        observeUserInfoAndUpdateViewState()
        persistClients()
        getMLSVerificationStatus()
        getIfConversationExist()
    }

    private fun getIfConversationExist() {
        viewModelScope.launch {
            state = state.copy(isConversationStarted = isOneToOneConversationCreated(userId))
        }
    }

    private fun getMLSVerificationStatus() {
        viewModelScope.launch {
            val isMLSVerified = getUserE2eiCertificateStatus(userId)
            state = state.copy(isMLSVerified = isMLSVerified)
        }
    }

    override fun observeClientList() {
        viewModelScope.launch(dispatchers.io()) {
            observeClientList(userId)
                .collect {
                    val e2eiCertificates = getUserE2eiCertificates(userId)
                    when (it) {
                        is ObserveClientsByUserIdUseCase.Result.Failure -> {
                            /* no-op */
                        }

                        is ObserveClientsByUserIdUseCase.Result.Success -> {
                            state = state.copy(otherUserDevices = it.clients.map { item ->
                                Device(item, e2eiCertificates[item.id.value])
                            })
                        }
                    }
                }
        }
    }

    private fun persistClients() {
        viewModelScope.launch(dispatchers.io()) {
            fetchUsersClients(listOf(userId))
        }
    }

    private fun observeUserInfoAndUpdateViewState() {
        viewModelScope.launch {
            combine(
                observeUserInfo(userId),
                observeGroupInfo(),
                observeOneToOneConversation(userId),
                ::Triple
            )
                .flowOn(dispatchers.io())
                .collect { (userResult, groupInfo, oneToOneConversation) ->
                    when (userResult) {
                        is GetUserInfoResult.Failure -> {
                            appLogger.e("Couldn't not find the user with provided id: ${userId.toLogString()}")
                            updateUserInfoStateForError()
                        }

                        is GetUserInfoResult.Success -> {
                            val conversation = when (oneToOneConversation) {
                                GetOneToOneConversationUseCase.Result.Failure -> null
                                is GetOneToOneConversationUseCase.Result.Success -> oneToOneConversation.conversation
                            }

                            updateUserInfoState(userResult, groupInfo, conversation)
                        }
                    }
                }
        }
    }

    private suspend fun observeGroupInfo(): Flow<OtherUserProfileGroupState?> {
        return conversationId?.let {
            observeConversationRoleForUser(it, userId)
                .map { conversationRoleData ->
                    conversationRoleData.userRole?.let { userRole ->
                        OtherUserProfileGroupState(
                            groupName = conversationRoleData.conversationName,
                            role = userRole,
                            isSelfAdmin = conversationRoleData.selfRole is Conversation.Member.Role.Admin,
                            conversationId = conversationRoleData.conversationId
                        )
                    }
                }
        } ?: flowOf(null)
    }

    override fun onChangeMemberRole(role: Conversation.Member.Role) {
        viewModelScope.launch {
            if (conversationId != null) {
                updateMemberRole(conversationId, userId, role).also {
                    if (it is UpdateConversationMemberRoleResult.Failure) {
                        closeBottomSheetAndShowInfoMessage(ChangeGroupRoleError)
                    }
                }
            }
        }
    }

    private suspend fun closeBottomSheetAndShowInfoMessage(type: SnackBarMessage) {
        _closeBottomSheet.emit(Unit)
        _infoMessage.emit(type.uiText)
    }

    override fun onRemoveConversationMember(state: RemoveConversationMemberState) {
        viewModelScope.launch {
            requestInProgress = true
            val response = withContext(dispatchers.io()) {
                removeMemberFromConversation(
                    state.conversationId,
                    userId
                )
            }

            if (response is RemoveMemberFromConversationUseCase.Result.Failure) {
                closeBottomSheetAndShowInfoMessage(RemoveConversationMemberError)
            }

            requestInProgress = false
        }
    }

    override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                when (withContext(dispatchers.io()) { updateConversationMutedStatus(conversationId, status, Date().time) }) {
                    ConversationUpdateStatusResult.Failure -> closeBottomSheetAndShowInfoMessage(MutingOperationError)
                    ConversationUpdateStatusResult.Success -> {
                        state = state.updateMuteStatus(status)
                        appLogger.i("MutedStatus changed for conversation: $conversationId to $status")
                    }
                }
            }
        }
    }

    override fun onBlockUser(blockUserState: BlockUserDialogState) {
        viewModelScope.launch {
            requestInProgress = true
            _closeBottomSheet.emit(Unit)
            when (val result = withContext(dispatchers.io()) { blockUser(userId) }) {
                BlockUserResult.Success -> {
                    appLogger.i("User $userId was blocked")
                    closeBottomSheetAndShowInfoMessage(BlockingUserOperationSuccess(blockUserState.userName))
                }

                is BlockUserResult.Failure -> {
                    appLogger.e("Error while blocking user $userId ; Error ${result.coreFailure}")
                    closeBottomSheetAndShowInfoMessage(BlockingUserOperationError)
                }
            }
            requestInProgress = false
        }
    }

    override fun onUnblockUser(userId: UserId) {
        viewModelScope.launch {
            requestInProgress = true
            _closeBottomSheet.emit(Unit)
            when (val result = withContext(dispatchers.io()) { unblockUser(userId) }) {
                UnblockUserResult.Success -> {
                    appLogger.i("User $userId was unblocked")
                }

                is UnblockUserResult.Failure -> {
                    appLogger.e("Error while unblocking user $userId ; Error ${result.coreFailure}")
                    closeBottomSheetAndShowInfoMessage(UnblockingUserOperationError)
                }
            }
            requestInProgress = false
        }
    }

    @Suppress("EmptyFunctionBlock")
    override fun onMoveConversationToFolder(conversationId: ConversationId?) {
    }

    override fun onMoveConversationToArchive(dialogState: DialogState) {
        viewModelScope.launch {
            val shouldArchive = !dialogState.isArchived
            requestInProgress = true
            val result = withContext(dispatchers.io()) {
                updateConversationArchivedStatus(
                    conversationId = dialogState.conversationId,
                    shouldArchiveConversation = shouldArchive,
                    onlyLocally = !dialogState.isMember
                )
            }
            requestInProgress = false
            when (result) {
                ArchiveStatusUpdateResult.Failure -> {
                    closeBottomSheetAndShowInfoMessage(OtherUserProfileInfoMessageType.ArchiveConversationError(shouldArchive))
                }

                ArchiveStatusUpdateResult.Success -> {
                    closeBottomSheetAndShowInfoMessage(
                        OtherUserProfileInfoMessageType.ArchiveConversationSuccess(
                            shouldArchive
                        )
                    )
                }
            }
        }
    }

    override fun onClearConversationContent(dialogState: DialogState) {
        viewModelScope.launch {
            requestInProgress = true
            with(dialogState) {
                val result = withContext(dispatchers.io()) { clearConversationContentUseCase(conversationId) }
                requestInProgress = false
                clearContentSnackbarResult(result, conversationTypeDetail)
            }
        }
    }

    private suspend fun clearContentSnackbarResult(
        clearContentResult: ClearConversationContentUseCase.Result,
        conversationTypeDetail: ConversationTypeDetail
    ) {
        if (conversationTypeDetail is ConversationTypeDetail.Connection) {
            throw IllegalStateException("Unsupported conversation type to clear content, something went wrong?")
        }

        if (clearContentResult is ClearConversationContentUseCase.Result.Failure) {
            closeBottomSheetAndShowInfoMessage(OtherUserProfileInfoMessageType.ConversationContentDeleteFailure)
        } else {
            closeBottomSheetAndShowInfoMessage(OtherUserProfileInfoMessageType.ConversationContentDeleted)
        }
    }

    private fun updateUserInfoStateForError() {
        state = state.copy(
            isDataLoading = false,
            isAvatarLoading = false,
            errorLoadingUser = ErrorLoadingUser.USER_NOT_FOUND
        )
    }

    private fun updateUserInfoState(
        userResult: GetUserInfoResult.Success,
        groupInfo: OtherUserProfileGroupState?,
        conversation: Conversation?
    ) {
        val otherUser = userResult.otherUser
        val userAvatarAsset = otherUser.completePicture
            ?.let { pic -> ImageAsset.UserAvatarAsset(pic) }

        state = state.copy(
            isDataLoading = false,
            isAvatarLoading = false,
            userAvatarAsset = userAvatarAsset,
            fullName = otherUser.name.orEmpty(),
            userName = fromOtherUser(otherUser),
            teamName = userResult.team?.name.orEmpty(),
            email = otherUser.email.orEmpty(),
            phone = otherUser.phone.orEmpty(),
            connectionState = otherUser.connectionStatus,
            membership = userTypeMapper.toMembership(otherUser.userType),
            groupState = groupInfo,
            botService = otherUser.botService,
            blockingState = otherUser.BlockState,
            isProteusVerified = otherUser.isProteusVerified,
            isUnderLegalHold = otherUser.isUnderLegalHold,
            expiresAt = otherUser.expiresAt,
            accentId = otherUser.accentId,
            isDeletedUser = otherUser.deleted,
            conversationSheetContent = conversation?.let {
                ConversationSheetContent(
                    title = otherUser.name.orEmpty(),
                    conversationId = conversation.id,
                    mutingConversationState = conversation.mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Private(
                        avatarAsset = userAvatarAsset,
                        userId = userId,
                        blockingState = otherUser.BlockState,
                        isUserDeleted = otherUser.deleted
                    ),
                    isTeamConversation = conversation.isTeamGroup(),
                    selfRole = Conversation.Member.Role.Member,
                    isArchived = conversation.archived,
                    protocol = conversation.protocol,
                    mlsVerificationStatus = conversation.mlsVerificationStatus,
                    proteusVerificationStatus = conversation.proteusVerificationStatus,
                    isUnderLegalHold = conversation.legalHoldStatus.showLegalHoldIndicator(),
                    isFavorite = null,
                    isDeletingConversationLocallyRunning = false
                )
            }
        )
    }
}
