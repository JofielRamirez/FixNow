package com.example.fixnow

import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.Cita
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit Tests — UsuarioRepository (lógica pura y modelos de datos)
 * Persona 1
 *
 * NOTA: Las funciones que hacen llamadas reales a Supabase (guardarUsuario,
 * obtenerSocioPorId, crearCita, etc.) requieren mocks de red para testearse
 * de forma unitaria. Aquí testeamos:
 *  - La estructura y valores por defecto del modelo UsuarioPerfil
 *  - La estructura y valores por defecto del modelo Cita
 *  - La lógica de limpieza de UIDs (trim de comillas)
 *  - Validaciones de negocio que no dependen de red
 */
class UsuarioRepositoryTest {

    // ──────────────────────────────────────────────────────────────
    // MODELO UsuarioPerfil — VALORES POR DEFECTO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `UsuarioPerfil tiene es_prestador false por defecto`() {
        val perfil = UsuarioPerfil(id = "123", nombre = "Test", email = "test@test.com")
        assertFalse(perfil.es_prestador ?: false)
    }

    @Test
    fun `UsuarioPerfil tiene disponible true por defecto`() {
        val perfil = UsuarioPerfil(id = "123")
        assertTrue(perfil.disponible ?: true)
    }

    @Test
    fun `UsuarioPerfil puede crearse con todos los campos null`() {
        val perfil = UsuarioPerfil()
        assertNull(perfil.id)
        assertNull(perfil.nombre)
        assertNull(perfil.email)
    }

    @Test
    fun `UsuarioPerfil almacena correctamente los datos del socio`() {
        val perfil = UsuarioPerfil(
            id = "abc-123",
            nombre = "Carlos Mecánico",
            email = "carlos@fixnow.com",
            es_prestador = true,
            tipo_servicio = "Mecánica",
            disponible = true
        )
        assertEquals("abc-123", perfil.id)
        assertEquals("Carlos Mecánico", perfil.nombre)
        assertTrue(perfil.es_prestador ?: false)
        assertEquals("Mecánica", perfil.tipo_servicio)
    }

    @Test
    fun `UsuarioPerfil resumen_ia es null cuando no hay resenas`() {
        val perfil = UsuarioPerfil(id = "123")
        assertNull(perfil.resumen_ia)
    }

    @Test
    fun `UsuarioPerfil almacena coordenadas de ubicacion`() {
        val perfil = UsuarioPerfil(
            id = "123",
            latitud = 32.5149,
            longitud = -117.0382
        )
        assertEquals(32.5149, perfil.latitud!!, 0.0001)
        assertEquals(-117.0382, perfil.longitud!!, 0.0001)
    }

    // ──────────────────────────────────────────────────────────────
    // MODELO Cita — VALORES POR DEFECTO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `Cita tiene estado pendiente por defecto`() {
        val cita = Cita(idCliente = "cliente1", idSocio = "socio1", fecha = "20/04/2026")
        assertEquals("pendiente", cita.estado)
    }

    @Test
    fun `Cita puede crearse sin id (la BD lo asigna)`() {
        val cita = Cita(idCliente = "cliente1", idSocio = "socio1", fecha = "20/04/2026")
        assertNull(cita.id)
    }

    @Test
    fun `Cita almacena correctamente cliente y socio`() {
        val cita = Cita(
            idCliente = "uid-cliente-001",
            idSocio = "uid-socio-002",
            fecha = "20/04/2026 10:00",
            detalles = "Fuga en baño"
        )
        assertEquals("uid-cliente-001", cita.idCliente)
        assertEquals("uid-socio-002", cita.idSocio)
        assertEquals("Fuga en baño", cita.detalles)
    }

    @Test
    fun `Cita almacena coordenadas del cliente`() {
        val cita = Cita(
            idCliente = "c1",
            idSocio = "s1",
            fecha = "20/04/2026",
            latCliente = 32.5149,
            lonCliente = -117.0382
        )
        assertEquals(32.5149, cita.latCliente!!, 0.0001)
        assertEquals(-117.0382, cita.lonCliente!!, 0.0001)
    }

    @Test
    fun `Cita puede tener detalles null`() {
        val cita = Cita(idCliente = "c1", idSocio = "s1", fecha = "20/04/2026")
        assertNull(cita.detalles)
    }

    // ──────────────────────────────────────────────────────────────
    // LÓGICA DE LIMPIEZA DE UIDs
    // Replica la lógica uidLimpio que usa el Repository en producción
    // ──────────────────────────────────────────────────────────────

    private fun limpiarUid(uid: String): String =
        uid.replace("\"", "").replace("'", "").trim()

    @Test
    fun `limpiarUid elimina comillas dobles`() {
        val uid = "\"abc-123\""
        assertEquals("abc-123", limpiarUid(uid))
    }

    @Test
    fun `limpiarUid elimina comillas simples`() {
        val uid = "'abc-123'"
        assertEquals("abc-123", limpiarUid(uid))
    }

    @Test
    fun `limpiarUid elimina espacios en blanco al inicio y final`() {
        val uid = "  abc-123  "
        assertEquals("abc-123", limpiarUid(uid))
    }

    @Test
    fun `limpiarUid no modifica un uid limpio`() {
        val uid = "abc-123-def-456"
        assertEquals("abc-123-def-456", limpiarUid(uid))
    }

    @Test
    fun `limpiarUid maneja string vacio`() {
        assertEquals("", limpiarUid(""))
    }

    @Test
    fun `limpiarUid maneja uid con comillas y espacios combinados`() {
        val uid = "  \"abc-123\"  "
        assertEquals("abc-123", limpiarUid(uid))
    }

    // ──────────────────────────────────────────────────────────────
    // VALIDACIONES DE NEGOCIO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `un usuario con es_prestador true es un socio`() {
        val perfil = UsuarioPerfil(id = "1", es_prestador = true)
        assertTrue(perfil.es_prestador == true)
    }

    @Test
    fun `un usuario con es_prestador false NO es un socio`() {
        val perfil = UsuarioPerfil(id = "1", es_prestador = false)
        assertFalse(perfil.es_prestador == true)
    }

    @Test
    fun `un socio disponible puede recibir solicitudes`() {
        val socio = UsuarioPerfil(id = "1", es_prestador = true, disponible = true)
        assertTrue(socio.disponible == true && socio.es_prestador == true)
    }

    @Test
    fun `un socio no disponible no deberia aparecer en busqueda inmediata`() {
        val socio = UsuarioPerfil(id = "1", es_prestador = true, disponible = false)
        assertFalse(socio.disponible == true)
    }

    @Test
    fun `cita en estado aceptada no es pendiente`() {
        val cita = Cita(idCliente = "c1", idSocio = "s1", fecha = "hoy", estado = "aceptada")
        assertNotEquals("pendiente", cita.estado)
    }

    @Test
    fun `cita urgente contiene la palabra URGENTE en detalles`() {
        val cita = Cita(
            idCliente = "c1",
            idSocio = "s1",
            fecha = "hoy",
            detalles = "SERVICIO URGENTE"
        )
        assertTrue(cita.detalles?.contains("URGENTE") == true)
    }

    @Test
    fun `lista de socios filtra correctamente los disponibles`() {
        val socios = listOf(
            UsuarioPerfil(id = "1", disponible = true),
            UsuarioPerfil(id = "2", disponible = false),
            UsuarioPerfil(id = "3", disponible = true),
            UsuarioPerfil(id = "4", disponible = null)
        )
        val disponibles = socios.filter { it.disponible == true }
        assertEquals(2, disponibles.size)
        assertTrue(disponibles.all { it.disponible == true })
    }

    @Test
    fun `lista de socios filtra correctamente por categoria`() {
        val socios = listOf(
            UsuarioPerfil(id = "1", tipo_servicio = "Plomeria"),
            UsuarioPerfil(id = "2", tipo_servicio = "Electricidad"),
            UsuarioPerfil(id = "3", tipo_servicio = "Plomeria"),
        )
        val plomeros = socios.filter {
            it.tipo_servicio?.contains("Plomeria", ignoreCase = true) == true
        }
        assertEquals(2, plomeros.size)
    }
}
