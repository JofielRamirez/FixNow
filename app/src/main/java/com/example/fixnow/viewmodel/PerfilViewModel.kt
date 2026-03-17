package com.example.fixnow.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import com.example.fixnow.utils.Resource
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerfilUiState(
    val perfil: UsuarioPerfil? = null,
    val userEmail: String = "",
    val userName: String = "",
    val cargando: Boolean = true,
    val subiendoFoto: Boolean = false,
    val error: String? = null
)

class PerfilViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState = _uiState.asStateFlow()

    private val client = SupabaseClient.client

    init {
        cargarPerfil()
    }

    fun cargarPerfil() {
        val user = client.auth.currentSessionOrNull()?.user ?: return
        val name = user.userMetadata?.get("nombre")?.toString()?.trim('"') 
            ?: user.email?.substringBefore("@") ?: "Usuario"
        
        _uiState.update { it.copy(userEmail = user.email ?: "", userName = name, cargando = true) }

        viewModelScope.launch {
            try {
                val uid = user.id
                val datos = UsuarioRepository.obtenerSocioPorId(uid)
                _uiState.update { it.copy(perfil = datos, cargando = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, cargando = false) }
            }
        }
    }

    fun subirFotoTrabajo(context: Context, uri: Uri) {
        val uid = client.auth.currentSessionOrNull()?.user?.id ?: return
        _uiState.update { it.copy(subiendoFoto = true) }
        
        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) {
                    UsuarioRepository.subirFotoTrabajo(uid, bytes)
                    _uiState.update { it.copy(subiendoFoto = false) }
                }
            } catch (e: Exception) {
                Log.e("PERFIL_VM", "Error subiendo foto: ${e.message}")
                _uiState.update { it.copy(subiendoFoto = false, error = "Error al subir foto") }
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            client.auth.signOut()
        }
    }
}