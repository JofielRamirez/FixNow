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

@Composable
fun PantallaDetalleSocio(navController: NavController, socioId: String) {
    var socio by remember { mutableStateOf<UsuarioPerfil?>(null) }
    var fotosTrabajos by remember { mutableStateOf<List<String>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // Estados para comentarios
    var nuevoComentario by remember { mutableStateOf("") }
    val listaComentarios = remember { mutableStateListOf<String>() }

    LaunchedEffect(socioId) {
        cargando = true
        socio = UsuarioRepository.obtenerSocioPorId(socioId)
        fotosTrabajos = UsuarioRepository.obtenerFotosDeTrabajos(socioId)
        cargando = false
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    navController.navigate("chat/${socioId}/${socio?.nombre ?: "Socio"}")
                },
                containerColor = Color(0xFFFFB300),
                contentColor = Color.White,
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null)
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
                    .background(Color.White)
                    .padding(paddingValues)
            ) {
                // Header
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        AsyncImage(
                            model = if (fotosTrabajos.isNotEmpty()) fotosTrabajos.first() else "https://via.placeholder.com/600x400",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            )
                        ))
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    }
                }

                // Info Perfil
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .offset(y = (-30).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(4.dp, Color.White),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.background(Color.White)) {
                                Text(
                                    text = socio?.nombre?.take(1)?.uppercase() ?: "S",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangePrimary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(socio?.nombre ?: "Sin nombre", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                        }
                        Text(socio?.tipo_servicio ?: "Servicios generales", color = Color.Gray, fontSize = 14.sp)
                    }
                }

                // Sección de Opiniones con botones pequeños
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Sus clientes dicen:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Los clientes están muy satisfechos con el trabajo realizado.",
                                fontSize = 13.sp, color = Color.DarkGray
                            )
                            Spacer(Modifier.height(12.dp))
                            
                            // BOTONES MÁS PEQUEÑOS Y CENTRADOS
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {},
                                    modifier = Modifier.height(38.dp).wrapContentWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.ThumbUp, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Positivas (22)", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {},
                                    modifier = Modifier.height(38.dp).wrapContentWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.ThumbDown, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Negativas (0)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Galería
                item {
                    Column(modifier = Modifier.padding(vertical = 20.dp)) {
                        Text(
                            "Trabajos realizados",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(start = 20.dp, bottom = 10.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (fotosTrabajos.isEmpty()) {
                                item { Text("Sin fotos", color = Color.Gray, fontSize = 13.sp) }
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

                // SECCIÓN DE COMENTARIOS
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("Escribe una opinión", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nuevoComentario,
                            onValueChange = { nuevoComentario = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("¿Qué te pareció el servicio?", fontSize = 14.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (nuevoComentario.isNotBlank()) {
                                    listaComentarios.add(0, nuevoComentario)
                                    nuevoComentario = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                        ) {
                            Text("Publicar")
                        }
                        
                        Spacer(Modifier.height(20.dp))
                        Text("Opiniones recientes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // Lista de comentarios dinámicos
                items(listaComentarios) { comentario ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Box(Modifier.size(30.dp).background(Color.LightGray, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp), tint = Color.White)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Usuario", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(comentario, fontSize = 13.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun ServicioItem(nombre: String, precio: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(nombre, fontWeight = FontWeight.Medium)
                Text(precio, color = Color.Gray, fontSize = 13.sp)
            }
            Icon(Icons.Default.Add, null, tint = OrangePrimary)
        }
    }
}
