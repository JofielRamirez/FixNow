package com.example.fixnow.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.utils.Resource
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)

    private val _loginState = MutableStateFlow<Resource<Unit>?>(null)
    val loginState = _loginState.asStateFlow()

    fun onEmailChange(newValue: String) { email = newValue }
    fun onPasswordChange(newValue: String) { password = newValue }
    fun togglePasswordVisibility() { passwordVisible = !passwordVisible }

    fun login() {
        val emailLimpio = email.trim().lowercase()
        val passLimpia = password.trim()

        if (emailLimpio.isBlank() || passLimpia.isBlank()) {
            _loginState.value = Resource.Error("Completa todos los campos")
            return
        }

        _loginState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = emailLimpio
                    this.password = passLimpia
                }
                _loginState.value = Resource.Success(Unit)
            } catch (e: Exception) {
                Log.e("LOGIN_ERROR", "Error: ${e.message}")
                val errorMsg = e.message ?: ""
                val friendlyMessage = when {
                    errorMsg.contains("Email not confirmed", ignoreCase = true) -> "Confirma tu correo electrónico"
                    errorMsg.contains("Invalid login credentials", ignoreCase = true) -> "Correo o contraseña incorrectos"
                    else -> "Error al iniciar sesión"
                }
                _loginState.value = Resource.Error(friendlyMessage)
            }
        }
    }
    
    fun resetState() {
        _loginState.value = null
    }
}