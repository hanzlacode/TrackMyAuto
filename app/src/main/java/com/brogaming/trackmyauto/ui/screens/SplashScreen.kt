package com.brogaming.trackmyauto.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {

    // --- LOGIC (Unchanged) ---
    val auth = FirebaseAuth.getInstance()
    val isLoggedIn = auth.currentUser != null

    LaunchedEffect(Unit) {
        delay(900) // Preserved your specific delay

        if (isLoggedIn) {
            navController.navigate("Dashboard") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("auth") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }
    // --------------------------

    // --- UI (Matched to AuthScreen Theme) ---

    // Simple fade-in animation
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "fade"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // 1. Same Background Color (Primary)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            // 2. Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(alphaAnim)
            ) {
                // Same Icon Size (80dp) as AuthScreen
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Logo",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Same Typography (HeadlineLarge) as AuthScreen
                Text(
                    text = "TrackMyAuto",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // 3. Loader at Bottom (White/OnPrimary)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(32.dp) // Slightly adjusted for visibility
                )
            }
        }
    }
}