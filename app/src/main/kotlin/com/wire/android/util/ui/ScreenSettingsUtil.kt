package com.wire.android.util.ui

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import cafe.adriel.voyager.navigator.Navigator
import com.wire.android.navigation.ScreenMode
import com.wire.android.navigation.VoyagerNavigationItem

fun Activity.updateScreenSettings(navigator: Navigator) {
    val screenMode = (navigator.lastItem as VoyagerNavigationItem).screenMode
    updateScreenSettings(screenMode)
}

private fun Activity.updateScreenSettings(screenMode: ScreenMode?) {
    when (screenMode) {
        ScreenMode.WAKE_UP -> wakeUpDevice()
        ScreenMode.KEEP_ON -> addScreenOnFlags()
        else -> removeScreenOnFlags()
    }
}

private fun Activity.wakeUpDevice() {

    addScreenOnFlags()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
}

private fun Activity.addScreenOnFlags() {
    window.addFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
    )
}

private fun Activity.removeScreenOnFlags() {
    window.clearFlags(
        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        setShowWhenLocked(false)
        setTurnScreenOn(false)
    } else {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
    }
}
