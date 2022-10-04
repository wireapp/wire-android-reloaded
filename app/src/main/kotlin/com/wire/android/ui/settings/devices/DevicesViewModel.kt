package com.wire.android.ui.settings.devices

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.settings.devices.model.DevicesState
import com.wire.kalium.logic.data.client.ClientType
import com.wire.kalium.logic.feature.client.SelfClientsResult
import com.wire.kalium.logic.feature.client.SelfClientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val selfClientsUseCase: SelfClientsUseCase
    ) : ViewModel() {

    var state: DevicesState by mutableStateOf(
        DevicesState(deviceList = listOf(), isLoadingClientsList = true, currentDevice = null)
    )
        private set

    init {
        loadClientsList()
    }

    private fun loadClientsList() {
        viewModelScope.launch {
            state = state.copy(isLoadingClientsList = true)
            val selfClientsResult = selfClientsUseCase()
            if (selfClientsResult is SelfClientsResult.Success)
                state = state.copy(
                    currentDevice = selfClientsResult.currentClient?.let { Device(it) },
                    isLoadingClientsList = false,
                    deviceList = selfClientsResult.clients.filter { it.type == ClientType.Permanent }.map { Device(it) },
                )
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

}
