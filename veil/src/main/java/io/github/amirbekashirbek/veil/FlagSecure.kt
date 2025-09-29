package io.github.amirbekashirbek.veil

import android.app.Activity
import android.view.WindowManager

object FlagSecure {
    fun setEnabled(activity: Activity, enabled: Boolean) {
        val window = activity.window
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}