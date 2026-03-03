package com.example.fixnow.screens

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import coil.compose.AsyncImage
import com.example.fixnow.data.AppEstadoPrefs
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.ui.theme.*
import io.github.jan.supabase.auth.auth

@Composable
fun PantallaInicio(navController: NavController) {
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val user = session?.user
    val context = LocalContext.current

    val nombreUsuario = user?.userMetadata?.get("nombre")?.toString()?.trim('"')
        ?: user?.email?.substringBefore("@") ?: "Usuario"

    var fotosTrabajos by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(Unit) { fotosTrabajos = UsuarioRepository.obtenerFotosDeTrabajos() }

    // Colores del tema
    val fondo      = MaterialTheme.colorScheme.background
    val superficie = MaterialTheme.colorScheme.surface
    val sobreFondo = MaterialTheme.colorScheme.onBackground
    val sobreSup   = MaterialTheme.colorScheme.onSurface
    val supVar     = MaterialTheme.colorScheme.surfaceVariant

    Scaffold(bottomBar = { BottomNavBar(navController) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(fondo)                              // ← tema
        ) {
            // ── Header naranja ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(brush = Brush.verticalGradient(colors = listOf(OrangeDark, OrangePrimary)))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Place, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tecate, Baja California", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(nombreUsuario.first().uppercaseChar().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Hola, ${nombreUsuario.split(" ").first()} 👋", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("¿Qué servicio necesitas hoy?", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 4.dp) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Buscar servicio o profesional...", color = Color(0xFFBDBDBD), fontSize = 14.sp)
                        }
                    }
                }
            }

            // ── Accesos rápidos ──────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Accesos rápidos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = sobreFondo)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        Triple("Plomería",  Icons.Default.Build,    "Plomería"),
                        Triple("Eléctrico", Icons.Default.Star,     "Electricidad"),
                        Triple("Mecánica",  Icons.Default.Settings, "Mecánica"),
                        Triple("Más",       Icons.Default.Apps,     null)
                    ).forEach { (label, icon, categoria) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f).clickable {
                                if (categoria != null) AppEstadoPrefs.guardarUltimaCategoria(context, categoria)
                                navController.navigate("servicios") {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(supVar), // ← tema
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, null, tint = OrangePrimary, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(label, fontSize = 11.sp, color = sobreFondo, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // ── Trabajos recientes ───────────────────────────────
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Trabajos recientes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = sobreFondo, modifier = Modifier.weight(1f))
                    Text("Ver todos", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(180.dp)
                ) {
                    if (fotosTrabajos.isEmpty()) {
                        items(4) { CardFotoSoloVista(url = null) }
                    } else {
                        items(fotosTrabajos) { url -> CardFotoSoloVista(url = url) }
                    }
                }
            }

            // ── Socios destacados ────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Socios destacados", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = sobreFondo, modifier = Modifier.weight(1f))
                    Text("Ver todos", fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { navController.navigate("servicios") })
                }
                Spacer(modifier = Modifier.height(12.dp))
                CardSocioDestacado("Carpintería El Super", 22, "A 12 min de ti", "Carpintería", superficie, sobreSup)
                Spacer(modifier = Modifier.height(10.dp))
                CardSocioDestacado("Plomería Tecate", 15, "A 5 min de ti", "Plomería", superficie, sobreSup)
            }
        }
    }
}

@Composable
fun CardFotoSoloVista(url: String?) {
    val supVar = MaterialTheme.colorScheme.surfaceVariant
    val sobreSupVar = MaterialTheme.colorScheme.onSurfaceVariant
    Card(modifier = Modifier.size(width = 140.dp, height = 180.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        if (url != null) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.fillMaxSize().background(supVar), contentAlignment = Alignment.Center) {  // ← tema
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Place, null, tint = sobreSupVar, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Sin foto", fontSize = 10.sp, color = sobreSupVar)
                }
            }
        }
    }
}

@Composable
fun CardSocioDestacado(nombre: String, resenas: Int, tiempo: String, categoria: String, superficie: Color, sobreSup: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = superficie),   // ← tema
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF3E0)), contentAlignment = Alignment.Center) {
                Text(nombre.first().toString(), fontWeight = FontWeight.ExtraBold, color = OrangePrimary, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = sobreSup)         // ← tema
                Text(categoria, fontSize = 12.sp, color = OrangePrimary, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(13.dp))
                    Text(" $resenas reseñas  ·  $tiempo", fontSize = 11.sp, color = sobreSup.copy(alpha = 0.6f))
                }
            }
            Icon(Icons.Default.KeyboardArrowRight, null, tint = sobreSup.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,          // ← tema
        tonalElevation = 8.dp
    ) {
        listOf(
            Triple("inicio",    Icons.Default.Home,   "Inicio"),
            Triple("servicios", Icons.Default.Apps,   "Servicios"),
            Triple("mensajes",  Icons.Default.Email,  "Mensajes"),
            Triple("perfil",    Icons.Default.Person, "Perfil")
        ).forEach { (ruta, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, null) },
                label = { Text(label, fontSize = 11.sp) },
                selected = currentRoute == ruta,
                onClick = {
                    if (currentRoute != ruta) {
                        navController.navigate(ruta) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangePrimary,
                    selectedTextColor = OrangePrimary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,   // ← tema
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,   // ← tema
                    indicatorColor = Color(0xFFFFF3E0)
                )
            )
        }
    }
}
