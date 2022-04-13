package com.wire.android.ui.home.newconversation.contacts

import com.wire.android.model.UserStatus
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.UserId

data class ContactsState(val contacts: List<Contact> = emptyList())

data class Contact(
    val id: String,
    val domain: String,
    val name: String,
    val userStatus: UserStatus = UserStatus.AVAILABLE,
    val avatarUrl: String = "",
    val label: String = "",
) {

    fun toMember(): Member {
        return Member(UserId(id, domain))
    }
}

fun OtherUser.toContact() =
    Contact(
        id = id.value,
        domain = id.domain,
        name = name ?: "",
        label = handle ?: "",
    )


