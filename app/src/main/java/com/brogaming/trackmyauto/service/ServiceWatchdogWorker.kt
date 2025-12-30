package com.brogaming.trackmyauto.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.brogaming.trackmyauto.data.storage.AppPrefs
//import com.brogaming.trackmyauto.service.LiveLocationService
import com.google.firebase.auth.FirebaseAuth

class ServiceWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        Log.d("Watchdog", "🔍 Checking service health...")

        // Only restart if user is logged in
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        val ownerId = AppPrefs.getOwnerId(applicationContext)

        if (uid == null || ownerId.isNullOrBlank()) {
            Log.d("Watchdog", "User not authenticated, skipping check")
            return Result.success()
        }

        if (!isServiceRunning()) {
            Log.w("Watchdog", "⚠️ Service is DEAD! Restarting...")
            restartService()
        } else {
            Log.d("Watchdog", "✅ Service is alive")
        }

        return Result.success()
    }

    private fun isServiceRunning(): Boolean {
        val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (LiveLocationService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun restartService() {
        val intent = Intent(applicationContext, LiveLocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }
}

