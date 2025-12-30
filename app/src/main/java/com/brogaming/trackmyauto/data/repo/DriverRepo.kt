package com.brogaming.trackmyauto.data.repo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DriverRepo @Inject constructor(
    private val fs: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) {

    private fun driverUid(): String =
        auth.currentUser?.uid ?: throw Exception("User not logged in")

    /**
     * Returns saved driverCode if present, otherwise generates, saves and returns it.
     */
    suspend fun getOrCreateDriverCode(): String {
        val uid = driverUid()
        val docRef = fs.collection("drivers").document(uid)
        val snap = docRef.get().await()

        if (snap.exists()) {
            val existing = snap.getString("driverCode")
            if (!existing.isNullOrBlank()) return existing
        }

        // ALEX'S FIX: Loop until we find a unique code
        var code = generateDriverCode()
        var isUnique = false

        while (!isUnique) {
            val check = fs.collection("drivers")
                .whereEqualTo("driverCode", code)
                .limit(1)
                .get()
                .await()

            if (check.isEmpty) {
                isUnique = true
            } else {
                code = generateDriverCode() // Collision! Try again.
            }
        }

        val data = mapOf(
            "driverCode" to code,
            "type" to null,
            "number" to null,
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
        return code
    }
    suspend fun getLinkedDriver(): LinkedDriver? {
        val uid = driverUid()
        val snap = fs.collection("drivers").document(uid).get().await()
        return if (!snap.exists()) null else LinkedDriver(
            type = snap.getString("type"),
            number = snap.getString("number"),
            linkedOwner = snap.getString("linkedOwner"),
            driverCode = snap.getString("driverCode")
        )
    }

    data class LinkedDriver(
        val type: String? = null,
        val number: String? = null,
        val linkedOwner: String? = null,
        val driverCode: String? = null
    )

    // simple 6-char code generator (uppercase letters + digits)
    private fun generateDriverCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    fun copyDriverCodeToClipboard(driverCode: String) {
        val code = driverCode

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("DriverCode", code))
    }

    suspend fun getDriverById(driverId: String): LinkedDriver? {
        val snap = fs.collection("drivers").document(driverId).get().await()
        if (!snap.exists()) return null
        return LinkedDriver(
            type = snap.getString("type"),
            number = snap.getString("number"),
            linkedOwner = snap.getString("linkedOwner"),
            driverCode = snap.getString("driverCode")
        )
    }

}

