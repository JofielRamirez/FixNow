package com.example.fixnow.data

import android.util.Log
import kotlinx.coroutines.delay

object IAService {
    
    private var tokensDisponibles = 1000 

    suspend fun generarResumenIA(comentarios: List<String>): kotlin.Result<String> {
        if (tokensDisponibles <= 0) return kotlin.Result.failure(Exception("Tokens agotados"))
        return try {
            delay(1000)
            tokensDisponibles -= 50
            val resumen = "Resumen: El socio tiene ${comentarios.size} opiniones. Destaca por su rapidez."
            kotlin.Result.success(resumen)
        } catch (e: Exception) { kotlin.Result.failure(e) }
    }

    suspend fun preguntarAsistente(pregunta: String, prestadores: List<UsuarioPerfil>): kotlin.Result<String> {
        if (tokensDisponibles <= 0) return kotlin.Result.failure(Exception("Sin tokens"))
        
        return try {
            delay(1500) // Simulación de procesamiento
            tokensDisponibles -= 100

            val preguntaLimpia = pregunta.lowercase()
            
            // Lógica de "Búsqueda Semántica" simulada
            val recomendados = prestadores.filter { socio ->
                val cumpleTipo = socio.tipo_servicio?.lowercase()?.let { preguntaLimpia.contains(it) } ?: false
                val cumpleNombre = socio.nombre?.lowercase()?.let { preguntaLimpia.contains(it) } ?: false
                cumpleTipo || cumpleNombre
            }

            val respuesta = if (recomendados.isNotEmpty()) {
                val nombres = recomendados.joinToString(", ") { it.nombre ?: "Anónimo" }
                "He encontrado estos profesionales para ti: $nombres. ¿Te gustaría ver el perfil de alguno?"
            } else {
                "Lo siento, no encontré a nadie que coincida exactamente con '$pregunta'. ¿Buscas algún servicio como plomería o electricidad?"
            }

            kotlin.Result.success(respuesta)
        } catch (e: Exception) { kotlin.Result.failure(e) }
    }
    
    fun obtenerTokens() = tokensDisponibles
}
