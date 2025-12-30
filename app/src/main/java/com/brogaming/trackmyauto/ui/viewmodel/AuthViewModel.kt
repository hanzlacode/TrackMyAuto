package com.brogaming.trackmyauto.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brogaming.trackmyauto.data.repo.AuthRepo
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthState {
    Idle,
    Loading,
    Success,
    Error
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepo
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    /**
     * Called from Activity when Google returns token.
     * Loading is started here, not before.
     */
    fun onGoogleCredential(idToken: String) {

        _state.value = AuthState.Loading

        viewModelScope.launch {

            try {
                authRepo.signInWithGoogle(idToken)

                // Check Firebase final state
                if (FirebaseAuth.getInstance().currentUser != null) {
                    _state.value = AuthState.Success
                } else {
                    _state.value = AuthState.Error
                }

            } catch (e: Exception) {
                _state.value = AuthState.Error
            }
        }
    }

    fun resetError() {
        _state.value = AuthState.Idle
    }
}
