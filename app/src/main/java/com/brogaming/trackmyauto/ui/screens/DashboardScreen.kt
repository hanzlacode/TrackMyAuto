package com.brogaming.trackmyauto.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.brogaming.trackmyauto.ui.viewmodel.DailyViewModel
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    vm: DailyViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsState()
    val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        vm.load(LocalDate.now().toString())
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) { padding ->

        // Root Column: Splits screen into Top (Header) and Bottom (Map)
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
            // NO SCROLL STATE HERE
        ) {

            // --- SECTION 1: HEADER & STATS (Fixed Height Area) ---
            Box(
                modifier = Modifier.wrapContentHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                // A. Blue Background Layer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Adjust this to control how much blue is visible
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                        )
                )

                // B. Foreground Content (AppBar + Stats Card)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 22.dp)
                ) {
                    // App Bar
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Dashboard",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // Floating Stats Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 24.dp, horizontal = 16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DashboardStatItem(
                                title = "Total Earnings",
                                value = "₹${ui.totalEarning}",
                                icon = Icons.Outlined.Payments,
                                color = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )

                            Divider(
                                modifier = Modifier
                                    .height(50.dp)
                                    .width(1.dp),
                                color = Color.LightGray.copy(alpha = 0.5f)
                            )

                            DashboardStatItem(
                                title = "Total KM Driven",
                                value = "${ui.totalKm} km",
                                icon = Icons.Outlined.DirectionsCar,
                                color = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- SECTION 2: MAP AREA (Fills Remaining Space) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // <--- THIS MAKES IT FILL THE REST OF THE SCREEN
                    .padding(horizontal = 22.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
            ) {
                // Title Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Live Fleet View",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color.Red, CircleShape))
                }

                Spacer(Modifier.height(12.dp))

                // Map Card (Fills the Column)
                Card(
                    modifier = Modifier
                        .fillMaxSize(), // <--- Map takes all available space in this column
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(Modifier.fillMaxSize()) {
                        if (ownerId.isNotEmpty()) {
                            OwnerLiveTrackingMapLibreScreen()
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("GPS Signal Not Available", color = Color.Gray)
                            }
                        }

                        // Focus Button (Uncommented as requested usually for maps)
//                        FilledIconButton(
//                            onClick = { /* TODO: Trigger Map Focus */ },
//                            containerColor = MaterialTheme.colorScheme.primary,
//                            modifier = Modifier
//                                .align(Alignment.BottomEnd)
//                                .padding(16.dp)
//                        ) {
//                            Icon(
//                                Icons.Outlined.MyLocation,
//                                contentDescription = "Focus",
//                                tint = MaterialTheme.colorScheme.onPrimary
//                            )
//                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatItem(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            value,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            title,
            color = Color.Gray,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}