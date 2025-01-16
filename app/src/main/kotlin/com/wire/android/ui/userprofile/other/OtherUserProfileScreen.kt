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

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.style.PopUpNavigationAnimation
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.common.bottomsheet.folder.ChangeConversationFavoriteViewModel
import com.wire.android.ui.common.bottomsheet.folder.ChangeConversationFavoriteViewModelImpl
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.ArchiveConversationDialog
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.dialogs.UserNotFoundDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.connection.ConnectionActionButton
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination
import com.wire.android.ui.destinations.ConversationMediaScreenDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.DeviceDetailsScreenDestination
import com.wire.android.ui.destinations.SearchConversationMessagesScreenDestination
import com.wire.android.ui.home.conversations.details.SearchAndMediaRow
import com.wire.android.ui.home.conversations.details.dialog.ClearConversationContentDialog
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavBackArgs
import com.wire.android.ui.home.conversations.folder.RemoveConversationFromFolderViewModel
import com.wire.android.ui.home.conversations.folder.RemoveConversationFromFolderViewModelImpl
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.legalhold.banner.LegalHoldSubjectBanner
import com.wire.android.ui.legalhold.dialog.subject.LegalHoldSubjectProfileDialog
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.ui.userprofile.other.bottomsheet.OtherUserBottomSheetState
import com.wire.android.ui.userprofile.other.bottomsheet.OtherUserProfileBottomSheetContent
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.SnackBarMessageHandler
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.ConnectionState
import io.github.esentsov.PackagePrivate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@RootNavGraph
@WireDestination(
    navArgsDelegate = OtherUserProfileNavArgs::class,
    style = PopUpNavigationAnimation::class,
)
@Composable
fun OtherUserProfileScreen(
    navigator: Navigator,
    navArgs: OtherUserProfileNavArgs,
    resultNavigator: ResultBackNavigator<String>,
    conversationFoldersScreenResultRecipient:
    ResultRecipient<ConversationFoldersScreenDestination, ConversationFoldersNavBackArgs>,
    viewModel: OtherUserProfileScreenViewModel = hiltViewModel(),
    changeConversationFavoriteViewModel: ChangeConversationFavoriteViewModel =
        hiltViewModel<ChangeConversationFavoriteViewModelImpl>(),
    removeConversationFromFolderViewModel: RemoveConversationFromFolderViewModel =
        hiltViewModel<RemoveConversationFromFolderViewModelImpl>(),
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val sheetState = rememberWireModalSheetState<Unit>()

    val conversationId = viewModel.state.conversationId
        ?: viewModel.state.conversationSheetContent?.conversationId
    val onSearchConversationMessagesClick: () -> Unit = {
        conversationId?.let {
            navigator.navigate(
                NavigationCommand(
                    SearchConversationMessagesScreenDestination(
                        conversationId = it
                    )
                )
            )
        }
    }

    val onConversationMediaClick: () -> Unit = {
        conversationId?.let {
            navigator.navigate(
                NavigationCommand(
                    ConversationMediaScreenDestination(
                        conversationId = it
                    )
                )
            )
        }
    }

    val legalHoldSubjectDialogState = rememberVisibilityState<Unit>()

    SnackBarMessageHandler(removeConversationFromFolderViewModel.infoMessage, onEmitted = sheetState::hide)
    SnackBarMessageHandler(changeConversationFavoriteViewModel.infoMessage, onEmitted = sheetState::hide)

    OtherProfileScreenContent(
        scope = scope,
        state = viewModel.state,
        requestInProgress = viewModel.requestInProgress,
        sheetState = sheetState,
        openBottomSheet = sheetState::show,
        closeBottomSheet = sheetState::hide,
        eventsHandler = viewModel as OtherUserProfileEventsHandler,
        bottomSheetEventsHandler = viewModel as OtherUserProfileBottomSheetEventsHandler,
        onIgnoreConnectionRequest = {
            resultNavigator.setResult(it)
            resultNavigator.navigateBack()
        },
        onOpenConversation = {
            navigator.navigate(
                NavigationCommand(
                    ConversationScreenDestination(it),
                    BackStackMode.UPDATE_EXISTED
                )
            )
        },
        onOpenDeviceDetails = {
            navigator.navigate(
                NavigationCommand(
                    DeviceDetailsScreenDestination(
                        navArgs.userId,
                        it.clientId
                    )
                )
            )
        },
        onSearchConversationMessagesClick = onSearchConversationMessagesClick,
        navigateBack = navigator::navigateBack,
        onConversationMediaClick = onConversationMediaClick,
        onLegalHoldLearnMoreClick = remember { { legalHoldSubjectDialogState.show(Unit) } },
        changeFavoriteState = changeConversationFavoriteViewModel::changeFavoriteState,
        removeFromFolder = removeConversationFromFolderViewModel::removeFromFolder,
        onMoveToFolder = null // TODO implement when conversation details will be available in OtherUserProfileScreenViewModel
    )

    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect {
            snackbarHostState.showSnackbar(it.asString(context.resources))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.closeBottomSheet.collect {
            sheetState.hide()
        }
    }

    VisibilityState(legalHoldSubjectDialogState) {
        LegalHoldSubjectProfileDialog(
            viewModel.state.userName,
            legalHoldSubjectDialogState::dismiss
        )
    }

    if (viewModel.state.errorLoadingUser != null) {
        UserNotFoundDialog(onActionButtonClicked = navigator::navigateBack)
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

@SuppressLint("UnusedCrossfadeTargetStateParameter", "LongParameterList")
@Composable
fun OtherProfileScreenContent(
    scope: CoroutineScope,
    state: OtherUserProfileState,
    requestInProgress: Boolean,
    sheetState: WireModalSheetState<Unit>,
    openBottomSheet: () -> Unit,
    closeBottomSheet: () -> Unit,
    eventsHandler: OtherUserProfileEventsHandler,
    bottomSheetEventsHandler: OtherUserProfileBottomSheetEventsHandler,
    onSearchConversationMessagesClick: () -> Unit,
    onIgnoreConnectionRequest: (String) -> Unit = { },
    onOpenConversation: (ConversationId) -> Unit = {},
    onOpenDeviceDetails: (Device) -> Unit = {},
    onConversationMediaClick: () -> Unit = {},
    navigateBack: () -> Unit = {},
    onLegalHoldLearnMoreClick: () -> Unit = {},
    onMoveToFolder: ((ConversationFoldersNavArgs) -> Unit)? = null,
    changeFavoriteState: (GroupDialogState, addToFavorite: Boolean) -> Unit,
    removeFromFolder: (conversationId: ConversationId, conversationName: String, folder: ConversationFolder) -> Unit,
) {
    val otherUserProfileScreenState = rememberOtherUserProfileScreenState()
    val blockUserDialogState = rememberVisibilityState<BlockUserDialogState>()
    val unblockUserDialogState = rememberVisibilityState<UnblockUserDialogState>()
    val removeMemberDialogState = rememberVisibilityState<RemoveConversationMemberState>()
    val clearConversationDialogState = rememberVisibilityState<DialogState>()
    val archivingConversationDialogState = rememberVisibilityState<DialogState>()
    val getBottomSheetVisibility: () -> Boolean = remember(sheetState) { { sheetState.isVisible } }
    val bottomSheetState = remember { OtherUserBottomSheetState() }
    bottomSheetState.setContents(state.conversationSheetContent, state.groupState)
    val openConversationBottomSheet: () -> Unit = remember(bottomSheetState) {
        {
            bottomSheetState.toConversation()
            openBottomSheet()
        }
    }
    val openChangeRoleBottomSheet: () -> Unit = remember(bottomSheetState) {
        {
            bottomSheetState.toChangeRole()
            openBottomSheet()
        }
    }

    LaunchedEffect(bottomSheetState) {
        snapshotFlow { sheetState.isVisible }.collect(FlowCollector { isVisible ->
            // without clearing BottomSheet after every closing there could be strange UI behaviour.
            // Example: open some big BottomSheet (ConversationBS), close it, then open small BS (ChangeRoleBS) ->
            // in that case user will see ChangeRoleBS at the center of the screen (just for few milliseconds)
            // and then it moves to the bottom.
            // It happens cause when `sheetState.show()` is called, it calculates animation offset by the old BS height (which was big)
            // To avoid such case we clear BS content on every closing
            if (!isVisible) bottomSheetState.clearBottomSheetState()
        })
    }

    val tabItems by remember(state) {
        derivedStateOf {
            listOfNotNull(
                state.groupState?.let { OtherUserProfileTabItem.GROUP },
                OtherUserProfileTabItem.DETAILS,
                OtherUserProfileTabItem.DEVICES
            )
        }
    }
    val initialPage = 0
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { tabItems.size })
    val lazyListStates = OtherUserProfileTabItem.entries.associateWith { rememberLazyListState() }
    val currentTabState by remember(state, pagerState) {
        derivedStateOf { if (state.isDataLoading) 0 else pagerState.calculateCurrentTab() }
    }

    if (!requestInProgress) {
        blockUserDialogState.dismiss()
        unblockUserDialogState.dismiss()
        removeMemberDialogState.dismiss()
        clearConversationDialogState.dismiss()
        archivingConversationDialogState.dismiss()
    }

    CollapsingTopBarScaffold(
        topBarHeader = {
            TopBarHeader(
                state = state,
                elevation = dimensions().spacing0x, // CollapsingTopBarScaffold already manages elevation
                onNavigateBack = navigateBack,
                openConversationBottomSheet = openConversationBottomSheet
            )
        },
        topBarCollapsing = {
            TopBarCollapsing(
                state = state,
                onSearchConversationMessagesClick = onSearchConversationMessagesClick,
                onConversationMediaClick = onConversationMediaClick,
                onLegalHoldLearnMoreClick = onLegalHoldLearnMoreClick,
            )
        },
        topBarFooter = {
            TopBarFooter(
                state = state,
                pagerState = pagerState,
                tabItems = tabItems,
                currentTab = currentTabState,
                scope = scope
            )
        },
        contentLazyListState = lazyListStates[tabItems[currentTabState]],
        content = {
            Content(
                state = state,
                pagerState = pagerState,
                tabItems = tabItems,
                otherUserProfileScreenState = otherUserProfileScreenState,
                lazyListStates = lazyListStates,
                openChangeRoleBottomSheet = openChangeRoleBottomSheet,
                openRemoveConversationMemberDialog = removeMemberDialogState::show,
                getOtherUserClients = eventsHandler::observeClientList,
                onDeviceClick = onOpenDeviceDetails
            )
        },
        bottomBar = {
            ContentFooter(
                state,
                onIgnoreConnectionRequest,
                onOpenConversation
            )
        },
        collapsingEnabled = state.connectionState != ConnectionState.BLOCKED
    )

    WireModalSheetLayout(
        sheetState = sheetState,
        sheetContent = {
            OtherUserProfileBottomSheetContent(
                getBottomSheetVisibility = getBottomSheetVisibility,
                bottomSheetState = bottomSheetState,
                eventsHandler = bottomSheetEventsHandler,
                blockUser = blockUserDialogState::show,
                unblockUser = unblockUserDialogState::show,
                clearContent = clearConversationDialogState::show,
                archivingStatusState = archivingConversationDialogState::show,
                changeFavoriteState = changeFavoriteState,
                closeBottomSheet = closeBottomSheet,
                onMoveToFolder = onMoveToFolder,
                removeFromFolder = removeFromFolder,
            )
        }
    )

    BlockUserDialogContent(
        dialogState = blockUserDialogState,
        onBlock = eventsHandler::onBlockUser,
        isLoading = requestInProgress,
    )
    UnblockUserDialogContent(
        dialogState = unblockUserDialogState,
        onUnblock = eventsHandler::onUnblockUser,
        isLoading = requestInProgress,
    )
    RemoveConversationMemberDialog(
        dialogState = removeMemberDialogState,
        onRemoveConversationMember = eventsHandler::onRemoveConversationMember,
        isLoading = requestInProgress,
    )
    ClearConversationContentDialog(
        dialogState = clearConversationDialogState,
        isLoading = requestInProgress,
        onClearConversationContent = {
            bottomSheetEventsHandler.onClearConversationContent(it)
        }
    )
    ArchiveConversationDialog(
        dialogState = archivingConversationDialogState,
        onArchiveButtonClicked = bottomSheetEventsHandler::onMoveConversationToArchive
    )
}

@Composable
private fun TopBarHeader(
    state: OtherUserProfileState,
    elevation: Dp,
    onNavigateBack: () -> Unit,
    openConversationBottomSheet: () -> Unit
) {
    val navigationIconType = if (state.groupState != null) {
        NavigationIconType.Close(R.string.content_description_user_profile_close_btn)
    } else if (state.connectionState == ConnectionState.PENDING || state.connectionState == ConnectionState.IGNORED) {
        NavigationIconType.Close(R.string.content_description_connection_request_close_btn)
    } else {
        NavigationIconType.Close()
    }

    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        navigationIconType = navigationIconType,
        titleContent = {
            WireTopAppBarTitle(
                title = stringResource(id = R.string.user_profile_title),
                style = MaterialTheme.wireTypography.title01,
                maxLines = 2
            )
        },
        elevation = elevation,
        actions = {
            if (state.conversationSheetContent != null) {
                MoreOptionIcon(
                    contentDescription = R.string.content_description_user_profile_more_btn,
                    onButtonClicked = openConversationBottomSheet,
                    state = if (state.isMetadataEmpty()) WireButtonState.Disabled else WireButtonState.Default
                )
            }
        }
    )
}

@Composable
private fun TopBarCollapsing(
    state: OtherUserProfileState,
    onSearchConversationMessagesClick: () -> Unit,
    onConversationMediaClick: () -> Unit = {},
    onLegalHoldLearnMoreClick: () -> Unit = {},
) {
    Crossfade(
        targetState = state,
        label = "OtherUserProfileScreenTopBarCollapsing"
    ) { targetState ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = dimensions().spacing16x),
        ) {
            UserProfileInfo(
                userId = targetState.userId,
                isLoading = targetState.isAvatarLoading,
                avatarAsset = targetState.userAvatarAsset,
                fullName = targetState.fullName,
                userName = targetState.userName,
                teamName = targetState.teamName,
                membership = targetState.membership,
                editableState = EditableState.NotEditable,
                connection = targetState.connectionState,
                isProteusVerified = targetState.isProteusVerified,
                isMLSVerified = targetState.isMLSVerified,
                expiresAt = targetState.expiresAt,
                accentId = targetState.accentId
            )
            if (state.isUnderLegalHold) {
                LegalHoldSubjectBanner(
                    onClick = onLegalHoldLearnMoreClick,
                    modifier = Modifier.padding(top = dimensions().spacing8x)
                )
            }
            if (state.shouldShowSearchButton()) {
                VerticalSpace.x24()
                SearchAndMediaRow(
                    onSearchConversationMessagesClick = onSearchConversationMessagesClick,
                    onConversationMediaClick = onConversationMediaClick
                )
            }
        }
    }
}

@Composable
private fun TopBarFooter(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabItems: List<OtherUserProfileTabItem>,
    currentTab: Int,
    scope: CoroutineScope
) {
    if (state.connectionState == ConnectionState.ACCEPTED) {
        AnimatedVisibility(
            visible = !state.isDataLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            WireTabRow(
                tabs = tabItems,
                selectedTabIndex = currentTab,
                onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabItems: List<OtherUserProfileTabItem>,
    otherUserProfileScreenState: OtherUserProfileScreenState,
    lazyListStates: Map<OtherUserProfileTabItem, LazyListState>,
    openChangeRoleBottomSheet: () -> Unit,
    openRemoveConversationMemberDialog: (RemoveConversationMemberState) -> Unit,
    getOtherUserClients: () -> Unit,
    onDeviceClick: (Device) -> Unit
) {

    Crossfade(targetState = tabItems to state, label = "OtherUserProfile") { (tabItems, state) ->
        Column {
            if (!state.isDataLoading && !state.isTemporaryUser()) {
                OtherUserConnectionStatusInfo(state.connectionState, state.membership)
                OtherUserConnectionUnverifiedWarning(state.fullName, state.connectionState)
            }
            when {
                state.isDataLoading || state.botService != null -> Box {} // no content visible while loading
                state.connectionState == ConnectionState.ACCEPTED -> {
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            state = pagerState,
                        ) { pageIndex ->
                            when (val tabItem = tabItems[pageIndex]) {
                                OtherUserProfileTabItem.DETAILS ->
                                    OtherUserProfileDetails(
                                        state = state,
                                        otherUserProfileScreenState = otherUserProfileScreenState,
                                        lazyListState = lazyListStates[tabItem]!!
                                    )

                                OtherUserProfileTabItem.GROUP ->
                                    OtherUserProfileGroup(
                                        state = state,
                                        onRemoveFromConversation = openRemoveConversationMemberDialog,
                                        openChangeRoleBottomSheet = openChangeRoleBottomSheet,
                                        lazyListState = lazyListStates[tabItem]!!,
                                    )

                                OtherUserProfileTabItem.DEVICES -> {
                                    getOtherUserClients()
                                    OtherUserDevicesScreen(
                                        lazyListState = lazyListStates[tabItem]!!,
                                        state = state,
                                        onDeviceClick = onDeviceClick
                                    )
                                }
                            }
                        }
                    }
                }

                state.groupState != null -> {
                    OtherUserProfileGroup(
                        state = state,
                        onRemoveFromConversation = openRemoveConversationMemberDialog,
                        openChangeRoleBottomSheet = openChangeRoleBottomSheet,
                        lazyListState = lazyListStates[OtherUserProfileTabItem.DETAILS]!!,
                    )
                }

                else -> Box {}
            }
        }
    }
}

@SuppressLint("ComposeModifierMissing")
@PackagePrivate
@Composable
fun ContentFooter(
    state: OtherUserProfileState,
    onIgnoreConnectionRequest: (String) -> Unit = {},
    onOpenConversation: (ConversationId) -> Unit = {}
) {
    AnimatedVisibility(
        visible = !state.isDataLoading,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // TODO show open conversation button for service bots after AR-2135
        val isNotTemporaryAndNotDeleted = !state.isTemporaryUser() && !state.isDeletedUser
        if (!state.isMetadataEmpty() && state.membership != Membership.Service && isNotTemporaryAndNotDeleted) {
            Surface(
                shadowElevation = dimensions().bottomNavigationShadowElevation,
                color = MaterialTheme.wireColorScheme.background
            ) {
                Box(modifier = Modifier.padding(all = dimensions().spacing16x)) {
                    ConnectionActionButton(
                        state.userId,
                        state.userName,
                        state.fullName,
                        state.connectionState,
                        state.isConversationStarted,
                        onIgnoreConnectionRequest,
                        onOpenConversation
                    )
                }
            }
        }
    }
}

enum class OtherUserProfileTabItem(@StringRes val titleResId: Int) : TabItem {
    GROUP(R.string.user_profile_group_tab),
    DETAILS(R.string.user_profile_details_tab),
    DEVICES(R.string.user_profile_devices_tab);

    override val title: UIText = UIText.StringResource(titleResId)
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherProfileScreenGroupMemberContent() {
    WireTheme {
        OtherProfileScreenContent(
            scope = rememberCoroutineScope(),
            state = OtherUserProfileState.PREVIEW.copy(
                connectionState = ConnectionState.ACCEPTED,
                isUnderLegalHold = true,
            ),
            requestInProgress = false,
            sheetState = rememberWireModalSheetState(),
            openBottomSheet = {},
            closeBottomSheet = {},
            eventsHandler = OtherUserProfileEventsHandler.PREVIEW,
            bottomSheetEventsHandler = OtherUserProfileBottomSheetEventsHandler.PREVIEW,
            onSearchConversationMessagesClick = {},
            changeFavoriteState = { _, _ -> },
            removeFromFolder = { _, _, _ -> },
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherProfileScreenContent() {
    WireTheme {
        OtherProfileScreenContent(
            scope = rememberCoroutineScope(),
            state = OtherUserProfileState.PREVIEW.copy(
                connectionState = ConnectionState.ACCEPTED,
                isUnderLegalHold = true,
                groupState = null
            ),
            requestInProgress = false,
            sheetState = rememberWireModalSheetState(),
            openBottomSheet = {},
            closeBottomSheet = {},
            eventsHandler = OtherUserProfileEventsHandler.PREVIEW,
            bottomSheetEventsHandler = OtherUserProfileBottomSheetEventsHandler.PREVIEW,
            onSearchConversationMessagesClick = {},
            changeFavoriteState = { _, _ -> },
            removeFromFolder = { _, _, _ -> },
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherProfileScreenContentNotConnected() {
    WireTheme {
        OtherProfileScreenContent(
            scope = rememberCoroutineScope(),
            state = OtherUserProfileState.PREVIEW.copy(
                connectionState = ConnectionState.CANCELLED,
                isUnderLegalHold = true,
            ),
            requestInProgress = false,
            sheetState = rememberWireModalSheetState(),
            openBottomSheet = {},
            closeBottomSheet = {},
            eventsHandler = OtherUserProfileEventsHandler.PREVIEW,
            bottomSheetEventsHandler = OtherUserProfileBottomSheetEventsHandler.PREVIEW,
            onSearchConversationMessagesClick = {},
            changeFavoriteState = { _, _ -> },
            removeFromFolder = { _, _, _ -> },
        )
    }
}

@Composable
@PreviewMultipleThemes
fun PreviewOtherProfileScreenTempUser() {
    WireTheme {
        OtherProfileScreenContent(
            scope = rememberCoroutineScope(),
            state = OtherUserProfileState.PREVIEW.copy(
                userName = "",
                connectionState = ConnectionState.CANCELLED,
                isUnderLegalHold = true,
                expiresAt = Instant.DISTANT_FUTURE
            ),
            requestInProgress = false,
            sheetState = rememberWireModalSheetState(),
            openBottomSheet = {},
            closeBottomSheet = {},
            eventsHandler = OtherUserProfileEventsHandler.PREVIEW,
            bottomSheetEventsHandler = OtherUserProfileBottomSheetEventsHandler.PREVIEW,
            onSearchConversationMessagesClick = {},
            changeFavoriteState = { _, _ -> },
            removeFromFolder = { _, _, _ -> },
        )
    }
}
