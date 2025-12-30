package com.brogaming.trackmyauto

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.brogaming.trackmyauto.service.ServiceWatchdogWorker
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        scheduleServiceWatchdog()
    }

    private fun scheduleServiceWatchdog() {
        val watchdogRequest = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
            40,
            TimeUnit.MINUTES
        ).build()

        WorkManager.Companion.getInstance(this).enqueueUniquePeriodicWork(
            "service_watchdog",
            ExistingPeriodicWorkPolicy.KEEP,
            watchdogRequest
        )
    }
}