package com.brogaming.trackmyauto.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.brogaming.trackmyauto.data.storage.AppPrefs
import com.brogaming.trackmyauto.ui.viewmodel.DriverViewModel

@Composable
fun DriverDashboardScreen(
    context: Context,
    onStartLiveTracking: () -> Unit,
    onStopLiveTracking: () -> Unit,
    vm: DriverViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current

//    // Store linked owner ID
//    LaunchedEffect(state.linkedOwner) {
//        if (!state.linkedOwner.isNullOrBlank()) {
//            AppPrefs.setOwnerId(context, state.linkedOwner!!)
//        }
//    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- HEADER ---
            Text(
                "Driver Console",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Manage your connection status",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(30.dp))

            if (state.loading) {
                CircularProgressIndicator()
                return@Column
            }

            state.error?.let { err ->
                errorCard(err)
                Spacer(Modifier.height(20.dp))
            }

            // --- 1. DIGITAL ID CARD ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "MY DRIVER CODE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = state.driverCode.chunked(3).joinToString(" "), // Format: ABC 123
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DriverActionButton(
                            text = "Copy",
                            icon = Icons.Default.ContentCopy,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(state.driverCode))
                                Toast.makeText(context, "Code Copied!", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DriverActionButton(
                            text = "Share",
                            icon = Icons.Default.Share,
                            onClick = {
                                shareDriverCode(context, state.driverCode)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- 2. CONNECTION STATUS ---
            Text(
                "Fleet Connection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(12.dp))

            val isLinked = !state.linkedOwner.isNullOrBlank()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = if (isLinked) BorderStroke(1.dp, Color(0xFF10B981)) else null
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isLinked) Color(0xFF10B981).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isLinked) Icons.Default.Link else Icons.Default.LinkOff,
                                contentDescription = null,
                                tint = if (isLinked) Color(0xFF10B981) else Color.Red
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column {
                            Text(
                                if (isLinked) "Linked Successfully" else "Not Linked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isLinked) "Data is syncing with owner" else "Share code with owner to link",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    if (isLinked) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = Color.LightGray.copy(alpha = 0.3f)
                        )

                        InfoRow(Icons.Default.DirectionsCar, "Vehicle Name", state.type ?: "Unknown")
                        Spacer(Modifier.height(8.dp))
                        InfoRow(Icons.Default.DirectionsCar, "Vehicle Number", state.number ?: "Unknown")
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

// --- HELPER FUNCTION FOR SHARING ---
private fun shareDriverCode(context: Context, code: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Hello! Here is my Driver Code to link with vehicle: $code")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Driver Code via")
    context.startActivity(shareIntent)
}

// --- SUB-COMPONENTS ---

@Composable
fun DriverActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.2f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun errorCard(error: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}