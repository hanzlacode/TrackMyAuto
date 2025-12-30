package com.brogaming.trackmyauto.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brogaming.trackmyauto.ui.viewmodel.DailyViewModel
import com.brogaming.trackmyauto.ui.viewmodel.VehicleViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import java.time.LocalDate
import kotlin.math.abs

// --- CONFIGURATION ---
const val OSM_RASTER_STYLE = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "&copy; OpenStreetMap Contributors"
    }
  },
  "layers": [
    {
      "id": "osm-tiles",
      "type": "raster",
      "source": "osm"
    }
  ]
}
"""

data class DriverLocation(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val speed: Double = 0.0,
    val bearing: Double = 0.0,
    val sentAt: Long = 0L
)

// FIX: More accurate relative time
fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < -1000 -> "Syncing..."
        diff < 5000 -> "Now"
        diff < 60_000 -> "${diff / 1000}s"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> "${diff / 86_400_000}d"
    }
}

fun getVehicleIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "bike", "motorcycle", "scooter" -> Icons.Default.DirectionsBike
        "truck" -> Icons.Default.LocalShipping
        else -> Icons.Default.DirectionsCar
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OwnerLiveTrackingMapLibreScreen(
    dailyVM: DailyViewModel = hiltViewModel(),
    vehicleVM: VehicleViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // --- OWNER ID RESOLVER ---
    var resolvedOwnerId by remember { mutableStateOf<String?>(null) }
    var isLoadingId by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            errorMessage = "Not authenticated"
            isLoadingId = false
            return@LaunchedEffect
        }

        val fs = FirebaseFirestore.getInstance()
        try {
            // Check if Driver linked to Owner
            val driverDoc = fs.collection("drivers").document(uid).get().await()
            if (driverDoc.exists()) {
                val linked = driverDoc.getString("linkedOwner")
                if (!linked.isNullOrBlank()) {
                    resolvedOwnerId = linked
                }
            }

            // If not found, check if Self is Owner
            if (resolvedOwnerId == null) {
                val ownerDoc = fs.collection("owners").document(uid).get().await()
                if (ownerDoc.exists()) {
                    resolvedOwnerId = uid
                }
            }

            if (resolvedOwnerId == null) {
                errorMessage = "User not linked to any fleet"
            }
        } catch (e: Exception) {
            errorMessage = "Error: ${e.message}"
        }
        isLoadingId = false
    }

    // --- LOADING STATE ---
    if (isLoadingId) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Loading fleet...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    // --- ERROR STATE ---
    if (errorMessage != null || resolvedOwnerId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        return
    }

    val ownerId = resolvedOwnerId!!

    // --- DATA SOURCES ---
    val dailyState by dailyVM.ui.collectAsState()
    val vehicleState by vehicleVM.uiState.collectAsState()

    // --- LIVE LOCATION STATE ---
    var driverLocations by remember { mutableStateOf<Map<String, DriverLocation>>(emptyMap()) }
    var selectedVehicleId by remember { mutableStateOf<String?>(null) }

    // --- FLAGS & CONTROLS ---
    var hasZoomedToInitialLocation by remember { mutableStateOf(false) }
    var isCameraLocked by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var showDriverSheet by remember { mutableStateOf(false) }
    var zoomAnimationEndTime by remember { mutableLongStateOf(0L) }
    var lastCameraPosition by remember { mutableStateOf<LatLng?>(null) }

    val sheetState = rememberModalBottomSheetState()

    // --- MAP OBJECTS ---
    val mapView = remember { MapView(ctx) }
    var symbolManager by remember { mutableStateOf<SymbolManager?>(null) }
    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }

    // FIX: Cache bitmaps to avoid recreating them every frame
    val bitmapCache = remember { mutableMapOf<String, Bitmap>() }

    // --- OPTIMIZED BITMAP GENERATOR ---
    fun generateVehicleBitmap(text: String): Bitmap {
        val cacheKey = text
        bitmapCache[cacheKey]?.let { return it }

        val width = 150
        val height = 150
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        // Draw Arrow
        val pathBorder = Path()
        pathBorder.moveTo(75f, 20f)
        pathBorder.lineTo(30f, 110f)
        pathBorder.lineTo(75f, 90f)
        pathBorder.lineTo(120f, 110f)
        pathBorder.close()

        paint.color = AndroidColor.DKGRAY
        paint.setShadowLayer(10f, 0f, 5f, AndroidColor.DKGRAY)
        canvas.drawPath(pathBorder, paint)
        paint.clearShadowLayer()

        paint.color = AndroidColor.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawPath(pathBorder, paint)

        val pathFill = Path()
        pathFill.moveTo(75f, 35f)
        pathFill.lineTo(45f, 100f)
        pathFill.lineTo(75f, 85f)
        pathFill.lineTo(105f, 100f)
        pathFill.close()
        paint.color = AndroidColor.parseColor("#4285F4")
        canvas.drawPath(pathFill, paint)

        // Draw Text Label
        if (text.isNotEmpty()) {
            val textPaint = Paint().apply {
                color = AndroidColor.BLACK
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                style = Paint.Style.FILL
                setShadowLayer(3f, 0f, 0f, AndroidColor.WHITE)
            }
            canvas.drawText(text, 75f, 140f, textPaint)
        }

        bitmapCache[cacheKey] = bitmap
        return bitmap
    }

    // --- PRESENCE SYSTEM ---
    DisposableEffect(ownerId, lifecycleOwner) {
        val db = FirebaseDatabase.getInstance()
        val sessionRef = db.reference.child("tracking_sessions").child(ownerId)
        sessionRef.onDisconnect().setValue(false)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> sessionRef.setValue(true)
                Lifecycle.Event.ON_PAUSE -> sessionRef.setValue(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            sessionRef.setValue(false)
        }
    }

    LaunchedEffect(Unit) {
        vehicleVM.observeVehicles()
    }

    // --- MAP SETUP ---
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        mapView.onCreate(null)
        mapView.getMapAsync { mapLibre ->
            mapInstance = mapLibre
            mapLibre.uiSettings.apply {
                isLogoEnabled = false
                isAttributionEnabled = false
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = false
            }

            mapLibre.addOnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                    isCameraLocked = false
                }
            }

            mapLibre.setStyle(Style.Builder().fromJson(OSM_RASTER_STYLE)) { style ->
                val manager = SymbolManager(mapView, mapLibre, style)
                manager.iconAllowOverlap = true
                manager.iconIgnorePlacement = true

                manager.addClickListener { symbol ->
                    val vId = symbol.data?.asJsonObject?.get("vehicleId")?.asString
                    if (vId != null) {
                        selectedVehicleId = vId
                        isCameraLocked = true

                        val latLng = symbol.latLng
                        zoomAnimationEndTime = System.currentTimeMillis() + 2500
                        mapLibre.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0), 2000)
                    }
                    true
                }
                symbolManager = manager
            }
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // FIX: Live Location Listener with proper cleanup
    DisposableEffect(ownerId) {
        val db = FirebaseDatabase.getInstance()
        val ref = db.reference.child("live_locations").child(ownerId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    driverLocations = emptyMap()
                    return
                }
                val newMap = mutableMapOf<String, DriverLocation>()
                snapshot.children.forEach { child ->
                    val loc = child.getValue(DriverLocation::class.java)
                    if (loc != null && loc.lat != 0.0 && loc.lng != 0.0) {
                        newMap[child.key ?: "unknown"] = loc
                    }
                }
                driverLocations = newMap
            }
            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }

        ref.addValueEventListener(listener)

        onDispose {
            ref.removeEventListener(listener)
        }
    }

    // Sync Action
    fun performSync() {
        scope.launch {
            isSyncing = true
            dailyVM.load(LocalDate.now().toString())
            isCameraLocked = true
            zoomAnimationEndTime = System.currentTimeMillis() + 2500
            delay(800)
            isSyncing = false
        }
    }

    // Lookup active info
    val activeDriverLoc = selectedVehicleId?.let { vId ->
        val vehicle = vehicleState.vehicles.find { it.id == vId }
        vehicle?.driverId?.let { dId -> driverLocations[dId] }
    } ?: run {
        val firstLinkedKey = driverLocations.keys.firstOrNull { key ->
            vehicleState.vehicles.any { it.driverId == key }
        }
        firstLinkedKey?.let { driverLocations[it] }
    }

    val activeVehicleInfo = selectedVehicleId?.let { vId ->
        vehicleState.vehicles.find { it.id == vId }
    } ?: run {
        val firstLinkedKey = driverLocations.keys.firstOrNull { key ->
            vehicleState.vehicles.any { it.driverId == key }
        }
        firstLinkedKey?.let { key ->
            vehicleState.vehicles.find { it.driverId == key }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- OPTIMIZED MAP RENDER ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { _ ->
                val manager = symbolManager ?: return@AndroidView
                val map = mapInstance ?: return@AndroidView
                val style = map.style ?: return@AndroidView

                manager.deleteAll()

                var validLocationFound = false
                var targetLocation: LatLng? = null

                driverLocations.forEach { (driverAuthId, loc) ->
                    if (loc.lat == 0.0 && loc.lng == 0.0) return@forEach

                    val matchedVehicle = vehicleState.vehicles.find {
                        it.driverId?.trim() == driverAuthId.trim()
                    } ?: return@forEach

                    validLocationFound = true

                    val latLng = LatLng(loc.lat, loc.lng)
                    val isSelected = (matchedVehicle.id == selectedVehicleId)
                    val labelText = matchedVehicle.number

                    if (isSelected) targetLocation = latLng

                    val imageId = "v_icon_${matchedVehicle.id}"

                    // FIX: Only add image if not already in style
                    if (style.getImage(imageId) == null) {
                        val bitmap = generateVehicleBitmap(labelText)
                        style.addImage(imageId, bitmap)
                    }

                    val data = JsonObject()
                    data.addProperty("vehicleId", matchedVehicle.id)

                    manager.create(
                        SymbolOptions()
                            .withLatLng(latLng)
                            .withIconImage(imageId)
                            .withIconSize(if (isSelected) 1.0f else 0.7f)
                            .withIconRotate(loc.bearing.toFloat())
                            .withData(data)
                    )
                }

                // --- OPTIMIZED CAMERA LOGIC ---
                if (validLocationFound && !hasZoomedToInitialLocation && isCameraLocked) {
                    val firstLinkedKey = driverLocations.keys.firstOrNull { key ->
                        vehicleState.vehicles.any { it.driverId == key }
                    }
                    val firstLoc = firstLinkedKey?.let { driverLocations[it] }

                    if (firstLoc != null) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(firstLoc.lat, firstLoc.lng), 14.0),
                            1500
                        )
                        hasZoomedToInitialLocation = true
                        selectedVehicleId = vehicleState.vehicles.find { it.driverId == firstLinkedKey }?.id
                    }
                }

                // FIX: Only update camera if position changed significantly (>10 meters)
                if (isCameraLocked && targetLocation != null) {
                    val shouldUpdate = lastCameraPosition?.let { last ->
                        val distance = distanceBetween(last, targetLocation)
                        distance > 10.0 // Only update if moved more than 10 meters
                    } ?: true

                    if (shouldUpdate && System.currentTimeMillis() > zoomAnimationEndTime) {
                        map.animateCamera(CameraUpdateFactory.newLatLng(targetLocation), 500)
                        lastCameraPosition = targetLocation
                    }
                }
            }
        )

        // --- STATUS BAR ---
        val linkedActiveDriversCount = driverLocations.keys.count { key ->
            vehicleState.vehicles.any { it.driverId == key }
        }

        if (linkedActiveDriversCount == 0 && !isLoadingId) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "No active vehicles",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // --- TOP BUTTONS ---
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { performSync() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Sync, "Sync")
                }
            }
        }

        // --- BOTTOM BUTTONS ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 230.dp, start = 20.dp)
        ) {
            FloatingActionButton(
                onClick = { showDriverSheet = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape
            ) {
                Icon(Icons.Default.List, "Fleet")
            }
        }

        // RECENTER BUTTON
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 230.dp, end = 20.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    isCameraLocked = true

                    val targetLoc = selectedVehicleId?.let { vId ->
                        val vehicle = vehicleState.vehicles.find { it.id == vId }
                        vehicle?.driverId?.let { dId -> driverLocations[dId] }
                    } ?: run {
                        val firstLinkedKey = driverLocations.keys.firstOrNull { key ->
                            vehicleState.vehicles.any { it.driverId == key }
                        }
                        firstLinkedKey?.let { driverLocations[it] }
                    }

                    if (targetLoc != null && targetLoc.lat != 0.0) {
                        val pos = LatLng(targetLoc.lat, targetLoc.lng)
                        zoomAnimationEndTime = System.currentTimeMillis() + 2500
                        mapInstance?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(pos, 14.0),
                            2000
                        )
                        lastCameraPosition = pos
                    }
                },
                containerColor = if (isCameraLocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                contentColor = if (isCameraLocked) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                shape = CircleShape
            ) {
                Icon(
                    if (isCameraLocked) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                    "Recenter"
                )
            }
        }

        // --- HUD ---
        if (activeDriverLoc != null && activeDriverLoc.lat != 0.0 && activeVehicleInfo != null) {
            val todayEarnings = dailyState.entries
                .filter { it.vehicleId == selectedVehicleId }
                .sumOf { it.earning }
            val todayKm = dailyState.entries
                .filter { it.vehicleId == selectedVehicleId }
                .sumOf { it.kmDriven }
            ModernVehicleHud(
                activeVehicleInfo.number,
                activeVehicleInfo.type,
                activeDriverLoc,
                todayEarnings,
                todayKm
            )
        }

        // --- FLEET LIST ---
        if (showDriverSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDriverSheet = false },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        "Active Fleet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    val linkedDrivers = driverLocations.keys.filter { key ->
                        vehicleState.vehicles.any { it.driverId == key }
                    }

                    if (linkedDrivers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No active vehicles linked",
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        LazyColumn {
                            items(linkedDrivers) { driverIdKey ->
                                val vInfo = vehicleState.vehicles.find { it.driverId == driverIdKey }
                                if (vInfo != null) {
                                    val loc = driverLocations[driverIdKey]
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                vInfo.number,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        supportingContent = {
                                            Column {
                                                Text(vInfo.type)
                                                if (loc != null) {
                                                    Text(
                                                        "Updated ${getRelativeTime(loc.sentAt)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                        },
                                        leadingContent = {
                                            Icon(
                                                getVehicleIcon(vInfo.type),
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            selectedVehicleId = vInfo.id
                                            isCameraLocked = true

                                            if (loc != null && loc.lat != 0.0) {
                                                val pos = LatLng(loc.lat, loc.lng)
                                                zoomAnimationEndTime = System.currentTimeMillis() + 2500
                                                mapInstance?.animateCamera(
                                                    CameraUpdateFactory.newLatLngZoom(pos, 14.0),
                                                    2000
                                                )
                                                lastCameraPosition = pos
                                            }
                                            showDriverSheet = false
                                        }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper function to calculate distance between two LatLng points
private fun distanceBetween(start: LatLng, end: LatLng): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(end.latitude - start.latitude)
    val dLng = Math.toRadians(end.longitude - start.longitude)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(start.latitude)) * Math.cos(Math.toRadians(end.latitude)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}

@Composable
fun BoxScope.ModernVehicleHud(
    vehicleName: String,
    vehicleType: String,
    driverLoc: DriverLocation,
    todayEarnings: Int,
    todayKm: Int
) {
    val kmh = (driverLoc.speed * 3.6).toInt()
    val syncStatus = getRelativeTime(driverLoc.sentAt)
    val icon = getVehicleIcon(vehicleType)

    Card(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(
                elevation = 16.dp,
                spotColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            vehicleName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            vehicleType,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingDot()
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Live • $syncStatus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$kmh",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 32.sp
                    )
                    Text(
                        "km/h",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    Icons.Outlined.AttachMoney,
                    "Earned",
                    "₹$todayEarnings",
                    MaterialTheme.colorScheme.secondary
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                StatItem(
                    Icons.Outlined.Speed,
                    "Driven",
                    "$todayKm km",
                    MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFF22C55E).copy(alpha = alpha))
    )
}