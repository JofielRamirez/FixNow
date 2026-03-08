package com.example.fixnow.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.fixnow.data.*
import com.example.fixnow.ui.theme.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Composable
fun PantallaInicio(navController: NavController) {
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val user = session?.user
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var perfil by remember { mutableStateOf<UsuarioPerfil?>(null) }
    var fotosTrabajos by remember { mutableStateOf<List<String>>(emptyList()) }
    var cargandoPerfil by remember { mutableStateOf(true) }
    
    // Estados de Citas y Notificaciones
    var servicioActivo by remember { mutableStateOf<Cita?>(null) }
    var solicitudPendiente by remember { mutableStateOf<Cita?>(null) } // Para el SOCIO
    var avisoAceptacion by remember { mutableStateOf<Cita?>(null) }    // Para el CLIENTE

    val nombreUsuario = user?.userMetadata?.get("nombre")?.toString()?.trim('"')
        ?: user?.email?.substringBefore("@") ?: "Usuario"

    LaunchedEffect(Unit) {
        user?.id?.let { uid ->
            val p = UsuarioRepository.obtenerSocioPorId(uid)
            perfil = p
            
            // Cargar estado inicial
            val citasSocio = UsuarioRepository.obtenerCitasSocio(uid)
            val citasCliente = UsuarioRepository.obtenerCitasCliente(uid)
            
            // 1. Ver si hay algo activo (aceptada/en_camino)
            servicioActivo = (citasSocio + citasCliente).find { 
                it.estado == "aceptada" || it.estado == "en_camino" 
            }

            // 2. Ver si el socio tiene una solicitud pendiente sin responder
            if (p?.es_prestador == true) {
                solicitudPendiente = citasSocio.find { it.estado == "pendiente" }
            }

            // --- Escucha en Tiempo Real (Realtime) ---
            val channel = SupabaseClient.client.channel("notificaciones_inicio")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "citas"
            }
            
            flow.onEach { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val nueva = action.decodeRecord<Cita>()
                        // Si soy el socio y me llega una nueva solicitud
                        if (nueva.idSocio == uid && nueva.estado == "pendiente") {
                            solicitudPendiente = nueva
                        }
                    }
                    is PostgresAction.Update -> {
                        val actualizada = action.decodeRecord<Cita>()
                        if (actualizada.idCliente == uid || actualizada.idSocio == uid) {
                            
                            // Caso: Socio acepta -> Notificar al cliente
                            if (actualizada.idCliente == uid && actualizada.estado == "aceptada") {
                                avisoAceptacion = actualizada
                                servicioActivo = actualizada
                            }

                            // Caso: Actualización de banner
                            if (actualizada.estado == "aceptada" || actualizada.estado == "en_camino") {
                                servicioActivo = actualizada
                                if (actualizada.idSocio == uid) solicitudPendiente = null // Limpiar dialog si aceptó
                            } else if (listOf("completada", "cancelada", "finalizada").contains(actualizada.estado)) {
                                if (servicioActivo?.id == actualizada.id) servicioActivo = null
                                if (solicitudPendiente?.id == actualizada.id) solicitudPendiente = null
                            }
                        }
                    }
                    else -> Unit
                }
            }.launchIn(scope)
            channel.subscribe()
        }
        fotosTrabajos = UsuarioRepository.obtenerFotosDeTrabajos()
        cargandoPerfil = false
    }

    // Lógica para Aceptar Cita (Socio)
    val aceptarCita: (String) -> Unit = { id ->
        scope.launch {
            try {
                UsuarioRepository.actualizarEstadoCita(id, "aceptada")
                solicitudPendiente = null
                Toast.makeText(context, "Servicio aceptado. ¡En marcha!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("INICIO", "Error al aceptar: ${e.message}")
            }
        }
    }

    if (cargandoPerfil) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangePrimary)
        }
    } else {
        Scaffold(
            bottomBar = { BottomNavBar(navController, perfil?.es_prestador == true) }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Header (Naranja)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(190.dp)
                            .background(Brush.verticalGradient(colors = listOf(OrangeDark, OrangePrimary)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Place, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tecate, Baja California", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Text(nombreUsuario.firstOrNull()?.uppercaseChar()?.toString() ?: "U", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Hola, ${nombreUsuario.split(" ").first()} 👋", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(if (perfil?.es_prestador == true) "Panel de Socio - ${perfil?.tipo_servicio}" else "¿Qué servicio necesitas?", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 4.dp) {
                                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Buscar profesional...", color = Color.LightGray, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // Contenido dependiendo del rol
                    if (perfil?.es_prestador == true) {
                        SeccionAccesosSocio(navController, MaterialTheme.colorScheme.onBackground, MaterialTheme.colorScheme.surfaceVariant)
                    } else {
                        SeccionAccesosCliente(navController, context, MaterialTheme.colorScheme.onBackground, MaterialTheme.colorScheme.surfaceVariant)
                        // Socios destacados (Simplificado)
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Socios destacados", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.height(10.dp))
                            CardSocioDestacado("Plomería Velázquez", 48, "A 5 min", "Plomería", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(120.dp))
                }

                // --- 1. DIALOG DE NUEVA SOLICITUD (PARA EL SOCIO) ---
                if (solicitudPendiente != null) {
                    DialogNuevaSolicitud(
                        cita = solicitudPendiente!!,
                        onAceptar = { aceptarCita(solicitudPendiente!!.id!!) },
                        onRechazar = { solicitudPendiente = null }
                    )
                }

                // --- 2. DIALOG DE AVISO "SOCIO EN CAMINO" (PARA EL CLIENTE) ---
                if (avisoAceptacion != null) {
                    DialogSocioEnCamino(
                        onEntendido = { avisoAceptacion = null; navController.navigate("seguimiento/${servicioActivo?.id}") }
                    )
                }

                // --- 3. BANNER ESTILO UBER (PERSISTENTE) ---
                AnimatedVisibility(
                    visible = servicioActivo != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 85.dp)
                ) {
                    servicioActivo?.let { cita ->
                        BannerServicioActivo(
                            cita = cita,
                            esSocio = cita.idSocio == user?.id,
                            onClick = { navController.navigate("seguimiento/${cita.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogNuevaSolicitud(cita: Cita, onAceptar: () -> Unit, onRechazar: () -> Unit) {
    Dialog(onDismissRequest = { }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(60.dp).background(OrangePrimary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("¡Nueva solicitud!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Un cliente necesita tu servicio ahora", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                
                Surface(color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                        Text("Detalles:", color = OrangePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(cita.detalles ?: "Sin descripción", color = Color.White, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAceptar,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ACEPTAR SERVICIO", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onRechazar) {
                    Text("Rechazar", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DialogSocioEnCamino(onEntendido: () -> Unit) {
    Dialog(onDismissRequest = onEntendido) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(70.dp).background(Color(0xFFE3F2FD), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF1976D2), modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("¡Socio en camino!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("Tu solicitud ha sido aceptada. Puedes ver la ubicación del trabajador en tiempo real.", 
                    textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onEntendido,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("VER MAPA")
                }
            }
        }
    }
}

@Composable
fun BannerServicioActivo(cita: Cita, esSocio: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        Row(
            modifier = Modifier.background(Brush.horizontalGradient(
                colors = if (esSocio) listOf(Color(0xFF121212), Color(0xFF2C2C2C)) else listOf(Color(0xFF0D47A1), Color(0xFF121212))
            )).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp).background(if (esSocio) OrangePrimary else Color(0xFF2196F3), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (esSocio) Icons.Default.DirectionsCar else Icons.Default.Engineering, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (esSocio) "Servicio en curso" else "Socio en camino", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Toca para ver mapa en vivo", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
            Surface(color = if (cita.estado == "en_camino") Color(0xFF4CAF50) else OrangePrimary, shape = RoundedCornerShape(6.dp)) {
                Text(cita.estado.uppercase(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.4f))
        }
    }
}

// Reutilización de componentes menores
@Composable
fun SeccionAccesosCliente(navController: NavController, context: android.content.Context, sobreFondo: Color, supVar: Color) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text("Categorías", fontWeight = FontWeight.Bold, color = sobreFondo)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Triple("Plomería", Icons.Default.Build, "Plomería"), Triple("Eléctrico", Icons.Default.Star, "Electricidad"), Triple("Mecánica", Icons.Default.Settings, "Mecánica"), Triple("Más", Icons.Default.Apps, null)).forEach { (label, icon, cat) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable {
                    if (cat != null) AppEstadoPrefs.guardarUltimaCategoria(context, cat)
                    navController.navigate("servicios")
                }) {
                    Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(supVar), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = OrangePrimary)
                    }
                    Text(label, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun SeccionAccesosSocio(navController: NavController, sobreFondo: Color, supVar: Color) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text("Gestión de Socio", fontWeight = FontWeight.Bold, color = sobreFondo)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Triple("Citas", Icons.Default.DateRange, "socio_citas"), Triple("Historial", Icons.Default.History, "socio_historial"), Triple("Chat", Icons.Default.Chat, "mensajes"), Triple("Perfil", Icons.Default.Person, "perfil")).forEach { (label, icon, ruta) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { navController.navigate(ruta) }) {
                    Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(supVar), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = OrangePrimary)
                    }
                    Text(label, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun CardSocioDestacado(nombre: String, resenas: Int, tiempo: String, categoria: String, superficie: Color, sobreSup: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = superficie)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(OrangePrimary.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Text(nombre.take(1), color = OrangePrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("$categoria • $tiempo", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
            Text(" 4.9", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, esSocio: Boolean) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        val items = if (esSocio) listOf(Triple("inicio", Icons.Default.Dashboard, "Panel"), Triple("socio_citas", Icons.Default.Event, "Citas"), Triple("mensajes", Icons.Default.Chat, "Chats"), Triple("perfil", Icons.Default.Person, "Perfil"))
                    else listOf(Triple("inicio", Icons.Default.Home, "Inicio"), Triple("servicios", Icons.Default.Apps, "Servicios"), Triple("mensajes", Icons.Default.Chat, "Chats"), Triple("perfil", Icons.Default.Person, "Perfil"))
        
        items.forEach { (ruta, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, null) },
                label = { Text(label, fontSize = 10.sp) },
                selected = currentRoute == ruta,
                onClick = { if (currentRoute != ruta) navController.navigate(ruta) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = OrangePrimary, indicatorColor = OrangePrimary.copy(0.1f))
            )
        }
    }
}
