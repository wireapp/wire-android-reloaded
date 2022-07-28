package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.android.navigation.nav
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.BaseGroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class GroupConversationDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val updateConversationAccessRoleUseCase: UpdateConversationAccessRoleUseCase,
    private val dispatcher: DispatcherProvider
) : BaseGroupConversationParticipantsViewModel(savedStateHandle, navigationManager, observeConversationMembers) {

    override val maxNumberOfItems: Int get() = MAX_NUMBER_OF_PARTICIPANTS

    var groupOptionsState: GroupConversationOptionsState by mutableStateOf(GroupConversationOptionsState())

    init {
        observeConversationDetails()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            // TODO(QOL): refactor to one usecase that return group info and members
            observeConversationMembers(conversationId).map { it.isSelfAnAdmin }.distinctUntilChanged().also { isSelfAdminFlow ->
                observeConversationDetails(conversationId).combine(isSelfAdminFlow) { conversationDetails, isSelfAdmin ->
                    Pair(conversationDetails, isSelfAdmin)
                }.collect { (conversationDetails, isSelfAdmin) ->
                    with(conversationDetails) {
                        if (this is ConversationDetails.Group) {
                            updateState(
                                groupOptionsState.copy(
                                    groupName = conversation.name.orEmpty(),
                                    isTeamGroup = conversation.isTeamGroup(),
                                    isGuestAllowed = (conversation.isGuestAllowed() || conversation.isNonTeamMemberAllowed()),
                                    isServicesAllowed = conversation.isServicesAllowed(),
                                    isUpdatingGuestAllowed = isSelfAdmin /* TODO: check if self belongs to the same team */,
                                    isUpdatingAllowed = isSelfAdmin
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun onGuestUpdate(enableGuestAndNonTeamMember: Boolean) {
        when (enableGuestAndNonTeamMember) {
            true -> updateGuestStatus(enableGuestAndNonTeamMember)
            false -> showGuestConformationDialog()
        }
    }

    fun onServicesUpdate(enableServices: Boolean) {
        updateServicesStatus(enableServices)
    }

    fun onGuestDialogDismiss() {
        updateState(groupOptionsState.copy(isGuestUpdateDialogShown = false, isGuestAllowed = groupOptionsState.isGuestAllowed))
    }

    fun onGuestDialogConfirm() {
        updateGuestStatus(false)
        onGuestDialogDismiss()
    }

    private fun updateGuestStatus(enableGuestAndNonTeamMember: Boolean) {
        viewModelScope.launch {
            updateConversationAccess(enableGuestAndNonTeamMember, groupOptionsState.isServicesAllowed, conversationId).also {
                when (it) {
                    is UpdateConversationAccessRoleUseCase.Result.Failure -> updateGuestErrorState(it.cause)
                    UpdateConversationAccessRoleUseCase.Result.Success -> Unit
                }
            }
        }
    }

    private fun updateServicesStatus(enableServices: Boolean) {
        viewModelScope.launch {
            updateConversationAccess(
                enableGuestAndNonTeamMember = groupOptionsState.isGuestAllowed,
                enableServices = enableServices,
                conversationId = conversationId
            ).also {
                when (it) {
                    is UpdateConversationAccessRoleUseCase.Result.Failure -> updateServicesErrorState(it.cause)
                    UpdateConversationAccessRoleUseCase.Result.Success -> Unit
                }
            }
        }
    }

    private suspend fun updateConversationAccess(
        enableGuestAndNonTeamMember: Boolean,
        enableServices: Boolean,
        conversationId: ConversationId
    ) = updateConversationAccessRoleUseCase(
        allowGuest = enableGuestAndNonTeamMember,
        allowNonTeamMember = enableGuestAndNonTeamMember,
        allowServices = enableServices,
        conversationId = conversationId
    )


    private fun updateState(newState: GroupConversationOptionsState) {
        viewModelScope.launch(dispatcher.main()) {
            groupOptionsState = newState
        }
    }

    private fun showGuestConformationDialog() = updateState(groupOptionsState.copy(isGuestUpdateDialogShown = true))

    private fun updateGuestErrorState(coreFailure: CoreFailure) =
        updateState(groupOptionsState.copy(error = GroupConversationOptionsState.Error.UpdateGuestError(coreFailure)))

    private fun updateServicesErrorState(coreFailure: CoreFailure) =
        updateState(groupOptionsState.copy(error = GroupConversationOptionsState.Error.UpdateServicesError(coreFailure)))

    fun navigateToFullParticipantsList() = viewModelScope.launch {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = VoyagerNavigationItem.GroupConversationAllParticipants(conversationId.nav())
            )
        )
    }

    fun navigateToAddParticants() = viewModelScope.launch {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = VoyagerNavigationItem.AddConversationParticipants(conversationId.nav())
            )
        )
    }

    companion object {
        const val MAX_NUMBER_OF_PARTICIPANTS = 4
    }
}
