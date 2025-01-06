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

package com.wire.android.ui.calling.model

import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.id.QualifiedID

data class UICallParticipant(
    val id: QualifiedID,
    val clientId: String,
    val isSelfUser: Boolean,
    val name: String? = null,
    val isMuted: Boolean,
    val isSpeaking: Boolean = false,
    val isCameraOn: Boolean,
    val isSharingScreen: Boolean,
    val avatar: ImageAsset.UserAvatarAsset? = null,
    val membership: Membership,
    val hasEstablishedAudio: Boolean,
    val accentId: Int
)
