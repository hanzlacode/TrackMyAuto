package com.brogaming.trackmyauto

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.brogaming.trackmyauto.data.storage.AppPrefs
import com.brogaming.trackmyauto.service.LiveLocationService
import com.brogaming.trackmyauto.ui.components.DrawerMenu
import com.brogaming.trackmyauto.ui.screens.*
import com.brogaming.trackmyauto.ui.viewmodel.AuthViewModel
import com.brogaming.trackmyauto.ui.viewmodel.DriverViewModel
import com.brogaming.trackmyauto.ui.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    // Google Sign-In launcher
    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { authViewModel.onGoogleCredential(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestGps()
        requestIgnoreBatteryOptimizations()

        setContent {
            TrackMyAutoApp(
                context = this,
                onGoogleSignIn = { launchGoogleSignIn() }
            )
        }
    }

    private fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        googleLauncher.launch(client.signInIntent)
    }

    // =========================
    // SYSTEM CHECKS
    // =========================

    private fun checkAndRequestGps() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!enabled) {
            AlertDialog.Builder(this)
                .setTitle("Enable GPS")
                .setMessage("Location is required for live tracking.")
                .setCancelable(false)
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(android.net.Uri.parse("package:$packageName"))
                )
            }
        }
    }
}

// =========================
// COMPOSE ROOT
// =========================

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TrackMyAutoApp(
    context: Context,
    onGoogleSignIn: () -> Unit
) {
    val navController = rememberNavController()

    val mainVM: MainViewModel = hiltViewModel()
    val driverVM: DriverViewModel = hiltViewModel()
    val authVM: AuthViewModel = hiltViewModel()
    val driverState by driverVM.state.collectAsState()

    // =========================
    // PERMISSION HANDLER
    // =========================

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (granted) {
                maybeStartLiveLocationService(context)
            }
        }

    // =========================
    // OWNER ID → START SERVICE (FIXED)
    // =========================

    LaunchedEffect(driverState.linkedOwner) {
        if (!driverState.linkedOwner.isNullOrBlank()) {
            AppPrefs.setOwnerId(context, driverState.linkedOwner!!)
            maybeStartLiveLocationService(context)
        }
    }

    // =========================
// PERMISSION LAUNCHERS (FIX)
// =========================

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* no-op */ }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val locationGranted =
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        result[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (locationGranted) {
                maybeStartLiveLocationService(context)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasNotificationPermission =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                    if (!hasNotificationPermission) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }
            }
        }


    LaunchedEffect(Unit) {

        val hasLocationPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return@LaunchedEffect
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    // =========================
    // NAVIGATION
    // =========================

    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("auth") {
            AuthScreen(
                navController = navController,
                onGoogleSignIn = onGoogleSignIn,
                vm = authVM
            )
        }

        drawerComposable("Dashboard", mainVM, navController) {
            DashboardScreen()
        }

        drawerComposable("driver", mainVM, navController) {
            DriverDashboardScreen(
                context = context,
                onStartLiveTracking = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                onStopLiveTracking = {
                    context.stopService(
                        Intent(context, LiveLocationService::class.java)
                    )
                }
            )
        }

        drawerComposable("vehicles", mainVM, navController) {
            VehiclesScreen()
        }

        drawerComposable("DailyReport", mainVM, navController) {
            DailyReportScreen()
        }

        drawerComposable("live_tracking", mainVM, navController) {
            OwnerLiveTrackingMapLibreScreen()
        }
    }
}

// =========================
// HELPERS
// =========================

fun maybeStartLiveLocationService(context: Context) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid ?: return

    val ownerId = AppPrefs.getOwnerId(context)
    if (ownerId.isNullOrBlank()) return

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) return

    val intent = Intent(context, LiveLocationService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ContextCompat.startForegroundService(context, intent)
    } else {
        context.startService(intent)
    }

    Log.d("ServiceDebug", "LiveLocationService started")
}

@RequiresApi(Build.VERSION_CODES.O)
fun NavGraphBuilder.drawerComposable(
    route: String,
    vm: MainViewModel,
    nav: NavHostController,
    content: @Composable () -> Unit
) {
    composable(route) {
        DrawerMenu(vm = vm, nav = nav) {
            content()
        }
    }
}
