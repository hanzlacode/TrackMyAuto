package com.brogaming.trackmyauto.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.brogaming.trackmyauto.data.model.Vehicle
import com.brogaming.trackmyauto.data.repo.DriverRepo
import com.brogaming.trackmyauto.ui.viewmodel.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(vm: VehicleViewModel = hiltViewModel()) {
    val ui by vm.uiState.collectAsState()
    val background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar( // Changed to CenterAligned specific component
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Vehicles",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Manage vehicles & drivers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.showAddDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Vehicle")
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(background)
        ) {
            when {
                ui.loading && ui.vehicles.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                ui.vehicles.isEmpty() -> {
                    VehiclesEmptyState(
                        error = ui.error,
                        onAddClick = { vm.showAddDialog(true) }
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {

                        ui.error?.let { err ->
                            VehiclesErrorBanner(err)
                            Spacer(Modifier.height(12.dp))
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(ui.vehicles, key = { it.id }) { vehicle ->
                                VehicleRow(
                                    vehicle = vehicle,
                                    expanded = (ui.expandedVehicleId == vehicle.id),
                                    onClick = { vm.toggleExpand(vehicle.id) },
                                    fetchDriver = { driverId -> vm.getDriverInfo(driverId) },
                                    onLinkDriverClicked = { vm.showLinkDialog(vehicle) },
                                    onUnlink = { vehicleId -> vm.unlinkDriver(vehicleId) }, // Correctly wired
                                    onEdit = { vm.showEditDialog(vehicle) },
                                    onDelete = { vm.showDeleteDialog(vehicle) }
                                )
                            }
                        }
                    }
                }
            }

            // --- Dialogs ---

            if (ui.showingAddDialog) {
                AddVehicleDialog(
                    initialNumber = "",
                    initialType = "Auto",
                    onDismiss = { vm.showAddDialog(false) },
                    onSave = { number, type, driverCode ->
                        vm.addVehicleAndMaybeLink(
                            number, type, driverCode,
                            onSuccess = { /* optional */ },
                            onError = { /* optional */ }
                        )
                    },
                    loading = ui.loading
                )
            }

            ui.editingVehicle?.let { veh ->
                EditVehicleDialog(
                    vehicle = veh,
                    onDismiss = { vm.closeEditDialog() },
                    onSave = { number, type -> vm.updateVehicle(veh.id, number, type) }
                )
            }

            ui.deletingVehicle?.let { veh ->
                DeleteVehicleDialog(
                    vehicle = veh,
                    onDismiss = { vm.closeDeleteDialog() },
                    onConfirm = { vm.deleteVehicle(veh.id) }
                )
            }

            ui.showingLinkDialogFor?.let { veh ->
                LinkDriverDialog(
                    vehicle = veh,
                    onDismiss = { vm.closeLinkDialog() },
                    onConfirm = { driverCode ->
                        vm.linkDriverToVehicle(veh.id, driverCode, veh.type, veh.number)
                    },
                    loading = ui.loading
                )
            }
        }
    }
}

/* ----------------------------------------------------------
   LIST STATES & ROWS
---------------------------------------------------------- */

@Composable
private fun VehicleRow(
    vehicle: Vehicle,
    expanded: Boolean,
    onClick: () -> Unit,
    fetchDriver: suspend (String) -> DriverRepo.LinkedDriver?,
    onLinkDriverClicked: (String) -> Unit,
    onUnlink: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Header (Always Visible)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vehicle.number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = vehicle.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    val driverId = vehicle.driverId

                    if (driverId.isNullOrBlank()) {
                        // NO DRIVER LINKED
                        Text(
                            text = "No driver linked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onLinkDriverClicked(vehicle.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Text("Link Driver")
                            }
                            OutlinedButton(onClick = onEdit) { Text("Edit") }
                            TextButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete")
                            }
                        }
                    } else {
                        // DRIVER LINKED
                        val driverInfo by produceState<DriverRepo.LinkedDriver?>(initialValue = null, key1 = driverId) {
                            value = try { fetchDriver(driverId) } catch (_: Exception) { null }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Driver Info Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = driverInfo?.type ?: "Active Driver",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "Code: ${driverInfo?.driverCode ?: "Loading..."}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Actions for Linked Driver
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onUnlink(vehicle.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text("Unlink Driver")
                            }
                            TextButton(onClick = onEdit) { Text("Edit Vehicle") }
                        }
                    }
                }
            }
        }
    }
}

// ... [VehiclesEmptyState, VehiclesErrorBanner, and Dialogs remain unchanged] ...
// (I didn't repeat the Dialog code blocks to save space, but they are correct in your original snippet)

/* ----------------------------------------------------------
   LIST STATES
---------------------------------------------------------- */

@Composable
private fun VehiclesEmptyState(
    error: String?,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (error != null) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Couldn’t load vehicles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No vehicles yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Add your first vehicle to start tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }

        OutlinedButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Vehicle")
        }
    }
}

@Composable
private fun VehiclesErrorBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "Something went wrong",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/* ----------------------------------------------------------
   VEHICLE ROW
---------------------------------------------------------- */

//@Composable
//private fun VehicleRow(
//    vehicle: Vehicle,
//    expanded: Boolean,
//    onClick: () -> Unit,
//    fetchDriver: suspend (String) -> DriverRepo.LinkedDriver?,
//    onLinkDriverClicked: (String) -> Unit,
//    onUnlink: (String) -> Unit, // <--- ADD THIS PARAMETER
//    onEdit: () -> Unit,
//    onDelete: () -> Unit
//){
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() },
//        shape = RoundedCornerShape(14.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//    ) {
//        Column(modifier = Modifier.padding(12.dp)) {
//
//            // Header
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Box(
//                    modifier = Modifier
//                        .size(36.dp)
//                        .clip(CircleShape)
//                        .background(
//                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
//                        ),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        Icons.Default.DirectionsCar,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                }
//
//                Spacer(Modifier.width(10.dp))
//
//                Column(
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text(
//                        text = vehicle.number,
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold,
//                        maxLines = 1,
//                        overflow = TextOverflow.Ellipsis
//                    )
//                    Text(
//                        text = vehicle.type,
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.onSurfaceVariant
//                    )
//                }
//            }
//
//// --- FIND THIS SECTION INSIDE VehicleRow COMPOSABLE ---
//// It is inside: AnimatedVisibility(visible = expanded) { ... }
//
//// REPLACE the entire logic inside AnimatedVisibility with this:
//
//            AnimatedVisibility(visible = expanded) {
//                Column(modifier = Modifier.padding(top = 12.dp)) {
//
//                    val driverId = vehicle.driverId
//
//                    // --- CASE 1: NO DRIVER LINKED ---
//                    if (driverId.isNullOrBlank()) {
//                        Text(
//                            text = "No driver linked",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                        Spacer(Modifier.height(8.dp))
//                        Row(
//                            horizontalArrangement = Arrangement.spacedBy(8.dp)
//                        ) {
//                            // GREEN LINK BUTTON
//                            Button(
//                                onClick = { onLinkDriverClicked(vehicle.id) },
//                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
//                            ) {
//                                Text("Link Driver")
//                            }
//
//                            OutlinedButton(onClick = onEdit) { Text("Edit") }
//
//                            // DELETE BUTTON (Red Text)
//                            TextButton(
//                                onClick = onDelete,
//                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
//                            ) {
//                                Text("Delete Vehicle")
//                            }
//                        }
//                    }
//                    // --- CASE 2: DRIVER IS LINKED ---
//                    else {
//                        // Load driver info
//                        val driverInfo by produceState<DriverRepo.LinkedDriver?>(initialValue = null, key1 = driverId) {
//                            value = try { fetchDriver(driverId) } catch (_: Exception) { null }
//                        }
//
//                        Spacer(Modifier.height(4.dp))
//
//                        // Show Driver Details
//                        Row(verticalAlignment = Alignment.CenterVertically) {
//                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.secondary)
//                            Spacer(Modifier.width(6.dp))
//                            Column {
//                                Text(
//                                    text = "Driver: ${driverInfo?.type ?: "Active"}",
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    fontWeight = FontWeight.Bold
//                                )
//                                Text(
//                                    text = "Code: ${driverInfo?.driverCode ?: "..."}",
//                                    style = MaterialTheme.typography.bodySmall,
//                                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                                )
//                            }
//                        }
//
//                        Spacer(Modifier.height(12.dp))
//
//                        // BUTTON ROW
//                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//
//                            // UNLINK BUTTON (Red Outline)
//                            OutlinedButton(
//                                onClick = {
//                                    // You need to pass an "onUnlink" callback to VehicleRow
//                                    // See Step 4 below on how to wire this up!
//                                    onUnlink(vehicle.id)
//                                },
//                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
//                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
//                            ) {
//                                Text("Unlink")
//                            }
//
//                            TextButton(onClick = onEdit) { Text("Edit") }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

/* ----------------------------------------------------------
   DIALOGS
---------------------------------------------------------- */
private const val MAX_CHARS_NUMBER = 10
private const val MAX_CHARS_TYPE = 14

@Composable
fun AddVehicleDialog(
    initialNumber: String = "",
    initialType: String = "Auto",
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    loading: Boolean = false
) {
    var number by remember { mutableStateOf(initialNumber) }
    var type by remember { mutableStateOf(initialType) }

    var numberOverflow by remember { mutableStateOf(false) }
    var typeOverflow by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Vehicle", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {

                OutlinedTextField(
                    value = number,
                    onValueChange = { newValue ->
                        numberOverflow = newValue.length > MAX_CHARS_NUMBER
                        if (!numberOverflow) number = newValue
                    },
                    label = { Text("Vehicle Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = numberOverflow,
                    supportingText = {
                        if (numberOverflow) {
                            Text(
                                "Maximum ${MAX_CHARS_NUMBER} characters allowed",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("${number.length}/${MAX_CHARS_NUMBER}}")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { newValue ->
                        typeOverflow = newValue.length > MAX_CHARS_TYPE
                        if (!typeOverflow) type = newValue
                    },
                    label = { Text("Vehicle Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = typeOverflow,
                    supportingText = {
                        if (typeOverflow) {
                            Text(
                                "Maximum ${MAX_CHARS_TYPE} characters allowed",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("${type.length}/${MAX_CHARS_TYPE}")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(number.trim(), type.trim(), null)
                },
                enabled = !loading &&
                        number.isNotBlank() &&
                        !numberOverflow &&
                        !typeOverflow
            ) {
                Text(if (loading) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditVehicleDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var number by remember { mutableStateOf(vehicle.number) }
    var type by remember { mutableStateOf(vehicle.type) }

    var numberOverflow by remember { mutableStateOf(false) }
    var typeOverflow by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Vehicle", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {

                OutlinedTextField(
                    value = number,
                    onValueChange = { newValue ->
                        numberOverflow = newValue.length > MAX_CHARS_NUMBER
                        if (!numberOverflow) number = newValue
                    },
                    label = { Text("Vehicle Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = numberOverflow,
                    supportingText = {
                        if (numberOverflow) {
                            Text(
                                "Maximum ${MAX_CHARS_NUMBER}} characters allowed",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("${number.length}/${MAX_CHARS_NUMBER}}")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = type,
                    onValueChange = { newValue ->
                        typeOverflow = newValue.length > MAX_CHARS_TYPE
                        if (!typeOverflow) type = newValue
                    },
                    label = { Text("Vehicle Type") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = typeOverflow,
                    supportingText = {
                        if (typeOverflow) {
                            Text(
                                "Maximum ${MAX_CHARS_TYPE} characters allowed",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("${type.length}/${MAX_CHARS_TYPE}")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(number.trim(), type.trim()) },
                enabled = number.isNotBlank() &&
                        !numberOverflow &&
                        !typeOverflow
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@Composable
fun DeleteVehicleDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete Vehicle",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text("Are you sure you want to delete ${vehicle.number}?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LinkDriverDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    loading: Boolean = false
) {
    var driverCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link driver to ${vehicle.number}") },
        text = {
            Column {
                OutlinedTextField(
                    value = driverCode,
                    onValueChange = { driverCode = it },
                    label = { Text("Driver Code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(driverCode.trim()) },
                enabled = !loading && driverCode.isNotBlank()
            ) {
                Text(if (loading) "Linking..." else "Link Driver")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}