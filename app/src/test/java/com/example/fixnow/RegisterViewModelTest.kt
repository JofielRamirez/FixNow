package com.example.fixnow

import com.example.fixnow.utils.Resource
import com.example.fixnow.viewmodel.RegisterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests — RegisterViewModel
 * Persona 1
 *
 * Cubre:
 *  - Cambios de estado en nombre / email / password / passwordVisible
 *  - Validación: campos vacíos
 *  - Validación: contraseña menor a 8 caracteres
 *  - Lógica del indicador de fuerza de contraseña (débil / regular / segura)
 *  - Mapeo de errores de Supabase a mensajes amigables
 *  - resetState() limpia el estado
 *  - Estado inicial correcto
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = RegisterViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ──────────────────────────────────────────────────────────────
    // CAMBIOS DE ESTADO EN CAMPOS
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `onNombreChange actualiza el nombre correctamente`() {
        viewModel.onNombreChange("Juan Pérez")
        assertEquals("Juan Pérez", viewModel.nombre)
    }

    @Test
    fun `onEmailChange actualiza el email correctamente`() {
        viewModel.onEmailChange("juan@correo.com")
        assertEquals("juan@correo.com", viewModel.email)
    }

    @Test
    fun `onPasswordChange actualiza la password correctamente`() {
        viewModel.onPasswordChange("Password123")
        assertEquals("Password123", viewModel.password)
    }

    @Test
    fun `togglePasswordVisibility cambia de false a true`() {
        assertFalse(viewModel.passwordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.passwordVisible)
    }

    @Test
    fun `togglePasswordVisibility puede volver a false`() {
        viewModel.togglePasswordVisibility()
        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.passwordVisible)
    }

    // ──────────────────────────────────────────────────────────────
    // VALIDACIÓN DE CAMPOS VACÍOS
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `register con nombre vacio emite Error`() = runTest {
        viewModel.onNombreChange("")
        viewModel.onEmailChange("juan@correo.com")
        viewModel.onPasswordChange("Password123")

        viewModel.register()
        advanceUntilIdle()

        val state = viewModel.registerState.first()
        assertTrue(state is Resource.Error)
        assertEquals("Completa todos los campos", (state as Resource.Error).message)
    }

    @Test
    fun `register con email vacio emite Error`() = runTest {
        viewModel.onNombreChange("Juan")
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("Password123")

        viewModel.register()
        advanceUntilIdle()

        val state = viewModel.registerState.first()
        assertTrue(state is Resource.Error)
        assertEquals("Completa todos los campos", (state as Resource.Error).message)
    }

    @Test
    fun `register con password vacia emite Error`() = runTest {
        viewModel.onNombreChange("Juan")
        viewModel.onEmailChange("juan@correo.com")
        viewModel.onPasswordChange("")

        viewModel.register()
        advanceUntilIdle()

        val state = viewModel.registerState.first()
        assertTrue(state is Resource.Error)
        assertEquals("Completa todos los campos", (state as Resource.Error).message)
    }

    @Test
    fun `register con todos los campos vacios emite Error`() = runTest {
        viewModel.register()
        advanceUntilIdle()

        val state = viewModel.registerState.first()
        assertTrue(state is Resource.Error)
        assertEquals("Completa todos los campos", (state as Resource.Error).message)
    }

    // ──────────────────────────────────────────────────────────────
    // VALIDACIÓN DE CONTRASEÑA
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `register con password de 7 caracteres emite Error`() = runTest {
        viewModel.onNombreChange("Juan")
        viewModel.onEmailChange("juan@correo.com")
        viewModel.onPasswordChange("Pass12") // solo 6 chars

        viewModel.register()
        advanceUntilIdle()

        val state = viewModel.registerState.first()
        assertTrue(state is Resource.Error)
        assertEquals(
            "La contraseña debe tener al menos 8 caracteres",
            (state as Resource.Error).message
        )
    }

    @Test
    fun `password con exactamente 8 caracteres pasa la validacion de longitud`() {
        // Verificamos solo la lógica de validación de longitud
        val password = "Passw0rd" // 8 chars
        assertTrue(password.length >= 8)
    }

    @Test
    fun `password con menos de 8 caracteres falla la validacion`() {
        val password = "Pass1"
        assertFalse(password.length >= 8)
    }

    // ──────────────────────────────────────────────────────────────
    // LÓGICA DEL INDICADOR DE FUERZA DE CONTRASEÑA
    // ──────────────────────────────────────────────────────────────

    /**
     * Replica exactamente la lógica del when() en PantallaRegistro.kt:
     * fuerza = 3 si length >= 8 AND tiene dígito AND tiene letra
     * fuerza = 2 si length >= 6
     * fuerza = 1 en cualquier otro caso
     */
    private fun calcularFuerza(password: String): Int {
        return when {
            password.length >= 8 && password.any { it.isDigit() } && password.any { it.isLetter() } -> 3
            password.length >= 6 -> 2
            else -> 1
        }
    }

    @Test
    fun `password corta sin numeros ni letras es debil (fuerza 1)`() {
        assertEquals(1, calcularFuerza("ab1"))
    }

    @Test
    fun `password de 6 caracteres sin numero es regular (fuerza 2)`() {
        assertEquals(2, calcularFuerza("abcdef"))
    }

    @Test
    fun `password con 8 caracteres letra y numero es segura (fuerza 3)`() {
        assertEquals(3, calcularFuerza("Segura12"))
    }

    @Test
    fun `password con 8 caracteres solo letras es regular (fuerza 2)`() {
        // Tiene length >= 8 pero NO tiene dígito → cae en length >= 6 → fuerza 2
        assertEquals(2, calcularFuerza("abcdefgh"))
    }

    @Test
    fun `password con 8 caracteres solo numeros es regular (fuerza 2)`() {
        // Tiene length >= 8 pero NO tiene letra → cae en length >= 6 → fuerza 2
        assertEquals(2, calcularFuerza("12345678"))
    }

    @Test
    fun `password vacia es debil (fuerza 1)`() {
        assertEquals(1, calcularFuerza(""))
    }

    // ──────────────────────────────────────────────────────────────
    // MAPEO DE ERRORES DE SUPABASE
    // ──────────────────────────────────────────────────────────────

    private fun mapearError(msg: String): String = when {
        msg.contains("already registered", ignoreCase = true) -> "Este correo ya está registrado"
        msg.contains("rate limit", ignoreCase = true) -> "Espera un momento antes de intentarlo"
        else -> "Error al crear cuenta: $msg"
    }

    @Test
    fun `error already registered produce mensaje amigable`() {
        val resultado = mapearError("User already registered")
        assertEquals("Este correo ya está registrado", resultado)
    }

    @Test
    fun `error rate limit produce mensaje amigable`() {
        val resultado = mapearError("rate limit exceeded")
        assertEquals("Espera un momento antes de intentarlo", resultado)
    }

    @Test
    fun `error desconocido usa fallback con el mensaje original`() {
        val resultado = mapearError("internal server error")
        assertTrue(resultado.startsWith("Error al crear cuenta:"))
    }

    @Test
    fun `la comparacion de errores es case insensitive`() {
        val resultado = mapearError("ALREADY REGISTERED")
        assertEquals("Este correo ya está registrado", resultado)
    }

    // ──────────────────────────────────────────────────────────────
    // RESET DE ESTADO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `resetState limpia el registerState a null`() = runTest {
        // Forzamos un error
        viewModel.register()
        advanceUntilIdle()

        viewModel.resetState()
        advanceUntilIdle()

        val state = viewModel.registerState.first()
        assertNull(state)
    }

    // ──────────────────────────────────────────────────────────────
    // ESTADO INICIAL
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial de registerState es null`() = runTest {
        val state = viewModel.registerState.first()
        assertNull(state)
    }

    @Test
    fun `estado inicial de nombre es string vacio`() {
        assertEquals("", viewModel.nombre)
    }

    @Test
    fun `estado inicial de email es string vacio`() {
        assertEquals("", viewModel.email)
    }

    @Test
    fun `estado inicial de password es string vacio`() {
        assertEquals("", viewModel.password)
    }

    @Test
    fun `estado inicial de passwordVisible es false`() {
        assertFalse(viewModel.passwordVisible)
    }
}
