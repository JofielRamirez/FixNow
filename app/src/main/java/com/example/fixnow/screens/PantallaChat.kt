package com.example.fixnow.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.OrangePrimary
import com.example.fixnow.data.ChatRepository
import com.example.fixnow.data.MensajeDB
import com.example.fixnow.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaChat(navController: NavController, socioId: String, socioNombre: String) {
    val scope = rememberCoroutineScope()
    val miId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
    var textoMensaje by remember { mutableStateOf("") }
    
    val listaChatUI = remember { mutableStateListOf<MensajeDB>() }
    val listState = rememberLazyListState()
    var cargando by remember { mutableStateOf(true) }

    // 1. CARGA INICIAL Y ESCUCHA AUTOMÁTICA
    LaunchedEffect(socioId) {
        // Cargar historial rápido (Esto elimina la pantalla blanca)
        val historial = ChatRepository.obtenerMensajesHistoricos(miId, socioId)
        listaChatUI.clear()
        listaChatUI.addAll(historial)
        cargando = false
        
        if (listaChatUI.isNotEmpty()) {
            listState.scrollToItem(listaChatUI.size - 1)
        }

        // 2. ESCUCHA REALTIME (Usando el repositorio corregido)
        ChatRepository.escucharMensajes(miId, socioId).collect { mensajesDB ->
            // Actualizamos la lista sin borrar lo que el usuario ve
            // Solo añadimos los que no tengamos (evita duplicados)
            val nuevos = mensajesDB.filter { db -> listaChatUI.none { it.id == db.id } }
            if (nuevos.isNotEmpty()) {
                // Si llega el real, quitamos el temporal por contenido
                val contenidosNuevos = nuevos.map { it.contenido }
                listaChatUI.removeAll { it.id.startsWith("temp_") && it.contenido in contenidosNuevos }
                
                listaChatUI.addAll(nuevos)
                listaChatUI.sortBy { it.createdAt }
            }
        }
    }

    // Scroll automático suave
    LaunchedEffect(listaChatUI.size) {
        if (listaChatUI.isNotEmpty()) {
            listState.animateScrollToItem(listaChatUI.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(socioNombre, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("En línea", fontSize = 11.sp, color = Color(0xFF4CAF50))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textoMensaje,
                        onValueChange = { textoMensaje = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe un mensaje...", fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F5F5),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(25.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textoMensaje.isNotBlank()) {
                                val textoAEnviar = textoMensaje
                                textoMensaje = ""
                                
                                // AGREGAR DE INMEDIATO A LA UI (Instantáneo)
                                val msjTemporal = MensajeDB(
                                    id = "temp_${System.currentTimeMillis()}",
                                    idEmisor = miId,
                                    idReceptor = socioId,
                                    contenido = textoAEnviar,
                                    createdAt = "Z" // Al final
                                )
                                listaChatUI.add(msjTemporal)

                                scope.launch {
                                    try {
                                        ChatRepository.enviarMensaje(miId, socioId, textoAEnviar)
                                    } catch (e: Exception) {
                                        listaChatUI.remove(msjTemporal)
                                    }
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = OrangePrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF3F3F3))) {
            if (cargando && listaChatUI.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(listaChatUI, key = { it.id }) { msj ->
                        BurbujaMensaje(msj, miId)
                    }
                }
            }
        }
    }
}

@Composable
fun BurbujaMensaje(mensaje: MensajeDB, miId: String) {
    val esMio = mensaje.idEmisor == miId
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (esMio) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (esMio) OrangePrimary else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (esMio) 16.dp else 2.dp,
                bottomEnd = if (esMio) 2.dp else 16.dp
            ),
            shadowElevation = 1.dp
        ) {
            Text(
                text = mensaje.contenido,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                color = if (esMio) Color.White else Color.Black,
                fontSize = 15.sp
            )
        }
    }
}
