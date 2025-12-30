package com.brogaming.trackmyauto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brogaming.trackmyauto.data.repo.DriverRepo
import com.brogaming.trackmyauto.data.storage.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverUiState(
    val driverCode: String = "",
    val type: String? = null,
    val number: String? = null,
    val linkedOwner: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DriverViewModel @Inject constructor(
    private val driverRepo: DriverRepo
) : ViewModel() {

    private val _state = MutableStateFlow(DriverUiState())
    val state = _state.asStateFlow()

    init {
        loadDriverDashboard()
    }

    fun loadDriverDashboard() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(loading = true)

                // Get or create driver code
                val code = driverRepo.getOrCreateDriverCode()

                // Get driver linking info
                val linked = driverRepo.getLinkedDriver()

                _state.value = _state.value.copy(
                    driverCode = code,
                    type = linked?.type,
                    number = linked?.number,
                    linkedOwner = linked?.linkedOwner,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load driver",
                    loading = false
                )
            }
        }
    }

    fun copyCode(driverCode:String) {
        driverRepo.copyDriverCodeToClipboard(driverCode)
    }
}
