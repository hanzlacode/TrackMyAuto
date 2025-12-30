package com.brogaming.trackmyauto.data.repo

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
class AuthRepo @Inject constructor(
    private val auth: FirebaseAuth
) {

    fun isLoggedIn(): Boolean = auth.currentUser != null

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
    }

    fun logout() {
        auth.signOut()
    }
}
