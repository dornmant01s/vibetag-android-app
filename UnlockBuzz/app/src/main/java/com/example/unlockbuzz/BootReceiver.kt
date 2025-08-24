package com.example.unlockbuzz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) {
            ContextCompat.startForegroundService(c, Intent(c, UnlockVibeService::class.java))
        }
    }
}
