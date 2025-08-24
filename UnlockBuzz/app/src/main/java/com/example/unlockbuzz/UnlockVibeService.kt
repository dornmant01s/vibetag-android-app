package com.example.unlockbuzz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationAttributes
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class UnlockVibeService : Service() {

    companion object {
        private const val CH_ID = "unlockbuzz.fgs"
        private const val NOTI_ID = 42
    }

    private lateinit var keyguard: KeyguardManager
    private var vibrator: Vibrator? = null
    private var isVibrating = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_USER_PRESENT -> startVibrationIfUnlocked()
                Intent.ACTION_SCREEN_OFF   -> stopVibration()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        keyguard = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        startForeground(NOTI_ID, buildNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        startVibrationIfUnlocked()
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        stopVibration()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            if (nm.getNotificationChannel(CH_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CH_ID, "Unlock Buzz",
                        NotificationManager.IMPORTANCE_LOW).apply {
                        setShowBadge(false)
                        description = "잠금 해제 중 진동 서비스"
                    }
                )
            }
        }
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CH_ID)
            .setContentTitle("잠금 해제 중 진동")
            .setContentText("화면이 잠금 해제된 동안 진동합니다.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun startVibrationIfUnlocked() {
        if (!keyguard.isKeyguardLocked) startVibration()
    }

    private fun startVibration() {
        if (isVibrating) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        val timings = longArrayOf(0, 250, 35)
        val amplitudes = intArrayOf(0, 255, 0)

        val effect = if (Build.VERSION.SDK_INT >= 26) {
            VibrationEffect.createWaveform(timings, amplitudes, 1)
        } else null

        if (Build.VERSION.SDK_INT >= 33) {
            vibrateApi33(v, effect)
        } else {
            vibrateLegacy(v, effect, timings)
        }
        isVibrating = true
    }

    private fun stopVibration() {
        vibrator?.cancel()
        isVibrating = false
    }

    @RequiresApi(33)
    private fun vibrateApi33(v: Vibrator, effect: VibrationEffect?) {
        val attrs = VibrationAttributes.Builder()
            .setUsage(VibrationAttributes.USAGE_ALARM)
            .build()
        if (effect != null) {
            v.vibrate(effect, attrs)
        } else {
            val oneShot = VibrationEffect.createOneShot(
                5000, VibrationEffect.DEFAULT_AMPLITUDE
            )
            v.vibrate(oneShot, attrs)
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy(
        v: Vibrator,
        effect: VibrationEffect?,
        timings: LongArray,
    ) {
        if (effect != null) {
            v.vibrate(effect)
        } else {
            // Use the repeating pattern on devices below API 26
            v.vibrate(timings, 1)
        }
    }
}
