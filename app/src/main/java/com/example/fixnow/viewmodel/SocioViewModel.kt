package com.example.fixnow.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import com.example.fixnow.utils.Resource
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SocioUiState(
    val perfil: UsuarioPerfil? = null,
    val citas: List<Cita> = emptyList(),
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val disponible: Boolean = false,
    val servicioEntrante: Cita? = null,
    val miUbicacion: Pair<Double, Double>? = null,
    val error: String? = null
)

class SocioViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SocioUiState())
    val uiState = _uiState.asStateFlow()

    private val client = SupabaseClient.client
    private val userId = client.auth.currentSessionOrNull()?.user?.id

    init {
        cargarDatosSocio()
        escucharSolicitudes()
    }

    private fun cargarDatosSocio() {
        val uid = userId ?: return
        viewModelScope.launch {
            try {
                val perfil = UsuarioRepository.obtenerSocioPorId(uid)
                val citas = UsuarioRepository.obtenerCitasSocio(uid)
                _uiState.update { it.copy(
                    perfil = perfil, 
                    citas = citas, 
                    disponible = perfil?.disponible ?: false,
                    cargando = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }

    private fun escucharSolicitudes() {
        val uid = userId ?: return
        val channel = client.channel("citas_socio_$uid")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "citas"
            filter = "id_socio=eq.$uid"
        }
        flow.onEach { action ->
            if (action is PostgresAction.Insert) {
                val nueva = action.decodeRecord<Cita>()
                if (nueva.estado == "pendiente" && nueva.detalles?.contains("URGENTE") == true) {
                    _uiState.update { it.copy(servicioEntrante = nueva) }
                }
                actualizarListaCitas()
            } else if (action is PostgresAction.Update) {
                actualizarListaCitas()
            }
        }.launchIn(viewModelScope)
        
        viewModelScope.launch { channel.subscribe() }
    }

    fun actualizarListaCitas() {
        val uid = userId ?: return
        viewModelScope.launch {
            val citas = UsuarioRepository.obtenerCitasSocio(uid)
            _uiState.update { it.copy(citas = citas) }
        }
    }

    fun toggleDisponibilidad(disponible: Boolean, context: Context) {
        val uid = userId ?: return
        _uiState.update { it.copy(disponible = disponible) }
        
        viewModelScope.launch {
            UsuarioRepository.actualizarDisponibilidad(uid, disponible)
            if (disponible) {
                iniciarRastreoUbicacion(context)
            }
        }
    }

    private fun iniciarRastreoUbicacion(context: Context) {
        val uid = userId ?: return
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        viewModelScope.launch {
            while (_uiState.value.disponible) {
                try {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { loc ->
                            loc?.let {
                                _uiState.update { it.copy(miUbicacion = Pair(it.latitude, it.longitude)) }
                                viewModelScope.launch {
                                    UsuarioRepository.actualizarUbicacion(uid, it.latitude, it.longitude)
                                }
                            }
                        }
                } catch (e: SecurityException) {
                    Log.e("SOCIO_VM", "Sin permisos GPS")
                    break
                }
                delay(30000)
            }
        }
    }

    fun aceptarServicio(citaId: String) {
        viewModelScope.launch {
            UsuarioRepository.actualizarEstadoCita(citaId, "aceptada")
            _uiState.update { it.copy(servicioEntrante = null) }
            actualizarListaCitas()
        }
    }

    fun rechazarServicio(citaId: String) {
        viewModelScope.launch {
            UsuarioRepository.actualizarEstadoCita(citaId, "cancelada")
            _uiState.update { it.copy(servicioEntrante = null) }
            actualizarListaCitas()
        }
    }
}