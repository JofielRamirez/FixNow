package com.example.fixnow

import com.example.fixnow.utils.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceTest {

    @Test
    fun `Success guarda data y no mensaje`() {
        val result = Resource.Success("ok")
        assertTrue(result is Resource.Success)
        assertEquals("ok", result.data)
        assertNull(result.message)
    }

    @Test
    fun `Error guarda mensaje y data opcional`() {
        val result = Resource.Error("fallo", 123)
        assertTrue(result is Resource.Error)
        assertEquals("fallo", result.message)
        assertEquals(123, result.data)
    }

    @Test
    fun `Loading puede mantener data parcial`() {
        val result = Resource.Loading("cache")
        assertTrue(result is Resource.Loading)
        assertEquals("cache", result.data)
        assertNull(result.message)
    }
}
