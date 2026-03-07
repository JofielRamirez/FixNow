package com.example.fixnow.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.compose.material.icons.filled.ThumbUp

@Serializable
data class Cita(
    val id: String? = null,
    @SerialName("id_cliente")
    val idCliente: String,
    @SerialName("id_socio")
    val idSocio: String,
    val fecha: String, 
    val estado: String = "pendiente",
    val detalles: String? = null
)

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

    suspend fun convertirseEnPrestador(
        uid: String,
        tipo: String,
        urlId: String? = null,
        urlAnt: String? = null
    ) {
        val uidLimpio = uid.replace("\"", "").trim()
        client.postgrest["Usuarios"].update(
            {
                set("es_prestador", true)
                set("tipo_servicio", tipo)
                if (urlId != null) set("url_identificacion", urlId)
                if (urlAnt != null) set("url_antecedentes", urlAnt)
            }
        ) {
            filter { eq("id", uidLimpio) }
        }
    }

    suspend fun actualizarPerfilSocio(uid: String, nombre: String, descripcion: String, urlFoto: String? = null) {
        val uidLimpio = uid.replace("\"", "").trim()
        client.postgrest["Usuarios"].update(
            {
                set("nombre", nombre)
                set("descripcion", descripcion)
                if (urlFoto != null) set("url_foto_perfil", urlFoto)
            }
        ) {
            filter { eq("id", uidLimpio) }
        }
    }

    suspend fun subirFotoPerfil(uid: String, data: ByteArray): String {
        val fileName = "$uid/perfil_${System.currentTimeMillis()}.jpg"
        val bucket = client.storage.from("fotos_perfiles")
        bucket.upload(path = fileName, data = data) {
            upsert = true
        }
        return bucket.publicUrl(fileName)
    }

    suspend fun subirDocumentoSocio(uid: String, fileName: String, data: ByteArray, bucketName: String): String {
        return try {
            val path = "$uid/$fileName"
            val bucket = client.storage.from(bucketName)
            bucket.upload(path = path, data = data) {
                upsert = true
            }
            bucket.publicUrl(path)
        } catch (e: Exception) {
            Log.e("REPO", "Error subiendo documento: ${e.message}")
            throw e
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
            val uidLimpio = uid.replace("\"", "").trim()
            client.postgrest["Usuarios"].select {
                filter { eq("id", uidLimpio) }
            }.decodeSingleOrNull<UsuarioPerfil>()
        } catch (e: Exception) {
            Log.e("REPO", "Error al obtener socio: ${e.message}")
            null
        }
    }

    // Métodos para Citas
    suspend fun crearCita(idCliente: String, idSocio: String, fecha: String, detalles: String) {
        val nuevaCita = Cita(
            idCliente = idCliente,
            idSocio = idSocio,
            fecha = fecha,
            estado = "pendiente",
            detalles = detalles
        )
        client.postgrest["citas"].insert(nuevaCita)
    }

    suspend fun obtenerCitasSocio(uid: String): List<Cita> {
        return try {
            client.postgrest["citas"].select {
                filter { eq("id_socio", uid) }
            }.decodeList<Cita>()
        } catch (e: Exception) {
            Log.e("REPO", "Error citas: ${e.message}")
            emptyList()
        }
    }

    suspend fun actualizarEstadoCita(citaId: String, nuevoEstado: String) {
        client.postgrest["citas"].update({
            set("estado", nuevoEstado)
        }) {
            filter { eq("id", citaId) }
        }
    }
}
