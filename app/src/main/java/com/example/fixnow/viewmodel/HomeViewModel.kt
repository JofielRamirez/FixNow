package com.example.fixnow.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val perfil: UsuarioPerfil? = null,
    val cargandoPerfil: Boolean = true,
    val servicioActivo: Cita? = null,
    val solicitudPendiente: Cita? = null,
    val avisoAceptacion: Cita? = null,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val client = SupabaseClient.client
    private val userId = client.auth.currentSessionOrNull()?.user?.id

    init {
        cargarDatos()
        escucharCambiosRealtime()
    }

    private fun cargarDatos() {
        val uid = userId ?: return
        
        viewModelScope.launch {
            try {
                // Cargar perfil
                val perfil = UsuarioRepository.obtenerSocioPorId(uid)
                _uiState.update { it.copy(perfil = perfil, cargandoPerfil = false) }

                // Cargar citas
                val citasSocioDeferred = async { UsuarioRepository.obtenerCitasSocio(uid) }
                val citasClienteDeferred = async { UsuarioRepository.obtenerCitasCliente(uid) }
                
                val citasSocio = citasSocioDeferred.await()
                val citasCliente = citasClienteDeferred.await()
                
                val activo = (citasSocio + citasCliente).find { 
                    it.estado == "aceptada" || it.estado == "en_camino" 
                }

                val pendiente = if (perfil?.es_prestador == true) {
                    citasSocio.find { it.estado == "pendiente" }
                } else null

                _uiState.update { it.copy(servicioActivo = activo, solicitudPendiente = pendiente) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, cargandoPerfil = false) }
            }
        }
    }

    private fun escucharCambiosRealtime() {
        val uid = userId ?: return
        val channel = client.channel("notificaciones_inicio")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "citas"
        }
        
        flow.onEach { action ->
            when (action) {
                is PostgresAction.Insert -> {
                    val nueva = action.decodeRecord<Cita>()
                    if (nueva.idSocio == uid && nueva.estado == "pendiente") {
                        _uiState.update { it.copy(solicitudPendiente = nueva) }
                    }
                }
                is PostgresAction.Update -> {
                    val actualizada = action.decodeRecord<Cita>()
                    if (actualizada.idCliente == uid || actualizada.idSocio == uid) {
                        _uiState.update { state ->
                            var nextState = state
                            
                            if (actualizada.idCliente == uid && actualizada.estado == "aceptada") {
                                nextState = nextState.copy(avisoAceptacion = actualizada, servicioActivo = actualizada)
                            }
                            
                            if (actualizada.estado == "aceptada" || actualizada.estado == "en_camino") {
                                nextState = nextState.copy(servicioActivo = actualizada)
                                if (actualizada.idSocio == uid) nextState = nextState.copy(solicitudPendiente = null)
                            } else if (listOf("completada", "cancelada", "finalizada").contains(actualizada.estado)) {
                                if (state.servicioActivo?.id == actualizada.id) nextState = nextState.copy(servicioActivo = null)
                                if (state.solicitudPendiente?.id == actualizada.id) nextState = nextState.copy(solicitudPendiente = null)
                            }
                            nextState
                        }
                    }
                }
                else -> Unit
            }
        }.launchIn(viewModelScope)
        
        viewModelScope.launch {
            channel.subscribe()
        }
    }

    fun aceptarCita(citaId: String) {
        viewModelScope.launch {
            try {
                UsuarioRepository.actualizarEstadoCita(citaId, "aceptada")
                _uiState.update { it.copy(solicitudPendiente = null) }
            } catch (e: Exception) {
                Log.e("HOME_VM", "Error al aceptar: ${e.message}")
            }
        }
    }

    fun rechazarSolicitud() {
        _uiState.update { it.copy(solicitudPendiente = null) }
    }

    fun cerrarAvisoAceptacion() {
        _uiState.update { it.copy(avisoAceptacion = null) }
    }
}