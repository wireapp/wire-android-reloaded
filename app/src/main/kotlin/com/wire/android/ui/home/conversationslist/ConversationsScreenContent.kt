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

package com.wire.android.ui.home.conversationslist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.feature.analytics.AnonymousAnalyticsManagerImpl
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.calling.ongoing.getOngoingCallIntent
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionNavigation
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.rememberConversationSheetState
import com.wire.android.ui.common.bottomsheet.folder.ChangeConversationFavoriteStateArgs
import com.wire.android.ui.common.bottomsheet.folder.ChangeConversationFavoriteVM
import com.wire.android.ui.common.bottomsheet.folder.ChangeConversationFavoriteVMImpl
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.dialogs.ArchiveConversationDialog
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.calling.JoinAnywayDialog
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.NewConversationSearchPeopleScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupLocallyDialog
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversationslist.common.ConversationList
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.search.SearchConversationsEmptyContent
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId

/**
 * This is a base for creating screens for displaying list of conversations.
 * Can be used to create proper navigation destination for different sources of conversations, like archive.
 */
@Suppress("ComplexMethod", "NestedBlockDepth", "Wrapping")
@Composable
fun ConversationsScreenContent(
    navigator: Navigator,
    searchBarState: SearchBarState,
    emptyListContent: @Composable (domain: String) -> Unit = {},
    lazyListState: LazyListState = rememberLazyListState(),
    loadingListContent: @Composable (LazyListState) -> Unit = { ConversationListLoadingContent(it) },
    conversationsSource: ConversationsSource = ConversationsSource.MAIN,
    conversationListViewModel: ConversationListViewModel = when {
        LocalInspectionMode.current -> ConversationListViewModelPreview()
        else -> hiltViewModel<ConversationListViewModelImpl, ConversationListViewModelImpl.Factory>(
            key = "list_$conversationsSource",
            creationCallback = { factory ->
                factory.create(conversationsSource = conversationsSource)
            }
        )
    },
    conversationCallListViewModel: ConversationCallListViewModel = when {
        LocalInspectionMode.current -> ConversationCallListViewModelPreview
        else -> hiltViewModel<ConversationCallListViewModelImpl>(key = "call_$conversationsSource")
    },
    changeConversationFavoriteStateViewModel: ChangeConversationFavoriteVM =
        hiltViewModelScoped<ChangeConversationFavoriteVMImpl, ChangeConversationFavoriteVM, ChangeConversationFavoriteStateArgs>(
            ChangeConversationFavoriteStateArgs
        ),
) {
    var currentConversationOptionNavigation by remember {
        mutableStateOf<ConversationOptionNavigation>(ConversationOptionNavigation.Home)
    }

    val sheetState = rememberWireModalSheetState<ConversationItem>()
    val conversationsDialogsState = rememberConversationsDialogsState(conversationListViewModel.requestInProgress)
    val permissionPermanentlyDeniedDialogState = rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        conversationListViewModel.infoMessage.collect {
            sheetState.hide()
        }
    }

    LaunchedEffect(Unit) {
        conversationListViewModel.closeBottomSheet.collect {
            sheetState.hide()
        }
    }

    LaunchedEffect(searchBarState.isSearchActive) {
        if (searchBarState.isSearchActive) {
            conversationListViewModel.refreshMissingMetadata()
        }
    }

    LaunchedEffect(searchBarState.searchQueryTextState.text) {
        conversationListViewModel.searchQueryChanged(searchBarState.searchQueryTextState.text.toString())
    }

    fun showConfirmationDialogOrUnarchive(): (DialogState) -> Unit {
        return { dialogState ->
            if (dialogState.isArchived) {
                conversationListViewModel.moveConversationToArchive(dialogState)
            } else {
                conversationsDialogsState.archiveConversationDialogState.show(dialogState)
            }
        }
    }

    with(conversationsDialogsState) {
        val onEditConversationItem: (ConversationItem) -> Unit = remember {
            {
                sheetState.show(it)
                currentConversationOptionNavigation = ConversationOptionNavigation.Home
            }
        }

        val onOpenConversation: (ConversationItem) -> Unit = remember(navigator) {
            {
                navigator.navigate(NavigationCommand(ConversationScreenDestination(it.conversationId)))
            }
        }
        val onOpenUserProfile: (UserId) -> Unit = remember(navigator) {
            {
                navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(it)))
            }
        }
        val onJoinedCall: (ConversationId) -> Unit = remember(navigator) {
            {
                AnonymousAnalyticsManagerImpl.sendEvent(event = AnalyticsEvent.CallJoined)
                getOngoingCallIntent(context, it.toString()).run {
                    context.startActivity(this)
                }
            }
        }
        val onJoinCall: (ConversationId) -> Unit = remember {
            {
                conversationCallListViewModel.joinOngoingCall(it, onJoinedCall)
            }
        }
        val onNewConversationClicked: () -> Unit = remember {
            {
                navigator.navigate(NavigationCommand(NewConversationSearchPeopleScreenDestination))
            }
        }

        val onPlayPauseCurrentAudio: () -> Unit = remember {
            {
                conversationListViewModel.playPauseCurrentAudio()
            }
        }

        val onStopCurrentAudio: () -> Unit = remember {
            {
                conversationListViewModel.stopCurrentAudio()
            }
        }

        when (val state = conversationListViewModel.conversationListState) {
            is ConversationListState.Paginated -> {
                val lazyPagingItems = state.conversations.collectAsLazyPagingItems()
                val showLoading = lazyPagingItems.loadState.refresh == LoadState.Loading && lazyPagingItems.itemCount == 0

                when {
                    // when conversation list is not yet fetched, show loading indicator
                    showLoading -> loadingListContent(lazyListState)
                    // when there is at least one conversation
                    lazyPagingItems.itemCount > 0 -> ConversationList(
                        lazyPagingConversations = lazyPagingItems,
                        lazyListState = lazyListState,
                        onOpenConversation = onOpenConversation,
                        onEditConversation = onEditConversationItem,
                        onOpenUserProfile = onOpenUserProfile,
                        onJoinCall = onJoinCall,
                        onAudioPermissionPermanentlyDenied = {
                            permissionPermanentlyDeniedDialogState.show(
                                PermissionPermanentlyDeniedDialogState.Visible(
                                    R.string.app_permission_dialog_title,
                                    R.string.call_permission_dialog_description
                                )
                            )
                        },
                        onPlayPauseCurrentAudio = onPlayPauseCurrentAudio,
                        onStopCurrentAudio = onStopCurrentAudio

                    )
                    // when there is no conversation in any folder
                    searchBarState.isSearchActive -> SearchConversationsEmptyContent(onNewConversationClicked = onNewConversationClicked)
                    else -> emptyListContent(state.domain)
                }
            }

            is ConversationListState.NotPaginated -> {
                when {
                    // when conversation list is not yet fetched, show loading indicator
                    state.isLoading -> loadingListContent(lazyListState)
                    // when there is at least one conversation in any folder
                    state.conversations.isNotEmpty() && state.conversations.any { it.value.isNotEmpty() } -> ConversationList(
                        lazyListState = lazyListState,
                        conversationListItems = state.conversations,
                        onOpenConversation = onOpenConversation,
                        onEditConversation = onEditConversationItem,
                        onOpenUserProfile = onOpenUserProfile,
                        onJoinCall = onJoinCall,
                        onAudioPermissionPermanentlyDenied = {
                            permissionPermanentlyDeniedDialogState.show(
                                PermissionPermanentlyDeniedDialogState.Visible(
                                    R.string.app_permission_dialog_title,
                                    R.string.call_permission_dialog_description
                                )
                            )
                        },
                        onPlayPauseCurrentAudio = onPlayPauseCurrentAudio,
                        onStopCurrentAudio = onStopCurrentAudio
                    )
                    // when there is no conversation in any folder
                    searchBarState.isSearchActive -> SearchConversationsEmptyContent(onNewConversationClicked = onNewConversationClicked)
                    else -> emptyListContent(state.domain)
                }
            }
        }

        VisibilityState(conversationCallListViewModel.joinCallDialogState) { callConversationId ->
            appLogger.i("$TAG showing showJoinAnywayDialog..")
            JoinAnywayDialog(
                onDismiss = conversationCallListViewModel.joinCallDialogState::dismiss,
                onConfirm = { conversationCallListViewModel.joinAnyway(callConversationId, onJoinedCall) }
            )
        }

        PermissionPermanentlyDeniedDialog(
            dialogState = permissionPermanentlyDeniedDialogState,
            hideDialog = permissionPermanentlyDeniedDialogState::dismiss
        )

        BlockUserDialogContent(
            isLoading = requestInProgress,
            dialogState = blockUserDialogState,
            onBlock = conversationListViewModel::blockUser
        )

        DeleteConversationGroupDialog(
            isLoading = requestInProgress,
            dialogState = deleteGroupDialogState,
            onDeleteGroup = conversationListViewModel::deleteGroup
        )

        DeleteConversationGroupLocallyDialog(
            isLoading = requestInProgress,
            dialogState = deleteGroupLocallyDialogState,
            onDeleteGroupLocally = { state ->
                conversationListViewModel.deleteGroupLocally(state)
                deleteGroupLocallyDialogState.dismiss()
            }
        )

        LeaveConversationGroupDialog(
            dialogState = leaveGroupDialogState,
            isLoading = requestInProgress,
            onLeaveGroup = conversationListViewModel::leaveGroup
        )

        UnblockUserDialogContent(
            dialogState = unblockUserDialogState,
            onUnblock = conversationListViewModel::unblockUser,
            isLoading = requestInProgress,
        )

        ClearConversationContentDialog(
            dialogState = clearContentDialogState,
            isLoading = requestInProgress,
            onClearConversationContent = conversationListViewModel::clearConversationContent
        )

        ArchiveConversationDialog(
            dialogState = archiveConversationDialogState,
            onArchiveButtonClicked = conversationListViewModel::moveConversationToArchive
        )

        WireModalSheetLayout(
            sheetState = sheetState,
            sheetContent = {
                val conversationDeletionInProgress by conversationListViewModel.observeIsDeletingConversationLocally(it.conversationId)
                    .collectAsStateWithLifecycle(false)
                val conversationState = rememberConversationSheetState(
                    conversationItem = it,
                    conversationOptionNavigation = currentConversationOptionNavigation,
                    isConversationDeletionLocallyRunning = conversationDeletionInProgress
                )

                ConversationSheetContent(
                    conversationSheetState = conversationState,
                    onMutingConversationStatusChange = {
                        conversationListViewModel.muteConversation(
                            conversationId = conversationState.conversationId,
                            mutedConversationStatus = conversationState.conversationSheetContent!!.mutingConversationState
                        )
                    },
                    changeFavoriteState = changeConversationFavoriteStateViewModel::changeFavoriteState,
                    moveConversationToFolder = { navArgs ->
                        navigator.navigate(NavigationCommand(ConversationFoldersScreenDestination(navArgs)))
                    },
                    updateConversationArchiveStatus = showConfirmationDialogOrUnarchive(),
                    clearConversationContent = clearContentDialogState::show,
                    blockUser = blockUserDialogState::show,
                    unblockUser = unblockUserDialogState::show,
                    leaveGroup = leaveGroupDialogState::show,
                    deleteGroup = deleteGroupDialogState::show,
                    deleteGroupLocally = deleteGroupLocallyDialogState::show
                )
            },
        )
    }

    SnackBarMessageHandler(infoMessages = conversationListViewModel.infoMessage)
    SnackBarMessageHandler(infoMessages = changeConversationFavoriteStateViewModel.infoMessage, onEmitted = {
        sheetState.hide()
    })
}

private const val TAG = "BaseConversationsScreen"

@PreviewMultipleThemes
@Composable
fun PreviewConversationsScreenContent() = WireTheme {
    ConversationsScreenContent(navigator = rememberNavigator { }, searchBarState = rememberSearchbarState())
}
