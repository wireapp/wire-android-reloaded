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

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.di.hiltViewModelScoped
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MLSVerifiedIcon
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.ProteusVerifiedIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.common.bottomsheet.conversation.rememberConversationSheetState
import com.wire.android.ui.common.bottomsheet.folder.ChangeConversationFavoriteStateArgs
import com.wire.android.ui.common.bottomsheet.folder.ChangeConversationFavoriteVM
import com.wire.android.ui.common.bottomsheet.folder.ChangeConversationFavoriteVMImpl
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.ArchiveConversationDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.AddMembersSearchScreenDestination
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination
import com.wire.android.ui.destinations.ConversationMediaScreenDestination
import com.wire.android.ui.destinations.EditConversationNameScreenDestination
import com.wire.android.ui.destinations.EditGuestAccessScreenDestination
import com.wire.android.ui.destinations.EditSelfDeletingMessagesScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SearchConversationMessagesScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.destinations.ServiceDetailsScreenDestination
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.details.editguestaccess.EditGuestAccessParams
import com.wire.android.ui.home.conversations.details.menu.DeleteConversationGroupDialog
import com.wire.android.ui.home.conversations.details.menu.GroupConversationDetailsBottomSheetEventsHandler
import com.wire.android.ui.home.conversations.details.menu.LeaveConversationGroupDialog
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptions
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipants
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsState
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavBackArgs
import com.wire.android.ui.home.conversations.folder.RemoveConversationFromFolderVM
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.model.LeaveGroupDialogState
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectConversationDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.mls.CipherSuite
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@RootNavGraph
@WireDestination(
    navArgsDelegate = GroupConversationDetailsNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun GroupConversationDetailsScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<GroupConversationDetailsNavBackArgs>,
    groupConversationDetailResultRecipient: ResultRecipient<EditConversationNameScreenDestination, Boolean>,
    conversationFoldersScreenResultRecipient:
    ResultRecipient<ConversationFoldersScreenDestination, ConversationFoldersNavBackArgs>,
    viewModel: GroupConversationDetailsViewModel = hiltViewModel(),
    removeConversationFromFolderVM: RemoveConversationFromFolderVM = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val snackbarHostState = LocalSnackbarHostState.current
    val showSnackbarMessage: (UIText) -> Unit = remember { { scope.launch { snackbarHostState.showSnackbar(it.asString(resources)) } } }

    val onSearchConversationMessagesClick: () -> Unit = {
        navigator.navigate(
            NavigationCommand(
                SearchConversationMessagesScreenDestination(
                    conversationId = viewModel.conversationId
                )
            )
        )
    }

    val onConversationMediaClick: () -> Unit = {
        navigator.navigate(
            NavigationCommand(
                ConversationMediaScreenDestination(
                    conversationId = viewModel.conversationId
                )
            )
        )
    }

    GroupConversationDetailsContent(
        conversationSheetContent = viewModel.conversationSheetContent,
        bottomSheetEventsHandler = viewModel as GroupConversationDetailsBottomSheetEventsHandler,
        onBackPressed = navigator::navigateBack,
        onProfilePressed = { participant ->
            when {
                participant.isSelf -> navigator.navigate(NavigationCommand(SelfUserProfileScreenDestination))
                participant.isService && participant.botService != null ->
                    navigator.navigate(NavigationCommand(ServiceDetailsScreenDestination(participant.botService, viewModel.conversationId)))

                else -> navigator.navigate(NavigationCommand(OtherUserProfileScreenDestination(participant.id, viewModel.conversationId)))
            }
        },
        onAddParticipantsPressed = {
            navigator.navigate(
                NavigationCommand(
                    AddMembersSearchScreenDestination(
                        viewModel.conversationId,
                        viewModel.groupOptionsState.value.isServicesAllowed
                    )
                )
            )
        },
        groupParticipantsState = viewModel.groupParticipantsState,
        onLeaveGroup = {
            viewModel.leaveGroup(
                it,
                onSuccess = {
                    resultNavigator.setResult(
                        GroupConversationDetailsNavBackArgs(
                            groupConversationActionType = GroupConversationActionType.LEAVE_GROUP,
                            hasLeftGroup = true,
                            conversationName = it.conversationName
                        )
                    )
                    resultNavigator.navigateBack()
                },
                onFailure = showSnackbarMessage
            )
        },
        onDeleteGroup = {
            viewModel.deleteGroup(
                it,
                onSuccess = {
                    resultNavigator.setResult(
                        GroupConversationDetailsNavBackArgs(
                            groupConversationActionType = GroupConversationActionType.DELETE_GROUP,
                            isGroupDeleted = true,
                            conversationName = it.conversationName
                        )
                    )
                    resultNavigator.navigateBack()
                },
                onFailure = showSnackbarMessage
            )
        },
        onEditGuestAccess = {
            navigator.navigate(
                NavigationCommand(
                    EditGuestAccessScreenDestination(
                        viewModel.conversationId,
                        EditGuestAccessParams(
                            viewModel.groupOptionsState.value.isGuestAllowed,
                            viewModel.groupOptionsState.value.isServicesAllowed,
                            viewModel.groupOptionsState.value.isUpdatingGuestAllowed
                        )
                    )
                )
            )
        },
        onEditSelfDeletingMessages = {
            navigator.navigate(NavigationCommand(EditSelfDeletingMessagesScreenDestination(viewModel.conversationId)))
        },
        onEditGroupName = {
            navigator.navigate(NavigationCommand(EditConversationNameScreenDestination(viewModel.conversationId)))
        },
        isLoading = viewModel.requestInProgress,
        onSearchConversationMessagesClick = onSearchConversationMessagesClick,
        onConversationMediaClick = onConversationMediaClick,
        isAbandonedOneOnOneConversation = viewModel.conversationSheetContent?.isAbandonedOneOnOneConversation(
            viewModel.groupParticipantsState.data.allCount
        ) ?: false,
        onMoveToFolder = {
            navigator.navigate(NavigationCommand(ConversationFoldersScreenDestination(it)))
        },
        removeFromFolder = removeConversationFromFolderVM::removeFromFolder
    )

    val tryAgainSnackBarMessage = stringResource(id = R.string.error_unknown_message)
    val successSnackBarMessage = stringResource(id = R.string.conversation_options_renamed)

    groupConversationDetailResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args from editGroupName in GroupConversationDetailsScreen")
            }

            is NavResult.Value -> {
                scope.launch {
                    if (result.value) {
                        snackbarHostState.showSnackbar(successSnackBarMessage)
                    } else {
                        snackbarHostState.showSnackbar(tryAgainSnackBarMessage)
                    }
                }
            }
        }
    }

    conversationFoldersScreenResultRecipient.onNavResult { result ->
        when (result) {
            NavResult.Canceled -> {}
            is NavResult.Value -> {
                scope.launch {
                    snackbarHostState.showSnackbar(result.value.message)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupConversationDetailsContent(
    conversationSheetContent: ConversationSheetContent?,
    bottomSheetEventsHandler: GroupConversationDetailsBottomSheetEventsHandler,
    onBackPressed: () -> Unit,
    onProfilePressed: (UIParticipant) -> Unit,
    onAddParticipantsPressed: () -> Unit,
    onEditGuestAccess: () -> Unit,
    onEditSelfDeletingMessages: () -> Unit,
    onEditGroupName: () -> Unit,
    onLeaveGroup: (LeaveGroupDialogState) -> Unit,
    onDeleteGroup: (GroupDialogState) -> Unit,
    groupParticipantsState: GroupConversationParticipantsState,
    isLoading: Boolean,
    isAbandonedOneOnOneConversation: Boolean,
    onSearchConversationMessagesClick: () -> Unit,
    onConversationMediaClick: () -> Unit,
    removeFromFolder: (conversationId: ConversationId, conversationName: String, folder: ConversationFolder) -> Unit,
    onMoveToFolder: (ConversationFoldersNavArgs) -> Unit = {},
    initialPageIndex: GroupConversationDetailsTabItem = GroupConversationDetailsTabItem.OPTIONS,
    changeConversationFavoriteStateViewModel: ChangeConversationFavoriteVM =
        hiltViewModelScoped<ChangeConversationFavoriteVMImpl, ChangeConversationFavoriteVM, ChangeConversationFavoriteStateArgs>(
            ChangeConversationFavoriteStateArgs
        ),
) {
    val scope = rememberCoroutineScope()
    val resources = LocalContext.current.resources
    val snackbarHostState = LocalSnackbarHostState.current
    val lazyListStates: List<LazyListState> = GroupConversationDetailsTabItem.entries.map { rememberLazyListState() }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.ordinal,
        pageCount = { GroupConversationDetailsTabItem.entries.size }
    )
    val currentTabState by remember { derivedStateOf { pagerState.calculateCurrentTab() } }

    val conversationSheetState = rememberConversationSheetState(conversationSheetContent)

    val sheetState = rememberWireModalSheetState<Unit>()
    val closeBottomSheetAndShowSnackbarMessage: (UIText) -> Unit = remember {
        {
            sheetState.hide {
                snackbarHostState.showSnackbar(it.asString(resources))
            }
        }
    }
    val getBottomSheetVisibility: () -> Boolean = remember(sheetState) { { sheetState.isVisible } }

    val deleteGroupDialogState = rememberVisibilityState<GroupDialogState>()
    val leaveGroupDialogState = rememberVisibilityState<LeaveGroupDialogState>()
    val clearConversationDialogState = rememberVisibilityState<DialogState>()
    val archiveConversationDialogState = rememberVisibilityState<DialogState>()
    val legalHoldSubjectDialogState = rememberVisibilityState<Unit>()

    LaunchedEffect(conversationSheetState.conversationSheetContent) {
        // on each closing BottomSheet we revert BSContent to Home.
        // So in case if user opened BS, went to MuteStatus BS and closed it by clicking outside of BS,
        // then opens BS again - Home BS suppose to be opened, not MuteStatus BS
        snapshotFlow { sheetState.isVisible }.collect { isVisible ->
            if (!isVisible) conversationSheetState.toHome()
        }
    }

    if (!isLoading) {
        deleteGroupDialogState.dismiss()
        leaveGroupDialogState.dismiss()
        clearConversationDialogState.dismiss()
        archiveConversationDialogState.dismiss()
        legalHoldSubjectDialogState.dismiss()
    }
    CollapsingTopBarScaffold(
        topBarHeader = {
            WireCenterAlignedTopAppBar(
                elevation = dimensions().spacing0x, // CollapsingTopBarScaffold already manages elevation
                titleContent = {
                    WireTopAppBarTitle(
                        title = stringResource(R.string.conversation_details_title),
                        style = MaterialTheme.wireTypography.title01,
                        maxLines = 2
                    )
                    VerificationInfo(conversationSheetContent)
                },
                navigationIconType = NavigationIconType.Close(R.string.content_description_conversation_details_close_btn),
                onNavigationPressed = onBackPressed,
                actions = {
                    MoreOptionIcon(
                        contentDescription = R.string.content_description_conversation_details_more_btn,
                        onButtonClicked = sheetState::show
                    )
                }
            )
        },
        topBarCollapsing = {
            conversationSheetState.conversationSheetContent?.let {
                GroupConversationDetailsTopBarCollapsing(
                    title = it.title,
                    conversationId = it.conversationId,
                    totalParticipants = groupParticipantsState.data.allCount,
                    isLoading = isLoading,
                    onSearchConversationMessagesClick = onSearchConversationMessagesClick,
                    onConversationMediaClick = onConversationMediaClick,
                    isUnderLegalHold = it.isUnderLegalHold,
                    onLegalHoldLearnMoreClick = remember { { legalHoldSubjectDialogState.show(Unit) } },
                    modifier = Modifier.padding(bottom = MaterialTheme.wireDimensions.spacing16x)
                )
            }
        },
        topBarFooter = {
            WireTabRow(
                tabs = GroupConversationDetailsTabItem.entries,
                selectedTabIndex = currentTabState,
                onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
        },
        bottomBar = {
            AnimatedContent(
                targetState = currentTabState,
                label = "Conversation details bottom bar crossfade",
                transitionSpec = {
                    val enter = slideInVertically(initialOffsetY = { it })
                    val exit = slideOutVertically(targetOffsetY = { it })
                    enter.togetherWith(exit)
                },
                modifier = Modifier.fillMaxWidth()
            ) { currentTabState ->
                Surface(
                    shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
                    color = MaterialTheme.wireColorScheme.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    when (GroupConversationDetailsTabItem.entries[currentTabState]) {
                        GroupConversationDetailsTabItem.OPTIONS -> {
                            // no bottom bar for options tab
                        }

                        GroupConversationDetailsTabItem.PARTICIPANTS -> {
                            if (groupParticipantsState.addParticipantsEnabled && !isAbandonedOneOnOneConversation) {
                                Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
                                    WirePrimaryButton(
                                        text = stringResource(R.string.conversation_details_group_participants_add),
                                        onClick = onAddParticipantsPressed,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        contentLazyListState = lazyListStates[currentTabState],
    ) {
        var focusedTabIndex: Int by remember { mutableStateOf(initialPageIndex.ordinal) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
            ) { pageIndex ->
                when (GroupConversationDetailsTabItem.entries[pageIndex]) {
                    GroupConversationDetailsTabItem.OPTIONS -> GroupConversationOptions(
                        lazyListState = lazyListStates[pageIndex],
                        onEditGuestAccess = onEditGuestAccess,
                        onEditSelfDeletingMessages = onEditSelfDeletingMessages,
                        onEditGroupName = onEditGroupName
                    )

                    GroupConversationDetailsTabItem.PARTICIPANTS -> GroupConversationParticipants(
                        groupParticipantsState = groupParticipantsState,
                        onProfilePressed = onProfilePressed,
                        lazyListState = lazyListStates[pageIndex]
                    )
                }
            }

            LaunchedEffect(pagerState.isScrollInProgress, focusedTabIndex, pagerState.currentPage) {
                if (!pagerState.isScrollInProgress && focusedTabIndex != pagerState.currentPage) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    focusedTabIndex = pagerState.currentPage
                }
            }
        }
    }

    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            ConversationSheetContent(
                isBottomSheetVisible = getBottomSheetVisibility,
                conversationSheetState = conversationSheetState,
                onMutingConversationStatusChange = {
                    conversationSheetContent?.let {
                        bottomSheetEventsHandler.onMutingConversationStatusChange(
                            conversationSheetState.conversationId,
                            conversationSheetState.conversationSheetContent!!.mutingConversationState,
                            closeBottomSheetAndShowSnackbarMessage
                        )
                    }
                },
                changeFavoriteState = changeConversationFavoriteStateViewModel::changeFavoriteState,
                moveConversationToFolder = onMoveToFolder,
                removeFromFolder = removeFromFolder,
                updateConversationArchiveStatus = {
                    // Only show the confirmation dialog if the conversation is not archived
                    if (!it.isArchived) {
                        archiveConversationDialogState.show(it)
                    } else {
                        bottomSheetEventsHandler.updateConversationArchiveStatus(
                            dialogState = it,
                            onMessage = closeBottomSheetAndShowSnackbarMessage
                        )
                    }
                },
                clearConversationContent = clearConversationDialogState::show,
                blockUser = {},
                unblockUser = {},
                leaveGroup = leaveGroupDialogState::show,
                deleteGroup = deleteGroupDialogState::show,
                deleteGroupLocally = {}
            )
        }
    )

    DeleteConversationGroupDialog(
        isLoading = isLoading,
        dialogState = deleteGroupDialogState,
        onDeleteGroup = onDeleteGroup
    )

    LeaveConversationGroupDialog(
        dialogState = leaveGroupDialogState,
        isLoading = isLoading,
        onLeaveGroup = onLeaveGroup
    )

    ClearConversationContentDialog(
        dialogState = clearConversationDialogState,
        isLoading = isLoading,
        onClearConversationContent = {
            bottomSheetEventsHandler.onClearConversationContent(dialogState = it, onMessage = closeBottomSheetAndShowSnackbarMessage)
        }
    )

    ArchiveConversationDialog(
        dialogState = archiveConversationDialogState,
        onArchiveButtonClicked = {
            bottomSheetEventsHandler.updateConversationArchiveStatus(dialogState = it, onMessage = closeBottomSheetAndShowSnackbarMessage)
        }
    )

    VisibilityState(legalHoldSubjectDialogState) {
        LegalHoldSubjectConversationDialog(legalHoldSubjectDialogState::dismiss)
    }
}

@Composable
private fun VerificationInfo(conversationSheetContent: ConversationSheetContent?) {
    if (conversationSheetContent == null) return

    val isProteusVerified = conversationSheetContent.proteusVerificationStatus == Conversation.VerificationStatus.VERIFIED
    val isMlsVerified = conversationSheetContent.mlsVerificationStatus == Conversation.VerificationStatus.VERIFIED
    val isProteusProtocol = conversationSheetContent.protocol == Conversation.ProtocolInfo.Proteus

    if (isProteusVerified && (isProteusProtocol || !isMlsVerified)) {
        ProteusVerifiedLabel()
    } else if (isMlsVerified) {
        MLSVerifiedLabel()
    }
}

@Composable
private fun MLSVerifiedLabel() {
    VerifiedLabel(
        stringResource(id = R.string.label_conversations_details_verified_mls).uppercase(),
        MaterialTheme.wireColorScheme.positive
    ) { MLSVerifiedIcon() }
}

@Composable
private fun ProteusVerifiedLabel() {
    VerifiedLabel(
        stringResource(id = R.string.label_conversations_details_verified_proteus).uppercase(),
        MaterialTheme.wireColorScheme.primary
    ) { ProteusVerifiedIcon() }
}

@Composable
private fun VerifiedLabel(text: String, color: Color, icon: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .padding(top = dimensions().spacing4x)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.padding(
                start = dimensions().spacing6x,
                end = dimensions().spacing6x
            ),
            text = text,
            style = MaterialTheme.wireTypography.label01,
            color = color,
            overflow = TextOverflow.Ellipsis
        )
        icon()
    }
}

enum class GroupConversationDetailsTabItem(@StringRes val titleResId: Int) : TabItem {
    OPTIONS(R.string.conversation_details_options_tab),
    PARTICIPANTS(R.string.conversation_details_participants_tab);

    override val title: UIText = UIText.StringResource(titleResId)
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationDetails() {
    WireTheme {
        GroupConversationDetailsContent(
            conversationSheetContent = ConversationSheetContent(
                title = "title",
                conversationId = ConversationId("value", "domain"),
                mutingConversationState = MutedConversationStatus.AllAllowed,
                conversationTypeDetail = ConversationTypeDetail.Group(ConversationId("value", "domain"), false),
                selfRole = null,
                isTeamConversation = true,
                isArchived = false,
                protocol = Conversation.ProtocolInfo.MLS(
                    groupId = GroupID("groupId"),
                    groupState = Conversation.ProtocolInfo.MLSCapable.GroupState.ESTABLISHED,
                    epoch = ULong.MIN_VALUE,
                    keyingMaterialLastUpdate = Instant.fromEpochMilliseconds(1648654560000),
                    cipherSuite = CipherSuite.MLS_128_DHKEMX25519_AES128GCM_SHA256_Ed25519
                ),
                mlsVerificationStatus = Conversation.VerificationStatus.VERIFIED,
                isUnderLegalHold = false,
                proteusVerificationStatus = Conversation.VerificationStatus.VERIFIED,
                isFavorite = false,
                folder = null,
                isDeletingConversationLocallyRunning = false
            ),
            bottomSheetEventsHandler = GroupConversationDetailsBottomSheetEventsHandler.PREVIEW,
            onBackPressed = {},
            onProfilePressed = {},
            onAddParticipantsPressed = {},
            onLeaveGroup = {},
            onDeleteGroup = {},
            groupParticipantsState = GroupConversationParticipantsState.PREVIEW,
            isLoading = false,
            onEditGroupName = {},
            onEditSelfDeletingMessages = {},
            onEditGuestAccess = {},
            onSearchConversationMessagesClick = {},
            onConversationMediaClick = {},
            isAbandonedOneOnOneConversation = false,
            initialPageIndex = GroupConversationDetailsTabItem.PARTICIPANTS,
            removeFromFolder = { _, _, _ -> }
        )
    }
}
