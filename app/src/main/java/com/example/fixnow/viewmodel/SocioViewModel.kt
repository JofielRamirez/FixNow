@file:OptIn(io.github.jan.supabase.annotations.SupabaseExperimental::class)
package com.example.fixnow.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
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
                _uiState.update { it.copy(perfil = perfil, citas = citas, disponible = perfil?.disponible ?: false, cargando = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }

    private fun escucharSolicitudes() {
        val uid = userId ?: return
        val canalDeSolicitudes = client.realtime.channel("citas_socio_$uid")

        canalDeSolicitudes.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "citas"
            // Sintaxis corregida para V3 usando FilterOperator.EQ
            filter("id_socio", FilterOperator.EQ, uid)
        }.onEach { action ->
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

        viewModelScope.launch {
            canalDeSolicitudes.subscribe()
        }
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
            if (disponible) iniciarRastreoUbicacion(context)
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
                                val lat = it.latitude
                                val lon = it.longitude
                                _uiState.update { state -> state.copy(miUbicacion = Pair(lat, lon)) }
                                viewModelScope.launch {
                                    UsuarioRepository.actualizarUbicacion(uid, lat, lon)
                                }
                            }
                        }
                } catch (e: SecurityException) { break }
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