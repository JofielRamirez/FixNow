package com.example.fixnow.screens

import android.util.Log
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.ui.theme.*
import com.example.fixnow.data.AppEstadoPrefs
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import kotlinx.coroutines.launch

data class CategoriaExtra(
    val nombre: String,
    val idBusqueda: String,
    val icon: ImageVector,
    val descripcion: String
)

@Composable
fun PantallaServicios(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var listaSocios by remember { mutableStateOf<List<UsuarioPerfil>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }

    // Colores del tema
    val fondo       = MaterialTheme.colorScheme.background
    val superficie  = MaterialTheme.colorScheme.surface
    val supVar      = MaterialTheme.colorScheme.surfaceVariant
    val sobreFondo  = MaterialTheme.colorScheme.onBackground
    val sobreSup    = MaterialTheme.colorScheme.onSurface
    val sobreSupVar = MaterialTheme.colorScheme.onSurfaceVariant

    val categorias = listOf(
        CategoriaExtra("Plomería",    "Plomeria",    Icons.Default.Build,       "Tuberías y más"),
        CategoriaExtra("Cerrajería",  "Cerrajeria",  Icons.Default.Lock,        "Llaves y cerraduras"),
        CategoriaExtra("Electricidad","Electricidad",Icons.Default.Star,        "Instalaciones"),
        CategoriaExtra("Mecánica",    "Mecanica",    Icons.Default.Settings,    "Autos y motores"),
        CategoriaExtra("Carpintería", "Carpinteria", Icons.Default.Home,        "Muebles y madera"),
        CategoriaExtra("Limpieza",    "Limpieza",    Icons.Default.Delete,      "Hogar y oficina"),
        CategoriaExtra("Pintura",     "Pintura",     Icons.Default.Create,      "Interior y exterior"),
        CategoriaExtra("Jardinería",  "Jardineria",  Icons.Default.Face,        "Jardines y plantas"),
        CategoriaExtra("Mudanzas",    "Mudanzas",    Icons.Default.ShoppingCart,"Carga y traslado")
    )

    LaunchedEffect(Unit) {
        val guardada = AppEstadoPrefs.obtenerUltimaCategoria(context)
        if (guardada.isNotEmpty()) {
            val cat = categorias.find { it.nombre == guardada }
            if (cat != null) {
                categoriaSeleccionada = cat.nombre
                cargando = true
                listaSocios = UsuarioRepository.obtenerSociosPorCategoria(cat.idBusqueda)
                cargando = false
            }
        }
    }

    Scaffold(bottomBar = { BottomNavBar(navController) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fondo)                       // ← era BackgroundWhite hardcodeado
                .padding(padding)
        ) {
            // Header naranja — siempre naranja
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(brush = Brush.verticalGradient(colors = listOf(OrangeDark, OrangePrimary)))
            ) {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (categoriaSeleccionada != null) {
                        IconButton(onClick = {
                            categoriaSeleccionada = null
                            AppEstadoPrefs.guardarUltimaCategoria(context, "")
                        }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    }
                    Column {
                        Text(categoriaSeleccionada ?: "Servicios", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (categoriaSeleccionada == null) "¿En qué te podemos ayudar?" else "Socios disponibles",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoriaSeleccionada == null) {
                Text("Servicios básicos", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = sobreFondo,                  // ← era Color(0xFF333333)
                    modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categorias) { cat ->
                        CardServicio(cat, superficie, supVar, sobreFondo) {
                            categoriaSeleccionada = cat.nombre
                            AppEstadoPrefs.guardarUltimaCategoria(context, cat.nombre)
                            scope.launch {
                                cargando = true
                                listaSocios = UsuarioRepository.obtenerSociosPorCategoria(cat.idBusqueda)
                                cargando = false
                            }
                        }
                    }
                }
            } else {
                if (cargando) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OrangePrimary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (listaSocios.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No hay socios en ${categoriaSeleccionada?.lowercase()}.", color = sobreSupVar)
                                }
                            }
                        } else {
                            items(listaSocios) { socio ->
                                CardSocioSimple(socio, superficie, sobreSup) {
                                    navController.navigate("detalle_socio/${socio.id}")
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
fun CardServicio(cat: CategoriaExtra, superficie: Color, supVar: Color, sobreFondo: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = superficie),   // ← era Color.White
        modifier = Modifier.aspectRatio(1f).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp)), // naranja decorativo
                contentAlignment = Alignment.Center
            ) {
                Icon(cat.icon, null, modifier = Modifier.size(24.dp), tint = OrangePrimary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(cat.nombre, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = sobreFondo) // ← era Color(0xFF333333)
        }
    }
}

@Composable
fun CardSocioSimple(socio: UsuarioPerfil, superficie: Color, sobreSup: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = superficie),   // ← era Color.White
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFFFF3E0), CircleShape), // naranja decorativo
                contentAlignment = Alignment.Center
            ) {
                Text(socio.nombre?.take(1)?.uppercase() ?: "S", fontWeight = FontWeight.Bold, color = OrangePrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(socio.nombre ?: "Socio", fontWeight = FontWeight.Bold, color = sobreSup)          // ← era Color(0xFF333333)
                Text(socio.tipo_servicio ?: "Servicio general", fontSize = 12.sp, color = sobreSup.copy(alpha = 0.6f)) // ← era Color.Gray
            }
        }
    }
}
