package com.example.fixnow.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.fixnow.ui.theme.*
import com.example.fixnow.data.AppEstadoPrefs
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.utils.LocationUtils
import com.google.android.gms.location.LocationServices
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

data class SubServicio(
    val nombre: String,
    val precioAprox: String,
    val icon: ImageVector
)

data class CategoriaExtra(
    val nombre: String,
    val idBusqueda: String,
    val icon: ImageVector,
    val descripcion: String
)

enum class ModoServicio {
    INMEDIATO, PROGRAMADO
}

@Composable
fun PantallaServicios(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val uid = session?.user?.id ?: ""

    var categoriaSeleccionada by remember { mutableStateOf<CategoriaExtra?>(null) }
    var subServicioSeleccionado by remember { mutableStateOf<SubServicio?>(null) }
    var modoSeleccionado by remember { mutableStateOf<ModoServicio?>(null) }
    
    var listaSocios by remember { mutableStateOf<List<UsuarioPerfil>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var esSocio by remember { mutableStateOf(false) }
    
    var miUbicacion by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val fondo       = MaterialTheme.colorScheme.background
    val superficie  = MaterialTheme.colorScheme.surface
    val sobreFondo  = MaterialTheme.colorScheme.onBackground
    val sobreSup    = MaterialTheme.colorScheme.onSurface
    val sobreSupVar = MaterialTheme.colorScheme.onSurfaceVariant

    val launcherPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let { miUbicacion = Pair(it.latitude, it.longitude) }
                }
            } catch (e: SecurityException) {}
        }
    }

    val categorias = listOf(
        CategoriaExtra("Plomería",    "Plomeria",    Icons.Default.Build,       "Tuberías y más"),
        CategoriaExtra("Cerrajería",  "Cerrajeria",  Icons.Default.Lock,        "Llaves y cerraduras"),
        CategoriaExtra("Electricidad","Electricidad",Icons.Default.Star,        "Instalaciones"),
        CategoriaExtra("Mecánica",    "Mecanica",    Icons.Default.Settings,    "Autos y motores"),
        CategoriaExtra("Carpintería", "Carpinteria", Icons.Default.Home,        "Muebles y madera"),
        CategoriaExtra("Limpieza",    "Limpieza",    Icons.Default.Delete,      "Hogar y oficina")
    )

    val subServiciosMap = mapOf(
        "Plomería" to listOf(
            SubServicio("Arreglar escusado", "$500 - $1,200", Icons.Default.Build),
            SubServicio("Cambiar tubería", "$1,500 - $3,500", Icons.Default.Warning),
            SubServicio("Instalar lavamanos", "$800 - $1,800", Icons.Default.Add)
        ),
        "Cerrajería" to listOf(
            SubServicio("Abrir puerta", "$400 - $900", Icons.Default.LockOpen),
            SubServicio("Cambiar chapa", "$600 - $1,500", Icons.Default.Lock),
            SubServicio("Duplicado de llaves", "$100 - $300", Icons.Default.Key)
        ),
        "Electricidad" to listOf(
            SubServicio("Cortocircuito", "$600 - $2,000", Icons.Default.FlashOn),
            SubServicio("Instalar lámpara", "$300 - $800", Icons.Default.Lightbulb),
            SubServicio("Revisión de tablero", "$500 - $1,200", Icons.Default.Settings)
        ),
        "Mecánica" to listOf(
            SubServicio("Cambio de aceite", "$800 - $1,500", Icons.Default.Settings),
            SubServicio("Frenos", "$1,200 - $3,000", Icons.Default.Warning),
            SubServicio("Afinación", "$1,500 - $4,000", Icons.Default.Build)
        )
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { miUbicacion = Pair(it.latitude, it.longitude) }
            }
        } else {
            launcherPermisos.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            val perfil = UsuarioRepository.obtenerSocioPorId(uid)
            esSocio = perfil?.es_prestador == true
        }
        
        val guardada = AppEstadoPrefs.obtenerUltimaCategoria(context)
        if (guardada.isNotEmpty()) {
            categorias.find { it.nombre == guardada || it.idBusqueda == guardada }?.let {
                categoriaSeleccionada = it
            }
        }
    }

    Scaffold(bottomBar = { BottomNavBar(navController, esSocio) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fondo)
                .padding(padding)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(brush = Brush.verticalGradient(colors = listOf(OrangeDark, OrangePrimary)))
            ) {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (categoriaSeleccionada != null) {
                        IconButton(onClick = {
                            if (modoSeleccionado != null) {
                                modoSeleccionado = null
                            } else if (subServicioSeleccionado != null) {
                                subServicioSeleccionado = null
                            } else {
                                categoriaSeleccionada = null
                                AppEstadoPrefs.guardarUltimaCategoria(context, "")
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    }
                    Column {
                        Text(
                            text = subServicioSeleccionado?.nombre ?: (categoriaSeleccionada?.nombre ?: "Servicios"),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                modoSeleccionado != null -> if (modoSeleccionado == ModoServicio.INMEDIATO) "Buscando socios cercanos..." else "Agenda tu cita"
                                subServicioSeleccionado != null -> "¿Cuándo lo necesitas?"
                                categoriaSeleccionada != null -> "¿Qué trabajo necesitas?"
                                else -> "¿En qué te podemos ayudar?"
                            },
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.weight(1f)
            ) {
                when {
                    categoriaSeleccionada == null -> {
                        PantallaSeleccionCategoria(categorias, superficie, sobreFondo) {
                            categoriaSeleccionada = it
                            AppEstadoPrefs.guardarUltimaCategoria(context, it.nombre)
                        }
                    }
                    subServicioSeleccionado == null -> {
                        PantallaSeleccionSubServicio(subServiciosMap, categoriaSeleccionada!!, superficie, sobreSup) {
                            subServicioSeleccionado = it
                        }
                    }
                    modoSeleccionado == null -> {
                        PantallaSeleccionModo(superficie, sobreFondo) { modo ->
                            modoSeleccionado = modo
                            scope.launch {
                                cargando = true
                                // Si es inmediato, solo socios disponibles
                                val rawSocios = if (modo == ModoServicio.INMEDIATO) {
                                    UsuarioRepository.obtenerSociosDisponiblesPorCategoria(categoriaSeleccionada!!.idBusqueda)
                                } else {
                                    UsuarioRepository.obtenerSociosPorCategoria(categoriaSeleccionada!!.idBusqueda)
                                }
                                
                                // Ordenar por distancia si tenemos ubicación
                                listaSocios = if (miUbicacion != null) {
                                    rawSocios.sortedBy { socio ->
                                        if (socio.latitud != null && socio.longitud != null) {
                                            LocationUtils.calcularDistancia(miUbicacion!!.first, miUbicacion!!.second, socio.latitud, socio.longitud)
                                        } else Double.MAX_VALUE
                                    }
                                } else {
                                    rawSocios
                                }
                                cargando = false
                            }
                        }
                    }
                    else -> {
                        PantallaListaSocios(cargando, listaSocios, miUbicacion, subServicioSeleccionado!!, superficie, sobreSup, sobreSupVar, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun PantallaSeleccionCategoria(categorias: List<CategoriaExtra>, superficie: Color, sobreFondo: Color, onSelect: (CategoriaExtra) -> Unit) {
    Column {
        Text("Explorar categorías", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            color = sobreFondo, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(modifier = Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categorias) { cat ->
                CardServicio(cat, superficie, sobreFondo) { onSelect(cat) }
            }
        }
    }
}

@Composable
fun PantallaSeleccionSubServicio(subServiciosMap: Map<String, List<SubServicio>>, categoria: CategoriaExtra, superficie: Color, sobreSup: Color, onSelect: (SubServicio) -> Unit) {
    val subs = subServiciosMap[categoria.nombre] ?: listOf(
        SubServicio("Servicio General", "A convenir", Icons.Default.Build)
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(subs) { sub ->
            CardSubServicio(sub, superficie, sobreSup) { onSelect(sub) }
        }
    }
}

@Composable
fun PantallaSeleccionModo(superficie: Color, sobreFondo: Color, onSelect: (ModoServicio) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("¿Cómo deseas tu servicio?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = sobreFondo)
        Spacer(modifier = Modifier.height(24.dp))
        
        CardModo(
            titulo = "Servicio Inmediato",
            subtitulo = "El socio más cercano vendrá a tu ubicación ahora mismo.",
            icono = Icons.Default.FlashOn,
            color = OrangePrimary,
            bg = superficie,
            onClick = { onSelect(ModoServicio.INMEDIATO) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CardModo(
            titulo = "Programar Cita",
            subtitulo = "Elige una fecha y hora que te convenga.",
            icono = Icons.Default.Event,
            color = Color(0xFF2196F3),
            bg = superficie,
            onClick = { onSelect(ModoServicio.PROGRAMADO) }
        )
    }
}

@Composable
fun CardModo(titulo: String, subtitulo: String, icono: ImageVector, color: Color, bg: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icono, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(titulo, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
                Text(subtitulo, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PantallaListaSocios(
    cargando: Boolean,
    listaSocios: List<UsuarioPerfil>,
    miUbicacion: Pair<Double, Double>?,
    subServicio: SubServicio,
    superficie: Color,
    sobreSup: Color,
    sobreSupVar: Color,
    navController: NavController
) {
    if (cargando) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = OrangePrimary)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (listaSocios.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay socios disponibles en este momento.", color = sobreSupVar, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(listaSocios) { socio ->
                    val distancia = if (miUbicacion != null && socio.latitud != null && socio.longitud != null) {
                        LocationUtils.calcularDistancia(miUbicacion.first, miUbicacion.second, socio.latitud, socio.longitud)
                    } else null
                    
                    CardSocioConProximidad(socio, subServicio.precioAprox, distancia, superficie, sobreSup) {
                        navController.navigate("detalle_socio/${socio.id}")
                    }
                }
            }
        }
    }
}

@Composable
fun CardSubServicio(sub: SubServicio, bg: Color, content: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(sub.icon, null, tint = OrangePrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sub.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = content)
                Text("Precio est.: ${sub.precioAprox}", fontSize = 13.sp, color = OrangePrimary, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ChevronRight, null, tint = content.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun CardSocioConProximidad(socio: UsuarioPerfil, precio: String, distancia: Double?, bg: Color, content: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFFFFF3E0)), contentAlignment = Alignment.Center) {
                Text(socio.nombre?.take(1)?.uppercase() ?: "S", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = OrangePrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(socio.nombre ?: "Socio", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = content)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(15.dp))
                    Text(" 4.9 ", fontSize = 13.sp, color = content, fontWeight = FontWeight.Medium)
                    if (distancia != null) {
                        Text(" · ${LocationUtils.formatoDistancia(distancia)} de ti", fontSize = 13.sp, color = OrangePrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Text(socio.tipo_servicio ?: "", fontSize = 11.sp, color = content.copy(alpha = 0.5f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Desde", fontSize = 10.sp, color = content.copy(alpha = 0.5f))
                Text(precio.split(" - ").first(), fontWeight = FontWeight.ExtraBold, color = OrangePrimary, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun CardServicio(cat: CategoriaExtra, superficie: Color, sobreFondo: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = superficie),
        modifier = Modifier.aspectRatio(1f).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(cat.icon, null, modifier = Modifier.size(24.dp), tint = OrangePrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(cat.nombre, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = sobreFondo)
        }
    }
}
