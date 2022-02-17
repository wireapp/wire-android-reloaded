package com.wire.android.ui.userprofile.image

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProfileImageViewModel() : ViewModel() {

    var state by mutableStateOf(ProfileImageViewModelState())
        private set

    fun onAvatarPicked(avatarBitmap: Bitmap) {
        state = state.copy(
            hasPickedAvatar = true,
            avatarBitmap = avatarBitmap
        )
    }

    //TODO:send to back-end
    fun onConfirmAvatar() {
        state = state.copy(isLoading = true)
        viewModelScope.launch {
            delay(2000)
            state = state.copy(
                uploadStatus = UploadStatus.Success,
                isLoading = false
            )
        }
    }

}

data class ProfileImageViewModelState(
    val isLoading: Boolean = false,
    val hasPickedAvatar: Boolean = false,
    val avatarBitmap: Bitmap = Bitmap.createBitmap(36, 36, Bitmap.Config.ARGB_8888),
    val uploadStatus: UploadStatus = UploadStatus.Initial
)

sealed class UploadStatus {
    object Initial : UploadStatus()
    object Error : UploadStatus()
    object Success : UploadStatus()
}
