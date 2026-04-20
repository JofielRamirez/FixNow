package com.example.fixnow

import com.example.fixnow.data.IARepository
import com.example.fixnow.data.ResenaDB
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IARepositoryTest {

    private lateinit var originalObtenerResenas: suspend (String) -> List<ResenaDB>
    private lateinit var originalGenerarResumenConGemini: suspend (String) -> String?
    private lateinit var originalGuardarResumenSocio: suspend (String, String) -> Unit

    @Before
    fun setUp() {
        originalObtenerResenas = IARepository.obtenerResenas
        originalGenerarResumenConGemini = IARepository.generarResumenConGemini
        originalGuardarResumenSocio = IARepository.guardarResumenSocio
    }

    @After
    fun tearDown() {
        IARepository.obtenerResenas = originalObtenerResenas
        IARepository.generarResumenConGemini = originalGenerarResumenConGemini
        IARepository.guardarResumenSocio = originalGuardarResumenSocio
    }

    @Test
    fun `generarResumenSocio regresa false cuando Supabase no devuelve resenas`() = runTest {
        var httpInvocado = false

        IARepository.obtenerResenas = { emptyList() }
        IARepository.generarResumenConGemini = {
            httpInvocado = true
            "Resumen"
        }

        val resultado = IARepository.generarResumenSocio("socio-1")

        assertFalse(resultado)
        assertFalse(httpInvocado)
    }

    @Test
    fun `generarResumenSocio exitoso usa mocks de Supabase y HTTP`() = runTest {
        var uidGuardado = ""
        var resumenGuardado = ""

        IARepository.obtenerResenas = {
            listOf(
                ResenaDB("socio-1", "cliente-1", 5, "Excelente trabajo"),
                ResenaDB("socio-1", "cliente-2", 4, "Llegó puntual")
            )
        }
        IARepository.generarResumenConGemini = { "Buen servicio y puntualidad consistente." }
        IARepository.guardarResumenSocio = { uid, resumen ->
            uidGuardado = uid
            resumenGuardado = resumen
        }

        val resultado = IARepository.generarResumenSocio(" \"socio-1\" ")

        assertTrue(resultado)
        assertEquals("socio-1", uidGuardado)
        assertEquals("Buen servicio y puntualidad consistente.", resumenGuardado)
    }

    @Test
    fun `generarResumenSocio regresa false cuando HTTP devuelve resumen vacio`() = runTest {
        var guardadoInvocado = false

        IARepository.obtenerResenas = {
            listOf(ResenaDB("socio-1", "cliente-1", 5, "Muy recomendado"))
        }
        IARepository.generarResumenConGemini = { "   " }
        IARepository.guardarResumenSocio = { _, _ -> guardadoInvocado = true }

        val resultado = IARepository.generarResumenSocio("socio-1")

        assertFalse(resultado)
        assertFalse(guardadoInvocado)
    }

    @Test
    fun `generarResumenSocio regresa false cuando Supabase lanza excepcion`() = runTest {
        IARepository.obtenerResenas = { throw RuntimeException("fallo supabase") }

        val resultado = IARepository.generarResumenSocio("socio-1")

        assertFalse(resultado)
    }
}
