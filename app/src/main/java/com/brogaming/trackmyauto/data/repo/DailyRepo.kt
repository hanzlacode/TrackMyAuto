package com.brogaming.trackmyauto.data.repo

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyRepo @Inject constructor(
    private val fs: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun ownerId(): String =
        auth.currentUser?.uid ?: throw Exception("Not logged in")

    @RequiresApi(Build.VERSION_CODES.O)
    private fun today(): String = LocalDate.now().toString()

    // ----------------------------------------------------------------------
    // SAVE OR UPDATE ENTRY
    // ----------------------------------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun saveDaily(
        vehicleId: String,
        kmStart: Int,
        kmEnd: Int,
        earning: Int,
        date: String? = null
    ) {
        val d = date ?: today()

        val ref = fs.collection("owners")
            .document(ownerId())
            .collection("vehicles")
            .document(vehicleId)
            .collection("daily")
            .document(d)

        val data = mapOf(
            "date" to d,
            "ownerId" to ownerId(),
            "vehicleId" to vehicleId,
            "kmStart" to kmStart,
            "kmEnd" to kmEnd,
            "earning" to earning,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        ref.set(data, SetOptions.merge()).await()
    }

    // ----------------------------------------------------------------------
    // GET VEHICLES
    // ----------------------------------------------------------------------
    suspend fun getOwnerVehicles(): List<VehicleInfo> {
        val snap = fs.collection("owners")
            .document(ownerId())
            .collection("vehicles")
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            val number = doc.getString("number") ?: return@mapNotNull null
            VehicleInfo(
                vehicleId = doc.id,
                number = number,
                type = doc.getString("type") ?: ""
            )
        }
    }

    // ----------------------------------------------------------------------
    // GET DAILY FOR ALL VEHICLES (NO INDEX NEEDED)
    // ----------------------------------------------------------------------
    suspend fun getDailyForAllVehicles(date: String): List<DailyEntry> {
        val vehicles = getOwnerVehicles()
        val results = mutableListOf<DailyEntry>()

        for (v in vehicles) {
            val snap = fs.collection("owners")
                .document(ownerId())
                .collection("vehicles")
                .document(v.vehicleId)
                .collection("daily")
                .document(date)
                .get()
                .await()

            if (snap.exists()) {
                results += DailyEntry(
                    date = date,
                    ownerId = ownerId(),
                    vehicleId = v.vehicleId,
                    kmStart = snap.getLong("kmStart")?.toInt() ?: 0,
                    kmEnd = snap.getLong("kmEnd")?.toInt() ?: 0,
                    earning = snap.getLong("earning")?.toInt() ?: 0
                )
            }
        }

        return results
    }

    // ----------------------------------------------------------------------
    // GET DAILY FOR SINGLE VEHICLE
    // ----------------------------------------------------------------------
    suspend fun getDailyForVehicle(vehicleId: String): List<DailyEntry> {
        val snap = fs.collection("owners")
            .document(ownerId())
            .collection("vehicles")
            .document(vehicleId)
            .collection("daily")
            .get()
            .await()

        return snap.documents.mapNotNull { doc ->
            DailyEntry(
                date = doc.getString("date") ?: doc.id,
                ownerId = ownerId(),
                vehicleId = vehicleId,
                kmStart = doc.getLong("kmStart")?.toInt() ?: 0,
                kmEnd = doc.getLong("kmEnd")?.toInt() ?: 0,
                earning = doc.getLong("earning")?.toInt() ?: 0
            )
        }
    }

    // ----------------------------------------------------------------------
    // GET ENTRY FOR SINGLE VEHICLE + DATE
    // ----------------------------------------------------------------------
    suspend fun getDailyFor(vehicleId: String, date: String): DailyEntry? {
        val snap = fs.collection("owners")
            .document(ownerId())
            .collection("vehicles")
            .document(vehicleId)
            .collection("daily")
            .document(date)
            .get()
            .await()

        return if (!snap.exists()) null else DailyEntry(
            date = snap.getString("date") ?: date,
            ownerId = ownerId(),
            vehicleId = vehicleId,
            kmStart = snap.getLong("kmStart")?.toInt() ?: 0,
            kmEnd = snap.getLong("kmEnd")?.toInt() ?: 0,
            earning = snap.getLong("earning")?.toInt() ?: 0
        )
    }

    // ----------------------------------------------------------------------
    // GET TOTAL REPORT (ALL TIME)
    // ----------------------------------------------------------------------
    suspend fun getAllDaily(): List<DailyEntry> {
        val vehicles = getOwnerVehicles()
        val allEntries = mutableListOf<DailyEntry>()

        for (v in vehicles) {
            val snap = fs.collection("owners")
                .document(ownerId())
                .collection("vehicles")
                .document(v.vehicleId)
                .collection("daily")
                .get()
                .await()

            snap.documents.forEach { doc ->
                allEntries += DailyEntry(
                    date = doc.getString("date") ?: doc.id,
                    ownerId = ownerId(),
                    vehicleId = v.vehicleId,
                    kmStart = doc.getLong("kmStart")?.toInt() ?: 0,
                    kmEnd = doc.getLong("kmEnd")?.toInt() ?: 0,
                    earning = doc.getLong("earning")?.toInt() ?: 0
                )
            }
        }

        return allEntries
    }

    // ----------------------------------------------------------------------
    // TOTAL SUMMARY
    // ----------------------------------------------------------------------
    suspend fun getTotalSummary(): Pair<Int, Int> {
        val all = getAllDaily()
        val totalKm = all.sumOf { it.kmDriven }
        val totalEarning = all.sumOf { it.earning }
        return totalKm to totalEarning
    }
}

// ----------------------------------------------------------------------
// DATA MODELS
// ----------------------------------------------------------------------
data class VehicleInfo(
    val vehicleId: String,
    val number: String,
    val type: String
)

data class DailyEntry(
    val date: String = "",
    val ownerId: String = "",
    val vehicleId: String = "",
    val kmStart: Int = 0,
    val kmEnd: Int = 0,
    val earning: Int = 0
) {
    val kmDriven: Int get() = kmEnd - kmStart
}
