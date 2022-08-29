package com.wire.android.ui.home.conversations.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.CollapsingTopBarScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.search.SearchTopBar
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import com.wire.android.ui.home.conversations.details.AddMembersToConversationViewModel
import com.wire.android.ui.home.newconversation.common.SearchListScreens
import com.wire.android.ui.home.newconversation.common.SelectParticipantsButtonsRow
import com.wire.android.ui.home.newconversation.contacts.ContactsScreen
import com.wire.android.ui.home.newconversation.model.Contact


@Composable
fun AddMembersSearchRouter(
    addMembersToConversationViewModel: AddMembersToConversationViewModel = hiltViewModel()
) {
    SearchPeopleContent(
        searchPeopleState = addMembersToConversationViewModel.state,
        onSearchQueryChanged = addMembersToConversationViewModel::searchQueryChanged,
        onOpenUserProfile = addMembersToConversationViewModel::openUserProfile,
        onAddContactToGroup = addMembersToConversationViewModel::addContactToGroup,
        onRemoveContactFromGroup = addMembersToConversationViewModel::removeContactFromGroup,
        // Members search does not has the option to add a contact
        onAddContact = { },
        onGroupSelectionSubmitAction = addMembersToConversationViewModel::addMembersToConversation,
        onClose = addMembersToConversationViewModel::close,
    )
}

@Composable
fun SearchPeopleRouter(
    onGroupSelectionSubmitAction: () -> Unit,
    searchAllUsersViewModel: SearchAllUsersViewModel,
) {
    SearchPeopleContent(
        searchPeopleState = searchAllUsersViewModel.state,
        onSearchQueryChanged = searchAllUsersViewModel::searchQueryChanged,
        onOpenUserProfile = searchAllUsersViewModel::openUserProfile,
        onAddContactToGroup = searchAllUsersViewModel::addContactToGroup,
        onRemoveContactFromGroup = searchAllUsersViewModel::removeContactFromGroup,
        onAddContact = searchAllUsersViewModel::addContact,
        onGroupSelectionSubmitAction = onGroupSelectionSubmitAction,
        onClose = searchAllUsersViewModel::close,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPeopleContent(
    searchPeopleState: SearchPeopleState,
    onSearchQueryChanged: (TextFieldValue) -> Unit,
    onGroupSelectionSubmitAction: () -> Unit,
    onAddContact: (Contact) -> Unit,
    onAddContactToGroup: (Contact) -> Unit,
    onRemoveContactFromGroup: (Contact) -> Unit,
    onOpenUserProfile: (Contact) -> Unit,
    onClose: () -> Unit
) {
    val searchBarState = rememberSearchbarState()
    val searchNavController: NavHostController = rememberNavController()

    with(searchPeopleState) {
        CollapsingTopBarScaffold(
            topBarHeader = { elevation ->
                AnimatedVisibility(
                    visible = !searchBarState.isSearchActive,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Box(modifier = Modifier.wrapContentSize()) {
                        WireCenterAlignedTopAppBar(
                            elevation = elevation,
                            title = "test",
                            navigationIconType = NavigationIconType.Close,
                            onNavigationPressed = onClose
                        )
                    }
                }
            },
            topBarCollapsing = {
                val onInputClicked: () -> Unit = remember(searchBarState) {
                    {
                        searchBarState.openSearch()
                        searchNavController.navigate(SearchListScreens.SearchPeopleScreen.route)
                    }
                }
                val onCloseSearchClicked: () -> Unit = remember {
                    {
                        searchBarState.closeSearch()
                        searchNavController.popBackStack()
                    }
                }
                SearchTopBar(
                    isSearchActive = searchBarState.isSearchActive,
                    isSearchBarCollapsed = searchBarState.isSearchBarCollapsed,
                    searchBarHint = stringResource(R.string.label_search_people),
                    searchQuery = searchQuery,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onInputClicked = onInputClicked,
                    onCloseSearchClicked = onCloseSearchClicked
                )
            },
            content = {
                NavHost(
                    navController = searchNavController,
                    startDestination = SearchListScreens.KnownContactsScreen.route
                ) {
                    composable(
                        route = SearchListScreens.KnownContactsScreen.route,
                        content = {
                            ContactsScreen(
                                allKnownContactResult = initialContacts,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onAddToGroup = onAddContactToGroup,
                                onRemoveFromGroup = onRemoveContactFromGroup,
                                onOpenUserProfile = onOpenUserProfile
                            )
                        }
                    )
                    composable(
                        route = SearchListScreens.SearchPeopleScreen.route,
                        content = {
                            SearchAllPeopleScreen(
                                searchQuery = searchQuery.text,
                                noneSearchSucceed = noneSearchSucceed,
                                searchResult = searchResult,
                                contactsAddedToGroup = contactsAddedToGroup,
                                onAddToGroup = onAddContactToGroup,
                                onRemoveFromGroup = onRemoveContactFromGroup,
                                onOpenUserProfile = onOpenUserProfile,
                                onAddContactClicked = onAddContact
                            )
                        }
                    )
                }
            },
            bottomBar = {
                SelectParticipantsButtonsRow(
                    count = contactsAddedToGroup.size,
                    mainButtonText = "test",
                    onMainButtonClick = onGroupSelectionSubmitAction
                )
            },
            snapOnFling = false,
            keepElevationWhenCollapsed = true
        )
    }

    BackHandler(searchBarState.isSearchActive) {
        searchBarState.closeSearch()
        searchNavController.popBackStack()
    }
}
