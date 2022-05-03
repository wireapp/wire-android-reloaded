package com.wire.android.ui.calling.incoming

import com.wire.android.model.ImageAsset.UserAvatarAsset

data class IncomingCallState(
    val conversationName: String? = null,
    val avatarAssetId: UserAvatarAsset? = null,
    val isMicrophoneMuted: Boolean = false,
    val isCameraOn: Boolean = false,
    val isSpeakerOn: Boolean = false
)
