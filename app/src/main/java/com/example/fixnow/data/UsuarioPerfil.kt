package com.example.fixnow.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsuarioPerfil(
    val id: String? = null,
    val nombre: String? = null,
    val email: String? = null,
    @SerialName("es_prestador")
    val es_prestador: Boolean? = false,
    @SerialName("tipo_servicio")
    val tipo_servicio: String? = null,
    @SerialName("fecha_registro")
    val fechaRegistro: Long? = null // Cambiado a Long para que coincida con la DB
)