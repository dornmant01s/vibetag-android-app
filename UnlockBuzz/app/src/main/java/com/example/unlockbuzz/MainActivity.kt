package com.example.unlockbuzz

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val reqNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            reqNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            ensureIgnoreBatteryOptimizations()
            ContextCompat.startForegroundService(
                this, Intent(this, UnlockVibeService::class.java)
            )
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopService(Intent(this, UnlockVibeService::class.java))
        }
        findViewById<TextView>(R.id.tip).text = getString(R.string.tip_text)
    }

    private fun ensureIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoring = try {
            pm.isIgnoringBatteryOptimizations(packageName)
        } catch (_: SecurityException) {
            false
        }
        if (!ignoring) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (_: Exception) {
                // Ignore if the settings activity cannot be launched
            }
        }
    }
}
