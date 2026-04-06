@file:OptIn(io.github.jan.supabase.annotations.SupabaseExperimental::class)
package com.example.fixnow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeguimientoUiState(
    val cita: Cita? = null,
    val socio: UsuarioPerfil? = null,
    val cargando: Boolean = true,
    val completada: Boolean = false,
    val error: String? = null
)

class SeguimientoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SeguimientoUiState())
    val uiState = _uiState.asStateFlow()

    private val client = SupabaseClient.client

    fun iniciarSeguimiento(citaId: String) {
        viewModelScope.launch {
            try {
                val cita = UsuarioRepository.obtenerCitaPorId(citaId)
                if (cita != null) {
                    val socio = UsuarioRepository.obtenerSocioPorId(cita.idSocio)
                    _uiState.update { it.copy(cita = cita, socio = socio, cargando = false) }
                    escucharCambiosRealtime(citaId, cita.idSocio)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(cargando = false, error = e.message) }
            }
        }
    }

    private fun escucharCambiosRealtime(citaId: String, socioId: String) {
        val canalSocioSeguimiento = client.realtime.channel("seguimiento_socio_$socioId")

        canalSocioSeguimiento.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "Usuarios"
            filter("id", FilterOperator.EQ, socioId)
        }.onEach { action ->
            if (action is PostgresAction.Update) {
                val updatedSocio = action.decodeRecord<UsuarioPerfil>()
                _uiState.update { it.copy(socio = updatedSocio) }
            }
        }.launchIn(viewModelScope)

        val canalCitaSeguimiento = client.realtime.channel("seguimiento_cita_$citaId")

        canalCitaSeguimiento.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "citas"
            filter("id", FilterOperator.EQ, citaId)
        }.onEach { action ->
            if (action is PostgresAction.Update) {
                val citaAct = action.decodeRecord<Cita>()
                _uiState.update { it.copy(cita = citaAct) }
                if (citaAct.estado == "completada" || citaAct.estado == "finalizada") {
                    _uiState.update { it.copy(completada = true) }
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            canalSocioSeguimiento.subscribe()
            canalCitaSeguimiento.subscribe()
        }
    }
}