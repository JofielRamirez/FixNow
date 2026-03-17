package com.example.fixnow.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixnow.data.*
import com.example.fixnow.utils.LocationUtils
import com.example.fixnow.screens.ModoServicio
import com.example.fixnow.screens.SubServicio
import com.example.fixnow.screens.CategoriaExtra
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServiciosUiState(
    val categoriaSeleccionada: CategoriaExtra? = null,
    val subServicioSeleccionado: SubServicio? = null,
    val modoSeleccionado: ModoServicio? = null,
    val listaSocios: List<UsuarioPerfil> = emptyList(),
    val cargando: Boolean = false,
    val esSocio: Boolean = false,
    val miUbicacion: Pair<Double, Double>? = null,
    val error: String? = null
)

class ServiciosViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ServiciosUiState())
    val uiState = _uiState.asStateFlow()

    private val client = SupabaseClient.client

    val categorias = listOf(
        CategoriaExtra("Plomería",    "Plomeria",    Icons.Default.Build,       "Tuberías y más"),
        CategoriaExtra("Cerrajería",  "Cerrajeria",  Icons.Default.Lock,        "Llaves y cerraduras"),
        CategoriaExtra("Electricidad","Electricidad",Icons.Default.Bolt,        "Instalaciones"),
        CategoriaExtra("Mecánica",    "Mecanica",    Icons.Default.Settings,    "Autos y motores"),
        CategoriaExtra("Carpintería", "Carpinteria", Icons.Default.Home,        "Muebles y madera"),
        CategoriaExtra("Limpieza",    "Limpieza",    Icons.Default.Delete,      "Hogar y oficina")
    )

    val subServiciosMap = mapOf(
        "Plomería" to listOf(
            SubServicio("Arreglar escusado", "$500 - $1,200", Icons.Default.Build),
            SubServicio("Cambiar tubería", "$1,500 - $3,500", Icons.Default.Warning),
            SubServicio("Instalar lavamanos", "$800 - $1,800", Icons.Default.Add)
        ),
        "Cerrajería" to listOf(
            SubServicio("Abrir puerta", "$400 - $900", Icons.Default.LockOpen),
            SubServicio("Cambiar chapa", "$600 - $1,500", Icons.Default.Lock),
            SubServicio("Duplicado de llaves", "$100 - $300", Icons.Default.Key)
        ),
        "Electricidad" to listOf(
            SubServicio("Cortocircuito", "$600 - $2,000", Icons.Default.FlashOn),
            SubServicio("Instalar lámpara", "$300 - $800", Icons.Default.Lightbulb),
            SubServicio("Revisión de tablero", "$500 - $1,200", Icons.Default.Settings)
        ),
        "Mecánica" to listOf(
            SubServicio("Cambio de aceite", "$800 - $1,500", Icons.Default.Settings),
            SubServicio("Frenos", "$1,200 - $3,000", Icons.Default.Warning),
            SubServicio("Afinación", "$1,500 - $4,000", Icons.Default.Build)
        )
    )

    init {
        verificarSiEsSocio()
    }

    private fun verificarSiEsSocio() {
        val uid = client.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            val perfil = UsuarioRepository.obtenerSocioPorId(uid)
            _uiState.update { it.copy(esSocio = perfil?.es_prestador == true) }
        }
    }

    fun setUbicacion(lat: Double, lon: Double) {
        _uiState.update { it.copy(miUbicacion = Pair(lat, lon)) }
    }

    fun seleccionarCategoria(cat: CategoriaExtra, context: Context) {
        _uiState.update { it.copy(categoriaSeleccionada = cat) }
        AppEstadoPrefs.guardarUltimaCategoria(context, cat.nombre)
    }

    fun seleccionarSubServicio(sub: SubServicio) {
        _uiState.update { it.copy(subServicioSeleccionado = sub) }
    }

    fun seleccionarModo(modo: ModoServicio) {
        _uiState.update { it.copy(modoSeleccionado = modo, cargando = true) }
        buscarSocios()
    }

    private fun buscarSocios() {
        val catId = _uiState.value.categoriaSeleccionada?.idBusqueda ?: return
        val modo = _uiState.value.modoSeleccionado ?: return

        viewModelScope.launch {
            try {
                val rawSocios = if (modo == ModoServicio.INMEDIATO) {
                    UsuarioRepository.obtenerSociosDisponiblesPorCategoria(catId)
                } else {
                    UsuarioRepository.obtenerSociosPorCategoria(catId)
                }

                val ordenados = _uiState.value.miUbicacion?.let { loc ->
                    rawSocios.sortedBy { socio ->
                        if (socio.latitud != null && socio.longitud != null) {
                            LocationUtils.calcularDistancia(loc.first, loc.second, socio.latitud, socio.longitud)
                        } else Double.MAX_VALUE
                    }
                } ?: rawSocios

                _uiState.update { it.copy(listaSocios = ordenados, cargando = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, cargando = false) }
            }
        }
    }

    fun retroceder(context: Context) {
        _uiState.update { state ->
            when {
                state.modoSeleccionado != null -> state.copy(modoSeleccionado = null)
                state.subServicioSeleccionado != null -> state.copy(subServicioSeleccionado = null)
                else -> {
                    AppEstadoPrefs.guardarUltimaCategoria(context, "")
                    state.copy(categoriaSeleccionada = null)
                }
            }
        }
    }
}