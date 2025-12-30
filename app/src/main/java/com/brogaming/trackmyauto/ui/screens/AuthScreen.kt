package com.brogaming.trackmyauto.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.brogaming.trackmyauto.ui.viewmodel.AuthState
import com.brogaming.trackmyauto.ui.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    navController: NavController,
    onGoogleSignIn: () -> Unit,
    vm: AuthViewModel
) {
    Log.d("AuthDebug", "AuthScreen Composed")

    val state by vm.state.collectAsState()

    Log.d("AuthDebug", "AuthScreen STATE = $state")

    // Navigate when SUCCESS (Logic Unchanged)
    LaunchedEffect(state) {
        if (state == AuthState.Success) {
            Log.d("AuthDebug", "NAV: navigating to Dashboard")
            navController.navigate("Dashboard") {
                popUpTo("auth") { inclusive = true }
            }
        }
    }

    // UI State Handling (Logic Unchanged)
    when (state) {
        AuthState.Idle -> {
            Log.d("AuthDebug", "UI: Idle")
            AuthUI(
                onGoogleSignIn = {
                    Log.d("AuthDebug", "UI Button Click → google launcher")
                    onGoogleSignIn()
                },
                showError = false
            )
        }

        AuthState.Loading -> {
            Log.d("AuthDebug", "UI: Loading Screen")
            FullScreenLoader()
        }

        AuthState.Error -> {
            Log.d("AuthDebug", "UI: Error Screen")
            AuthUI(
                onGoogleSignIn = {

                    Log.d("AuthDebug", "UI Button Click retry → google launcher")
                    onGoogleSignIn()
                },
                showError = true
            )
        }

        AuthState.Success -> {
            Log.d("AuthDebug", "UI: Success Screen block recomposition")
            FullScreenLoader()
        }
    }
}

@Composable
fun FullScreenLoader() {
    // UPDATED: Matches Splash Screen (Primary Background)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                strokeWidth = 4.dp,
                modifier = Modifier.size(50.dp),
                color = MaterialTheme.colorScheme.onPrimary // White loader
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Securing Connection...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun AuthUI(
    onGoogleSignIn: () -> Unit,
    showError: Boolean
) {
    // UPDATED: Matches Splash Screen (Primary Background)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // SECTION 1: Top Spacer
            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 2: Hero / Brand Area (Clean White on Color)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon directly on background for cleaner look
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Logo",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "TrackMyAuto",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Manage your vehicle effortlessly",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }

            // SECTION 3: Action Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showError) {
                    // White Card for Error to pop against Blue/Primary background
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in failed. Please try again.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // INVERTED BUTTON: White button with Primary Text
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary, // White Button
                        contentColor = MaterialTheme.colorScheme.primary      // Colored Text
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Footer Text (White/Transparent)
                Text(
                    text = "By continuing you accept our Terms & Policy.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}