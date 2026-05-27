package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.service.MileageTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("mileage_tracker_preferences", Context.MODE_PRIVATE)
            val isAutostartEnabled = prefs.getBoolean("is_autostart_enabled", false)
            
            if (isAutostartEnabled) {
                // Launch MainActivity to open on screen
                val activityIntent = Intent(context, com.example.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                try {
                    context.startActivity(activityIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val serviceIntent = Intent(context, MileageTrackingService::class.java).apply {
                    action = MileageTrackingService.ACTION_START_TRACKING
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    // Fail-safe catch for OS limitations
                }
            }
        }
    }
}
