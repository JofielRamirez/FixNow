package com.example.fixnow.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fixnow.ui.theme.OrangePrimary
import com.example.fixnow.data.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleSocio(navController: NavController, socioId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val idCliente = session?.user?.id ?: ""

    // Estados de datos
    var socio by remember { mutableStateOf<UsuarioPerfil?>(null) }
    var fotosTrabajos by remember { mutableStateOf<List<String>>(emptyList()) }
    var resenas by remember { mutableStateOf<List<ResenaDB>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    // Estados de UI/Interacción
    var mostrarDialogoCita by remember { mutableStateOf(false) }
    var solicitandoInmediato by remember { mutableStateOf(false) }
    var esSocioUsuarioActual by remember { mutableStateOf(false) }
    var nuevoComentario by remember { mutableStateOf("") }
    var puntuacionSeleccionada by remember { mutableIntStateOf(5) }
    var enviandoResena by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val fondo = MaterialTheme.colorScheme.background
    val superficie = MaterialTheme.colorScheme.surface
    val sobreSup = MaterialTheme.colorScheme.onSurface
    val sobreSupVar = MaterialTheme.colorScheme.onSurfaceVariant

    fun cargarDatos() {
        scope.launch {
            try {
                socio = UsuarioRepository.obtenerSocioPorId(socioId)
                fotosTrabajos = UsuarioRepository.obtenerFotosDeTrabajos(socioId)
                resenas = SupabaseClient.client.postgrest["resenas"]
                    .select { filter { eq("id_socio", socioId) } }
                    .decodeList<ResenaDB>()

                if (idCliente.isNotEmpty()) {
                    val p = UsuarioRepository.obtenerSocioPorId(idCliente)
                    esSocioUsuarioActual = p?.es_prestador == true
                }
            } catch (e: Exception) {
                // Error silencioso o log
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(socioId) {
        cargando = true
        cargarDatos()
    }

    val launcherPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Se necesita el permiso para pedir servicio ahora", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController, esSocioUsuarioActual) }
    ) { paddingValues ->
        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (socio != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                LazyColumn(modifier = Modifier.fillMaxSize().background(fondo)) {
                    // HEADER CON IMAGEN Y NOMBRE
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                            AsyncImage(
                                model = socio?.urlFotoPerfil ?: (if (fotosTrabajos.isNotEmpty()) fotosTrabajos.first() else "https://via.placeholder.com/600x400"),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.fillMaxSize().background(
                                brush = Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                            ))
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                            }

                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(socio?.nombre ?: "Sin nombre", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    if (socio?.disponible == true) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                    }
                                }
                                Text(socio?.tipo_servicio ?: "Servicios generales", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                            }
                        }
                    }

                    // CARD DE ESTATUS Y CHAT
                    item {
                        Card(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = superficie),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (socio?.disponible == true) "Disponible ahora" else "No disponible",
                                        fontWeight = FontWeight.Bold,
                                        color = if (socio?.disponible == true) Color(0xFF2E7D32) else Color.Gray
                                    )
                                    Text("Tecate, B.C.", fontSize = 12.sp, color = sobreSupVar)
                                }
                                IconButton(onClick = { navController.navigate("chat/${socioId}/${socio?.nombre}") }) {
                                    Icon(Icons.Default.Chat, null, tint = OrangePrimary)
                                }
                            }
                        }
                    }

                    // RESUMEN IA - CORREGIDO PARA MODO OSCURO
                    item {
                        Card(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), // Fondo azul claro fijo
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, "IA", tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Resumen con IA", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = socio?.resumen_ia ?: "No hay reseñas suficientes para generar un resumen.",
                                    fontSize = 14.sp, 
                                    fontStyle = FontStyle.Italic, 
                                    lineHeight = 20.sp,
                                    color = Color(0xFF0D47A1) // Azul oscuro fijo para contraste siempre (incluso en dark mode)
                                )
                            }
                        }
                    }

                    // DESCRIPCIÓN
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                            Text("Sobre mi servicio", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = sobreSup)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                socio?.descripcion ?: "Este profesional aún no ha añadido una descripción detallada.",
                                fontSize = 15.sp, color = sobreSupVar, lineHeight = 22.sp
                            )
                        }
                    }

                    // GALERÍA
                    item {
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Text("Galería de trabajos", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                                color = sobreSup, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))

                            if (fotosTrabajos.isEmpty()) {
                                Text("No hay fotos disponibles", color = sobreSupVar, fontSize = 14.sp, modifier = Modifier.padding(start = 20.dp))
                            } else {
                                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(fotosTrabajos) { url ->
                                        Card(modifier = Modifier.size(160.dp, 120.dp), shape = RoundedCornerShape(12.dp)) {
                                            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECCIÓN PARA DEJAR RESEÑA
                    item {
                        Card(modifier = Modifier.padding(20.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Deja tu opinión", fontWeight = FontWeight.Bold)
                                Row {
                                    (1..5).forEach { star ->
                                        IconButton(onClick = { puntuacionSeleccionada = star }) {
                                            Icon(Icons.Default.Star, null, tint = if (star <= puntuacionSeleccionada) Color(0xFFFFB300) else Color.LightGray)
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = nuevoComentario,
                                    onValueChange = { nuevoComentario = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("¿Cómo fue tu experiencia?") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (nuevoComentario.isNotBlank()) {
                                            enviandoResena = true
                                            scope.launch {
                                                try {
                                                    val resena = ResenaDB(socioId, idCliente, puntuacionSeleccionada, nuevoComentario)
                                                    SupabaseClient.client.postgrest["resenas"].insert(resena)
                                                    IARepository.generarResumenSocio(socioId)
                                                    Toast.makeText(context, "¡Reseña publicada!", Toast.LENGTH_SHORT).show()
                                                    nuevoComentario = ""
                                                    cargarDatos()
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    enviandoResena = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    enabled = !enviandoResena
                                ) {
                                    if (enviandoResena) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    else Text("Publicar")
                                }
                            }
                        }
                    }

                    // LISTA DE RESEÑAS
                    item {
                        Text("Reseñas de clientes", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                    }

                    items(resenas) { resena ->
                        Card(Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cliente", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(Modifier.weight(1f))
                                    Text("⭐ ${resena.puntuacion}", fontSize = 12.sp, color = Color(0xFFFFB300))
                                }
                                Text(resena.comentario, fontSize = 14.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(120.dp)) }
                }

                // BARRA INFERIOR DE ACCIÓN (AGENDAR / PEDIR AHORA)
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    tonalElevation = 8.dp, shadowElevation = 16.dp, color = superficie,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { mostrarDialogoCita = true },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, OrangePrimary)
                            ) {
                                Icon(Icons.Default.Event, null, tint = OrangePrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Agendar", color = OrangePrimary, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        launcherPermisos.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        return@Button
                                    }
                                    solicitandoInmediato = true
                                    scope.launch {
                                        try {
                                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                                .addOnSuccessListener { loc ->
                                                    scope.launch {
                                                        val ahora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                                                        UsuarioRepository.crearCita(idCliente, socioId, ahora, "SERVICIO URGENTE", lat = loc?.latitude, lon = loc?.longitude)
                                                        Toast.makeText(context, "¡Solicitud enviada!", Toast.LENGTH_LONG).show()
                                                        solicitandoInmediato = false
                                                    }
                                                }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            solicitandoInmediato = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (socio?.disponible == true) OrangePrimary else Color.Gray),
                                enabled = socio?.disponible == true && !solicitandoInmediato
                            ) {
                                if (solicitandoInmediato) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                else {
                                    Icon(Icons.Default.FlashOn, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("PEDIR AHORA", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGOS
    if (mostrarDialogoCita) {
        DialogoAgendarCitaVisual(
            onDismiss = { mostrarDialogoCita = false },
            onConfirm = { fecha, detalles ->
                scope.launch {
                    try {
                        UsuarioRepository.crearCita(idCliente, socioId, fecha, detalles)
                        Toast.makeText(context, "¡Cita solicitada con éxito!", Toast.LENGTH_LONG).show()
                        mostrarDialogoCita = false
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoAgendarCitaVisual(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    var detalles by remember { mutableStateOf("") }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when(step) { 1 -> "Selecciona la Fecha"; 2 -> "Selecciona la Hora"; else -> "Detalles" }, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when(step) {
                    1 -> DatePicker(state = datePickerState, showModeToggle = false, modifier = Modifier.scale(0.8f))
                    2 -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) }
                    3 -> OutlinedTextField(value = detalles, onValueChange = { detalles = it }, label = { Text("¿Qué necesitas?") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step < 3) step++
                    else {
                        val fechaStr = datePickerState.selectedDateMillis?.let { dateFormatter.format(Date(it)) } ?: ""
                        val horaStr = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        onConfirm("$fechaStr $horaStr", detalles)
                    }
                },
                enabled = when(step) { 1 -> datePickerState.selectedDateMillis != null; 3 -> detalles.isNotBlank(); else -> true },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
            ) {
                Text(if (step < 3) "Siguiente" else "Confirmar Cita")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (step > 1) step-- else onDismiss() }) {
                Text(if (step > 1) "Atrás" else "Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}