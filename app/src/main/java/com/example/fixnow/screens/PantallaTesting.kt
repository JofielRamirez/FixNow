package com.example.fixnow.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.fixnow.data.SupabaseClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class Cita(
    val cliente_nombre: String,
    val servicio: String,
    val fecha: String,
    val hora: String,
    val descripcion: String,
    val estado: String = "pendiente"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTesting(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- Estado del formulario de cita ---
    var nombre by remember { mutableStateOf("") }
    var servicio by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var mensajeCita by remember { mutableStateOf("") }
    var cargandoCita by remember { mutableStateOf(false) }

    // --- Estado de ubicación ---
    var latitud by remember { mutableStateOf<Double?>(null) }
    var longitud by remember { mutableStateOf<Double?>(null) }
    var mensajeUbicacion by remember { mutableStateOf("Presiona el botón para obtener tu ubicación") }

    // =====================================================
    // ACTIVIDAD 1.5: MONITOR DE CICLO DE VIDA EN PANTALLA
    // Registra y muestra cada evento del ciclo de vida
    // =====================================================
    var ultimoEvento by remember { mutableStateOf("⏳ Esperando eventos...") }
    val listaEventos = remember { mutableStateListOf<String>() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val mensaje = when (event) {
                Lifecycle.Event.ON_CREATE  -> " onCreate → Pantalla creada"
                Lifecycle.Event.ON_START   -> " onStart → Pantalla visible"
                Lifecycle.Event.ON_RESUME  -> " onResume → App en primer plano"
                Lifecycle.Event.ON_PAUSE   -> " onPause → App perdió el foco"
                Lifecycle.Event.ON_STOP    -> " onStop → App en segundo plano"
                Lifecycle.Event.ON_DESTROY -> " onDestroy → Pantalla destruida"
                else -> null
            }
            mensaje?.let {
                ultimoEvento = it
                listaEventos.add(0, it)
                Log.d("CICLO_VIDA_UI", it)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    latitud = location.latitude
                    longitud = location.longitude
                    mensajeUbicacion = "Lat: ${"%.6f".format(location.latitude)}, Lng: ${"%.6f".format(location.longitude)}"
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
                .setMinUpdateIntervalMillis(2000L)
                .build()
            try {
                fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
                mensajeUbicacion = "Rastreando ubicación..."
            } catch (e: SecurityException) {
                mensajeUbicacion = "Error al acceder a la ubicación"
            }
        } else {
            mensajeUbicacion = "Permiso denegado"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pantalla de Testeo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFBC02D),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFFDFDFD))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // =====================================================
            // SECCIÓN: MONITOR DE CICLO DE VIDA (Actividad 1.5)
            // =====================================================
            Text(
                text = "Monitor de Ciclo de Vida",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Último evento detectado:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = ultimoEvento,
                        color = Color(0xFF4CAF50),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color.DarkGray)
                    Spacer(Modifier.height(8.dp))
                    Text("Historial de eventos:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    if (listaEventos.isEmpty()) {
                        Text(
                            "Minimiza la app, regresa, rota el dispositivo...",
                            color = Color(0xFF888888),
                            fontSize = 12.sp
                        )
                    } else {
                        listaEventos.take(8).forEach { evento ->
                            Text(
                                text = evento,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { listaEventos.clear() }) {
                        Text("Limpiar historial", color = Color(0xFFFBC02D), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(4.dp))

            // =====================================================
            // SECCIÓN: CITAS (Actividad 1.4 - persistencia real)
            // =====================================================
            Text(
                text = "Agendar Cita",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre del cliente") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = servicio,
                        onValueChange = { servicio = it },
                        label = { Text("Servicio (ej. Plomería)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = fecha,
                        onValueChange = { fecha = it },
                        label = { Text("Fecha (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("2025-06-15") }
                    )
                    OutlinedTextField(
                        value = hora,
                        onValueChange = { hora = it },
                        label = { Text("Hora (HH:MM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("10:00") }
                    )
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { descripcion = it },
                        label = { Text("Descripción del problema") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Button(
                        onClick = {
                            if (nombre.isBlank() || servicio.isBlank() || fecha.isBlank() || hora.isBlank()) {
                                mensajeCita = "Llena todos los campos obligatorios"
                                return@Button
                            }
                            cargandoCita = true
                            mensajeCita = ""
                            scope.launch {
                                try {
                                    SupabaseClient.client.postgrest["citas"].insert(
                                        buildJsonObject {
                                            put("cliente_nombre", nombre)
                                            put("servicio", servicio)
                                            put("fecha", fecha)
                                            put("hora", hora)
                                            put("descripcion", descripcion)
                                            put("estado", "pendiente")
                                        }
                                    )
                                    mensajeCita = " Cita guardada exitosamente en Supabase"
                                    nombre = ""; servicio = ""; fecha = ""; hora = ""; descripcion = ""
                                } catch (e: Exception) {
                                    mensajeCita = " Error: ${e.message}"
                                } finally {
                                    cargandoCita = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D)),
                        enabled = !cargandoCita
                    ) {
                        if (cargandoCita) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Agendar Cita", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (mensajeCita.isNotEmpty()) {
                        Text(
                            text = mensajeCita,
                            color = if (mensajeCita.startsWith("bien")) Color(0xFF388E3C) else Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(4.dp))

            // =====================================================
            // SECCIÓN: UBICACIÓN
            // =====================================================
            Text(
                text = "Ubicación en Tiempo Real",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = mensajeUbicacion, fontSize = 14.sp, color = Color(0xFF555555))

                    Button(
                        onClick = {
                            val permiso = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            if (permiso == PackageManager.PERMISSION_GRANTED) {
                                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
                                    .setMinUpdateIntervalMillis(2000L).build()
                                try {
                                    fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
                                    mensajeUbicacion = "Rastreando ubicación..."
                                } catch (e: SecurityException) {
                                    mensajeUbicacion = "Error de permisos"
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D))
                    ) {
                        Text("Obtener Mi Ubicación", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (latitud != null && longitud != null) {
                        val mapView = remember { MapView(context) }
                        AndroidView(
                            factory = { mapView.apply { onCreate(null); onResume() } },
                            modifier = Modifier.fillMaxWidth().height(280.dp),
                            update = { mv ->
                                mv.getMapAsync { map ->
                                    map.uiSettings.isZoomControlsEnabled = true
                                    val pos = LatLng(latitud!!, longitud!!)
                                    map.clear()
                                    map.addMarker(MarkerOptions().position(pos).title("Mi ubicación"))
                                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}