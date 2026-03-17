package com.example.fixnow.viewmodel

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListaChatsUiState(
    val chats: List<UsuarioPerfil> = emptyList(),
    val cargando: Boolean = true,
    val esSocio: Boolean = false,
    val error: String? = null
)

class ListaChatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ListaChatsUiState())
    val uiState = _uiState.asStateFlow()

    private val _noLeidosMap = mutableStateMapOf<String, Int>()
    val noLeidosMap: Map<String, Int> get() = _noLeidosMap

    private val client = SupabaseClient.client

    init {
        cargarConversaciones()
    }

    fun cargarConversaciones() {
        val miId = client.auth.currentSessionOrNull()?.user?.id ?: return
        
        viewModelScope.launch {
            try {
                val perfil = UsuarioRepository.obtenerSocioPorId(miId)
                val esSocio = perfil?.es_prestador == true
                
                val ids = ChatRepository.obtenerConversaciones(miId)
                val perfiles = ids.mapNotNull { id -> UsuarioRepository.obtenerSocioPorId(id) }
                
                _uiState.update { it.copy(chats = perfiles, esSocio = esSocio, cargando = false) }

                // Escuchar no leídos en tiempo real
                ids.forEach { otroId ->
                    launch {
                        ChatRepository.escucharMensajes(miId, otroId).collect { mensajes ->
                            val count = mensajes.count { it.idReceptor == miId && !it.leido }
                            _noLeidosMap[otroId] = count
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }
}