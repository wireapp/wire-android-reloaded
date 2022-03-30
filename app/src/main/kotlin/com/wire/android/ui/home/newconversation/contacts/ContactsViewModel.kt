package com.wire.android.ui.home.newconversation.contacts

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.publicuser.GetAllKnownUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val getAllKnownUsersUseCase: GetAllKnownUsersUseCase
) : ViewModel() {

    var contactState by mutableStateOf(ContactsState())

    init {
        viewModelScope.launch {
            contactState = contactState.copy(isLoading = true)

            getAllKnownUsersUseCase().catch {
                Log.d("TEST", "error $it")
            }.collect {
                Log.d("TEST", "result $it")
                contactState = contactState.copy(
                    isLoading = false,
                    contacts = it.map { publicUser -> publicUser.toContact() }
                )
            }
        }
    }

    fun close() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

}
