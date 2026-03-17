package com.example.fixnow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListaServiciosUiState(
    val socios: List<UsuarioPerfil> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class ListaServiciosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ListaServiciosUiState())
    val uiState = _uiState.asStateFlow()

    fun cargarSocios(categoria: String) {
        _uiState.update { it.copy(cargando = true) }
        viewModelScope.launch {
            try {
                val lista = UsuarioRepository.obtenerSociosPorCategoria(categoria)
                _uiState.update { it.copy(socios = lista, cargando = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }
}