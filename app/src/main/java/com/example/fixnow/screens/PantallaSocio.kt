package com.example.fixnow.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fixnow.data.Cita
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.ui.theme.OrangePrimary
import com.example.fixnow.utils.LocationUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada]),
                            color = OrangePrimary
                        )
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
                            if (tabSeleccionada == 0) Icons.Default.EventNote else Icons.Default.TaskAlt,
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
                            CardCita(
                                cita = cita,
                                onStatusChange = { nuevoEstado ->
                                    scope.launch {
                                        UsuarioRepository.actualizarEstadoCita(cita.id ?: "", nuevoEstado)
                                        citas = UsuarioRepository.obtenerCitasSocio(uid)
                                    }
                                },
                                onChatClick = {
                                    navController.navigate("chat/${cita.idCliente}/Cliente")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardCita(cita: Cita, onStatusChange: (String) -> Unit, onChatClick: () -> Unit) {
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
                
                IconButton(onClick = onChatClick) {
                    Icon(Icons.Default.Chat, "Chat", tint = OrangePrimary)
                }

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
                    CardCitaHistorial(
                        cita = cita,
                        onChatClick = {
                            navController.navigate("chat/${cita.idCliente}/Cliente")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CardCitaHistorial(cita: Cita, onChatClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trabajo #${cita.id?.takeLast(4)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onChatClick) {
                    Icon(Icons.Default.Chat, "Chat", tint = OrangePrimary, modifier = Modifier.size(20.dp))
                }

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
    var disponible by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    
    // Alerta de servicio entrante
    var servicioEntrante by remember { mutableStateOf<Cita?>(null) }
    var miUltimaUbicacion by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            val perfil = UsuarioRepository.obtenerSocioPorId(uid)
            if (perfil != null) {
                nombre = perfil.nombre ?: ""
                descripcion = perfil.descripcion ?: ""
                urlFoto = perfil.urlFotoPerfil
                disponible = perfil.disponible ?: false
            }
            cargando = false
            
            // ESCUCHAR SOLICITUDES EN TIEMPO REAL
            val channel = SupabaseClient.client.channel("citas_alert")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "citas"
            }
            flow.onEach { action ->
                if (action is PostgresAction.Insert) {
                    val nuevaCita = action.decodeRecord<Cita>()
                    if (nuevaCita.idSocio == uid && nuevaCita.estado == "pendiente" && nuevaCita.detalles?.contains("URGENTE") == true) {
                        servicioEntrante = nuevaCita
                    }
                }
            }.launchIn(scope)
            channel.subscribe()
        }
    }

    // Efecto para actualizar ubicación mientras esté disponible
    LaunchedEffect(disponible) {
        if (disponible) {
            while (disponible) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { loc ->
                                loc?.let {
                                    miUltimaUbicacion = Pair(it.latitude, it.longitude)
                                    scope.launch {
                                        UsuarioRepository.actualizarUbicacion(uid, it.latitude, it.longitude)
                                    }
                                }
                            }
                    } catch (e: SecurityException) {
                        Log.e("GPS", "Permiso denegado")
                    }
                }
                delay(30000) // Cada 30 segundos
            }
        }
    }

    val launcherPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            disponible = true
            scope.launch { UsuarioRepository.actualizarDisponibilidad(uid, true) }
        } else {
            Toast.makeText(context, "Se requiere GPS para estar en línea", Toast.LENGTH_LONG).show()
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
                title = { Text("Mi Panel de Socio", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- ESTATUS UBER-STYLE ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (disponible) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (disponible) Color(0xFF4CAF50) else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (disponible) "ESTÁS EN LÍNEA" else "ESTÁS DESCONECTADO",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (disponible) Color(0xFF2E7D32) else Color.Gray
                                )
                                Text(
                                    text = if (disponible) "Los clientes pueden encontrarte" else "No aparecerás en el mapa",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = disponible,
                                onCheckedChange = { nuevo ->
                                    if (nuevo) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                            disponible = true
                                            scope.launch { UsuarioRepository.actualizarDisponibilidad(uid, true) }
                                        } else {
                                            launcherPermisos.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    } else {
                                        disponible = false
                                        scope.launch { UsuarioRepository.actualizarDisponibilidad(uid, false) }
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF4CAF50), checkedTrackColor = Color(0xFFA5D6A7))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

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
                    Text("Foto de perfil", fontSize = 12.sp, color = Color.Gray)
                    
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
                
                // --- ALERTA DE SERVICIO ENTRANTE (OVERLAY) ---
                if (servicioEntrante != null) {
                    AlertaServicioEntrante(
                        cita = servicioEntrante!!,
                        miUbicacion = miUltimaUbicacion,
                        onAccept = {
                            scope.launch {
                                UsuarioRepository.actualizarEstadoCita(servicioEntrante!!.id ?: "", "aceptada")
                                Toast.makeText(context, "¡Servicio aceptado! Dirígete al cliente.", Toast.LENGTH_LONG).show()
                                servicioEntrante = null
                                navController.navigate("socio_citas")
                            }
                        },
                        onDecline = {
                            scope.launch {
                                UsuarioRepository.actualizarEstadoCita(servicioEntrante!!.id ?: "", "cancelada")
                                servicioEntrante = null
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertaServicioEntrante(cita: Cita, miUbicacion: Pair<Double, Double>?, onAccept: () -> Unit, onDecline: () -> Unit) {
    val context = LocalContext.current
    val distancia = if (miUbicacion != null && cita.latCliente != null && cita.lonCliente != null) {
        LocationUtils.calcularDistancia(miUbicacion.first, miUbicacion.second, cita.latCliente, cita.lonCliente)
    } else null
    
    // Estimación simple: 2 min por km + 3 min de preparación
    val etaMinutos = distancia?.let { (it * 2 + 3).toInt() }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(OrangePrimary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FlashOn, null, tint = OrangePrimary, modifier = Modifier.size(32.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("¡NUEVO SERVICIO!", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = OrangePrimary)
                
                if (distancia != null && etaMinutos != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("A ${LocationUtils.formatoDistancia(distancia)} de ti", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("  •  ", color = Color.LightGray)
                        Text("$etaMinutos min aprox.", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // --- MAPA DE LA UBICACIÓN DEL CLIENTE ---
                if (cita.latCliente != null && cita.lonCliente != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))) {
                        val mapView = remember { MapView(context) }
                        AndroidView(
                            factory = { mapView.apply { onCreate(null); onResume() } },
                            modifier = Modifier.fillMaxSize(),
                            update = { mv ->
                                mv.getMapAsync { map ->
                                    val posCliente = LatLng(cita.latCliente, cita.lonCliente)
                                    map.clear()
                                    map.addMarker(MarkerOptions().position(posCliente).title("Ubicación del cliente"))
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(posCliente, 15f))
                                    map.uiSettings.setAllGesturesEnabled(false)
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Cliente: ${cita.idCliente.take(8)}", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Red)
                    ) {
                        Text("RECHAZAR", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("ACEPTAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
