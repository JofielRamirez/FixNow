package com.example.fixnow.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResenaDB(
    @SerialName("id_socio")
    val idSocio: String,
    @SerialName("id_cliente")
    val idCliente: String,
    val puntuacion: Int,
    val comentario: String,
    @SerialName("created_at")
    val createdAt: String? = null
)
