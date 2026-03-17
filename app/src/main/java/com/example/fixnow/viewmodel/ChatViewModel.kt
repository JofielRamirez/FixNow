package com.example.fixnow.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.ChatRepository
import com.example.fixnow.data.MensajeDB
import com.example.fixnow.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val mensajes: List<MensajeDB> = emptyList(),
    val cargando: Boolean = true,
    val miId: String = "",
    val socioNombre: String = "",
    val error: String? = null
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _mensajesList = mutableStateListOf<MensajeDB>()
    val mensajesList: List<MensajeDB> get() = _mensajesList

    fun initChat(socioId: String, nombre: String) {
        val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
        _uiState.update { it.copy(miId = uid, socioNombre = nombre) }

        viewModelScope.launch {
            try {
                ChatRepository.marcarComoLeidos(uid, socioId)
                val historial = ChatRepository.obtenerMensajesHistoricos(uid, socioId)
                _mensajesList.clear()
                _mensajesList.addAll(historial)
                _uiState.update { it.copy(cargando = false, mensajes = _mensajesList) }

                ChatRepository.escucharMensajes(uid, socioId).collect { listaDB ->
                    val nuevos = listaDB.filter { db -> _mensajesList.none { it.id == db.id } }
                    if (nuevos.isNotEmpty()) {
                        val contenidosNuevos = nuevos.map { it.contenido }
                        _mensajesList.removeAll { it.id.startsWith("temp_") && it.contenido in contenidosNuevos }
                        _mensajesList.addAll(nuevos)
                        _mensajesList.sortBy { it.createdAt }
                        _uiState.update { it.copy(mensajes = _mensajesList) }
                        
                        ChatRepository.marcarComoLeidos(uid, socioId)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }

    fun enviarMensaje(receptorId: String, texto: String) {
        if (texto.isBlank()) return
        val miId = _uiState.value.miId
        
        val temporal = MensajeDB(
            id = "temp_${System.currentTimeMillis()}",
            idEmisor = miId,
            idReceptor = receptorId,
            contenido = texto,
            createdAt = "Z"
        )
        _mensajesList.add(temporal)
        _uiState.update { it.copy(mensajes = _mensajesList) }

        viewModelScope.launch {
            try {
                ChatRepository.enviarMensaje(miId, receptorId, texto)
            } catch (e: Exception) {
                _mensajesList.remove(temporal)
                _uiState.update { it.copy(mensajes = _mensajesList) }
            }
        }
    }
}