package com.wire.android.mapper

import com.wire.android.model.ImageAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.user.OtherUser
import javax.inject.Inject

class ContactMapper
@Inject constructor(
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader
) {

    fun fromOtherUser(otherUser: OtherUser): Contact {
        with(otherUser) {
            return Contact(
                id = id.value,
                domain = id.domain,
                name = name ?: "",
                label = handle ?: "",
                avatarData = UserAvatarData(completePicture?.let { ImageAsset.UserAvatarAsset(wireSessionImageLoader, it) }),
                membership = userTypeMapper.toMembership(userType),
                connectionState = otherUser.connectionStatus
            )
        }
    }
}
