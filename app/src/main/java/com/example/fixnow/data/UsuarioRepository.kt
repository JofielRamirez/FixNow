package com.example.fixnow.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import androidx.compose.material.icons.filled.ThumbUp

object UsuarioRepository {
    private val client = SupabaseClient.client

    // Ajustado a tu nuevo modelo que solo pide ID y Nombre
    suspend fun guardarUsuario(uid: String, email: String, nombre: String) {
        val perfil = UsuarioPerfil(
            id = uid,
            nombre = nombre,
            email = email,
            fechaRegistro = System.currentTimeMillis()
        )
        client.postgrest["Usuarios"].insert(perfil)
    }

    suspend fun convertirseEnPrestador(uid: String, tipo: String) {
        // Limpieza total: PostgreSQL no acepta el ID con comillas
        val uidLimpio = uid.replace("\"", "").trim()

        // Log para que revises en el Logcat de Android Studio si el ID es correcto
        Log.d("DEBUG_SUPABASE", "ID Limpio enviado: $uidLimpio")

        client.postgrest["Usuarios"].update(
            {
                set("es_prestador", true)
                set("tipo_servicio", tipo)
            }
        ) {
            filter { eq("id", uidLimpio) }
        }
    }

    suspend fun obtenerFotosDeTrabajos(): List<String> {
        return try {
            val resultado = client.postgrest["trabajos"].select().decodeList<Map<String, String>>()
            resultado.map { it["url_imagen"] ?: "" }.filter { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e("REPO", "Error al obtener fotos: ${e.message}")
            emptyList()
        }
    }

    suspend fun subirFotoTrabajo(uid: String, imageBytes: ByteArray) {
        try {
            val fileName = "$uid/${System.currentTimeMillis()}.jpg"
            val bucket = client.storage.from("fotos_trabajos")
            bucket.upload(path = fileName, data = imageBytes)
            val urlPublica = bucket.publicUrl(fileName)
            client.postgrest["trabajos"].insert(mapOf("id_socio" to uid, "url_imagen" to urlPublica))
        } catch (e: Exception) {
            Log.e("REPO", "Error subiendo foto: ${e.message}")
            throw e
        }
    }

    suspend fun obtenerSociosPorCategoria(categoria: String): List<UsuarioPerfil> {
        return try {
            val respuesta = client.postgrest["Usuarios"].select {
                filter {
                    // ILIKE ignora si es "electricidad" o "Electricidad"
                    ilike("tipo_servicio", categoria)
                    eq("es_prestador", true)
                }
            }.decodeList<UsuarioPerfil>()

            // LOG DE CONTROL - Revisa esto en tu Logcat
            Log.d("SOCIOS_DB", "Busqué: $categoria | Encontrados: ${respuesta.size}")

            respuesta
        } catch (e: Exception) {
            Log.e("REPO_ERROR", "Error: ${e.message}")
            emptyList()
        }
    }
}