package com.example.fixnow

import org.junit.Assert.assertEquals
import org.junit.Test

class IATest {

    @Test
    fun testActivacionIA_LogicaContador() {
        // La IA debe activarse en la 1ra, 4ta, 7ma, 10ma... reseña
        val casos = listOf(
            Pair(1, true),
            Pair(2, false),
            Pair(3, false),
            Pair(4, true),
            Pair(5, false),
            Pair(6, false),
            Pair(7, true)
        )

        for (caso in casos) {
            val numResenas = caso.first
            val debeActivarse = caso.second
            val resultado = checkDebeActivarIA(numResenas)
            assertEquals("Error para $numResenas reseñas", debeActivarse, resultado)
        }
    }

    @Test
    fun testIA_TokensAgotados() {
        // Simulamos tokens agotados
        val tokens = 0
        val resultado = simularLlamadaIA(tokens)
        assertEquals("Error: Sin tokens", resultado)
    }

    @Test
    fun testIA_ConTokens() {
        val tokens = 100
        val resultado = simularLlamadaIA(tokens)
        assertEquals("Resumen generado", resultado)
    }

    // Funciones auxiliares para la prueba (espejo de la lógica de la app)
    private fun checkDebeActivarIA(total: Int): Boolean {
        if (total <= 0) return false
        if (total == 1) return true
        return (total - 1) % 3 == 0
    }

    private fun simularLlamadaIA(tokens: Int): String {
        return if (tokens <= 0) "Error: Sin tokens" else "Resumen generado"
    }
}
