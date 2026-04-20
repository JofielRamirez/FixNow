@file:OptIn(io.github.jan.supabase.annotations.SupabaseExperimental::class)
package com.example.fixnow.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

object IARepository {
    private val supabase = SupabaseClient.client

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
        // Aumentamos los tiempos de espera para evitar el "Socket timeout"
        install(HttpTimeout) {
            requestTimeoutMillis = 60000 // 60 segundos
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }

    private const val API_KEY = "AIzaSyBibMlduR9eZqeBQdqYBPqOtXCf0ftlhSE"
    private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=$API_KEY"

    internal var obtenerResenas: suspend (String) -> List<ResenaDB> = { uidLimpio ->
        supabase.postgrest["resenas"].select {
            filter { eq("id_socio", uidLimpio) }
        }.decodeList<ResenaDB>()
    }

    internal var generarResumenConGemini: suspend (String) -> String? = { prompt ->
        val response = httpClient.post(GEMINI_URL) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            addJsonObject { put("text", prompt) }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    putJsonObject("thinkingConfig") {
                        put("thinkingLevel", "HIGH")
                    }
                }
            })
        }

        if (response.status != HttpStatusCode.OK) {
            val errorBody = response.bodyAsText()
            Log.e("IA_DEBUG", "Error de Gemini (${response.status}): $errorBody")
            null
        } else {
            val responseText = response.bodyAsText()
            val jsonRes = Json.parseToJsonElement(responseText).jsonObject
            jsonRes["candidates"]?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("text")?.jsonPrimitive?.content
        }
    }

    internal var guardarResumenSocio: suspend (String, String) -> Unit = { uidLimpio, resumenFinal ->
        supabase.postgrest["Usuarios"].update(
            update = { set("resumen_ia", resumenFinal) }
        ) {
            filter { eq("id", uidLimpio) }
        }
    }

    suspend fun generarResumenSocio(socioId: String): Boolean {
        val uidLimpio = socioId.replace("\"", "").replace("'", "").trim()
        Log.d("IA_DEBUG", ">>> INICIANDO IA (Gemini 3 Flash) para socio: [$uidLimpio]")
        
        return try {
            val resenas = obtenerResenas(uidLimpio)

            if (resenas.isEmpty()) {
                Log.w("IA_DEBUG", "No hay reseñas para este socio.")
                return false
            }

            val comentariosConcat = resenas.joinToString("\n") { "- ${it.comentario}" }
            
            // PROMPT MEJORADO: Más honesto y equilibrado
            val prompt = """
                Eres un analista de calidad para una plataforma de servicios. 
                Tu objetivo es generar un resumen HONESTO y OBJETIVO de 3 líneas sobre este trabajador basado ÚNICAMENTE en las reseñas de los clientes.
                
                Instrucciones críticas:
                1. No ignores las críticas. Si los clientes mencionan retrasos, mala comunicación o falta de limpieza, inclúyelo de forma profesional.
                2. Si la mayoría es positivo, destaca sus fortalezas, pero mantén un tono realista.
                3. No uses frases publicitarias vacías. Sé específico según lo que dicen los usuarios.
                4. Máximo 3 líneas.
                
                Reseñas de los clientes:
                $comentariosConcat
            """.trimIndent()

            Log.d("IA_DEBUG", "Paso 2: Enviando petición a Gemini...")

            val resumenFinal = generarResumenConGemini(prompt).orEmpty()

            if (resumenFinal.isNotBlank()) {
                Log.d("IA_DEBUG", "Resumen generado: $resumenFinal")
                guardarResumenSocio(uidLimpio, resumenFinal)
                Log.d("IA_DEBUG", ">>> ¡PROCESO COMPLETADO EXITOSAMENTE!")
                true
            } else {
                Log.e("IA_DEBUG", "Error: Texto vacío.")
                false
            }
        } catch (e: Exception) {
            Log.e("IA_DEBUG", "ERROR: ${e.message}")
            false
        }
    }
}
