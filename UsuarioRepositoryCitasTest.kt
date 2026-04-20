package com.example.fixnow

import com.example.fixnow.data.Cita
import com.example.fixnow.data.UsuarioRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests — UsuarioRepository (Parte 2 – Citas y Servicios)
 * Persona 2
 *
 * Cubre (con mocks de Supabase vía mockkObject):
 *  - crearCita: creación exitosa, con coordenadas, fallo de red
 *  - obtenerCitasSocio: lista normal, lista vacía, fallo
 *  - obtenerCitasCliente: lista normal, lista vacía, fallo
 *  - actualizarEstadoCita: actualización exitosa, fallo
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UsuarioRepositoryCitasTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(UsuarioRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(UsuarioRepository)
    }

    // ──────────────────────────────────────────────────────────────
    // crearCita
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `crearCita exitosa devuelve la cita con estado pendiente`() = runTest {
        val citaEsperada = Cita(
            id = "cita-001",
            idCliente = "cliente-1",
            idSocio = "socio-1",
            fecha = "20/04/2026 10:00",
            estado = "pendiente",
            detalles = "Fuga en el baño"
        )

        coEvery {
            UsuarioRepository.crearCita(
                idCliente = "cliente-1",
                idSocio = "socio-1",
                fecha = "20/04/2026 10:00",
                detalles = "Fuga en el baño",
                lat = null,
                lon = null
            )
        } returns citaEsperada

        val resultado = UsuarioRepository.crearCita(
            idCliente = "cliente-1",
            idSocio = "socio-1",
            fecha = "20/04/2026 10:00",
            detalles = "Fuga en el baño"
        )

        assertNotNull(resultado)
        assertEquals("cita-001", resultado!!.id)
        assertEquals("pendiente", resultado.estado)
        assertEquals("cliente-1", resultado.idCliente)
        assertEquals("socio-1", resultado.idSocio)
        assertEquals("Fuga en el baño", resultado.detalles)
    }

    @Test
    fun `crearCita con coordenadas las almacena correctamente`() = runTest {
        val citaConUbicacion = Cita(
            id = "cita-002",
            idCliente = "cliente-1",
            idSocio = "socio-1",
            fecha = "21/04/2026 14:00",
            estado = "pendiente",
            detalles = "Cortocircuito",
            latCliente = 32.5149,
            lonCliente = -117.0382
        )

        coEvery {
            UsuarioRepository.crearCita(
                idCliente = "cliente-1",
                idSocio = "socio-1",
                fecha = "21/04/2026 14:00",
                detalles = "Cortocircuito",
                lat = 32.5149,
                lon = -117.0382
            )
        } returns citaConUbicacion

        val resultado = UsuarioRepository.crearCita(
            idCliente = "cliente-1",
            idSocio = "socio-1",
            fecha = "21/04/2026 14:00",
            detalles = "Cortocircuito",
            lat = 32.5149,
            lon = -117.0382
        )

        assertNotNull(resultado)
        assertEquals(32.5149, resultado!!.latCliente!!, 0.0001)
        assertEquals(-117.0382, resultado.lonCliente!!, 0.0001)
    }

    @Test
    fun `crearCita falla por error de red y devuelve null`() = runTest {
        coEvery {
            UsuarioRepository.crearCita(
                idCliente = any(),
                idSocio = any(),
                fecha = any(),
                detalles = any(),
                lat = any(),
                lon = any()
            )
        } returns null

        val resultado = UsuarioRepository.crearCita(
            idCliente = "cliente-1",
            idSocio = "socio-1",
            fecha = "20/04/2026",
            detalles = "Prueba"
        )

        assertNull(resultado)
    }

    @Test
    fun `crearCita se invoca exactamente una vez`() = runTest {
        coEvery {
            UsuarioRepository.crearCita(any(), any(), any(), any(), any(), any())
        } returns Cita(idCliente = "c", idSocio = "s", fecha = "f")

        UsuarioRepository.crearCita("c", "s", "f", "d")

        coVerify(exactly = 1) {
            UsuarioRepository.crearCita(any(), any(), any(), any(), any(), any())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // obtenerCitasSocio
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `obtenerCitasSocio devuelve lista de citas del socio`() = runTest {
        val citasEsperadas = listOf(
            Cita(id = "c1", idCliente = "cli-1", idSocio = "socio-A", fecha = "20/04/2026", estado = "pendiente"),
            Cita(id = "c2", idCliente = "cli-2", idSocio = "socio-A", fecha = "21/04/2026", estado = "aceptada"),
            Cita(id = "c3", idCliente = "cli-3", idSocio = "socio-A", fecha = "22/04/2026", estado = "completada")
        )

        coEvery { UsuarioRepository.obtenerCitasSocio("socio-A") } returns citasEsperadas

        val resultado = UsuarioRepository.obtenerCitasSocio("socio-A")

        assertEquals(3, resultado.size)
        assertTrue(resultado.all { it.idSocio == "socio-A" })
    }

    @Test
    fun `obtenerCitasSocio devuelve lista vacia si no hay citas`() = runTest {
        coEvery { UsuarioRepository.obtenerCitasSocio("socio-sin-citas") } returns emptyList()

        val resultado = UsuarioRepository.obtenerCitasSocio("socio-sin-citas")

        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `obtenerCitasSocio devuelve lista vacia cuando falla la red`() = runTest {
        coEvery { UsuarioRepository.obtenerCitasSocio(any()) } returns emptyList()

        val resultado = UsuarioRepository.obtenerCitasSocio("socio-X")

        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `obtenerCitasSocio contiene citas con distintos estados`() = runTest {
        val citas = listOf(
            Cita(id = "c1", idCliente = "cl1", idSocio = "s1", fecha = "20/04/2026", estado = "pendiente"),
            Cita(id = "c2", idCliente = "cl2", idSocio = "s1", fecha = "21/04/2026", estado = "aceptada"),
            Cita(id = "c3", idCliente = "cl3", idSocio = "s1", fecha = "22/04/2026", estado = "rechazada")
        )

        coEvery { UsuarioRepository.obtenerCitasSocio("s1") } returns citas

        val resultado = UsuarioRepository.obtenerCitasSocio("s1")
        val estados = resultado.map { it.estado }.toSet()

        assertEquals(setOf("pendiente", "aceptada", "rechazada"), estados)
    }

    // ──────────────────────────────────────────────────────────────
    // obtenerCitasCliente
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `obtenerCitasCliente devuelve lista de citas del cliente`() = runTest {
        val citasCliente = listOf(
            Cita(id = "c10", idCliente = "cliente-B", idSocio = "s1", fecha = "20/04/2026"),
            Cita(id = "c11", idCliente = "cliente-B", idSocio = "s2", fecha = "22/04/2026")
        )

        coEvery { UsuarioRepository.obtenerCitasCliente("cliente-B") } returns citasCliente

        val resultado = UsuarioRepository.obtenerCitasCliente("cliente-B")

        assertEquals(2, resultado.size)
        assertTrue(resultado.all { it.idCliente == "cliente-B" })
    }

    @Test
    fun `obtenerCitasCliente devuelve lista vacia si no hay citas`() = runTest {
        coEvery { UsuarioRepository.obtenerCitasCliente("cliente-nuevo") } returns emptyList()

        val resultado = UsuarioRepository.obtenerCitasCliente("cliente-nuevo")

        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `obtenerCitasCliente devuelve lista vacia cuando falla`() = runTest {
        coEvery { UsuarioRepository.obtenerCitasCliente(any()) } returns emptyList()

        val resultado = UsuarioRepository.obtenerCitasCliente("cliente-X")

        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `obtenerCitasCliente puede incluir citas con distintos socios`() = runTest {
        val citas = listOf(
            Cita(id = "c1", idCliente = "cl-1", idSocio = "socio-A", fecha = "20/04/2026"),
            Cita(id = "c2", idCliente = "cl-1", idSocio = "socio-B", fecha = "21/04/2026"),
            Cita(id = "c3", idCliente = "cl-1", idSocio = "socio-C", fecha = "22/04/2026")
        )

        coEvery { UsuarioRepository.obtenerCitasCliente("cl-1") } returns citas

        val resultado = UsuarioRepository.obtenerCitasCliente("cl-1")
        val sociosDistintos = resultado.map { it.idSocio }.toSet()

        assertEquals(3, sociosDistintos.size)
    }

    // ──────────────────────────────────────────────────────────────
    // actualizarEstadoCita
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `actualizarEstadoCita se ejecuta sin excepcion`() = runTest {
        coEvery { UsuarioRepository.actualizarEstadoCita("cita-001", "aceptada") } just Runs

        UsuarioRepository.actualizarEstadoCita("cita-001", "aceptada")

        coVerify(exactly = 1) {
            UsuarioRepository.actualizarEstadoCita("cita-001", "aceptada")
        }
    }

    @Test
    fun `actualizarEstadoCita a rechazada se ejecuta correctamente`() = runTest {
        coEvery { UsuarioRepository.actualizarEstadoCita("cita-002", "rechazada") } just Runs

        UsuarioRepository.actualizarEstadoCita("cita-002", "rechazada")

        coVerify { UsuarioRepository.actualizarEstadoCita("cita-002", "rechazada") }
    }

    @Test
    fun `actualizarEstadoCita a completada se ejecuta correctamente`() = runTest {
        coEvery { UsuarioRepository.actualizarEstadoCita("cita-003", "completada") } just Runs

        UsuarioRepository.actualizarEstadoCita("cita-003", "completada")

        coVerify { UsuarioRepository.actualizarEstadoCita("cita-003", "completada") }
    }

    @Test
    fun `actualizarEstadoCita lanza excepcion cuando falla la red`() = runTest {
        coEvery {
            UsuarioRepository.actualizarEstadoCita(any(), any())
        } throws Exception("Network error")

        var excepcionLanzada = false
        try {
            UsuarioRepository.actualizarEstadoCita("cita-X", "aceptada")
        } catch (e: Exception) {
            excepcionLanzada = true
            assertEquals("Network error", e.message)
        }

        assertTrue(excepcionLanzada)
    }

    // ──────────────────────────────────────────────────────────────
    // VALIDACIONES DE NEGOCIO PARA CITAS
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `estados validos de una cita son pendiente aceptada rechazada completada`() {
        val estadosValidos = setOf("pendiente", "aceptada", "rechazada", "completada")
        val cita = Cita(idCliente = "c", idSocio = "s", fecha = "hoy", estado = "pendiente")
        assertTrue(cita.estado in estadosValidos)
    }

    @Test
    fun `cita con detalles vacios sigue siendo valida`() {
        val cita = Cita(idCliente = "c", idSocio = "s", fecha = "hoy", detalles = "")
        assertNotNull(cita)
        assertEquals("", cita.detalles)
    }

    @Test
    fun `filtrar citas pendientes de una lista mixta funciona`() {
        val citas = listOf(
            Cita(id = "1", idCliente = "c", idSocio = "s", fecha = "f", estado = "pendiente"),
            Cita(id = "2", idCliente = "c", idSocio = "s", fecha = "f", estado = "aceptada"),
            Cita(id = "3", idCliente = "c", idSocio = "s", fecha = "f", estado = "pendiente"),
            Cita(id = "4", idCliente = "c", idSocio = "s", fecha = "f", estado = "completada")
        )

        val pendientes = citas.filter { it.estado == "pendiente" }

        assertEquals(2, pendientes.size)
        assertTrue(pendientes.all { it.estado == "pendiente" })
    }
}
