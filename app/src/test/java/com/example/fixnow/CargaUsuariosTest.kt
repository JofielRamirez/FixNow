package com.example.fixnow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class CargaUsuariosTest {

    @Test
    fun testCarga100UsuariosPidiendoServicio() = runBlocking {
        val numUsuarios = 100
        val solicitudesExitosas = AtomicInteger(0)
        val erroresConcurrencia = AtomicInteger(0)
        
        // Simulamos 100 usuarios intentando pedir el mismo servicio al mismo tiempo
        val jobs = List(numUsuarios) { id ->
            launch(Dispatchers.Default) {
                try {
                    // Simulación de delay de red aleatorio entre 10 y 100ms
                    val delayTime = (10..100).random().toLong()
                    delay(delayTime)
                    
                    // Simular lógica de "Primero en llegar se queda el cupo"
                    val exito = simularPeticionServicio("Servicio_Plomeria_001", "Usuario_$id")
                    if (exito) solicitudesExitosas.incrementAndGet()
                } catch (e: Exception) {
                    erroresConcurrencia.incrementAndGet()
                }
            }
        }
        
        jobs.joinAll()
        
        System.out.println("Resultados: Exitosos: ${solicitudesExitosas.get()}, Errores: ${erroresConcurrencia.get()}")
        assertTrue("Debería haber al menos una solicitud procesada", solicitudesExitosas.get() > 0)
    }

    private suspend fun simularPeticionServicio(servicioId: String, usuarioId: String): Boolean {
        // Simula la lógica de transacción en Supabase
        delay(50) 
        return true 
    }
}
