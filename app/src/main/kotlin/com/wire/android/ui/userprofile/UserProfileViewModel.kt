package com.wire.android.ui.userprofile

import android.graphics.Bitmap
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.model.UserStatus
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.feature.user.UploadUserAvatarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@ExperimentalMaterial3Api
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dataStore: UserDataStore,
    private val uploadUserAvatarUseCase: UploadUserAvatarUseCase
) : ViewModel() {

    var userProfileState by mutableStateOf(
        SelfUserProfileState(
            Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888),
            UserStatus.BUSY,
            "Tester Tost_long_long_long long  long  long  long  long  long ",
            "@userName_long_long_long_long_long_long_long_long_long_long",
            "Best team ever long  long  long  long  long  long  long  long  long ",
            listOf(
                OtherAccount("someId", "", "Other Name 0", "team A"),
//                OtherAccount("someId", "", "Other Name 1", "team B"),
//                OtherAccount("someId", "", "Other Name 2", "team C"),
//                OtherAccount("someId", "", "Other Name", "team A"),
                OtherAccount("someId", "", "New Name")
            )
        )
    )
        private set

    fun close() = viewModelScope.launch { navigationManager.navigateBack() }

    fun logout() {
        //TODO
        viewModelScope.launch {
            dataStore.clear() //TODO this should be moved to some service that will clear all the data in the app
            navigationManager.navigate(
                NavigationCommand(
                    NavigationItem.Authentication.route,
                    BackStackMode.CLEAR_WHOLE
                )
            )
        }
    }

    fun addAccount() {
        //TODO
    }

    fun editProfile() {
        viewModelScope.launch {
            navigationManager.navigate(NavigationCommand(NavigationItem.Settings.route))
        }
    }

    fun dismissStatusDialog() {
        userProfileState = userProfileState.copy(statusDialogData = null)
    }

    fun changeStatus(status: UserStatus) {
        setNotShowStatusRationaleAgainIfNeeded(status)
        //TODO
        dismissStatusDialog()
    }

    fun dialogCheckBoxStateChanged(isChecked: Boolean) {
        userProfileState.run {
            userProfileState = copy(statusDialogData = statusDialogData?.changeCheckBoxState(isChecked))
        }
    }

    fun changeStatusClick(status: UserStatus) {
        if (userProfileState.status == status) return

        viewModelScope.launch {
            if (shouldShowStatusRationaleDialog(status)) {
                val statusDialogInfo = when (status) {
                    UserStatus.AVAILABLE -> StatusDialogData.StateAvailable()
                    UserStatus.BUSY -> StatusDialogData.StateBusy()
                    UserStatus.AWAY -> StatusDialogData.StateAway()
                    UserStatus.NONE -> StatusDialogData.StateNone()
                }
                userProfileState = userProfileState.copy(statusDialogData = statusDialogInfo)
            } else {
                changeStatus(status)
            }
        }
    }

    private fun setNotShowStatusRationaleAgainIfNeeded(status: UserStatus) {
        userProfileState.statusDialogData.let { dialogState ->
            if (dialogState?.isCheckBoxChecked == true) {
                viewModelScope.launch { dataStore.dontShowStatusRationaleAgain(status) }
            }
        }
    }

    private suspend fun shouldShowStatusRationaleDialog(status: UserStatus): Boolean =
        dataStore.shouldShowStatusRationaleFlow(status).first()

    fun changeUserProfile(avatarBitmap: Bitmap) {
        changeUserProfile(avatarBitmap, onFailure = {
            val backupBitmap = userProfileState.avatarBitmap

            userProfileState = userProfileState.copy(
                avatarBitmap = backupBitmap
            )
        })
    }

    private fun changeUserProfile(avatarBitmap: Bitmap, onFailure: () -> Unit) {
        viewModelScope.launch {
            userProfileState = userProfileState.copy(
                isAvatarLoading = true,
                avatarBitmap = avatarBitmap
            )
            delay(2000)
            userProfileState = userProfileState.copy(isAvatarLoading = false)
        }
    }

}
