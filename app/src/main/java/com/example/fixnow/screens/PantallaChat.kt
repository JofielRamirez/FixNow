package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.ui.theme.*
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
    val inicial = socioNombre.firstOrNull()?.uppercaseChar()?.toString() ?: "S"

    // ── Colores del tema ─────────────────────────────────
    val fondo        = MaterialTheme.colorScheme.background
    val superficie   = MaterialTheme.colorScheme.surface
    val sobreSup     = MaterialTheme.colorScheme.onSurface
    val supVar       = MaterialTheme.colorScheme.surfaceVariant
    val bordeInactivo = MaterialTheme.colorScheme.outlineVariant

    LaunchedEffect(socioId) {
        val historial = ChatRepository.obtenerMensajesHistoricos(miId, socioId)
        listaChatUI.clear()
        listaChatUI.addAll(historial)
        cargando = false
        if (listaChatUI.isNotEmpty()) listState.scrollToItem(listaChatUI.size - 1)

        ChatRepository.escucharMensajes(miId, socioId).collect { mensajesDB ->
            val nuevos = mensajesDB.filter { db -> listaChatUI.none { it.id == db.id } }
            if (nuevos.isNotEmpty()) {
                val contenidosNuevos = nuevos.map { it.contenido }
                listaChatUI.removeAll { it.id.startsWith("temp_") && it.contenido in contenidosNuevos }
                listaChatUI.addAll(nuevos)
                listaChatUI.sortBy { it.createdAt }
            }
        }
    }

    LaunchedEffect(listaChatUI.size) {
        if (listaChatUI.isNotEmpty()) listState.animateScrollToItem(listaChatUI.size - 1)
    }

    Scaffold(
        topBar = {
            // Header naranja — siempre naranja en ambos temas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.horizontalGradient(colors = listOf(OrangeDark, OrangePrimary)))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(inicial, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(socioNombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF69F0AE)))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("En línea", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = superficie,           // ← era Color.White
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textoMensaje,
                        onValueChange = { textoMensaje = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe un mensaje...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangePrimary,
                            unfocusedBorderColor = bordeInactivo,     // ← era Color(0xFFE0E0E0)
                            focusedContainerColor = supVar,           // ← era Color(0xFFFAFAFA)
                            unfocusedContainerColor = supVar,         // ← era Color(0xFFFAFAFA)
                            cursorColor = OrangePrimary,
                            focusedTextColor = sobreSup,              // ← nuevo
                            unfocusedTextColor = sobreSup             // ← nuevo
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(brush = Brush.linearGradient(colors = listOf(OrangeDark, OrangePrimary))),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (textoMensaje.isNotBlank()) {
                                    val texto = textoMensaje
                                    textoMensaje = ""
                                    val temporal = MensajeDB(
                                        id = "temp_${System.currentTimeMillis()}",
                                        idEmisor = miId,
                                        idReceptor = socioId,
                                        contenido = texto,
                                        createdAt = "Z"
                                    )
                                    listaChatUI.add(temporal)
                                    scope.launch {
                                        try {
                                            ChatRepository.enviarMensaje(miId, socioId, texto)
                                        } catch (e: Exception) {
                                            listaChatUI.remove(temporal)
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(fondo)            // ← era Color(0xFFF5F5F5)
        ) {
            if (cargando && listaChatUI.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
    val superficie = MaterialTheme.colorScheme.surface
    val sobreSup   = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (esMio) 18.dp else 4.dp,
                        bottomEnd = if (esMio) 4.dp else 18.dp
                    )
                )
                .background(
                    if (esMio)
                        Brush.linearGradient(colors = listOf(OrangeDark, OrangePrimary))
                    else
                        Brush.linearGradient(colors = listOf(superficie, superficie)) // ← era Color.White
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = mensaje.contenido,
                color = if (esMio) Color.White else sobreSup,  // ← era TextPrimary hardcodeado
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        }
    }
}
