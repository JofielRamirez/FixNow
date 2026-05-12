package com.example.fixnow.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Resena(
    val id: Int? = null,
    @SerialName("socio_id")
    val socioId: String,
    @SerialName("usuario_id")
    val usuarioId: String,
    @SerialName("usuario_nombre")
    val usuarioNombre: String,
    val comentario: String,
    @SerialName("fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
)
