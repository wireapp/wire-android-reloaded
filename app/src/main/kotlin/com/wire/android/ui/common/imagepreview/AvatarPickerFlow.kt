package com.wire.android.ui.common.imagepreview

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.wire.android.ui.userprofile.image.ImageSource
import com.wire.android.util.permission.UseCameraRequestFlow
import com.wire.android.util.permission.UseStorageRequestFlow
import com.wire.android.util.permission.rememberOpenGalleryFlow
import com.wire.android.util.permission.rememberTakePictureFlow

class AvatarPickerFlow(
    private val takePictureFlow: UseCameraRequestFlow,
    private val openGalleryFlow: UseStorageRequestFlow
) {
    fun launch(imageSource: ImageSource) {
        when (imageSource) {
            ImageSource.Camera -> takePictureFlow.launch()
            ImageSource.Gallery -> openGalleryFlow.launch()
        }
    }
}

@Composable
fun rememberPickPictureState(
    onImageSelected: (Uri) -> Unit,
    onPictureTaken: (Boolean) -> Unit,
    targetPictureFileUri: Uri
): AvatarPickerFlow {

    val takePictureFLow = rememberTakePictureFlow(
        onPictureTaken = { wasSaved -> onPictureTaken(wasSaved) },
        onPermissionDenied = {
            // TODO: Implement denied permission rationale
        },
        targetPictureFileUri = targetPictureFileUri
    )

    val openGalleryFlow = rememberOpenGalleryFlow(
        onGalleryItemPicked = { pickedPictureUri -> onImageSelected(pickedPictureUri) },
        onPermissionDenied = {
            // TODO: Implement denied permission rationale
        }
    )

    return remember {
        AvatarPickerFlow(takePictureFLow, openGalleryFlow)
    }
}

sealed class PictureState(open val avatarUri: Uri) {
    data class Initial(override val avatarUri: Uri) : PictureState(avatarUri)
    data class Picked(override val avatarUri: Uri) : PictureState(avatarUri)
}
