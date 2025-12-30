package com.brogaming.trackmyauto.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.brogaming.trackmyauto.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private val EDGE_DRAG_WIDTH = 32.dp

@Composable
fun DrawerMenu(
    vm: MainViewModel,
    nav: NavController,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Get current route to hide button on screens that have their own TopAppBar
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Logic: Show button only if Drawer is CLOSED AND we are NOT on specific screens
    // (Add "DailyReport" here if you used the specific TopAppBar design I gave you earlier)
    val showFloatingButton = drawerState.isClosed

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen, // Only swipe-to-close allowed
        scrimColor = Color.Black.copy(alpha = 0.5f),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 24.dp, horizontal = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactDrawerHeader()
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        NavigationItems(vm, nav)
                    }
                    LogoutItem(vm, nav)
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // A. The Map / Screen Content
            content()

            // B. ANIMATED FLOATING HAMBURGER BUTTON
            AnimatedVisibility(
                visible = showFloatingButton,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp),
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200))
            ) {
                FloatingMenuButton(
                    onClick = { scope.launch { drawerState.open() } }
                )
            }

            // C. Edge Drag Detector (Invisible strip)
            if (!drawerState.isOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(EDGE_DRAG_WIDTH)
                        .align(Alignment.CenterStart)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (dragAmount > 10) { // Dragged right
                                    scope.launch { drawerState.open() }
                                }
                            }
                        }
                )
            }
        }
    }
}

// --- FLOATING BUTTON COMPONENT ---
@Composable
fun FloatingMenuButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 6.dp,
        modifier = modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Menu",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* --------------------------------------------------
   SUB-COMPONENTS (Unchanged)
-------------------------------------------------- */

@Composable
private fun CompactDrawerHeader() {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Track My Auto",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Text(
                text = "Fleet Manager",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NavigationItems(vm: MainViewModel, nav: NavController) {
    val items = listOf(
        DrawerItem("Dashboard", Icons.Default.Dashboard, "Dashboard"),
        DrawerItem("Live Map", Icons.Default.Map, "live_tracking"),
        DrawerItem("Vehicles", Icons.Default.DirectionsCar, "vehicles"),
        DrawerItem("Driver Mode", Icons.Default.AirlineSeatReclineNormal, "driver"),
        DrawerItem("Reports", Icons.Default.BarChart, "DailyReport"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            CompactDrawerItem(
                item = item,
                onClick = { nav.navigate(item.route) }
            )
        }
    }
}

@Composable
private fun CompactDrawerItem(
    item: DrawerItem,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(item.title) },
        icon = { Icon(item.icon, null) },
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = Color.Transparent
        )
    )
}

@Composable
private fun LogoutItem(vm: MainViewModel, nav: NavController) {
    Button(
        onClick = {
            vm.logout()
            nav.navigate("auth") { popUpTo("home") { inclusive = true } }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.ExitToApp, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Logout")
    }
}

private data class DrawerItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)