package com.wire.android.util

import android.content.res.Resources
import com.wire.android.R
import com.wire.kalium.logic.CoreFailure


fun CoreFailure.dialogErrorStrings(resources: Resources): DialogErrorStrings = when (this) {
    CoreFailure.NoNetworkConnection -> DialogErrorStrings(
            resources.getString(R.string.error_no_network_title),
            resources.getString(R.string.error_no_network_message))
    CoreFailure.ServerMiscommunication -> DialogErrorStrings(
            resources.getString(R.string.error_server_miscommunication_title),
            resources.getString(R.string.error_server_miscommunication_message))
    else -> DialogErrorStrings(
            resources.getString(R.string.error_unknown_title),
            resources.getString(R.string.error_unknown_message))
}

data class DialogErrorStrings(val title: String, val message: String)
