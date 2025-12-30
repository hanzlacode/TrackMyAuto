package com.brogaming.trackmyauto.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OwnerRepo @Inject constructor(
    private val fs: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private fun ownerUid(): String =
        auth.currentUser?.uid ?: throw Exception("User not logged in")

    // --- existing functions (addVehicle, linkDriverByCode, assignDriverToVehicle)
    // keep the implementations you already have (from prior messages)
// -----------------------------
// UPDATE VEHICLE
// -----------------------------
    suspend fun updateVehicle(id: String, number: String, type: String) {
        val uid = ownerUid()
        fs.collection("owners").document(uid)
            .collection("vehicles").document(id)
            .update(
                mapOf(
                    "number" to number,
                    "type" to type,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()
    }

    // -----------------------------
// DELETE VEHICLE
// -----------------------------
    suspend fun deleteVehicle(id: String) {
        val uid = ownerUid()

        // Read the vehicle doc
        val vehicleRef = fs.collection("owners")
            .document(uid)
            .collection("vehicles")
            .document(id)

        val snap = vehicleRef.get().await()

        // Get driverId instead of driverCode
        val driverId = snap.getString("driverId")

        // Delete the vehicle
        vehicleRef.delete().await()

        // If no driverId → stop
        if (driverId.isNullOrEmpty()) {
            return
        }

        // Get driver doc by ID
        val driverRef = fs.collection("drivers")
            .document(driverId)

        val driverSnap = driverRef.get().await()

        if (driverSnap.exists()) {

            driverRef.update("linkedOwner", "").await()

        }
    }


    suspend fun addVehicle(number: String, type: String): String {
        val uid = ownerUid()
        val vehiclesRef = fs.collection("owners").document(uid).collection("vehicles")
        val data = mapOf(
            "number" to number,
            "type" to type,
            "driverId" to null,
            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        val docRef = vehiclesRef.add(data).await()
        return docRef.id
    }

    // Paste this inside OwnerRepo class
    suspend fun verifyDriverCode(driverCode: String) {
        val q = fs.collection("drivers")
            .whereEqualTo("driverCode", driverCode)
            .limit(1)
            .get()
            .await()
        if (q.isEmpty) throw Exception("Invalid driver code: $driverCode")
    }
    suspend fun linkDriverByCode(driverCode: String, type1: String, number1: String): String {
        val q = fs.collection("drivers")
            .whereEqualTo("driverCode", driverCode)
            .limit(1)
            .get()
            .await()

        if (q.isEmpty) throw Exception("Invalid driver code")
        val doc = q.documents.first()
        val driverUid = doc.id
        val ownerUid = ownerUid()

        // update driver doc
        fs.collection("drivers").document(driverUid)
            .update(mapOf("linkedOwner" to ownerUid,"type" to type1 ,"number" to number1 , "linkedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
            .await()

        // add linkedDrivers doc under owner
        fs.collection("owners").document(ownerUid)
            .collection("linkedDrivers").document(driverUid)
            .set(mapOf("linked" to true, "linkedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
            .await()

        return driverUid
    }

    // --- PASTE THIS INSIDE OwnerRepo CLASS ---

    // 1. Check if driver exists AND is free
    // Returns: null if available, or an error message string if unavailable
    suspend fun checkDriverAvailability(driverCode: String): String? {
        val q = fs.collection("drivers")
            .whereEqualTo("driverCode", driverCode)
            .limit(1)
            .get()
            .await()

        if (q.isEmpty) return "Invalid Driver Code. Please check and try again."

        val driverDoc = q.documents.first()
        val currentOwner = driverDoc.getString("linkedOwner")

        // If linkedOwner exists and is not empty, the driver is busy
        if (!currentOwner.isNullOrBlank()) {
            return "Driver is already linked to another vehicle/owner."
        }

        return null // Driver is free!
    }

    // 2. Unlink Driver safely
// Inside OwnerRepo class...

    suspend fun unlinkDriverFromVehicle(vehicleId: String) {
        val uid = ownerUid()
        val vehicleRef = fs.collection("owners").document(uid).collection("vehicles").document(vehicleId)

        // 1. Get vehicle to find who the driver is
        val vehicleSnap = vehicleRef.get().await()
        val driverId = vehicleSnap.getString("driverId")

        // 2. Remove driver from Vehicle (Firestore)
        vehicleRef.update(
            mapOf(
                "driverId" to null,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
        ).await()

        // 3. Remove owner from Driver (Firestore)
        if (!driverId.isNullOrBlank()) {
            fs.collection("drivers").document(driverId)
                .update(
                    mapOf(
                        "linkedOwner" to null,
                        "type" to null,
                        "number" to null
                    )
                ).await()

            // --- ALEX'S FIX: DELETE REALTIME LOCATION DATA ---
            // This instantly removes the "Ghost Car" from the map.
            try {
                FirebaseDatabase.getInstance().reference
                    .child("live_locations")
                    .child(uid)      // Under Owner ID
                    .child(driverId) // Specific Driver ID
                    .removeValue()   // DELETE IT
                    .await()
            } catch (e: Exception) {
                // Log it, but don't crash if network is flaky
                e.printStackTrace()
            }
        }
    }
    suspend fun assignDriverToVehicle(vehicleId: String, driverUid: String) {
        val uid = ownerUid()
        val vehicleDoc = fs.collection("owners").document(uid).collection("vehicles").document(vehicleId)
        vehicleDoc.update(mapOf("driverId" to driverUid, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())).await()
    }

    // -------------------------
    // Real-time Flow (snapshot) for vehicles list
    // -------------------------
    fun getVehiclesFlow(): Flow<List<com.brogaming.trackmyauto.data.model.Vehicle>> = callbackFlow {
        val uid = try { ownerUid() } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        val ref = fs.collection("owners").document(uid).collection("vehicles")
        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(emptyList()).isSuccess
                return@addSnapshotListener
            }
            if (snap == null) {
                trySend(emptyList()).isSuccess
                return@addSnapshotListener
            }
            val list = snap.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val number = doc.getString("number") ?: ""
                    val type = doc.getString("type") ?: ""
                    val driverId = doc.getString("driverId")
                    com.brogaming.trackmyauto.data.model.Vehicle(
                        id = id,
                        number = number,
                        type = type,
                        driverId = driverId
                    )
                } catch (_: Exception) {
                    null
                }
            }
            trySend(list).isSuccess
        }

        awaitClose { listener.remove() }
    }
}
