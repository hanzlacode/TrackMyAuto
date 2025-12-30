package com.brogaming.trackmyauto.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.brogaming.trackmyauto.data.repo.AuthRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class UserUi(
    val name: String = "",
    val photoUrl: String = ""
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {

    // ALEX'S FIX: Changed from 'val user =' to a getter.
    // This ensures we always check the FRESH login state when asked.
    val isUserLoggedIn: Boolean
        get() = authRepo.isLoggedIn()

    val isDriverMode: Boolean = false

    fun logout() {
        authRepo.logout()
    }
}