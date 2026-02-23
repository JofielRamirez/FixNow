package com.example.fixnow.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import androidx.compose.material.icons.filled.ThumbUp

object UsuarioRepository {
    private val client = SupabaseClient.client

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
        val uidLimpio = uid.replace("\"", "").trim()
        client.postgrest["Usuarios"].update(
            {
                set("es_prestador", true)
                set("tipo_servicio", tipo)
            }
        ) {
            filter { eq("id", uidLimpio) }
        }
    }

    suspend fun obtenerSociosPorCategoria(categoria: String): List<UsuarioPerfil> {
        return try {
            val respuesta = client.postgrest["Usuarios"].select {
                filter {
                    eq("es_prestador", true)
                    ilike("tipo_servicio", "%$categoria%") 
                }
            }.decodeList<UsuarioPerfil>()

            Log.d("SOCIOS_DB", "Categoría: $categoria | Encontrados: ${respuesta.size}")
            respuesta
        } catch (e: Exception) {
            Log.e("REPO_ERROR", "Error al obtener socios: ${e.message}")
            emptyList()
        }
    }

    suspend fun obtenerFotosDeTrabajos(uid: String? = null): List<String> {
        return try {
            val respuesta = client.postgrest["trabajos"].select {
                if (uid != null) {
                    filter { eq("id_socio", uid) }
                }
            }.decodeList<Map<String, String>>()
            respuesta.map { it["url_imagen"] ?: "" }.filter { it.isNotEmpty() }
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

    suspend fun obtenerSocioPorId(uid: String): UsuarioPerfil? {
        return try {
            client.postgrest["Usuarios"].select {
                filter { eq("id", uid) }
            }.decodeSingleOrNull<UsuarioPerfil>()
        } catch (e: Exception) {
            Log.e("REPO", "Error al obtener socio: ${e.message}")
            null
        }
    }
}