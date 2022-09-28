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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.TabItem
import com.wire.android.ui.common.WireTabRow
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.dialogs.BlockUserDialogContent
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogContent
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.kalium.logic.data.user.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, InternalCoroutinesApi::class)
@Composable
fun OtherUserProfileScreen(viewModel: OtherUserProfileScreenViewModel = hiltViewModel()) {
    val screenState = rememberOtherUserProfileScreenState()

    OtherProfileScreenContent(
        screenState = screenState,
        state = viewModel.state,
        requestInProgress = viewModel.requestInProgress,
        eventsHandler = viewModel,
        footerEventsHandler = viewModel,
        bottomSheetEventsHandler = viewModel
    )

    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect {
            screenState.closeBottomSheet()
            screenState.showSnackbar(it)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { screenState.sheetState.isVisible }.collect { isVisible ->
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
    ExperimentalMaterial3Api::class,
)
@Composable
fun OtherProfileScreenContent(
    screenState: OtherUserProfileScreenState,
    state: OtherUserProfileState,
    requestInProgress: Boolean,
    eventsHandler: OtherUserProfileEventsHandler,
    footerEventsHandler: OtherUserProfileFooterEventsHandler,
    bottomSheetEventsHandler: OtherUserProfileBottomSheetEventsHandler,
) {
    if (!requestInProgress) {
        screenState.dismissDialogs()
    }

    with(screenState) {
        WireModalSheetLayout(
            sheetState = sheetState,
            coroutineScope = coroutineScope,
            sheetContent = {
                OtherUserProfileBottomSheetContent(
                    bottomSheetState = state.bottomSheetContentState,
                    eventsHandler = bottomSheetEventsHandler,
                    blockUser = blockUserDialogState::show,
                    closeBottomSheet = ::closeBottomSheet,
                )
            }
        ) {
            val otherProfilePagerState = rememberOtherUserProfilePagerState(state.groupState != null)

            CollapsingTopBarScaffold(
                snackbarHost = {
                    SwipeDismissSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                topBarHeader = { elevation ->
                    TopBarHeader(
                        state = state,
                        elevation = elevation,
                        onNavigateBack = eventsHandler::navigateBack,
                        openConversationBottomSheet = {
                            bottomSheetEventsHandler.setBottomSheetStateToConversation()
                            openBottomSheet()
                        })
                },
                topBarCollapsing = { TopBarCollapsing(state) },
                topBarFooter = {
                    TopBarFooter(
                        state = state,
                        pagerState = otherProfilePagerState.pagerState,
                        tabBarElevation = otherProfilePagerState.tabBarElevationState,
                        tabItems = otherProfilePagerState.tabItems,
                        currentTab = otherProfilePagerState.currentTabState,
                        scope = coroutineScope
                    )
                },
                content = {
                    Content(
                        state = state,
                        pagerState = otherProfilePagerState.pagerState,
                        tabItems = otherProfilePagerState.tabItems,
                        lazyListStates = otherProfilePagerState.tabItemsLazyListState,
                        openChangeRoleBottomSheet = {
                            eventsHandler.setBottomSheetStateToChangeRole()
                            openBottomSheet()
                        },
                        openRemoveConversationMemberDialog = removeMemberDialogState::show,
                        getOtherUserClients = eventsHandler::getOtherUserClients,
                        onCopy = ::copy
                    )
                },
                bottomBar = {
                    ContentFooter(
                        state,
                        otherProfilePagerState.topBarMaxBarElevation,
                        footerEventsHandler,
                        unblockUserDialogState::show
                    )
                },
                isSwipeable = state.connectionState == ConnectionState.ACCEPTED
            )
        }

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
    }
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
    lazyListStates: Map<OtherUserProfileTabItem, LazyListState>,
    openChangeRoleBottomSheet: () -> Unit,
    openRemoveConversationMemberDialog: (RemoveConversationMemberState) -> Unit,
    onCopy: (String) -> Unit,
    getOtherUserClients: () -> Unit
) {
    Crossfade(targetState = tabItems to state) { (tabItems, state) ->
        when {
            state.isDataLoading || state.botService != null -> Box {} // no content visible while loading
            state.connectionState == ConnectionState.ACCEPTED ->
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        state = pagerState,
                        count = tabItems.size
                    ) { pageIndex ->
                        when (val tabItem = tabItems[pageIndex]) {
                            OtherUserProfileTabItem.DETAILS -> {
                                OtherUserProfileDetails(
                                    lazyListState = lazyListStates[tabItem]!!,
                                    email = state.email,
                                    phoneNumber = state.phone,
                                    onCopy = onCopy
                                )
                            }
                            OtherUserProfileTabItem.GROUP -> {
                                OtherUserProfileGroup(
                                    state = state,
                                    lazyListState = lazyListStates[tabItem]!!,
                                    onRemoveFromConversation = openRemoveConversationMemberDialog,
                                    openChangeRoleBottomSheet = openChangeRoleBottomSheet
                                )
                            }
                            OtherUserProfileTabItem.DEVICES -> {
                                getOtherUserClients()
                                OtherUserDevicesScreen(
                                    lazyListState = lazyListStates[tabItem]!!,
                                    state = state
                                )
                            }
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
    footerEventsHandler: OtherUserProfileFooterEventsHandler,
    onUnblockUser: (UnblockUserDialogState) -> Unit
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
                        footerEventsHandler::onSendConnectionRequest,
                        footerEventsHandler::onOpenConversation,
                        footerEventsHandler::onCancelConnectionRequest,
                        footerEventsHandler::onAcceptConnectionRequest,
                        footerEventsHandler::onIgnoreConnectionRequest
                    ) { onUnblockUser(UnblockUserDialogState(state.userName, state.userId)) }
                }
            }
        }
    }
}

enum class OtherUserProfileTabItem(@StringRes override val titleResId: Int) : TabItem {
    GROUP(R.string.user_profile_group_tab),
    DETAILS(R.string.user_profile_details_tab),
    DEVICES(R.string.user_profile_devices_tab);
}
//
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//@Preview(name = "Connected")
//fun OtherProfileScreenContentPreview() {
//    WireTheme(isPreview = true) {
//        OtherProfileScreenContent(
//            rememberCoroutineScope(),
//            OtherUserProfileState.PREVIEW.copy(connectionState = ConnectionState.ACCEPTED), false,
//            rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
//            {}, {}, OtherUserProfileEventsHandler.PREVIEW,
//            OtherUserProfileFooterEventsHandler.PREVIEW, OtherUserProfileBottomSheetEventsHandler.PREVIEW
//        )
//    }
//}
//
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//@Preview(name = "Not Connected")
//fun OtherProfileScreenContentNotConnectedPreview() {
//    WireTheme(isPreview = true) {
//        OtherProfileScreenContent(
//            rememberCoroutineScope(),
//            OtherUserProfileState.PREVIEW.copy(connectionState = ConnectionState.CANCELLED), false,
//            rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
//            {}, {}, OtherUserProfileEventsHandler.PREVIEW,
//            OtherUserProfileFooterEventsHandler.PREVIEW, OtherUserProfileBottomSheetEventsHandler.PREVIEW
//        )
//    }
//}
