package com.safehome.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.safehome.app.SafeHomeApp
import com.safehome.app.service.VoiceDetectionService
class BootReceiver : BroadcastReceiver(){

    override fun onReceive(context: Context, intent: Intent){
        if(intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val tokenManager = (context.applicationContext as SafeHomeApp).tokenManager

        if (tokenManager.getAccessToken().isNullOrEmpty()) return

        if (tokenManager.isVoiceDetectionEnabled()){
            context.startForegroundService(Intent(context, VoiceDetectionService::class.java))
        }

        if (tokenManager.isLockScreenSosEnabled()){
            LockScreenNotificationHelper.show(context)
        }

        if (tokenManager.isNightModeEnabled()) {
            NightModeManager.scheduleNightMode(context)
            if(NightModeManager.isNightTime()){
                NightModeManager.activate(context)
            }
        }
    }
}