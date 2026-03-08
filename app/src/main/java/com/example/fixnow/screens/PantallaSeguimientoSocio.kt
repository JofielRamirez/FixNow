package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.fixnow.data.Cita
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.ui.theme.OrangePrimary
import com.example.fixnow.utils.LocationUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSeguimientoSocio(navController: NavController, citaId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var cita by remember { mutableStateOf<Cita?>(null) }
    var socio by remember { mutableStateOf<UsuarioPerfil?>(null) }
    var cargando by remember { mutableStateOf(true) }

    val superficie = MaterialTheme.colorScheme.surface

    LaunchedEffect(citaId) {
        val c = UsuarioRepository.obtenerCitaPorId(citaId)
        cita = c
        if (c != null) {
            socio = UsuarioRepository.obtenerSocioPorId(c.idSocio)
            
            // ESCUCHAR CAMBIOS EN EL SOCIO (UBICACIÓN) EN TIEMPO REAL
            val channelSocio = SupabaseClient.client.channel("seguimiento_socio")
            val flowSocio = channelSocio.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "Usuarios"
            }
            flowSocio.onEach { action ->
                if (action is PostgresAction.Update) {
                    val updatedSocio = action.decodeRecord<UsuarioPerfil>()
                    if (updatedSocio.id == c.idSocio) {
                        socio = updatedSocio
                    }
                }
            }.launchIn(scope)
            channelSocio.subscribe()

            // ESCUCHAR CAMBIOS EN LA CITA (ESTADO)
            val channelCita = SupabaseClient.client.channel("seguimiento_cita")
            val flowCita = channelCita.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "citas"
            }
            flowCita.onEach { action ->
                if (action is PostgresAction.Update) {
                    val citaActualizada = action.decodeRecord<Cita>()
                    if (citaActualizada.id == citaId) {
                        cita = citaActualizada
                        if (citaActualizada.estado == "completada") {
                            navController.navigate("inicio") { popUpTo(0) }
                        }
                    }
                }
            }.launchIn(scope)
            channelCita.subscribe()
        }
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Socio en camino", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (cita != null && socio != null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                // MAPA DE SEGUIMIENTO
                val mapView = remember { MapView(context) }
                AndroidView(
                    factory = { mapView.apply { onCreate(null); onResume() } },
                    modifier = Modifier.fillMaxSize(),
                    update = { mv ->
                        mv.getMapAsync { map ->
                            map.clear()
                            val posSocio = LatLng(socio?.latitud ?: 0.0, socio?.longitud ?: 0.0)
                            val posCliente = LatLng(cita?.latCliente ?: 0.0, cita?.lonCliente ?: 0.0)
                            
                            map.addMarker(
                                MarkerOptions()
                                    .position(posSocio)
                                    .title(socio?.nombre ?: "Socio")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                            )
                            map.addMarker(
                                MarkerOptions()
                                    .position(posCliente)
                                    .title("Tu ubicación")
                            )
                            
                            val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                                .include(posSocio)
                                .include(posCliente)
                                .build()
                            
                            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
                        }
                    }
                )

                // INFO CARD INFERIOR
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = superficie),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = OrangePrimary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(socio?.nombre ?: "Socio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(socio?.tipo_servicio ?: "", fontSize = 14.sp, color = Color.Gray)
                            }
                            IconButton(
                                onClick = { navController.navigate("chat/${socio?.id}/${socio?.nombre}") },
                                modifier = Modifier.background(OrangePrimary.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, tint = OrangePrimary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            val dist = LocationUtils.calcularDistancia(
                                socio?.latitud ?: 0.0, socio?.longitud ?: 0.0,
                                cita?.latCliente ?: 0.0, cita?.lonCliente ?: 0.0
                            )
                            Column {
                                Text("Distancia", fontSize = 12.sp, color = Color.Gray)
                                Text(LocationUtils.formatoDistancia(dist), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Llegada aprox.", fontSize = 12.sp, color = Color.Gray)
                                val eta = (dist * 2 + 2).toInt()
                                Text("$eta min", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }
        }
    }
}
