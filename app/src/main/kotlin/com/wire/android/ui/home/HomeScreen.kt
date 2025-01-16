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

package com.wire.android.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.navigation.handleNavigation
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.FloatingActionButton
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.destinations.ConversationFoldersScreenDestination
import com.wire.android.ui.destinations.ConversationScreenDestination
import com.wire.android.ui.destinations.NewConversationSearchPeopleScreenDestination
import com.wire.android.ui.destinations.OtherUserProfileScreenDestination
import com.wire.android.ui.destinations.SelfUserProfileScreenDestination
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.home.conversations.details.GroupConversationActionType
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsNavBackArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersNavBackArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersStateArgs
import com.wire.android.ui.home.conversations.folder.ConversationFoldersViewModel
import com.wire.android.ui.home.conversations.folder.ConversationFoldersViewModelImpl
import com.wire.android.ui.home.conversationslist.filter.ConversationFilterSheetContent
import com.wire.android.ui.home.conversationslist.filter.ConversationFilterSheetData
import com.wire.android.ui.home.conversationslist.filter.rememberFilterSheetState
import com.wire.android.ui.home.drawer.HomeDrawer
import com.wire.android.ui.home.drawer.HomeDrawerState
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.util.permission.rememberShowNotificationsPermissionFlow
import com.wire.kalium.logic.data.conversation.ConversationFolder
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@RootNavGraph
@WireDestination
@Composable
fun HomeScreen(
    navigator: Navigator,
    groupDetailsScreenResultRecipient: ResultRecipient<ConversationScreenDestination, GroupConversationDetailsNavBackArgs>,
    otherUserProfileScreenResultRecipient: ResultRecipient<OtherUserProfileScreenDestination, String>,
    conversationFoldersScreenResultRecipient:
    ResultRecipient<ConversationFoldersScreenDestination, ConversationFoldersNavBackArgs>,
    homeViewModel: HomeViewModel = hiltViewModel(),
    appSyncViewModel: AppSyncViewModel = hiltViewModel(),
    homeDrawerViewModel: HomeDrawerViewModel = hiltViewModel(),
    analyticsUsageViewModel: AnalyticsUsageViewModel = hiltViewModel(),
    foldersViewModel: ConversationFoldersViewModel =
        hiltViewModel<ConversationFoldersViewModelImpl, ConversationFoldersViewModelImpl.Factory>(
            creationCallback = {
                it.create(ConversationFoldersStateArgs(null))
            }
        )
) {
    homeViewModel.checkRequirements { it.navigate(navigator::navigate) }
    val context = LocalContext.current

    val homeScreenState = rememberHomeScreenState(navigator)
    val notificationsPermissionDeniedDialogState = rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()
    val showNotificationsPermissionDeniedDialog = {
        notificationsPermissionDeniedDialogState.show(
            PermissionPermanentlyDeniedDialogState.Visible(
                title = R.string.app_permission_dialog_title,
                description = R.string.notifications_permission_dialog_description,
            )
        )
    }
    val showNotificationsFlow =
        rememberShowNotificationsPermissionFlow(
            onPermissionGranted = { /* do nothing */ },
            onPermissionDenied = showNotificationsPermissionDeniedDialog,
            onPermissionPermanentlyDenied = { /* do nothing */ },
        )

    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appSyncViewModel.startSyncingAppConfig()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(homeViewModel.savedStateHandle) {
        showNotificationsFlow.launch()
    }

    val homeState = homeViewModel.homeState
    if (homeViewModel.homeState.shouldDisplayWelcomeMessage) {
        WelcomeNewUserDialog(
            dismissDialog = homeViewModel::dismissWelcomeMessage
        )
    }

    if (analyticsUsageViewModel.state.shouldDisplayDialog) {
        AnalyticsUsageDialog(
            agreeOption = analyticsUsageViewModel::agreeAnalyticsUsage,
            declineOption = analyticsUsageViewModel::declineAnalyticsUsage
        )
    }

    HomeContent(
        homeState = homeState,
        homeDrawerState = homeDrawerViewModel.drawerState,
        homeStateHolder = homeScreenState,
        onNewConversationClick = { navigator.navigate(NavigationCommand(NewConversationSearchPeopleScreenDestination)) },
        onSelfUserClick = {
            homeViewModel.sendOpenProfileEvent()
            navigator.navigate(NavigationCommand(SelfUserProfileScreenDestination))
        },
        folders = foldersViewModel.state().folders
    )

    BackHandler(homeScreenState.drawerState.isOpen) {
        homeScreenState.coroutineScope.launch {
            homeScreenState.drawerState.close()
        }
    }
    BackHandler(homeScreenState.searchBarState.isSearchActive) {
        homeScreenState.searchBarState.closeSearch()
    }

    groupDetailsScreenResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args from groupDetails in ConversationScreen")
            }

            is NavResult.Value -> {
                when (result.value.groupConversationActionType) {
                    GroupConversationActionType.LEAVE_GROUP -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar((HomeSnackBarMessage.LeftConversationSuccess.uiText.asString(context.resources)))
                        }
                    }

                    GroupConversationActionType.DELETE_GROUP -> {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                HomeSnackBarMessage.DeletedConversationGroupSuccess(result.value.conversationName).uiText.asString(
                                    context.resources
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    otherUserProfileScreenResultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                appLogger.i("Error with receiving navigation back args from OtherUserProfile in ConversationScreen")
            }

            is NavResult.Value -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        HomeSnackBarMessage.SuccessConnectionIgnoreRequest(result.value).uiText.asString(context.resources)
                    )
                }
            }
        }
    }

    conversationFoldersScreenResultRecipient.onNavResult { result ->
        when (result) {
            NavResult.Canceled -> {}
            is NavResult.Value -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(result.value.message)
                }
            }
        }
    }

    PermissionPermanentlyDeniedDialog(
        dialogState = notificationsPermissionDeniedDialogState,
        hideDialog = notificationsPermissionDeniedDialogState::dismiss
    )
}

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeContent(
    homeState: HomeState,
    homeDrawerState: HomeDrawerState,
    homeStateHolder: HomeStateHolder,
    onNewConversationClick: () -> Unit,
    onSelfUserClick: () -> Unit,
    modifier: Modifier = Modifier,
    folders: PersistentList<ConversationFolder> = persistentListOf()
) {
    val context = LocalContext.current
    val filterSheetState = rememberWireModalSheetState<ConversationFilterSheetData>()

    with(homeStateHolder) {
        fun openHomeDestination(item: HomeDestination) {
            item.direction.handleNavigation(
                context = context,
                handleOtherDirection = { direction ->
                    navController.navigate(direction.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                inclusive = true
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }

        ModalNavigationDrawer(
            modifier = modifier,
            drawerState = drawerState,
            drawerContent = {
                BoxWithConstraints {
                    val maxWidth = min(this.maxWidth - dimensions().homeDrawerSheetEndPadding, DrawerDefaults.MaximumDrawerWidth)
                    ModalDrawerSheet(
                        drawerContainerColor = MaterialTheme.colorScheme.surface,
                        drawerTonalElevation = 0.dp,
                        drawerShape = RectangleShape,
                        modifier = Modifier.widthIn(max = maxWidth)
                    ) {
                        HomeDrawer(
                            currentRoute = currentNavigationItem.direction.route,
                            homeDrawerState = homeDrawerState,
                            navigateToHomeItem = ::openHomeDestination,
                            onCloseDrawer = ::closeDrawer
                        )
                    }
                }
            },
            gesturesEnabled = drawerState.isOpen,
            content = {
                CollapsingTopBarScaffold(
                    snapOnFling = false,
                    topBarHeader = {
                        AnimatedVisibility(
                            modifier = Modifier.background(MaterialTheme.colorScheme.background),
                            visible = !searchBarState.isSearchActive,
                            enter = fadeIn() + expandVertically(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            HomeTopBar(
                                title = currentTitle.asString(),
                                currentFilter = currentConversationFilter,
                                navigationItem = currentNavigationItem,
                                userAvatarData = homeState.userAvatarData,
                                elevation = dimensions().spacing0x, // CollapsingTopBarScaffold manages applied elevation
                                withLegalHoldIndicator = homeState.shouldDisplayLegalHoldIndicator,
                                shouldShowCreateTeamUnreadIndicator = homeState.shouldShowCreateTeamUnreadIndicator,
                                onHamburgerMenuClick = ::openDrawer,
                                onNavigateToSelfUserProfile = onSelfUserClick,
                                onOpenConversationFilter = {
                                    filterSheetState.show(
                                        ConversationFilterSheetData(
                                            currentFilter = it,
                                            folders = folders
                                        )
                                    )
                                }
                            )
                        }
                    },
                    topBarCollapsing = {
                        if (currentNavigationItem.isSearchable) {
                            SearchTopBar(
                                isSearchActive = searchBarState.isSearchActive,
                                searchBarHint = stringResource(R.string.search_bar_conversations_hint),
                                searchQueryTextState = searchBarState.searchQueryTextState,
                                onActiveChanged = searchBarState::searchActiveChanged,
                            )
                        }
                    },
                    collapsingEnabled = !searchBarState.isSearchActive,
                    contentLazyListState = homeStateHolder.lazyListStateFor(currentNavigationItem),
                    content = {
                        /**
                         * This "if" is a workaround, otherwise it can crash because of the SubcomposeLayout's nature.
                         * We need to communicate to the sub-compositions when they are to be disposed by the parent and ignore
                         * compositions in the round they are to be disposed. More here:
                         * https://github.com/google/accompanist/issues/1487
                         * https://issuetracker.google.com/issues/268422136
                         * https://issuetracker.google.com/issues/254645321
                         */
                        if (LocalLifecycleOwner.current.lifecycle.currentState != Lifecycle.State.DESTROYED) {
                            val navHostEngine = rememberAnimatedNavHostEngine(
                                rootDefaultAnimations = RootNavGraphDefaultAnimations.ACCOMPANIST_FADING
                            )
                            DestinationsNavHost(
                                navGraph = NavGraphs.home,
                                engine = navHostEngine,
                                navController = navController,
                                dependenciesContainerBuilder = {
                                    dependency(homeStateHolder)
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        AnimatedVisibility(
                            visible = currentNavigationItem.withNewConversationFab && !searchBarState.isSearchActive,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            FloatingActionButton(
                                text = stringResource(R.string.label_new),
                                icon = {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_conversation),
                                        contentDescription = stringResource(R.string.content_description_new_conversation),
                                        contentScale = ContentScale.FillBounds,
                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                                        modifier = Modifier
                                            .padding(
                                                start = dimensions().spacing4x,
                                                top = dimensions().spacing2x
                                            )
                                            .size(dimensions().fabIconSize)
                                    )
                                },
                                onClick = onNewConversationClick
                            )
                        }
                    }
                )
            }
        )
        WireModalSheetLayout(
            sheetState = filterSheetState,
            sheetContent = { sheetData ->
                val sheetContentState = rememberFilterSheetState(sheetData)
                ConversationFilterSheetContent(
                    onChangeFilter = { filter ->
                        filterSheetState.hide()
                        homeStateHolder.changeFilter(filter)
                    },
                    filterSheetState = sheetContentState
                )
            }
        )
    }
}
