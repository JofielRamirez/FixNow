package com.example.fixnow.screens

import android.net.Uri
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
import com.example.fixnow.data.UsuarioPerfil
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

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            citas = UsuarioRepository.obtenerCitasSocio(uid)
            cargando = false
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, esSocio = true) },
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Citas Pendientes", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (citas.isEmpty()) {
            Column(modifier = Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.EventNote, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No tienes citas pendientes por ahora", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(citas) { cita ->
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
                    color = when(cita.estado) {
                        "pendiente" -> Color(0xFFFFB74D)
                        "aceptada" -> Color(0xFF81C784)
                        else -> Color.LightGray
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(cita.estado.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Fecha: ${cita.fecha}", fontSize = 14.sp)
            if (!cita.detalles.isNullOrEmpty()) {
                Text("Detalles: ${cita.detalles}", fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (cita.estado == "pendiente") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onStatusChange("cancelada") }) {
                        Text("Rechazar", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onStatusChange("aceptada") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSocioHistorial(navController: NavController) {
    Scaffold(
        bottomBar = { BottomNavBar(navController, esSocio = true) },
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Historial de Trabajos", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Aún no has completado trabajos", color = Color.Gray)
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
            CenterAlignedTopAppBar(title = { Text("Personalizar Perfil", fontWeight = FontWeight.Bold) })
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
            }
        }
    }
}
