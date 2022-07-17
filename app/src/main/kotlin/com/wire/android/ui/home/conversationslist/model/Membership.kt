package com.wire.android.ui.home.conversationslist.model

import androidx.annotation.StringRes
import com.wire.android.R

enum class Membership(@StringRes val stringResourceId: Int) {
    Guest(R.string.label_membership_guest),
    Federated(R.string.label_federated_membership),
    External(R.string.label_membership_external),
    Internal(-1),
    None(-1)
}

fun Membership.hasLabel(): Boolean = stringResourceId != -1;
