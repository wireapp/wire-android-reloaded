package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.newconversation.contacts.Contact
import com.wire.android.ui.home.newconversation.contacts.toContact
import com.wire.android.ui.home.newconversation.search.ContactSearchResult
import com.wire.android.ui.home.newconversation.search.SearchPeopleState
import com.wire.android.ui.home.newconversation.search.SearchResultState
import com.wire.android.util.flow.SearchQueryStateFlow
import com.wire.kalium.logic.feature.publicuser.SearchKnownUsersUseCase
import com.wire.kalium.logic.feature.publicuser.SearchUserDirectoryUseCase
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NewConversationViewModel
@Inject constructor(
    private val navigationManager: NavigationManager,
    private val searchKnownUsers: SearchKnownUsersUseCase,
    private val searchPublicUsers: SearchUserDirectoryUseCase
) : ViewModel() {

    //TODO: map this value out with the given back-end configuration later on
    private companion object {
        const val HARDCODED_TEST_DOMAIN = "staging.zinfra.io"
    }

    val state: SearchPeopleState by derivedStateOf {
        val noneSearchSucceed: Boolean =
            localContactSearchResult.searchResultState is SearchResultState.Failure
                    && publicContactsSearchResult.searchResultState is SearchResultState.Failure
                    && federatedContactSearchResult.searchResultState is SearchResultState.Failure

        innerSearchPeopleState.copy(
            noneSearchSucceed = noneSearchSucceed,
            localContactSearchResult = localContactSearchResult,
            publicContactsSearchResult = publicContactsSearchResult,
            federatedContactSearchResult = federatedContactSearchResult
        )
    }

    private var innerSearchPeopleState: SearchPeopleState by mutableStateOf(SearchPeopleState())

    private var localContactSearchResult by mutableStateOf(ContactSearchResult.InternalContact(searchResultState = SearchResultState.Initial))

    private var publicContactsSearchResult by mutableStateOf(ContactSearchResult.ExternalContact(searchResultState = SearchResultState.Initial))

    private var federatedContactSearchResult by mutableStateOf(ContactSearchResult.ExternalContact(searchResultState = SearchResultState.Initial))

    private val searchQueryStateFlow = SearchQueryStateFlow()

    init {
        viewModelScope.launch {
            searchQueryStateFlow.onSearchAction { searchTerm ->
                innerSearchPeopleState = state.copy(searchQuery = searchTerm)

                launch { searchPublic(searchTerm) }
                launch { searchKnown(searchTerm) }
            }
        }
    }

    private suspend fun searchKnown(searchTerm: String) {
        //TODO: this is going to be refactored on the Kalium side so that we do not use Flow
        searchKnownUsers(searchTerm).onStart {
            localContactSearchResult = ContactSearchResult.InternalContact(SearchResultState.InProgress)
        }.catch {
            localContactSearchResult = ContactSearchResult.InternalContact(SearchResultState.Failure())

        }.flowOn(Dispatchers.IO).collect {
            localContactSearchResult = ContactSearchResult.InternalContact(
                SearchResultState.Success(it.result.map { publicUser -> publicUser.toContact() })
            )
        }
    }

    private suspend fun searchPublic(searchTerm: String) {
        publicContactsSearchResult = ContactSearchResult.ExternalContact(SearchResultState.InProgress)

        val result = withContext(Dispatchers.IO) {
            searchPublicUsers(
                searchQuery = searchTerm,
                domain = HARDCODED_TEST_DOMAIN
            )
        }

        publicContactsSearchResult = when (result) {
            is Either.Left -> {
                ContactSearchResult.ExternalContact(SearchResultState.Failure())
            }
            is Either.Right -> {
                ContactSearchResult.ExternalContact(
                    SearchResultState.Success(result.value.result.map { it.toContact() })
                )
            }
        }
    }

    fun search(searchTerm: String) {
        searchQueryStateFlow.search(searchTerm)
    }

    fun addContactToGroup(contact: Contact) {
        innerSearchPeopleState = innerSearchPeopleState.copy(addToGroupContacts = innerSearchPeopleState.addToGroupContacts + contact)
    }

    fun removeContactFromGroup(contact: Contact) {
        innerSearchPeopleState = innerSearchPeopleState.copy(addToGroupContacts = innerSearchPeopleState.addToGroupContacts - contact)
    }

    fun openUserProfile(contact: Contact) {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(contact.id))
                )
            )
        }
    }

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

}
