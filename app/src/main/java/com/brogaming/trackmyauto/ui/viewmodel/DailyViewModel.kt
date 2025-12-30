package com.brogaming.trackmyauto.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brogaming.trackmyauto.data.repo.DailyEntry
import com.brogaming.trackmyauto.data.repo.DailyRepo
import com.brogaming.trackmyauto.data.repo.VehicleInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DailyUiState(
    val loading: Boolean = false,
    val error: String? = null,

    val date: String = "",
    val entries: List<DailyEntry> = emptyList(),
    val vehicles: List<VehicleInfo> = emptyList(),

    val totalKm: Int = 0,
    val totalEarning: Int = 0,

    // trends
    val weeklyKm: List<Pair<String, Int>> = emptyList(),       // last 7 days (label -> km)
    val weeklyEarning: List<Pair<String, Int>> = emptyList(),  // last 7 days (label -> earning)
    val monthlyKm: List<Pair<String, Int>> = emptyList(),      // last 12 months (YYYY-MM -> km)
    val monthlyEarning: List<Pair<String, Int>> = emptyList()  // last 12 months (YYYY-MM -> earning)
)
@RequiresApi(Build.VERSION_CODES.O)

@HiltViewModel
class DailyViewModel @Inject constructor(
    private val repo: DailyRepo
) : ViewModel() {

    private val _ui = MutableStateFlow(DailyUiState())
    val ui: StateFlow<DailyUiState> = _ui.asStateFlow()

    private val isoDateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val monthFmt = DateTimeFormatter.ofPattern("yyyy-MM")

    // ---------------------------------------------------------------------
    // LOAD DAILY REPORT FOR SELECTED DATE + refresh totals & trends
    // ---------------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun load(date: String = LocalDate.now().toString()) {
        viewModelScope.launch {
            try {
                _ui.update { it.copy(loading = true, error = null) }

                val vehicles = repo.getOwnerVehicles()
                val daily = repo.getDailyForAllVehicles(date)

                _ui.update {
                    it.copy(
                        loading = false,
                        date = date,
                        entries = daily,
                        vehicles = vehicles
                    )
                }

                // Refresh totals & trends (async)
                loadTotalAndTrends()

            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Failed to load daily"
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // SAVE DAILY (add or update)
    // ---------------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveDaily(vehicleId: String, kmStart: Int, kmEnd: Int, earning: Int) {
        viewModelScope.launch {
            try {
                repo.saveDaily(vehicleId, kmStart, kmEnd, earning, ui.value.date)
                // refresh daily and totals/trends
                load(ui.value.date)
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Failed to save entry") }
            }
        }
    }

    // ---------------------------------------------------------------------
    // LOAD SINGLE ENTRY (for editing)
    // ---------------------------------------------------------------------
    suspend fun loadEntry(vehicleId: String, date: String): DailyEntry? {
        return repo.getDailyFor(vehicleId, date)
    }

    // ---------------------------------------------------------------------
    // TOTAL & TRENDS - All-time data -> compute totals, weekly, monthly
    // ---------------------------------------------------------------------
    // ---------------------------------------------------------------------
    // TOTAL & TRENDS - OPTIMIZED (Runs on Background Thread)
    // ---------------------------------------------------------------------
    fun loadTotalAndTrends() {
        viewModelScope.launch {
            try {
                // 1. Fetch data (IO safe)
                val all = repo.getAllDaily()

                // 2. SWITCH TO IO THREAD FOR HEAVY CALCULATION
                // This prevents the UI from freezing when you have lots of data
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {

                    // Total
                    val totalKm = all.sumOf { it.kmDriven }
                    val totalEarning = all.sumOf { it.earning }

                    // Build weekly (last 7 days based on currently selected date)
                    val selectedDate = try {
                        LocalDate.parse(_ui.value.date, isoDateFmt)
                    } catch (_: Exception) {
                        LocalDate.now()
                    }
                    val last7 = (0..6).map { selectedDate.minusDays(it.toLong()) }.reversed()

                    val mapByDate = all.groupBy { it.date } // date string -> list

                    val weeklyKm = last7.map { d ->
                        val label = d.format(isoDateFmt)
                        val sum = (mapByDate[label]?.sumOf { it.kmDriven } ?: 0)
                        label to sum
                    }
                    val weeklyEarning = last7.map { d ->
                        val label = d.format(isoDateFmt)
                        val sum = (mapByDate[label]?.sumOf { it.earning } ?: 0)
                        label to sum
                    }

                    // Monthly for last 12 months ending at selected month
                    val selectedMonth = YearMonth.from(selectedDate)
                    val last12Months = (0..11).map { selectedMonth.minusMonths(it.toLong()) }.reversed()
                    val mapByMonth = all.groupBy { entry ->
                        // entry.date expected "yyyy-MM-dd" -> take yyyy-MM
                        try {
                            val ym = YearMonth.parse(entry.date.substring(0, 7))
                            ym.toString() // "yyyy-MM"
                        } catch (_: Exception) {
                            // fallback
                            try {
                                val ld = LocalDate.parse(entry.date, isoDateFmt)
                                YearMonth.from(ld).toString()
                            } catch (_: Exception) {
                                ""
                            }
                        }
                    }

                    val monthlyKm = last12Months.map { ym ->
                        val key = ym.toString()
                        val sum = (mapByMonth[key]?.sumOf { it.kmDriven } ?: 0)
                        key to sum
                    }

                    val monthlyEarning = last12Months.map { ym ->
                        val key = ym.toString()
                        val sum = (mapByMonth[key]?.sumOf { it.earning } ?: 0)
                        key to sum
                    }

                    // Return the calculated data
                    DailyUiState(
                        totalKm = totalKm,
                        totalEarning = totalEarning,
                        weeklyKm = weeklyKm,
                        weeklyEarning = weeklyEarning,
                        monthlyKm = monthlyKm,
                        monthlyEarning = monthlyEarning
                    )
                }

                // 3. Update State back on Main Thread
                _ui.update {
                    it.copy(
                        totalKm = result.totalKm,
                        totalEarning = result.totalEarning,
                        weeklyKm = result.weeklyKm,
                        weeklyEarning = result.weeklyEarning,
                        monthlyKm = result.monthlyKm,
                        monthlyEarning = result.monthlyEarning
                    )
                }
            } catch (_: Exception) {
                // Silently fail or log error
            }
        }
    }
}
