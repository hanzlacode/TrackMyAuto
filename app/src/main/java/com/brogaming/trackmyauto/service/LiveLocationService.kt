package com.brogaming.trackmyauto.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.brogaming.trackmyauto.data.storage.AppPrefs
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class LiveLocationService : LifecycleService() {

    @Inject lateinit var auth: FirebaseAuth

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var alarmManager: AlarmManager

    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private var wakeLock: PowerManager.WakeLock? = null

    private var ownerId: String? = null
    private var driverId: String? = null

    private var isTracking = false
    private var lastUploadTime = 0L

    private var lastLocation: android.location.Location? = null
    private var lastRealtimeNanos = 0L
    private var lastComputedSpeed = 0f

    private var trackingListener: ValueEventListener? = null
    private var lastTrackingState: Boolean? = null

    private val CHANNEL_ID = "live_location_channel"
    private val NOTIFICATION_ID = 101
    private val ALARM_ACTION = "TRACKMYAUTO_KICK"

    // =========================
    // LIFECYCLE
    // =========================

    override fun onCreate() {
        super.onCreate()

        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        createNotificationChannel()
        acquireWakeLock()
        setupLocationCallback()
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForegroundUpdated("📡 Location service running")

        if (intent?.action == ALARM_ACTION) {
            scheduleAlarmKick()
            return START_STICKY
        }

        driverId = auth.currentUser?.uid
        ownerId = AppPrefs.getOwnerId(this)

        if (driverId.isNullOrBlank() || ownerId.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        listenTrackingFlag()
        scheduleAlarmKick()
        startHeartbeat()

        return START_STICKY
    }

    override fun onDestroy() {
        stopTracking()

        trackingListener?.let {
            db.child("tracking_sessions")
                .child(ownerId!!)
                .removeEventListener(it)
        }

        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    // =========================
    // HEARTBEAT
    // =========================

    private fun startHeartbeat() {
        lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(2 * 60 * 1000L)
                renewWakeLock()
                scheduleAlarmKick()
            }
        }
    }

    // =========================
    // ALARM
    // =========================

    @SuppressLint("ScheduleExactAlarm", "MissingPermission")
    private fun scheduleAlarmKick() {
        val intent = Intent(this, LiveLocationService::class.java).apply {
            action = ALARM_ACTION
        }

        val pendingIntent = PendingIntent.getService(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + 10 * 60 * 1000L

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    // =========================
    // LOCATION
    // =========================

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (isTracking || !hasPermission()) return
        isTracking = true

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            4000L
        )
            .setMinUpdateDistanceMeters(5f)
            .setWaitForAccurateLocation(false)
            .build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )

        startForegroundUpdated("📍 Live Tracking")
    }

    private fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        fusedClient.removeLocationUpdates(locationCallback)
        startForegroundUpdated("⏸️ Standby")
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return

                // Check time BEFORE processing speed/validity to save CPU
                val now = System.currentTimeMillis()
                if (now - lastUploadTime < 3000) return

                if (isValidLocation(loc)) {
                    uploadLocation(loc) // Pass 'now' to avoid getting it twice
                }            }
        }
    }

    private fun uploadLocation(loc: android.location.Location) {
        val now = System.currentTimeMillis()
        if (now - lastUploadTime < 3000) return

        val speedKmh = calculateSpeed(loc) * 3.6f

        val payload = mapOf(
            "lat" to loc.latitude,
            "lng" to loc.longitude,
            "speed" to speedKmh,
            "bearing" to loc.bearing,
            "accuracy" to loc.accuracy,
            "sentAt" to ServerValue.TIMESTAMP
        )

        db.child("live_locations")
            .child(ownerId!!)
            .child(driverId!!)
            .setValue(payload)
            .addOnSuccessListener {
                lastUploadTime = now
                lastLocation = loc
                lastRealtimeNanos = loc.elapsedRealtimeNanos
            }
    }

    private fun calculateSpeed(loc: android.location.Location): Float {
        // 1. Prefer the GPS chip's speed if available (Doppler shift is accurate)
        if (loc.hasSpeed()) {
            lastComputedSpeed = loc.speed
            return loc.speed
        }

        // 2. Fallback to manual calculation
        val prev = lastLocation ?: return 0f
        val dtNs = loc.elapsedRealtimeNanos - lastRealtimeNanos
        if (dtNs <= 0) return lastComputedSpeed

        val dt = dtNs / 1_000_000_000f
        val distance = prev.distanceTo(loc)

        // Filter massive jumps or tiny movements to avoid noise
        if (distance < 2f || distance > 200f) return lastComputedSpeed

        val calc = distance / dt

        // Smooth the fallback calculation
        lastComputedSpeed = (lastComputedSpeed * 0.7f) + (calc * 0.3f)
        return lastComputedSpeed.coerceAtLeast(0f)
    }

    // =========================
    // FIREBASE FLAG
    // =========================

    private fun listenTrackingFlag() {
        trackingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val enabled = snapshot.getValue(Boolean::class.java) ?: false
                if (enabled == lastTrackingState) return
                lastTrackingState = enabled

                if (enabled) startTracking()
                else stopTracking()
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        db.child("tracking_sessions")
            .child(ownerId!!)
            .addValueEventListener(trackingListener!!)
    }

    // =========================
    // FOREGROUND NOTIFICATION
    // =========================

    private fun startForegroundUpdated(text: String) {
        val notification = buildNotification(text)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pi = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TrackMyAuto")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Live Location Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)          // 🔕 Disable sound
                enableVibration(false)        // 🔕 Disable vibration
                vibrationPattern = null
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(ch)
        }
    }

    // =========================
    // UTILS
    // =========================

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TrackMyAuto::LocationLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    private fun renewWakeLock() {
        wakeLock?.let {
            if (!it.isHeld) it.acquire(10 * 60 * 1000L)
        }
    }

    private fun hasPermission(): Boolean =
        ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun isValidLocation(loc: android.location.Location): Boolean =
        loc.accuracy <= 100f &&
                abs(loc.latitude) <= 90 &&
                abs(loc.longitude) <= 180
}
