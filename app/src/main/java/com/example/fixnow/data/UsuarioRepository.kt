package com.example.fixnow.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

/**
 * Modelo de datos para la tabla 'usuarios' en Supabase.
 * El uso de @Serializable permite que el SDK de Supabase convierta
 * automáticamente este objeto a JSON para la base de datos.
 */
@Serializable
data class UsuarioPerfil(
    val uid: String,
    val email: String,
    val nombre: String
)

object UsuarioRepository {
    // Referencia al cliente que creaste en SupabaseClient.kt
    private val client = SupabaseClient.client

    /**
     * Guarda la información del usuario en la tabla de PostgreSQL.
     * Al ser una función 'suspend', debe ser llamada dentro de un CoroutineScope.
     */
    suspend fun guardarUsuario(uid: String, email: String, nombre: String) {
        val perfil = UsuarioPerfil(
            uid = uid,
            email = email,
            nombre = nombre
        )

        // Realiza la inserción en la tabla 'usuarios'
        client.postgrest["usuarios"].insert(perfil)
    }
}