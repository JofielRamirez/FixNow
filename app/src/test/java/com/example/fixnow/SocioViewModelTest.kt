package com.example.fixnow

import android.content.Context
import android.location.Location
import com.example.fixnow.data.Cita
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.viewmodel.SocioUiState
import com.example.fixnow.viewmodel.SocioViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SocioViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SocioViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockkObject(SupabaseClient)
        mockkObject(UsuarioRepository)

        val mockClient = mockk<JanSupabaseClient>(relaxed = true)
        val mockAuth = mockk<Auth>(relaxed = true)

        every { SupabaseClient.client } returns mockClient
        every { mockClient.auth } returns mockAuth
        every { mockAuth.currentSessionOrNull() } returns null

        viewModel = SocioViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(SupabaseClient)
        unmockkObject(UsuarioRepository)
        runCatching { unmockkStatic(LocationServices::class) }
    }

    @Test
    fun `aceptarServicio actualiza estado y limpia servicioEntrante`() = runTest {
        val citaId = "cita-1"
        setServicioEntrante(Cita(id = citaId, idCliente = "c1", idSocio = "s1", fecha = "hoy", estado = "pendiente"))

        coEvery { UsuarioRepository.actualizarEstadoCita(citaId, "aceptada") } just runs

        viewModel.aceptarServicio(citaId)
        runCurrent()

        coVerify(exactly = 1) { UsuarioRepository.actualizarEstadoCita(citaId, "aceptada") }
        assertNull(viewModel.uiState.value.servicioEntrante)
    }

    @Test
    fun `rechazarServicio actualiza estado y limpia servicioEntrante`() = runTest {
        val citaId = "cita-2"
        setServicioEntrante(Cita(id = citaId, idCliente = "c1", idSocio = "s1", fecha = "hoy", estado = "pendiente"))

        coEvery { UsuarioRepository.actualizarEstadoCita(citaId, "cancelada") } just runs

        viewModel.rechazarServicio(citaId)
        runCurrent()

        coVerify(exactly = 1) { UsuarioRepository.actualizarEstadoCita(citaId, "cancelada") }
        assertNull(viewModel.uiState.value.servicioEntrante)
    }

    @Test
    fun `toggleDisponibilidad true actualiza disponibilidad y rastrea ubicacion`() = runTest {
        val context = mockk<Context>(relaxed = true)
        val fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)
        val task = mockk<Task<Location>>(relaxed = true)
        val location = Location("test").apply {
            latitude = 32.5149
            longitude = -117.0382
        }

        viewModel.userIdOverride = "socio-1"

        coEvery { UsuarioRepository.actualizarDisponibilidad("socio-1", true) } just runs
        coEvery { UsuarioRepository.actualizarDisponibilidad("socio-1", false) } just runs
        coEvery { UsuarioRepository.actualizarUbicacion("socio-1", 32.5149, -117.0382) } just runs

        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationClient
        every { fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null) } returns task
        every { task.addOnSuccessListener(any<OnSuccessListener<Location>>()) } answers {
            val listener = firstArg<OnSuccessListener<Location>>()
            listener.onSuccess(location)
            task
        }

        viewModel.toggleDisponibilidad(true, context)
        runCurrent()

        assertTrue(viewModel.uiState.value.disponible)
        assertEquals(32.5149, viewModel.uiState.value.miUbicacion!!.first, 0.0001)
        assertEquals(-117.0382, viewModel.uiState.value.miUbicacion!!.second, 0.0001)
        coVerify(exactly = 1) { UsuarioRepository.actualizarDisponibilidad("socio-1", true) }
        coVerify(exactly = 1) { UsuarioRepository.actualizarUbicacion("socio-1", 32.5149, -117.0382) }

        viewModel.toggleDisponibilidad(false, context)
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()

        assertFalse(viewModel.uiState.value.disponible)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setServicioEntrante(cita: Cita?) {
        val field = SocioViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as MutableStateFlow<SocioUiState>
        stateFlow.value = stateFlow.value.copy(servicioEntrante = cita)
    }
}
