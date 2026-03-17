package com.example.fixnow.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import com.example.fixnow.utils.Resource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DetalleSocioUiState(
    val socio: UsuarioPerfil? = null,
    val fotosTrabajos: List<String> = emptyList(),
    val cargando: Boolean = true,
    val esSocioUsuarioActual: Boolean = false,
    val solicitandoInmediato: Boolean = false,
    val error: String? = null
)

class DetalleSocioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleSocioUiState())
    val uiState = _uiState.asStateFlow()

    private val client = SupabaseClient.client

    fun cargarDetalle(socioId: String) {
        val miId = client.auth.currentSessionOrNull()?.user?.id ?: ""
        
        viewModelScope.launch {
            try {
                val socio = UsuarioRepository.obtenerSocioPorId(socioId)
                val fotos = UsuarioRepository.obtenerFotosDeTrabajos(socioId)
                
                var esSocio = false
                if (miId.isNotEmpty()) {
                    val p = UsuarioRepository.obtenerSocioPorId(miId)
                    esSocio = p?.es_prestador == true
                }

                _uiState.update { it.copy(
                    socio = socio,
                    fotosTrabajos = fotos,
                    esSocioUsuarioActual = esSocio,
                    cargando = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }

    fun pedirServicioInmediato(context: Context, socioId: String, onExito: () -> Unit) {
        val miId = client.auth.currentSessionOrNull()?.user?.id ?: return
        _uiState.update { it.copy(solicitandoInmediato = true) }
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    viewModelScope.launch {
                        try {
                            val ahora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                            UsuarioRepository.crearCita(
                                miId, 
                                socioId, 
                                ahora, 
                                "SERVICIO URGENTE - SOLICITADO AHORA",
                                lat = loc?.latitude,
                                lon = loc?.longitude
                            )
                            _uiState.update { it.copy(solicitandoInmediato = false) }
                            onExito()
                        } catch (e: Exception) {
                            _uiState.update { it.copy(solicitandoInmediato = false, error = e.message) }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    _uiState.update { it.copy(solicitandoInmediato = false, error = e.message) }
                }
        } catch (e: SecurityException) {
            _uiState.update { it.copy(solicitandoInmediato = false, error = "Permiso de ubicación denegado") }
        }
    }

    fun agendarCita(socioId: String, fecha: String, detalles: String, onExito: () -> Unit) {
        val miId = client.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            try {
                UsuarioRepository.crearCita(miId, socioId, fecha, detalles)
                onExito()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}