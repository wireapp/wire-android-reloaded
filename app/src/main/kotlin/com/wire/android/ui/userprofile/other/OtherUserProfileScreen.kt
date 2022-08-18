package com.wire.android.ui.userprofile.other

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import com.wire.android.R
import com.wire.android.model.PreservedState
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.calculateCurrentTab
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topBarElevation
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OtherUserProfileScreen(viewModel: OtherUserProfileScreenViewModel = hiltViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val openBottomSheet: () -> Unit = remember { { scope.launch { sheetState.show() } } }
    val closeBottomSheet: () -> Unit = remember { { scope.launch { sheetState.hide() } } }

    OtherProfileScreenContent(
        scope = scope,
        state = viewModel.state,
        removeMemberDialogState = viewModel.removeConversationMemberDialogState,
        blockUserDialogState = viewModel.blockUserDialogState,
        unblockUserDialogState = viewModel.unblockUserDialogState,
        sheetState = sheetState,
        openBottomSheet = openBottomSheet,
        closeBottomSheet = closeBottomSheet,
        snackbarHostState = snackbarHostState,
        eventsHandler = viewModel
    )
    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect {
            closeBottomSheet()
            snackbarHostState.showSnackbar(it.asString(context.resources))
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { sheetState.isVisible }.collect { isVisible ->
            // without clearing BottomSheet after every closing there could be strange UI behaviour.
            // Example: open some big BottomSheet (ConversationBS), close it, then open small BS (ChangeRoleBS) ->
            // in that case user will see ChangeRoleBS at the center of the screen (just for few milliseconds)
            // and then it moves to the bottom.
            // It happens cause when `sheetState.show()` is called, it calculates animation offset by the old BS height (which was big)
            // To avoid such case we clear BS content on every closing
            if (!isVisible) viewModel.clearBottomSheetState()
        }
    }
}

@SuppressLint("UnusedCrossfadeTargetStateParameter", "LongParameterList")
@OptIn(
    ExperimentalMaterialApi::class,
    ExperimentalPagerApi::class,
)
@Composable
fun OtherProfileScreenContent(
    scope: CoroutineScope,
    state: OtherUserProfileState,
    sheetState: ModalBottomSheetState,
    openBottomSheet: () -> Unit,
    closeBottomSheet: () -> Unit,
    removeMemberDialogState: PreservedState<RemoveConversationMemberState>?,
    blockUserDialogState: PreservedState<BlockUserDialogState>?,
    unblockUserDialogState: PreservedState<UnblockUserDialogState>?,
    eventsHandler: OtherUserProfileEventsHandler,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val otherUserProfileScreenState = rememberOtherUserProfileScreenState(snackbarHostState)
    val tabItems by remember(state) {
        derivedStateOf { listOfNotNull(state.groupState?.let { OtherUserProfileTabItem.GROUP }, OtherUserProfileTabItem.DETAILS) }
    }
    val initialPage = 0
    val pagerState = rememberPagerState(initialPage = initialPage)
    val lazyListStates = OtherUserProfileTabItem.values().associateWith { rememberLazyListState() }
    val currentTabState by remember(state, pagerState) {
        derivedStateOf { if (state.isDataLoading) 0 else pagerState.calculateCurrentTab() }
    }
    val maxBarElevation = MaterialTheme.wireDimensions.topBarShadowElevation
    val tabBarElevationState by remember(tabItems, lazyListStates, currentTabState) {
        derivedStateOf { lazyListStates[tabItems[currentTabState]]?.topBarElevation(maxBarElevation) ?: 0.dp }
    }

    WireModalSheetLayout(
        sheetState = sheetState,
        coroutineScope = rememberCoroutineScope(),
        sheetContent = {
            OtherUserProfileBottomSheetContent(
                bottomSheetState = state.bottomSheetContentState,
                onMutingConversationStatusChange = eventsHandler::onMutingConversationStatusChange,
                addConversationToFavourites = eventsHandler::onAddConversationToFavourites,
                moveConversationToArchive = eventsHandler::onMoveConversationToArchive,
                clearConversationContent = eventsHandler::onClearConversationContent,
                moveConversationToFolder = eventsHandler::onMoveConversationToFolder,
                blockUser = eventsHandler::showBlockUserDialog,
                closeBottomSheet = closeBottomSheet,
                changeMemberRole = eventsHandler::onChangeMemberRole,
                openConversationSheet = eventsHandler::setBottomSheetStateToConversation,
                openMuteOptionsSheet = eventsHandler::setBottomSheetStateToMuteOptions
            )
        }
    ) {
        CollapsingTopBarScaffold(
            snackbarHost = {
                SwipeDismissSnackbarHost(
                    hostState = otherUserProfileScreenState.snackbarHostState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            topBarHeader = { elevation ->
                TopBarHeader(
                    state = state,
                    elevation = elevation,
                    onNavigateBack = eventsHandler::navigateBack,
                    openConversationBottomSheet = {
                        eventsHandler.setBottomSheetStateToConversation()
                        openBottomSheet()
                    })
            },
            topBarCollapsing = { TopBarCollapsing(state) },
            topBarFooter = { TopBarFooter(state, pagerState, tabBarElevationState, tabItems, currentTabState, scope) },
            content = {
                Content(
                    state = state,
                    pagerState = pagerState,
                    tabItems = tabItems,
                    otherUserProfileScreenState = otherUserProfileScreenState,
                    lazyListStates = lazyListStates,
                    openChangeRoleBottomSheet = {
                        eventsHandler.setBottomSheetStateToChangeRole()
                        openBottomSheet()
                    },
                    eventsHandler::showRemoveConversationMemberDialog
                )
            },
            contentFooter = {
                ContentFooter(
                    state,
                    maxBarElevation,
                    eventsHandler::onSendConnectionRequest,
                    eventsHandler::onOpenConversation,
                    eventsHandler::onCancelConnectionRequest,
                    eventsHandler::onAcceptConnectionRequest,
                    eventsHandler::onIgnoreConnectionRequest,
                    eventsHandler::showUnblockUserDialog
                )
            },
            isSwipeable = state.connectionState == ConnectionState.ACCEPTED
        )
    }

    BlockUserDialogContent(
        dialogState = blockUserDialogState,
        dismiss = eventsHandler::hideBlockUserDialog,
        onBlock = eventsHandler::onBlockUser
    )
    UnblockUserDialogContent(
        dialogState = unblockUserDialogState,
        dismiss = eventsHandler::hideUnblockUserDialog,
        onUnblock = eventsHandler::onUnblockUser
    )
    RemoveConversationMemberDialog(
        dialogState = removeMemberDialogState,
        onDialogDismiss = eventsHandler::hideRemoveConversationMemberDialog,
        onRemoveConversationMember = eventsHandler::onRemoveConversationMember
    )
}

@Composable
private fun TopBarHeader(
    state: OtherUserProfileState,
    elevation: Dp,
    onNavigateBack: () -> Unit,
    openConversationBottomSheet: () -> Unit
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = stringResource(id = R.string.user_profile_title),
        elevation = elevation,
        actions = {
            if (state.connectionState in listOf(ConnectionState.ACCEPTED, ConnectionState.BLOCKED)) {
                MoreOptionIcon(openConversationBottomSheet)
            }
        }
    )
}

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
private fun TopBarCollapsing(state: OtherUserProfileState) {
    Crossfade(targetState = state.isDataLoading) {
        UserProfileInfo(
            isLoading = state.isAvatarLoading,
            avatarAsset = state.userAvatarAsset,
            fullName = state.fullName,
            userName = state.userName,
            teamName = state.teamName,
            membership = state.membership,
            editableState = EditableState.NotEditable,
            modifier = Modifier.padding(bottom = dimensions().spacing16x),
            connection = state.connectionState
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TopBarFooter(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabBarElevation: Dp,
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
            Surface(
                shadowElevation = tabBarElevation,
                color = MaterialTheme.wireColorScheme.background
            ) {
                WireTabRow(
                    tabs = tabItems,
                    selectedTabIndex = currentTab,
                    onTabChange = { scope.launch { pagerState.animateScrollToPage(it) } },
                    divider = {} // no divider
                )
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Content(
    state: OtherUserProfileState,
    pagerState: PagerState,
    tabItems: List<OtherUserProfileTabItem>,
    otherUserProfileScreenState: OtherUserProfileScreenState,
    lazyListStates: Map<OtherUserProfileTabItem, LazyListState>,
    openChangeRoleBottomSheet: () -> Unit,
    openRemoveConversationMemberDialog: () -> Unit
) {
    Crossfade(targetState = state) { state ->
        when {
            state.isDataLoading || state.botService != null -> Box {} // no content visible while loading
            state.connectionState == ConnectionState.ACCEPTED ->
                CompositionLocalProvider(LocalOverScrollConfiguration provides null) {
                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = pagerState,
                        count = tabItems.size
                    ) { pageIndex ->
                        when (val tabItem = tabItems[pageIndex]) {
                            OtherUserProfileTabItem.DETAILS ->
                                OtherUserProfileDetails(state, otherUserProfileScreenState, lazyListStates[tabItem]!!)
                            OtherUserProfileTabItem.GROUP ->
                                OtherUserProfileGroup(
                                    state.groupState!!,
                                    lazyListStates[tabItem]!!,
                                    openRemoveConversationMemberDialog,
                                    openChangeRoleBottomSheet
                                )
                        }
                    }
                }
            state.connectionState == ConnectionState.BLOCKED -> Box {} // no content visible for blocked users
            else -> {
                OtherUserConnectionStatusInfo(state.connectionState, state.membership)
            }
        }
    }
}

@Composable
private fun ContentFooter(
    state: OtherUserProfileState,
    maxBarElevation: Dp,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
    onUnblockUser: (String) -> Unit
) {
    AnimatedVisibility(
        visible = !state.isDataLoading,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            shadowElevation = maxBarElevation,
            color = MaterialTheme.wireColorScheme.background
        ) {
            Box(modifier = Modifier.padding(all = dimensions().spacing16x)) {
                // TODO show open conversation button for service bots after AR-2135
                if (state.membership != Membership.Service) {
                    OtherUserConnectionActionButton(
                        state.connectionState,
                        onSendConnectionRequest,
                        onOpenConversation,
                        onCancelConnectionRequest,
                        acceptConnectionRequest,
                        ignoreConnectionRequest
                    ) { onUnblockUser(state.userName) }
                }
            }
        }
    }
}

enum class OtherUserProfileTabItem(@StringRes override val titleResId: Int) : TabItem {
    GROUP(R.string.user_profile_group_tab),
    DETAILS(R.string.user_profile_details_tab);
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview(name = "Connected")
fun OtherProfileScreenContentPreview() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            rememberCoroutineScope(),
            OtherUserProfileState.PREVIEW.copy(connectionState = ConnectionState.ACCEPTED),
            rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
            {}, {}, null, null, null, OtherUserProfileEventsHandler.PREVIEW
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview(name = "Not Connected")
fun OtherProfileScreenContentNotConnectedPreview() {
    WireTheme(isPreview = true) {
        OtherProfileScreenContent(
            rememberCoroutineScope(),
            OtherUserProfileState.PREVIEW.copy(connectionState = ConnectionState.CANCELLED),
            rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
            {}, {}, null, null, null, OtherUserProfileEventsHandler.PREVIEW
        )
    }
}
