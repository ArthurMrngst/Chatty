package com.CO1102.Chatty.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // ── Shared state ──────────────────────────────────────────────────────
    private val _isLoading     = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error         = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ── Login ─────────────────────────────────────────────────────────────
    private val _loginSuccess  = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    fun login(email: String, password: String) {
        _isLoading.value = true
        _error.value     = null

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()

                // Update online status on login
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    db.collection("users").document(uid)
                        .update(mapOf(
                            "online"   to true,
                            "lastSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        ))
                }

                _loginSuccess.value = true
            } catch (e: Exception) {
                _error.value = friendlyError(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Register ──────────────────────────────────────────────────────────
    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess

    fun register(email: String, password: String) {
        _isLoading.value = true
        _error.value     = null

        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid    = result.user?.uid ?: return@launch

                // Create user document in Firestore
                db.collection("users").document(uid).set(
                    mapOf(
                        "uid"      to uid,
                        "email"    to email,
                        "online"   to true,
                        "lastSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                ).await()

                _registerSuccess.value = true
            } catch (e: Exception) {
                _error.value = friendlyError(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────
    fun logout() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.collection("users").document(uid)
                .update(mapOf(
                    "online"   to false,
                    "lastSeen" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
        }
        auth.signOut()
    }

    // ── Reset state (call when navigating back to login) ──────────────────
    fun resetState() {
        _loginSuccess.value    = false
        _registerSuccess.value = false
        _error.value           = null
    }

    // ── Error message helper ──────────────────────────────────────────────
    private fun friendlyError(message: String?): String {
        return when {
            message == null                          -> "Something went wrong."
            message.contains("password")             -> "Incorrect password. Please try again."
            message.contains("email")                -> "No account found with that email."
            message.contains("already in use")       -> "This email is already registered."
            message.contains("network")              -> "No internet connection."
            message.contains("weak-password")        -> "Password should be at least 6 characters."
            message.contains("invalid-email")        -> "Please enter a valid email address."
            else                                     -> "Login failed. Please try again."
        }
    }
}