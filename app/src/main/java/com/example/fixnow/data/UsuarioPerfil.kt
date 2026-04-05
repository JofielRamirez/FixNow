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
    @SerialName("descripcion")
    val descripcion: String? = null,
    @SerialName("url_foto_perfil")
    val urlFotoPerfil: String? = null,
    @SerialName("fecha_registro")
    val fechaRegistro: Long? = null,
    @SerialName("url_identificacion")
    val urlIdentificacion: String? = null,
    @SerialName("url_antecedentes")
    val urlAntecedentes: String? = null,
    @SerialName("latitud")
    val latitud: Double? = null,
    @SerialName("longitud")
    val longitud: Double? = null,
    @SerialName("disponible")
    val disponible: Boolean? = true,
    @SerialName("resumen_ia")
    val resumen_ia: String? = null
)
