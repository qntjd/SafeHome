package com.safehome.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NightModeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ACTIVATE   = "com.safehome.app.NIGHT_MODE_ACTIVATE"
        const val ACTION_DEACTIVATE = "com.safehome.app.NIGHT_MODE_DEACTIVATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ACTIVATE   -> NightModeManager.activate(context)
            ACTION_DEACTIVATE -> NightModeManager.deactivate(context)
        }
    }
}