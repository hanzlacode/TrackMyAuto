package com.brogaming.trackmyauto.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brogaming.trackmyauto.data.model.Vehicle
import com.brogaming.trackmyauto.data.repo.DriverRepo
import com.brogaming.trackmyauto.data.repo.OwnerRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VehiclesUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val expandedVehicleId: String? = null,
    val showingAddDialog: Boolean = false,
    val editingVehicle: Vehicle? = null,
    val deletingVehicle: Vehicle? = null,
    val showingLinkDialogFor: Vehicle? = null
)

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val ownerRepo: OwnerRepo,
    private val driverRepo: DriverRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehiclesUiState(loading = true))
    val uiState: StateFlow<VehiclesUiState> = _uiState.asStateFlow()

    init {
        observeVehicles()
    }

    // --- MAIN DATA LOADING ---
    // Changed from 'private' to public so you can call it from UI if needed
    fun observeVehicles() {
        Log.d("VehicleViewModel", "Starting to observe vehicles...")

        ownerRepo.getVehiclesFlow()
            .onEach { list ->
                Log.d("VehicleViewModel", "Vehicles loaded: ${list.size} items")
                _uiState.update { it.copy(vehicles = list, loading = false, error = null) }
            }
            .catch { e ->
                Log.e("VehicleViewModel", "Error loading vehicles", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to load vehicles", loading = false) }
            }
            .launchIn(viewModelScope)
    }

    // Call this if you want to manually force a refresh/retry
    fun retry() {
        _uiState.update { it.copy(loading = true, error = null) }
        observeVehicles()
    }

    // --- UI HELPERS ---

    fun toggleExpand(vehicleId: String) {
        _uiState.update {
            val current = it.expandedVehicleId
            val next = if (current == vehicleId) null else vehicleId
            it.copy(expandedVehicleId = next)
        }
    }

    fun showAddDialog(show: Boolean) = _uiState.update { it.copy(showingAddDialog = show) }

    // --- ACTIONS: CREATE ---

    fun addVehicleAndMaybeLink(
        number: String,
        type: String,
        driverCode: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loading = true, error = null) }

                // ALEX'S FIX: Verify code FIRST.
                // If code is wrong, it throws Exception, and we NEVER create the vehicle.
                if (!driverCode.isNullOrBlank()) {
                    ownerRepo.verifyDriverCode(driverCode)
                }

                // Code is valid (or null), proceed to create vehicle
                val vehicleId = ownerRepo.addVehicle(number, type)

                if (!driverCode.isNullOrBlank()) {
                    val driverUid = ownerRepo.linkDriverByCode(driverCode, type, number)
                    ownerRepo.assignDriverToVehicle(vehicleId, driverUid)
                }

                _uiState.update { it.copy(loading = false, showingAddDialog = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
                onError(e.message ?: "Failed to add vehicle")
            }
        }
    }

    // --- ACTIONS: EDIT ---

    fun showEditDialog(vehicle: Vehicle) = _uiState.update { it.copy(editingVehicle = vehicle) }
    fun closeEditDialog() = _uiState.update { it.copy(editingVehicle = null) }

    fun updateVehicle(id: String, number: String, type: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loading = true) }
                ownerRepo.updateVehicle(id, number, type)
                _uiState.update { it.copy(loading = false, editingVehicle = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    // --- ACTIONS: DELETE ---

    fun showDeleteDialog(vehicle: Vehicle) = _uiState.update { it.copy(deletingVehicle = vehicle) }
    fun closeDeleteDialog() = _uiState.update { it.copy(deletingVehicle = null) }

    fun deleteVehicle(id: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loading = true) }
                ownerRepo.deleteVehicle(id)
                _uiState.update { it.copy(loading = false, deletingVehicle = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    // --- ACTIONS: LINK ---

    fun showLinkDialog(vehicle: Vehicle) = _uiState.update { it.copy(showingLinkDialogFor = vehicle) }
    fun closeLinkDialog() = _uiState.update { it.copy(showingLinkDialogFor = null) }

    // --- REPLACE existing linkDriverToVehicle with this ---
    fun linkDriverToVehicle(vehicleId: String, driverCode: String, type: String, number: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loading = true, error = null) }

                // 1. CHECK AVAILABILITY FIRST
                val errorMsg = ownerRepo.checkDriverAvailability(driverCode)
                if (errorMsg != null) {
                    // Stop! Driver is taken or invalid.
                    _uiState.update { it.copy(loading = false, error = errorMsg) }
                    return@launch
                }

                // 2. PROCEED TO LINK
                val driverUid = ownerRepo.linkDriverByCode(driverCode, type, number)
                ownerRepo.assignDriverToVehicle(vehicleId, driverUid)

                _uiState.update { it.copy(loading = false, showingLinkDialogFor = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    // --- ADD THIS NEW FUNCTION (for the Unlink Button) ---
    fun unlinkDriver(vehicleId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(loading = true) }
                ownerRepo.unlinkDriverFromVehicle(vehicleId)
                _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }
    // --- HELPER: DRIVER INFO ---

    suspend fun getDriverInfo(driverId: String): DriverRepo.LinkedDriver? {
        return try {
            driverRepo.getDriverById(driverId)
        } catch (e: Exception) {
            null
        }
    }
}