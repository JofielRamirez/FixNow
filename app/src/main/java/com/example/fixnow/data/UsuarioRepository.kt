package com.example.fixnow.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioPerfil(
    val id: String,
    val email: String,
    val nombre: String,
    @SerialName("fecha_registro")
    val fechaRegistro: Long = System.currentTimeMillis()
)

object UsuarioRepository {
    private val client = SupabaseClient.client

    suspend fun guardarUsuario(uid: String, email: String, nombre: String) {
        val perfil = UsuarioPerfil(
            id = uid,
            email = email,
            nombre = nombre,
            fechaRegistro = System.currentTimeMillis()
        )
        client.postgrest["Usuarios"].insert(perfil)
    }
}