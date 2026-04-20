package com.example.fixnow

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import com.example.fixnow.data.*
import com.example.fixnow.screens.CategoriaExtra
import com.example.fixnow.screens.ModoServicio
import com.example.fixnow.screens.SubServicio
import com.example.fixnow.viewmodel.ServiciosViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.UserSession
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests — ServiciosViewModel
 * Persona 2
 *
 * Cubre (con mocks de Supabase y UsuarioRepository):
 *  - Selección de categoría y guardado en prefs
 *  - Selección de sub-servicio
 *  - Modo inmediato vs programado
 *  - Búsqueda de socios (éxito con datos, lista vacía, fallo)
 *  - Ordenamiento por distancia
 *  - Retroceso en la navegación de estados
 *  - Lista de categorías disponibles
 *  - Mapa de sub-servicios
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiciosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ServiciosViewModel
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock de SupabaseClient para evitar la inicialización real
        mockkObject(com.example.fixnow.data.SupabaseClient)
        val mockClient = mockk<SupabaseClient>(relaxed = true)
        every { com.example.fixnow.data.SupabaseClient.client } returns mockClient

        // Mock de Auth para que currentSessionOrNull devuelva null (no autenticado)
        val mockAuth = mockk<Auth>(relaxed = true)
        every { mockClient.auth } returns mockAuth
        every { mockAuth.currentSessionOrNull() } returns null

        // Mock de UsuarioRepository
        mockkObject(UsuarioRepository)

        // Mock de AppEstadoPrefs
        mockkObject(AppEstadoPrefs)
        every { AppEstadoPrefs.guardarUltimaCategoria(any(), any()) } just Runs

        // Mock de Context
        mockContext = mockk(relaxed = true)

        viewModel = ServiciosViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(com.example.fixnow.data.SupabaseClient)
        unmockkObject(UsuarioRepository)
        unmockkObject(AppEstadoPrefs)
    }

    // ──────────────────────────────────────────────────────────────
    // ESTADO INICIAL
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial no tiene categoria seleccionada`() {
        assertNull(viewModel.uiState.value.categoriaSeleccionada)
    }

    @Test
    fun `estado inicial no tiene sub-servicio seleccionado`() {
        assertNull(viewModel.uiState.value.subServicioSeleccionado)
    }

    @Test
    fun `estado inicial no tiene modo seleccionado`() {
        assertNull(viewModel.uiState.value.modoSeleccionado)
    }

    @Test
    fun `estado inicial tiene lista de socios vacia`() {
        assertTrue(viewModel.uiState.value.listaSocios.isEmpty())
    }

    @Test
    fun `estado inicial no esta cargando`() {
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `estado inicial no tiene error`() {
        assertNull(viewModel.uiState.value.error)
    }

    // ──────────────────────────────────────────────────────────────
    // CATEGORÍAS DISPONIBLES
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `la lista de categorias contiene 6 elementos`() {
        assertEquals(6, viewModel.categorias.size)
    }

    @Test
    fun `la lista de categorias contiene Plomeria`() {
        assertTrue(viewModel.categorias.any { it.nombre == "Plomería" })
    }

    @Test
    fun `la lista de categorias contiene Electricidad`() {
        assertTrue(viewModel.categorias.any { it.nombre == "Electricidad" })
    }

    @Test
    fun `cada categoria tiene idBusqueda sin acentos`() {
        viewModel.categorias.forEach { cat ->
            assertFalse(
                "idBusqueda '${cat.idBusqueda}' no debe tener acentos",
                cat.idBusqueda.contains(Regex("[áéíóúñ]"))
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // SUB-SERVICIOS
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `subServiciosMap contiene Plomeria`() {
        assertTrue(viewModel.subServiciosMap.containsKey("Plomería"))
    }

    @Test
    fun `subServiciosMap contiene Cerrajeria`() {
        assertTrue(viewModel.subServiciosMap.containsKey("Cerrajería"))
    }

    @Test
    fun `subServiciosMap de Plomeria tiene 3 sub-servicios`() {
        assertEquals(3, viewModel.subServiciosMap["Plomería"]?.size)
    }

    @Test
    fun `subServiciosMap de Electricidad tiene 3 sub-servicios`() {
        assertEquals(3, viewModel.subServiciosMap["Electricidad"]?.size)
    }

    // ──────────────────────────────────────────────────────────────
    // SELECCIÓN DE CATEGORÍA
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `seleccionarCategoria actualiza el estado`() {
        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Tuberías y más")

        viewModel.seleccionarCategoria(cat, mockContext)

        assertEquals(cat, viewModel.uiState.value.categoriaSeleccionada)
    }

    @Test
    fun `seleccionarCategoria guarda en AppEstadoPrefs`() {
        val cat = CategoriaExtra("Cerrajería", "Cerrajeria", Icons.Default.Lock, "Llaves")

        viewModel.seleccionarCategoria(cat, mockContext)

        verify { AppEstadoPrefs.guardarUltimaCategoria(mockContext, "Cerrajería") }
    }

    @Test
    fun `seleccionarCategoria permite cambiar de categoria`() {
        val cat1 = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        val cat2 = CategoriaExtra("Cerrajería", "Cerrajeria", Icons.Default.Lock, "Desc")

        viewModel.seleccionarCategoria(cat1, mockContext)
        assertEquals("Plomería", viewModel.uiState.value.categoriaSeleccionada?.nombre)

        viewModel.seleccionarCategoria(cat2, mockContext)
        assertEquals("Cerrajería", viewModel.uiState.value.categoriaSeleccionada?.nombre)
    }

    // ──────────────────────────────────────────────────────────────
    // SELECCIÓN DE SUB-SERVICIO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `seleccionarSubServicio actualiza el estado`() {
        val sub = SubServicio("Arreglar escusado", "$500 - $1,200", Icons.Default.Build)

        viewModel.seleccionarSubServicio(sub)

        assertEquals(sub, viewModel.uiState.value.subServicioSeleccionado)
    }

    @Test
    fun `seleccionarSubServicio permite cambiar de sub-servicio`() {
        val sub1 = SubServicio("Arreglar escusado", "$500", Icons.Default.Build)
        val sub2 = SubServicio("Cambiar tubería", "$1,500", Icons.Default.Build)

        viewModel.seleccionarSubServicio(sub1)
        assertEquals("Arreglar escusado", viewModel.uiState.value.subServicioSeleccionado?.nombre)

        viewModel.seleccionarSubServicio(sub2)
        assertEquals("Cambiar tubería", viewModel.uiState.value.subServicioSeleccionado?.nombre)
    }

    // ──────────────────────────────────────────────────────────────
    // MODO INMEDIATO vs PROGRAMADO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `seleccionarModo INMEDIATO actualiza el estado`() = runTest {
        // Preparar categoría antes de seleccionar modo
        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)

        coEvery { UsuarioRepository.obtenerSociosDisponiblesPorCategoria("Plomeria") } returns emptyList()

        viewModel.seleccionarModo(ModoServicio.INMEDIATO)

        assertEquals(ModoServicio.INMEDIATO, viewModel.uiState.value.modoSeleccionado)
    }

    @Test
    fun `seleccionarModo PROGRAMADO actualiza el estado`() = runTest {
        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)

        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Plomeria") } returns emptyList()

        viewModel.seleccionarModo(ModoServicio.PROGRAMADO)

        assertEquals(ModoServicio.PROGRAMADO, viewModel.uiState.value.modoSeleccionado)
    }

    @Test
    fun `seleccionarModo activa cargando`() = runTest {
        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)

        coEvery { UsuarioRepository.obtenerSociosDisponiblesPorCategoria(any()) } returns emptyList()

        viewModel.seleccionarModo(ModoServicio.INMEDIATO)

        // Justo después de seleccionar modo, cargando debe ser true
        assertTrue(viewModel.uiState.value.cargando)
    }

    // ──────────────────────────────────────────────────────────────
    // BÚSQUEDA DE SOCIOS
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `buscarSocios en modo INMEDIATO usa obtenerSociosDisponibles`() = runTest {
        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)

        val sociosDisponibles = listOf(
            UsuarioPerfil(id = "s1", nombre = "Juan", es_prestador = true, disponible = true, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "s2", nombre = "Pedro", es_prestador = true, disponible = true, tipo_servicio = "Plomeria")
        )

        coEvery { UsuarioRepository.obtenerSociosDisponiblesPorCategoria("Plomeria") } returns sociosDisponibles

        viewModel.seleccionarModo(ModoServicio.INMEDIATO)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.listaSocios.size)
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `buscarSocios en modo PROGRAMADO usa obtenerSociosPorCategoria`() = runTest {
        val cat = CategoriaExtra("Electricidad", "Electricidad", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)

        val todos = listOf(
            UsuarioPerfil(id = "s1", nombre = "Ana", disponible = true, tipo_servicio = "Electricidad"),
            UsuarioPerfil(id = "s2", nombre = "Luis", disponible = false, tipo_servicio = "Electricidad")
        )

        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Electricidad") } returns todos

        viewModel.seleccionarModo(ModoServicio.PROGRAMADO)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.listaSocios.size)
        coVerify { UsuarioRepository.obtenerSociosPorCategoria("Electricidad") }
    }

    @Test
    fun `buscarSocios devuelve lista vacia si no hay socios`() = runTest {
        val cat = CategoriaExtra("Limpieza", "Limpieza", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)

        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Limpieza") } returns emptyList()

        viewModel.seleccionarModo(ModoServicio.PROGRAMADO)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.listaSocios.isEmpty())
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `buscarSocios con error actualiza el estado de error`() = runTest {
        val cat = CategoriaExtra("Mecánica", "Mecanica", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)

        coEvery {
            UsuarioRepository.obtenerSociosPorCategoria("Mecanica")
        } throws Exception("Timeout de red")

        viewModel.seleccionarModo(ModoServicio.PROGRAMADO)
        advanceUntilIdle()

        assertEquals("Timeout de red", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.cargando)
    }

    // ──────────────────────────────────────────────────────────────
    // UBICACIÓN Y ORDENAMIENTO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `setUbicacion actualiza las coordenadas en el estado`() {
        viewModel.setUbicacion(32.5149, -117.0382)

        val ubicacion = viewModel.uiState.value.miUbicacion
        assertNotNull(ubicacion)
        assertEquals(32.5149, ubicacion!!.first, 0.0001)
        assertEquals(-117.0382, ubicacion.second, 0.0001)
    }

    @Test
    fun `buscarSocios ordena por distancia cuando hay ubicacion`() = runTest {
        // Socio lejano (CDMX) y socio cercano (Tijuana)
        val socios = listOf(
            UsuarioPerfil(id = "s-lejano", nombre = "Lejano", latitud = 19.4326, longitud = -99.1332, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "s-cercano", nombre = "Cercano", latitud = 32.5200, longitud = -117.0400, tipo_servicio = "Plomeria")
        )

        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)
        viewModel.setUbicacion(32.5149, -117.0382) // Ubicación en Tijuana

        coEvery { UsuarioRepository.obtenerSociosDisponiblesPorCategoria("Plomeria") } returns socios

        viewModel.seleccionarModo(ModoServicio.INMEDIATO)
        advanceUntilIdle()

        val resultado = viewModel.uiState.value.listaSocios
        assertEquals(2, resultado.size)
        assertEquals("s-cercano", resultado[0].id) // El cercano va primero
        assertEquals("s-lejano", resultado[1].id)
    }

    // ──────────────────────────────────────────────────────────────
    // RETROCESO EN NAVEGACIÓN
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `retroceder desde modo limpia el modo seleccionado`() {
        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        val sub = SubServicio("Arreglar escusado", "$500", Icons.Default.Build)

        viewModel.seleccionarCategoria(cat, mockContext)
        viewModel.seleccionarSubServicio(sub)
        // Simular que el modo está en el estado
        coEvery { UsuarioRepository.obtenerSociosDisponiblesPorCategoria(any()) } returns emptyList()
        viewModel.seleccionarModo(ModoServicio.INMEDIATO)

        viewModel.retroceder(mockContext)

        assertNull(viewModel.uiState.value.modoSeleccionado)
        assertNotNull(viewModel.uiState.value.subServicioSeleccionado) // Sub-servicio sigue
    }

    @Test
    fun `retroceder desde sub-servicio limpia el sub-servicio`() {
        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        val sub = SubServicio("Arreglar escusado", "$500", Icons.Default.Build)

        viewModel.seleccionarCategoria(cat, mockContext)
        viewModel.seleccionarSubServicio(sub)

        viewModel.retroceder(mockContext)

        assertNull(viewModel.uiState.value.subServicioSeleccionado)
        assertNotNull(viewModel.uiState.value.categoriaSeleccionada) // Categoría sigue
    }

    @Test
    fun `retroceder desde categoria limpia la categoria y prefs`() {
        val cat = CategoriaExtra("Plomería", "Plomeria", Icons.Default.Build, "Desc")
        viewModel.seleccionarCategoria(cat, mockContext)

        viewModel.retroceder(mockContext)

        assertNull(viewModel.uiState.value.categoriaSeleccionada)
        verify { AppEstadoPrefs.guardarUltimaCategoria(mockContext, "") }
    }
}
