@file:OptIn(io.github.jan.supabase.annotations.SupabaseExperimental::class)
package com.example.fixnow.data

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MensajeDB(
    val id: String = "",
    @SerialName("id_emisor")
    val idEmisor: String,
    @SerialName("id_receptor")
    val idReceptor: String,
    val contenido: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

object ChatRepository {
    private val client = SupabaseClient.client

    suspend fun enviarMensaje(emisorId: String, receptorId: String, texto: String) {
        val mensaje = MensajeDB(
            idEmisor = emisorId,
            idReceptor = receptorId,
            contenido = texto
        )
        client.postgrest["mensajes"].insert(mensaje)
    }

    suspend fun obtenerMensajesHistoricos(miId: String, otroId: String): List<MensajeDB> {
        return try {
            val mensajes = client.postgrest["mensajes"].select().decodeList<MensajeDB>()
            mensajes.filter {
                (it.idEmisor == miId && it.idReceptor == otroId) ||
                (it.idEmisor == otroId && it.idReceptor == miId)
            }.sortedBy { it.createdAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun escucharMensajes(miId: String, otroId: String): Flow<List<MensajeDB>> {
        return client.postgrest["mensajes"]
            .selectAsFlow(primaryKey = MensajeDB::id)
            .map { lista ->
                lista.filter {
                    (it.idEmisor == miId && it.idReceptor == otroId) ||
                    (it.idEmisor == otroId && it.idReceptor == miId)
                }.sortedBy { it.createdAt }
            }
    }

    suspend fun obtenerConversaciones(miId: String): List<String> {
        return try {
            val mensajes = client.postgrest["mensajes"].select().decodeList<MensajeDB>()
            mensajes.map {
                if (it.idEmisor == miId) it.idReceptor else it.idEmisor
            }.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }
}