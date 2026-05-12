package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.fixnow.data.IAService
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.ui.theme.OrangePrimary
import kotlinx.coroutines.launch

data class MensajeIA(val texto: String, val esUsuario: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAsistenteIA(navController: NavController) {
    val scope = rememberCoroutineScope()
    val listaMensajes = remember { mutableStateListOf<MensajeIA>() }
    var textoEntrada by remember { mutableStateOf("") }
    var escribiendoIA by remember { mutableStateOf(false) }
    var prestadores by remember { mutableStateOf<List<UsuarioPerfil>>(emptyList()) }

    val fondo = MaterialTheme.colorScheme.background
    val superficie = MaterialTheme.colorScheme.surface
    val sobreSup = MaterialTheme.colorScheme.onSurface

    // Cargar datos de prestadores al inicio para que la IA los conozca
    LaunchedEffect(Unit) {
        prestadores = UsuarioRepository.obtenerTodosLosPrestadores()
        listaMensajes.add(MensajeIA("¡Hola! Soy tu asistente de FixNow. ¿En qué tipo de servicio técnico te puedo ayudar hoy? (Ej: Busco un plomero)", false))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asistente FixNow AI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OrangePrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(fondo)) {
            // Lista de mensajes
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(listaMensajes) { mensaje ->
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (mensaje.esUsuario) Alignment.CenterEnd else Alignment.CenterStart) {
                        Surface(
                            color = if (mensaje.esUsuario) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (mensaje.esUsuario) 16.dp else 0.dp,
                                bottomEnd = if (mensaje.esUsuario) 0.dp else 16.dp
                            ),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = mensaje.texto,
                                modifier = Modifier.padding(12.dp),
                                color = if (mensaje.esUsuario) Color.White else sobreSup,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                if (escribiendoIA) {
                    item {
                        Text("FixNow AI está pensando...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            // Entrada de texto
            Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = textoEntrada,
                        onValueChange = { textoEntrada = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Pregunta por un servicio...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textoEntrada.isNotBlank() && !escribiendoIA) {
                                val userQuery = textoEntrada
                                listaMensajes.add(MensajeIA(userQuery, true))
                                textoEntrada = ""
                                escribiendoIA = true
                                
                                scope.launch {
                                    val result = IAService.preguntarAsistente(userQuery, prestadores)
                                    escribiendoIA = false
                                    result.onSuccess { respuesta ->
                                        listaMensajes.add(MensajeIA(respuesta, false))
                                    }.onFailure {
                                        listaMensajes.add(MensajeIA("Lo siento, tuve un problema al procesar tu solicitud. Verifica tu conexión.", false))
                                    }
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = OrangePrimary, contentColor = Color.White)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null)
                    }
                }
            }
        }
    }
}
