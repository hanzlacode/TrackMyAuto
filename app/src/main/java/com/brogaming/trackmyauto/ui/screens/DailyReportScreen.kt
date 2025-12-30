package com.brogaming.trackmyauto.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.brogaming.trackmyauto.ui.viewmodel.DailyUiState
import com.brogaming.trackmyauto.ui.viewmodel.DailyViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DailyReportScreen(
    vm: DailyViewModel = hiltViewModel(),
//    onOpenDrawer: () -> Unit // Callback to open the drawer
) {
    val ui by vm.ui.collectAsState()

    var showSheet by remember { mutableStateOf(false) }
    var editingEntryVehicleId by remember { mutableStateOf<String?>(null) }

    // 1. SETUP SCROLL BEHAVIOR FOR ANIMATION
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(Unit) {
        vm.load(LocalDate.now().toString())
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), // 2. ATTACH SCROLL

        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Daily Report",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
//                    IconButton(onClick = onOpenDrawer) {
//                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
//                    }
                },
                actions = {
                    IconButton(onClick = { vm.load(LocalDate.now().toString()) }) {
                        Icon(Icons.Default.Today, contentDescription = "Go to Today")
                    }
                },
                scrollBehavior = scrollBehavior, // 3. LINK BEHAVIOR
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingEntryVehicleId = null
                    showSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Entry") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(8.dp))

            // DATE SELECTION
            DateSelector(
                date = ui.date,
                onPrevious = {
                    val newDate = LocalDate.parse(ui.date).minusDays(1)
                    vm.load(newDate.toString())
                },
                onNext = {
                    val newDate = LocalDate.parse(ui.date).plusDays(1)
                    vm.load(newDate.toString())
                },
                onToday = {
                    vm.load(LocalDate.now().toString())
                }
            )

            Spacer(Modifier.height(16.dp))

            // ANIMATED SUMMARY PANEL
            SummaryPanel(
                todayKm = ui.entries.sumOf { it.kmDriven },
                todayEarning = ui.entries.sumOf { it.earning },
                totalKm = ui.totalKm,
                totalEarning = ui.totalEarning
            )

            Spacer(Modifier.height(24.dp))

            if (ui.loading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            ui.error?.let {
                ErrorCard(it)
                Spacer(Modifier.height(10.dp))
            }

            // LIST OF ENTRIES
            Text(
                "Activity Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (ui.entries.isEmpty()) {
                EmptyState(
                    isToday = ui.date == LocalDate.now().toString(),
                    onAddClick = {
                        showSheet = true
                        editingEntryVehicleId = null
                    }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    items(ui.entries) { entry ->
                        val vehicle = ui.vehicles.find { it.vehicleId == entry.vehicleId }
                        EntryCard(
                            vehicleName = vehicle?.number ?: "Unknown",
                            kmStart = entry.kmStart,
                            kmEnd = entry.kmEnd,
                            earning = entry.earning,
                            isToday = ui.date == LocalDate.now().toString(),
                            onEdit = {
                                editingEntryVehicleId = entry.vehicleId
                                showSheet = true
                            }
                        )
                    }
                }
            }
        }

        if (showSheet) {
            AddOrEditEntrySheet(
                ui = ui,
                vm = vm,
                editingVehicleId = editingEntryVehicleId,
                onDismiss = { showSheet = false }
            )
        }
    }
}

/* ----------------------------------------------------------
   DATE SELECTOR
---------------------------------------------------------- */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateSelector(
    date: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(date, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (date == LocalDate.now().toString()) {
                Text(
                    "Today",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

/* ----------------------------------------------------------
   ANIMATED SUMMARY PANEL (ROLLING NUMBERS)
---------------------------------------------------------- */
@Composable
fun SummaryPanel(
    todayKm: Int,
    todayEarning: Int,
    totalKm: Int,
    totalEarning: Int
) {
    // Animation States
    val animTodayKm by animateIntAsState(targetValue = todayKm, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "km")
    val animTodayEarn by animateIntAsState(targetValue = todayEarning, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "earn")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Performance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                AnimatedStatItem("Today's KM", "$animTodayKm km", Icons.Default.Speed)
                // Assuming you use Indian Rupee symbol, otherwise use '$'
                AnimatedStatItem("Today's Earnings", "₹$animTodayEarn", Icons.Default.CurrencyRupee)
            }

            Spacer(Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                AnimatedStatItem("Total KM", "$totalKm km", Icons.Default.Map, isSmall = true)
                AnimatedStatItem("Total Earnings", "₹$totalEarning", Icons.Default.AccountBalanceWallet, isSmall = true)
            }
        }
    }
}

@Composable
fun AnimatedStatItem(label: String, value: String, icon: ImageVector, isSmall: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isSmall) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
        } else {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }

        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Text(
                value,
                fontSize = if (isSmall) 15.sp else 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/* ----------------------------------------------------------
   ENTRY CARD
---------------------------------------------------------- */
@Composable
fun EntryCard(
    vehicleName: String,
    kmStart: Int,
    kmEnd: Int,
    earning: Int,
    isToday: Boolean,
    onEdit: () -> Unit
) {
    val driven = kmEnd - kmStart

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp) // Slight shadow for depth
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(vehicleName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (isToday) {
                        Text("Today's entry", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("$kmStart → $kmEnd", fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("₹$earning", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/* ----------------------------------------------------------
   EMPTY & ERROR STATES
---------------------------------------------------------- */
@Composable
fun EmptyState(isToday: Boolean, onAddClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(40.dp))
        Icon(Icons.Default.NoteAdd, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("No entries found", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            if (isToday) "Start adding your today's report."
            else "No data found for this date.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add First Entry", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Error Loading Data", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

/* ----------------------------------------------------------
   ADD/EDIT ENTRY SHEET
---------------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditEntrySheet(
    ui: DailyUiState,
    vm: DailyViewModel,
    editingVehicleId: String?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {

        var selectedVehicle by remember { mutableStateOf("") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        var kmStart by remember { mutableStateOf("") }
        var kmEnd by remember { mutableStateOf("") }
        var earning by remember { mutableStateOf("") }

        var isUpdate by remember { mutableStateOf(false) }

        val today = ui.date

        LaunchedEffect(editingVehicleId) {
            if (editingVehicleId != null) {
                val v = ui.vehicles.find { it.vehicleId == editingVehicleId }
                selectedVehicle = v?.number ?: ""

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val entry = vm.loadEntry(editingVehicleId, today)
                    entry?.let {
                        kmStart = it.kmStart.toString()
                        kmEnd = it.kmEnd.toString()
                        earning = it.earning.toString()
                        isUpdate = true
                    }
                }
            }
        }

        Column(Modifier.padding(24.dp).padding(bottom = 24.dp)) {

            Text(if (isUpdate) "Edit Entry" else "New Entry", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))

            // Vehicle Dropdown
            ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = !dropdownExpanded }) {
                OutlinedTextField(
                    value = selectedVehicle,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Vehicle") },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                    ui.vehicles.forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v.number, fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedVehicle = v.number
                                dropdownExpanded = false
                                scope.launch {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val entry = vm.loadEntry(v.vehicleId, today)
                                        if (entry != null) {
                                            isUpdate = true
                                            kmStart = entry.kmStart.toString()
                                            kmEnd = entry.kmEnd.toString()
                                            earning = entry.earning.toString()
                                        } else {
                                            isUpdate = false
                                            kmStart = ""
                                            kmEnd = ""
                                            earning = ""
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = kmStart,
                    onValueChange = { kmStart = it },
                    label = { Text("Start KM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = kmEnd,
                    onValueChange = { kmEnd = it },
                    label = { Text("End KM") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = earning,
                onValueChange = { earning = it },
                label = { Text("Total Earnings (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                prefix = { Text("₹ ") }
            )

            Spacer(Modifier.height(32.dp))

            val vehicleId = ui.vehicles.find { it.number == selectedVehicle }?.vehicleId

            Button(
                onClick = {
                    if (vehicleId != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vm.saveDaily(
                                vehicleId,
                                kmStart.toIntOrNull() ?: 0,
                                kmEnd.toIntOrNull() ?: 0,
                                earning.toIntOrNull() ?: 0
                            )
                        }
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = vehicleId != null && kmStart.isNotBlank() && kmEnd.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isUpdate) "Update Report" else "Save Report", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}