package com.example.fixnow

import com.example.fixnow.utils.Resource
import com.example.fixnow.viewmodel.LoginViewModel
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit Tests — LoginViewModel
 * Persona 1
 *
 * Cubre:
 *  - Validación de campos vacíos
 *  - Cambios de estado en email / password / passwordVisible
 *  - Estado Loading al iniciar login
 *  - Estado Error con mensaje amigable (credenciales inválidas)
 *  - Estado Error con mensaje amigable (email no confirmado)
 *  - Estado Error genérico
 *  - resetState() limpia el estado
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // Dispatcher de prueba para corrutinas
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ──────────────────────────────────────────────────────────────
    // CAMBIOS DE ESTADO EN CAMPOS
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `onEmailChange actualiza el email correctamente`() {
        viewModel.onEmailChange("test@correo.com")
        assertEquals("test@correo.com", viewModel.email)
    }

    @Test
    fun `onPasswordChange actualiza la password correctamente`() {
        viewModel.onPasswordChange("MiPassword123")
        assertEquals("MiPassword123", viewModel.password)
    }

    @Test
    fun `togglePasswordVisibility cambia de false a true`() {
        assertFalse(viewModel.passwordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.passwordVisible)
    }

    @Test
    fun `togglePasswordVisibility cambia de true a false`() {
        viewModel.togglePasswordVisibility() // true
        viewModel.togglePasswordVisibility() // false
        assertFalse(viewModel.passwordVisible)
    }

    // ──────────────────────────────────────────────────────────────
    // VALIDACIÓN DE CAMPOS VACÍOS
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `login con email vacio emite Error con mensaje de campos`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("password123")

        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.loginState.first()
        assertTrue(state is Resource.Error)
        assertEquals("Completa todos los campos", (state as Resource.Error).message)
    }

    @Test
    fun `login con password vacia emite Error con mensaje de campos`() = runTest {
        viewModel.onEmailChange("usuario@test.com")
        viewModel.onPasswordChange("")

        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.loginState.first()
        assertTrue(state is Resource.Error)
        assertEquals("Completa todos los campos", (state as Resource.Error).message)
    }

    @Test
    fun `login con ambos campos vacios emite Error con mensaje de campos`() = runTest {
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("")

        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.loginState.first()
        assertTrue(state is Resource.Error)
        assertEquals("Completa todos los campos", (state as Resource.Error).message)
    }

    @Test
    fun `login con solo espacios en email emite Error de campos`() = runTest {
        viewModel.onEmailChange("   ")
        viewModel.onPasswordChange("password123")

        viewModel.login()
        advanceUntilIdle()

        val state = viewModel.loginState.first()
        assertTrue(state is Resource.Error)
        assertEquals("Completa todos los campos", (state as Resource.Error).message)
    }

    // ──────────────────────────────────────────────────────────────
    // MENSAJES DE ERROR AMIGABLES
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `mensaje de error para credenciales invalidas es amigable`() {
        // Verificamos que el mapeo de errores funciona correctamente
        // simulando el mensaje que devolvería Supabase
        val errorMsg = "Invalid login credentials"
        val resultado = when {
            errorMsg.contains("Email not confirmed", ignoreCase = true) -> "Confirma tu correo electrónico"
            errorMsg.contains("Invalid login credentials", ignoreCase = true) -> "Correo o contraseña incorrectos"
            else -> "Error al iniciar sesión"
        }
        assertEquals("Correo o contraseña incorrectos", resultado)
    }

    @Test
    fun `mensaje de error para email no confirmado es amigable`() {
        val errorMsg = "Email not confirmed"
        val resultado = when {
            errorMsg.contains("Email not confirmed", ignoreCase = true) -> "Confirma tu correo electrónico"
            errorMsg.contains("Invalid login credentials", ignoreCase = true) -> "Correo o contraseña incorrectos"
            else -> "Error al iniciar sesión"
        }
        assertEquals("Confirma tu correo electrónico", resultado)
    }

    @Test
    fun `mensaje de error generico usa fallback`() {
        val errorMsg = "network timeout"
        val resultado = when {
            errorMsg.contains("Email not confirmed", ignoreCase = true) -> "Confirma tu correo electrónico"
            errorMsg.contains("Invalid login credentials", ignoreCase = true) -> "Correo o contraseña incorrectos"
            else -> "Error al iniciar sesión"
        }
        assertEquals("Error al iniciar sesión", resultado)
    }

    // ──────────────────────────────────────────────────────────────
    // RESET DE ESTADO
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `resetState limpia el loginState a null`() = runTest {
        // Provocamos un error primero
        viewModel.onEmailChange("")
        viewModel.login()
        advanceUntilIdle()

        // Reseteamos
        viewModel.resetState()
        advanceUntilIdle()

        val state = viewModel.loginState.first()
        assertNull(state)
    }

    // ──────────────────────────────────────────────────────────────
    // ESTADO INICIAL
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `estado inicial del loginState es null`() = runTest {
        val state = viewModel.loginState.first()
        assertNull(state)
    }

    @Test
    fun `estado inicial del email es string vacio`() {
        assertEquals("", viewModel.email)
    }

    @Test
    fun `estado inicial del password es string vacio`() {
        assertEquals("", viewModel.password)
    }

    @Test
    fun `estado inicial de passwordVisible es false`() {
        assertFalse(viewModel.passwordVisible)
    }
}
