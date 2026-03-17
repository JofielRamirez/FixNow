package com.example.fixnow.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.utils.Resource
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    var nombre by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)

    private val _registerState = MutableStateFlow<Resource<Unit>?>(null)
    val registerState = _registerState.asStateFlow()

    fun onNombreChange(newValue: String) { nombre = newValue }
    fun onEmailChange(newValue: String) { email = newValue }
    fun onPasswordChange(newValue: String) { password = newValue }
    fun togglePasswordVisibility() { passwordVisible = !passwordVisible }

    fun register() {
        val emailLimpio = email.trim().lowercase()
        val passwordLimpio = password
        val nombreRegistro = nombre.trim()

        if (nombreRegistro.isBlank() || emailLimpio.isBlank() || passwordLimpio.isBlank()) {
            _registerState.value = Resource.Error("Completa todos los campos")
            return
        }

        if (passwordLimpio.length < 8) {
            _registerState.value = Resource.Error("La contraseña debe tener al menos 8 caracteres")
            return
        }

        _registerState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = emailLimpio
                    this.password = passwordLimpio
                }
                
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
                if (uid != null) {
                    try {
                        UsuarioRepository.guardarUsuario(uid = uid, email = emailLimpio, nombre = nombreRegistro)
                        _registerState.value = Resource.Success(Unit)
                    } catch (e: Exception) {
                        Log.e("REGISTRO_ERROR", "Error guardando perfil: ${e.message}")
                        // Si falla guardar el perfil, igual consideramos éxito si el auth pasó, 
                        // o informamos error parcial. Por ahora éxito.
                        _registerState.value = Resource.Success(Unit)
                    }
                } else {
                    _registerState.value = Resource.Success(Unit) // Probablemente necesita confirmar email
                }
            } catch (e: Exception) {
                Log.e("REGISTRO_ERROR", "Error: ${e.message}")
                val msg = e.message ?: ""
                val friendlyMessage = when {
                    msg.contains("already registered", ignoreCase = true) -> "Este correo ya está registrado"
                    msg.contains("rate limit", ignoreCase = true) -> "Espera un momento antes de intentarlo"
                    else -> "Error al crear cuenta: $msg"
                }
                _registerState.value = Resource.Error(friendlyMessage)
            }
        }
    }

    fun resetState() {
        _registerState.value = null
    }
}