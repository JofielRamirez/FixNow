package com.example.fixnow.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.utils.Resource
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterSocioUiState(
    val pasoActual: Int = 1,
    val categoriaSeleccionada: String = "",
    val uriIdentificacion: Uri? = null,
    val uriAntecedentes: Uri? = null,
    val subiendo: Boolean = false,
    val completado: Boolean = false,
    val error: String? = null
)

class RegisterSocioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterSocioUiState())
    val uiState = _uiState.asStateFlow()

    private val client = SupabaseClient.client

    fun seleccionarCategoria(cat: String) {
        _uiState.update { it.copy(categoriaSeleccionada = cat) }
    }

    fun setUriIdentificacion(uri: Uri) {
        _uiState.update { it.copy(uriIdentificacion = uri) }
    }

    fun setUriAntecedentes(uri: Uri) {
        _uiState.update { it.copy(uriAntecedentes = uri) }
    }

    fun siguientePaso() {
        if (_uiState.value.pasoActual < 3) {
            _uiState.update { it.copy(pasoActual = it.pasoActual + 1) }
        }
    }

    fun anteriorPaso() {
        if (_uiState.value.pasoActual > 1) {
            _uiState.update { it.copy(pasoActual = it.pasoActual - 1) }
        }
    }

    fun finalizarRegistro(context: Context) {
        val uid = client.auth.currentSessionOrNull()?.user?.id ?: return
        val state = _uiState.value
        
        _uiState.update { it.copy(subiendo = true) }
        
        viewModelScope.launch {
            try {
                var urlId: String? = null
                var urlAnt: String? = null

                state.uriIdentificacion?.let { uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        urlId = UsuarioRepository.subirDocumentoSocio(uid, "identificacion.jpg", bytes, "documentos_socios")
                    }
                }

                state.uriAntecedentes?.let { uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        urlAnt = UsuarioRepository.subirDocumentoSocio(uid, "antecedentes.pdf", bytes, "documentos_socios")
                    }
                }

                UsuarioRepository.convertirseEnPrestador(uid, state.categoriaSeleccionada, urlId, urlAnt)
                _uiState.update { it.copy(subiendo = false, completado = true) }
            } catch (e: Exception) {
                Log.e("REGISTRO_SOCIO_VM", "Error: ${e.message}")
                _uiState.update { it.copy(subiendo = false, error = e.message) }
            }
        }
    }
}