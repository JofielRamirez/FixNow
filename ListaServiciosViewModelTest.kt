package com.example.fixnow

import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.viewmodel.ListaServiciosViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests — ListaServiciosViewModel
 * Persona 2
 *
 * Cubre (con mocks de Supabase y UsuarioRepository):
 *  - cargarSocios exitoso con datos
 *  - cargarSocios con lista vacía
 *  - cargarSocios con error de red
 *  - Estado de carga (cargando)
 *  - Filtrado por categoría (verificación de parámetro)
 *  - Recarga de socios con nueva categoría
 *  - Validaciones de lógica de filtrado local
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListaServiciosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ListaServiciosViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(UsuarioRepository)
        viewModel = ListaServiciosViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(UsuarioRepository)
    }

    // ──────────────────────────────────────────────────────────────
    // ESTADO INICIAL
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial tiene lista de socios vacia`() {
        assertTrue(viewModel.uiState.value.socios.isEmpty())
    }

    @Test
    fun `estado inicial esta cargando`() {
        assertTrue(viewModel.uiState.value.cargando)
    }

    @Test
    fun `estado inicial no tiene error`() {
        assertNull(viewModel.uiState.value.error)
    }

    // ──────────────────────────────────────────────────────────────
    // cargarSocios — ÉXITO CON DATOS
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `cargarSocios exitoso actualiza la lista de socios`() = runTest {
        val sociosPlomeria = listOf(
            UsuarioPerfil(id = "s1", nombre = "Juan Plomero", es_prestador = true, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "s2", nombre = "María Plomera", es_prestador = true, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "s3", nombre = "Pedro Plomero", es_prestador = true, tipo_servicio = "Plomeria")
        )

        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Plomeria") } returns sociosPlomeria

        viewModel.cargarSocios("Plomeria")
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.socios.size)
        assertFalse(viewModel.uiState.value.cargando)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `cargarSocios exitoso marca cargando como false`() = runTest {
        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Electricidad") } returns listOf(
            UsuarioPerfil(id = "e1", nombre = "Electricista 1", tipo_servicio = "Electricidad")
        )

        viewModel.cargarSocios("Electricidad")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `cargarSocios preserva todos los datos del perfil`() = runTest {
        val socio = UsuarioPerfil(
            id = "socio-123",
            nombre = "Carlos Electricista",
            email = "carlos@email.com",
            es_prestador = true,
            tipo_servicio = "Electricidad",
            descripcion = "10 años de experiencia",
            disponible = true,
            latitud = 32.5149,
            longitud = -117.0382
        )

        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Electricidad") } returns listOf(socio)

        viewModel.cargarSocios("Electricidad")
        advanceUntilIdle()

        val resultado = viewModel.uiState.value.socios.first()
        assertEquals("socio-123", resultado.id)
        assertEquals("Carlos Electricista", resultado.nombre)
        assertEquals("10 años de experiencia", resultado.descripcion)
        assertEquals(true, resultado.disponible)
        assertEquals(32.5149, resultado.latitud!!, 0.0001)
    }

    // ──────────────────────────────────────────────────────────────
    // cargarSocios — LISTA VACÍA
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `cargarSocios con resultado vacio deja la lista vacia`() = runTest {
        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Carpinteria") } returns emptyList()

        viewModel.cargarSocios("Carpinteria")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.socios.isEmpty())
        assertFalse(viewModel.uiState.value.cargando)
        assertNull(viewModel.uiState.value.error)
    }

    // ──────────────────────────────────────────────────────────────
    // cargarSocios — ERROR DE RED
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `cargarSocios con error de red actualiza el estado de error`() = runTest {
        coEvery {
            UsuarioRepository.obtenerSociosPorCategoria("Mecanica")
        } throws Exception("Sin conexión a internet")

        viewModel.cargarSocios("Mecanica")
        advanceUntilIdle()

        assertEquals("Sin conexión a internet", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun `cargarSocios con error mantiene la lista vacia`() = runTest {
        coEvery {
            UsuarioRepository.obtenerSociosPorCategoria(any())
        } throws Exception("Error")

        viewModel.cargarSocios("Limpieza")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.socios.isEmpty())
    }

    @Test
    fun `cargarSocios con timeout reporta el error`() = runTest {
        coEvery {
            UsuarioRepository.obtenerSociosPorCategoria("Plomeria")
        } throws Exception("Request timeout")

        viewModel.cargarSocios("Plomeria")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error!!.contains("timeout", ignoreCase = true))
    }

    // ──────────────────────────────────────────────────────────────
    // FILTRADO POR CATEGORÍA (VERIFICACIÓN DE PARÁMETRO)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `cargarSocios pasa la categoria correcta al repositorio`() = runTest {
        coEvery { UsuarioRepository.obtenerSociosPorCategoria(any()) } returns emptyList()

        viewModel.cargarSocios("Cerrajeria")
        advanceUntilIdle()

        coVerify(exactly = 1) { UsuarioRepository.obtenerSociosPorCategoria("Cerrajeria") }
    }

    @Test
    fun `cargarSocios con Plomeria llama al repo con Plomeria`() = runTest {
        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Plomeria") } returns emptyList()

        viewModel.cargarSocios("Plomeria")
        advanceUntilIdle()

        coVerify { UsuarioRepository.obtenerSociosPorCategoria("Plomeria") }
    }

    @Test
    fun `cargarSocios con Electricidad llama al repo con Electricidad`() = runTest {
        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Electricidad") } returns emptyList()

        viewModel.cargarSocios("Electricidad")
        advanceUntilIdle()

        coVerify { UsuarioRepository.obtenerSociosPorCategoria("Electricidad") }
    }

    // ──────────────────────────────────────────────────────────────
    // RECARGA CON NUEVA CATEGORÍA
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `cargarSocios con nueva categoria reemplaza la lista anterior`() = runTest {
        val plomeros = listOf(
            UsuarioPerfil(id = "p1", nombre = "Plomero 1", tipo_servicio = "Plomeria")
        )
        val electricistas = listOf(
            UsuarioPerfil(id = "e1", nombre = "Electricista 1", tipo_servicio = "Electricidad"),
            UsuarioPerfil(id = "e2", nombre = "Electricista 2", tipo_servicio = "Electricidad")
        )

        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Plomeria") } returns plomeros
        coEvery { UsuarioRepository.obtenerSociosPorCategoria("Electricidad") } returns electricistas

        viewModel.cargarSocios("Plomeria")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.socios.size)

        viewModel.cargarSocios("Electricidad")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.socios.size)
        assertTrue(viewModel.uiState.value.socios.all { it.tipo_servicio == "Electricidad" })
    }

    // ──────────────────────────────────────────────────────────────
    // ESTADO DE CARGA (cargando)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `cargarSocios activa cargando al iniciar`() = runTest {
        coEvery { UsuarioRepository.obtenerSociosPorCategoria(any()) } returns emptyList()

        viewModel.cargarSocios("Plomeria")
        // Antes de avanzar, cargando debe estar en true
        assertTrue(viewModel.uiState.value.cargando)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.cargando)
    }

    // ──────────────────────────────────────────────────────────────
    // LÓGICA DE FILTRADO LOCAL (sin red)
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `filtrar socios por disponibilidad funciona localmente`() {
        val socios = listOf(
            UsuarioPerfil(id = "1", disponible = true, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "2", disponible = false, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "3", disponible = true, tipo_servicio = "Plomeria")
        )

        val disponibles = socios.filter { it.disponible == true }

        assertEquals(2, disponibles.size)
    }

    @Test
    fun `filtrar socios por categoria case-insensitive funciona`() {
        val socios = listOf(
            UsuarioPerfil(id = "1", tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "2", tipo_servicio = "plomeria"),
            UsuarioPerfil(id = "3", tipo_servicio = "PLOMERIA"),
            UsuarioPerfil(id = "4", tipo_servicio = "Electricidad")
        )

        val plomeros = socios.filter {
            it.tipo_servicio?.contains("plomeria", ignoreCase = true) == true
        }

        assertEquals(3, plomeros.size)
    }

    @Test
    fun `filtrar socios sin tipo_servicio los excluye`() {
        val socios = listOf(
            UsuarioPerfil(id = "1", tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "2", tipo_servicio = null),
            UsuarioPerfil(id = "3", tipo_servicio = "Plomeria")
        )

        val plomeros = socios.filter {
            it.tipo_servicio?.contains("Plomeria", ignoreCase = true) == true
        }

        assertEquals(2, plomeros.size)
    }

    @Test
    fun `filtrar socios que son prestadores funciona`() {
        val socios = listOf(
            UsuarioPerfil(id = "1", es_prestador = true, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "2", es_prestador = false, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "3", es_prestador = true, tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "4", es_prestador = null, tipo_servicio = "Plomeria")
        )

        val prestadores = socios.filter { it.es_prestador == true }

        assertEquals(2, prestadores.size)
    }
}
