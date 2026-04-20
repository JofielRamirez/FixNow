package com.example.fixnow

import com.example.fixnow.utils.LocationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun `calcularDistancia entre puntos iguales devuelve cero`() {
        val distancia = LocationUtils.calcularDistancia(32.5149, -117.0382, 32.5149, -117.0382)
        assertEquals(0.0, distancia, 0.0001)
    }

    @Test
    fun `calcularDistancia entre Tijuana y CDMX cae en rango esperado`() {
        val distancia = LocationUtils.calcularDistancia(32.5149, -117.0382, 19.4326, -99.1332)
        assertTrue(distancia in 2250.0..2350.0)
    }

    @Test
    fun `formatoDistancia menor a un kilometro devuelve metros`() {
        val texto = LocationUtils.formatoDistancia(0.45)
        assertEquals("450 m", texto)
    }

    @Test
    fun `formatoDistancia de un kilometro o mas devuelve kilometros con un decimal`() {
        val texto = LocationUtils.formatoDistancia(1.26)
        assertEquals("1.3 km", texto)
    }
}
