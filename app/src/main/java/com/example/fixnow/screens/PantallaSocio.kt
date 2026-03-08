package com.example.fixnow.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fixnow.data.Cita
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.ui.theme.OrangePrimary
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSocioCitas(navController: NavController) {
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val uid = session?.user?.id ?: ""
    val scope = rememberCoroutineScope()
    var citas by remember { mutableStateOf<List<Cita>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var tabSeleccionada by remember { mutableIntStateOf(0) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            citas = UsuarioRepository.obtenerCitasSocio(uid)
            cargando = false
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, esSocio = true) },
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Gestión de Citas", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(
                selectedTabIndex = tabSeleccionada,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = OrangePrimary,
                indicator = { tabPositions ->
                    if (tabSeleccionada < tabPositions.size) {
                        with(TabRowDefaults) {
                            SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada]),
                                color = OrangePrimary
                            )
                        }
                    }
                }
            ) {
                Tab(
                    selected = tabSeleccionada == 0,
                    onClick = { tabSeleccionada = 0 },
                    text = { Text("Solicitudes") }
                )
                Tab(
                    selected = tabSeleccionada == 1,
                    onClick = { tabSeleccionada = 1 },
                    text = { Text("Aceptadas") }
                )
            }

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else {
                val citasFiltradas = when (tabSeleccionada) {
                    0 -> citas.filter { it.estado == "pendiente" }
                    1 -> citas.filter { it.estado == "aceptada" }
                    else -> emptyList()
                }

                if (citasFiltradas.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (tabSeleccionada == 0) Icons.AutoMirrored.Filled.EventNote else Icons.Default.TaskAlt,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (tabSeleccionada == 0) "No hay solicitudes pendientes" else "No tienes citas aceptadas",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(citasFiltradas) { cita ->
                            CardCita(cita) { nuevoEstado ->
                                scope.launch {
                                    UsuarioRepository.actualizarEstadoCita(cita.id ?: "", nuevoEstado)
                                    citas = UsuarioRepository.obtenerCitasSocio(uid)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardCita(cita: Cita, onStatusChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = OrangePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cliente ID: ${cita.idCliente.take(8)}...", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = when (cita.estado) {
                        "pendiente" -> Color(0xFFFFB74D)
                        "aceptada" -> Color(0xFF4CAF50)
                        "completada" -> Color(0xFF2196F3)
                        "cancelada" -> Color(0xFFF44336)
                        else -> Color.LightGray
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        cita.estado.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Fecha: ${cita.fecha}", fontSize = 14.sp)
            }
            if (!cita.detalles.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Detalles: ${cita.detalles}", fontSize = 14.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when (cita.estado) {
                "pendiente" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onStatusChange("cancelada") }) {
                            Text("Rechazar", color = Color.Red)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onStatusChange("aceptada") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Aceptar")
                        }
                    }
                }
                "aceptada" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = { onStatusChange("cancelada") },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Cancelar Cita")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onStatusChange("completada") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Completar")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSocioHistorial(navController: NavController) {
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val uid = session?.user?.id ?: ""
    var citas by remember { mutableStateOf<List<Cita>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            citas = UsuarioRepository.obtenerCitasSocio(uid)
                .filter { it.estado == "completada" || it.estado == "cancelada" }
                .sortedByDescending { it.id }
            cargando = false
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, esSocio = true) },
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Historial de Trabajos", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (citas.isEmpty()) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No hay historial de trabajos", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(citas) { cita ->
                    CardCitaHistorial(cita)
                }
            }
        }
    }
}

@Composable
fun CardCitaHistorial(cita: Cita) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trabajo #${cita.id?.takeLast(4)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = if (cita.estado == "completada") Color(0xFFE3F2FD) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        cita.estado.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = if (cita.estado == "completada") Color(0xFF1976D2) else Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Fecha: ${cita.fecha}", fontSize = 14.sp)
            Text("Cliente: ${cita.idCliente.take(8)}...", fontSize = 14.sp, color = Color.Gray)
            if (!cita.detalles.isNullOrEmpty()) {
                Text("Detalles: ${cita.detalles}", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSocioResenas(navController: NavController) {
    Scaffold(
        bottomBar = { BottomNavBar(navController, esSocio = true) },
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Calificaciones y Reseñas", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Star, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No tienes reseñas todavía", color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSocioPerfil(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val uid = session?.user?.id ?: ""

    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var urlFoto by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            val perfil = UsuarioRepository.obtenerSocioPorId(uid)
            if (perfil != null) {
                nombre = perfil.nombre ?: ""
                descripcion = perfil.descripcion ?: ""
                urlFoto = perfil.urlFotoPerfil
            }
            cargando = false
        }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    guardando = true
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    if (bytes != null) {
                        val url = UsuarioRepository.subirFotoPerfil(uid, bytes)
                        urlFoto = url
                        Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("PERFIL", "Error: ${e.message}")
                } finally {
                    guardando = false
                }
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, esSocio = true) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Personalizar Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("perfil") }) {
                        Icon(Icons.Default.Settings, null)
                    }
                }
            )
        }
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (urlFoto != null) {
                        AsyncImage(model = urlFoto, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(40.dp), tint = Color.White)
                    }
                    if (guardando) {
                        CircularProgressIndicator(color = OrangePrimary, modifier = Modifier.size(120.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Toca para cambiar foto", fontSize = 12.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre Público") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción de tu servicio") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Ej: Especialista en instalaciones eléctricas residenciales con 10 años de experiencia...") }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                guardando = true
                                UsuarioRepository.actualizarPerfilSocio(uid, nombre, descripcion, urlFoto)
                                Toast.makeText(context, "Perfil guardado correctamente", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                guardando = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    enabled = !guardando
                ) {
                    if (guardando) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { scope.launch { SupabaseClient.client.auth.signOut() } }) {
                    Text("Cerrar Sesión", color = Color.Red)
                }
            }
        }
    }
}
