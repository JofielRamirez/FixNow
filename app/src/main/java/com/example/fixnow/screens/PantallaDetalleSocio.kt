package com.example.fixnow.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fixnow.ui.theme.OrangePrimary
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import io.github.jan.supabase.auth.auth
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

    var socio by remember { mutableStateOf<UsuarioPerfil?>(null) }
    var fotosTrabajos by remember { mutableStateOf<List<String>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mostrarDialogoCita by remember { mutableStateOf(false) }

    val fondo       = MaterialTheme.colorScheme.background
    val superficie  = MaterialTheme.colorScheme.surface
    val sobreSup    = MaterialTheme.colorScheme.onSurface
    val sobreSupVar = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(socioId) {
        cargando = true
        socio = UsuarioRepository.obtenerSocioPorId(socioId)
        fotosTrabajos = UsuarioRepository.obtenerFotosDeTrabajos(socioId)
        cargando = false
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { navController.navigate("chat/${socioId}/${socio?.nombre ?: "Socio"}") },
                    containerColor = superficie,
                    contentColor = OrangePrimary,
                    shape = CircleShape
                ) { Icon(Icons.Default.Email, null) }
                
                Spacer(Modifier.height(12.dp))
                
                ExtendedFloatingActionButton(
                    onClick = { mostrarDialogoCita = true },
                    containerColor = OrangePrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agendar Cita")
                }
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
                            Text(socio?.nombre ?: "Sin nombre", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(socio?.tipo_servicio ?: "Servicios generales", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Sobre mi servicio", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = sobreSup)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            socio?.descripcion ?: "Este profesional aún no ha añadido una descripción detallada de sus servicios.",
                            fontSize = 15.sp,
                            color = sobreSupVar,
                            lineHeight = 22.sp
                        )
                    }
                }

                item {
                    Column(modifier = Modifier.padding(vertical = 10.dp)) {
                        Text("Galería de trabajos", fontWeight = FontWeight.Bold, fontSize = 18.sp,
                            color = sobreSup,
                            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
                        
                        if (fotosTrabajos.isEmpty()) {
                            Text("No hay fotos disponibles", color = sobreSupVar, fontSize = 14.sp, modifier = Modifier.padding(start = 20.dp))
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(fotosTrabajos) { url ->
                                    Card(
                                        modifier = Modifier.size(160.dp, 120.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

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
    var step by remember { mutableStateOf(1) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    var detalles by remember { mutableStateOf("") }
    
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                when(step) {
                    1 -> "Selecciona la Fecha"
                    2 -> "Selecciona la Hora"
                    else -> "Detalles de la Cita"
                }, 
                fontWeight = FontWeight.Bold 
            ) 
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when(step) {
                    1 -> {
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                    2 -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TimePicker(state = timePickerState)
                        }
                    }
                    3 -> {
                        OutlinedTextField(
                            value = detalles,
                            onValueChange = { detalles = it },
                            label = { Text("¿Qué necesitas exactamente?") },
                            placeholder = { Text("Ej: Instalación de 3 lámparas...") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        val fechaStr = datePickerState.selectedDateMillis?.let { 
                            dateFormatter.format(Date(it)) 
                        } ?: ""
                        val horaStr = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        onConfirm("$fechaStr $horaStr", detalles)
                    }
                },
                enabled = when(step) {
                    1 -> datePickerState.selectedDateMillis != null
                    3 -> detalles.isNotBlank()
                    else -> true
                },
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
