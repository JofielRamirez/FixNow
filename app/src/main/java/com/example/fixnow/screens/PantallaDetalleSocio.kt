package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fixnow.ui.theme.OrangePrimary
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.data.Resena
import com.example.fixnow.data.IAService
import com.example.fixnow.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun PantallaDetalleSocio(navController: NavController, socioId: String) {
    val scope = rememberCoroutineScope()
    var socio by remember { mutableStateOf<UsuarioPerfil?>(null) }
    var fotosTrabajos by remember { mutableStateOf<List<String>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var nuevoComentario by remember { mutableStateOf("") }
    val listaResenas = remember { mutableStateListOf<Resena>() }

    // Colores del tema
    val fondo       = MaterialTheme.colorScheme.background
    val superficie  = MaterialTheme.colorScheme.surface
    val supVar      = MaterialTheme.colorScheme.surfaceVariant
    val sobreSup    = MaterialTheme.colorScheme.onSurface
    val sobreSupVar = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(socioId) {
        cargando = true
        socio = UsuarioRepository.obtenerSocioPorId(socioId)
        fotosTrabajos = UsuarioRepository.obtenerFotosDeTrabajos(socioId)
        val resenas = UsuarioRepository.obtenerResenasPorSocio(socioId)
        listaResenas.clear()
        // Agregamos las reseñas invertidas para ver las más nuevas primero
        listaResenas.addAll(resenas.reversed())
        cargando = false
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("chat/${socioId}/${socio?.nombre ?: "Socio"}") },
                containerColor = OrangePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Email, null)
                Spacer(Modifier.width(8.dp))
                Text("Chat")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (socio != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fondo)
                    .padding(paddingValues)
            ) {
                // ── Header con foto ──────────────────────────────
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        AsyncImage(
                            model = if (fotosTrabajos.isNotEmpty()) fotosTrabajos.first() else "https://via.placeholder.com/600x400",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(
                            brush = Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent))
                        ))
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    }
                }

                // ── Info perfil ──────────────────────────────────
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(4.dp, fondo),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.background(superficie)) {
                                Text(socio?.nombre?.take(1)?.uppercase() ?: "S", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(socio?.nombre ?: "Sin nombre", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = sobreSup)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        }
                        Text(socio?.tipo_servicio ?: "Servicios generales", color = sobreSupVar, fontSize = 14.sp)
                    }
                }

                // ── Resumen de IA ────────────────────────────────
                item {
                    val resumenActual = socio?.resumenIA
                    if (resumenActual != null && resumenActual.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, "IA", tint = Color(0xFF1976D2))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Resumen Inteligente", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1565C0))
                                    Text(resumenActual, fontSize = 13.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }

                // ── Opiniones estadísticas ───────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Opinión de la comunidad:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF5D4037))
                            Spacer(Modifier.height(4.dp))
                            Text("Basado en ${listaResenas.size} reseñas.", fontSize = 13.sp, color = Color(0xFF6D4C41))
                        }
                    }
                }

                // ── Galería ──────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(vertical = 20.dp)) {
                        Text("Trabajos realizados", fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            color = sobreSup,
                            modifier = Modifier.padding(start = 20.dp, bottom = 10.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (fotosTrabajos.isEmpty()) {
                                item { Text("Sin fotos", color = sobreSupVar, fontSize = 13.sp) }
                            } else {
                                items(fotosTrabajos) { url ->
                                    Card(modifier = Modifier.size(140.dp, 100.dp), shape = RoundedCornerShape(8.dp)) {
                                        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Comentarios ──────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("Escribe una opinión", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = sobreSup)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nuevoComentario,
                            onValueChange = { nuevoComentario = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("¿Qué te pareció el servicio?", fontSize = 14.sp, color = sobreSupVar) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = superficie,
                                unfocusedContainerColor = superficie,
                                focusedTextColor = sobreSup,
                                unfocusedTextColor = sobreSup
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (nuevoComentario.isNotBlank()) {
                                    val user = SupabaseClient.client.auth.currentUserOrNull()
                                    val metadata = user?.userMetadata
                                    val rawName = metadata?.get("nombre")?.toString() ?: "Anónimo"
                                    val userName = rawName.replace("\"", "")
                                    
                                    val resena = Resena(
                                        socioId = socioId,
                                        usuarioId = user?.id ?: "anon",
                                        usuarioNombre = userName,
                                        comentario = nuevoComentario
                                    )
                                    
                                    scope.launch {
                                        try {
                                            UsuarioRepository.insertarResena(resena)
                                            listaResenas.add(0, resena)
                                            val textoEnviado = nuevoComentario
                                            nuevoComentario = ""
                                            
                                            // Lógica de activación de IA: 1ra reseña o cada 3 nuevas (4, 7, 10...)
                                            val total = listaResenas.size
                                            if (total == 1 || (total - 1) % 3 == 0) {
                                                val todosLosComentarios = listaResenas.map { it.comentario }
                                                val aiResult = IAService.generarResumenIA(todosLosComentarios)
                                                if (aiResult.isSuccess) {
                                                    val nuevoResumen = aiResult.getOrNull() ?: ""
                                                    UsuarioRepository.actualizarResumenIA(socioId, nuevoResumen)
                                                    socio = socio?.copy(resumenIA = nuevoResumen)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // Error al guardar reseña
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) { Text("Publicar") }
                        Spacer(Modifier.height(20.dp))
                        Text("Opiniones recientes", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = sobreSup)
                        Spacer(Modifier.height(12.dp))
                    }
                }

                items(listaResenas) { resena ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = supVar)
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Box(Modifier.size(30.dp).background(sobreSupVar.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp), tint = superficie)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(resena.usuarioNombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = sobreSup)
                                Text(resena.comentario, fontSize = 13.sp, color = sobreSupVar)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}
