package com.example.fixnow.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fixnow.OrangePrimary
import com.example.fixnow.OrangeLight
import com.example.fixnow.data.SupabaseClient
import androidx.compose.material.icons.filled.Place
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import com.example.fixnow.data.UsuarioRepository
import kotlinx.coroutines.launch
import com.example.fixnow.data.UsuarioPerfil


@Composable
fun PantallaInicio(navController: NavController) {
    val scope = rememberCoroutineScope()
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val user = session?.user

    val nombreUsuario = user?.userMetadata?.get("nombre")?.toString()?.trim('"')
        ?: user?.email?.substringBefore("@") ?: "Usuario"

    var fotosTrabajos by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        fotosTrabajos = UsuarioRepository.obtenerFotosDeTrabajos()
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFFDFDFD))
        ) {
            HeaderAmarillo()

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "¿Qué servicio buscas hoy, $nombreUsuario?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Carrusel de fotos
                LazyRow(
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (fotosTrabajos.isEmpty()) {
                        items(3) { CardFotoSoloVista(url = null) }
                    } else {
                        items(fotosTrabajos) { url -> CardFotoSoloVista(url = url) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Socios destacados",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CardSocioDestacado(
                        nombre = "Carpintería El Super",
                        resenas = 22,
                        tiempo = "A 12 minutos de ti"
                    )

                    CardSocioDestacado(
                        nombre = "Plomería Tecate",
                        resenas = 15,
                        tiempo = "A 5 minutos de ti"
                    )
                }
            }
        }
    }
}

@Composable
fun CardFotoSoloVista(url: String?) {
    Card(
        modifier = Modifier.size(width = 150.dp, height = 200.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)

    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Place, null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun HeaderAmarillo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFBC02D), Color(0xFFFFF176))
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Calle Alcatraz #1515, Tecate",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(25.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ingresa tu ubicacion", color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun SeccionSubirFotos() {
    val fotosSeleccionadas = remember { mutableStateListOf<Uri>() }
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) fotosSeleccionadas.add(uri) }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "¿Que servicio buscas el dia de hoy, Jofiel?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.height(210.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                CardFotoTrabajo(
                    esBotonAgregar = true,
                    onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )
            }
            items(fotosSeleccionadas) { uri ->
                CardFotoTrabajo(uri = uri)
            }
            if (fotosSeleccionadas.isEmpty()) {
                items(2) { CardFotoTrabajo(esDecorativo = true) }
            }
        }
    }
}

@Composable
fun CardFotoTrabajo(
    uri: Uri? = null,
    esBotonAgregar: Boolean = false,
    esDecorativo: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .size(width = 120.dp, height = 160.dp)
            .clickable(enabled = esBotonAgregar) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (uri != null) Color.Transparent else Color(0xFFD1D1D1)),
                contentAlignment = Alignment.Center
            ) {
                if (uri != null) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = if (esBotonAgregar) Color.White else Color(0xFFE0E0E0),
                            modifier = Modifier.size(32.dp)
                        )
                        if (esBotonAgregar) {
                            Text("Tomar Foto", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
            Text(
                "Añade fotos de tu trabajo",
                fontSize = 10.sp,
                modifier = Modifier.padding(8.dp),
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun SeccionSociosDinamica(
    listaSocios: List<UsuarioPerfil>,
    estaCargando: Boolean,
    categoria: String
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = if (categoria.isEmpty()) "Socios destacados" else "Especialistas en $categoria",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (estaCargando) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrangePrimary)
            }
        } else if (categoria.isNotEmpty() && listaSocios.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDEEE9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No se encontraron socios en esta categoría.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listaSocios.forEach { socio ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(socio.nombre ?: "Socio", fontWeight = FontWeight.Bold)
                            Text(socio.tipo_servicio ?: "Servicio General", fontSize = 12.sp, color = OrangePrimary)
                            Text("Disponible ahora", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color.White) {
        // INICIO
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Inicio") },
            selected = currentRoute == "inicio",
            onClick = {
                if (currentRoute != "inicio") {
                    navController.navigate("inicio") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFB300),
                selectedTextColor = Color(0xFFFFB300),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )

        // SERVICIOS
        NavigationBarItem(
            icon = { Icon(Icons.Default.Apps, null) },
            label = { Text("Servicios") },
            selected = currentRoute == "servicios",
            onClick = {
                if (currentRoute != "servicios") {
                    navController.navigate("servicios") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFB300),
                selectedTextColor = Color(0xFFFFB300),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )

        // MENSAJES (NUEVA PESTAÑA)
        NavigationBarItem(
            icon = { Icon(Icons.Default.Email, null) },
            label = { Text("Mensajes") },
            selected = currentRoute == "mensajes",
            onClick = {
                if (currentRoute != "mensajes") {
                    navController.navigate("mensajes") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFB300),
                selectedTextColor = Color(0xFFFFB300),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )

        // PERFIL
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Perfil") },
            selected = currentRoute == "perfil",
            onClick = {
                if (currentRoute != "perfil") {
                    navController.navigate("perfil") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFFFB300),
                selectedTextColor = Color(0xFFFFB300),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun CardSocioDestacado(
    nombre: String,
    resenas: Int,
    tiempo: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF333333)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = " ($resenas reseñas)",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Text(
                text = tiempo,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
