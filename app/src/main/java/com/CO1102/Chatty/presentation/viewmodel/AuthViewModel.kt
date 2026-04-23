package com.CO1102.Chatty.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.CO1102.Chatty.data.AuthRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.*

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true

            val result = repo.login(email, password)

            isLoading = false

            result.onSuccess {
                onSuccess()
            }.onFailure {
                errorMessage = it.message
            }
        }
    }

    fun register(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading = true

            val result = repo.register(email, password)

            isLoading = false

            result.onSuccess {
                onSuccess()
            }.onFailure {
                errorMessage = it.message
            }
        }
    }
}