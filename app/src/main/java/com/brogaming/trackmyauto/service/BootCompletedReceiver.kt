package com.brogaming.trackmyauto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.brogaming.trackmyauto.data.storage.AppPrefs
import com.brogaming.trackmyauto.service.LiveLocationService
import com.google.firebase.auth.FirebaseAuth

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "🔄 Device rebooted, checking if service should restart...")

            // Only restart if user is logged in and has owner ID
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid
            val ownerId = AppPrefs.getOwnerId(context)

            if (uid != null && !ownerId.isNullOrBlank()) {
                Log.d("BootReceiver", "✅ User authenticated, restarting service...")

                val serviceIntent = Intent(context, LiveLocationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                Log.d("BootReceiver", "⏭️ User not authenticated, skipping service start")
            }
        }
    }
}
