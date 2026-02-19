package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.fixnow.R
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.model.Categoria
import com.example.fixnow.OrangePrimary
import com.example.fixnow.OrangeLight
import com.example.fixnow.BackgroundWhite
import com.example.fixnow.TextGray
import io.github.jan.supabase.auth.auth

@Composable
fun PantallaInicio(navController: NavController) {
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val user = session?.user
    val nombreUsuario = user?.userMetadata?.get("nombre")?.toString()?.trim('"')
        ?: user?.email?.substringBefore("@")
        ?: "Usuario"

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(padding)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(brush = Brush.verticalGradient(colors = listOf(OrangePrimary, OrangeLight)))
                ) {
                    Text(
                        "Sin ubicación",
                        color = Color.White,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Hola, $nombreUsuario ",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 40.dp)
                    )
                }
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ingresa tu ubicación", color = TextGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "¿Qué servicio buscas?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))

            val categorias = listOf(
                Categoria("Plomería", Icons.Default.Build),
                Categoria("Cerrajería", Icons.Default.Lock),
                Categoria("Electricidad", Icons.Default.Star),
                Categoria("Mecánica", Icons.Default.Settings),
                Categoria("Carpintería", Icons.Default.Home),
                Categoria("Limpieza", Icons.Default.Delete)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categorias) { cat ->
                    CardCategoria(cat) {
                        navController.navigate("servicios/${cat.nombre}")
                    }
                }
            }
        }
    }
}

@Composable
fun CardCategoria(cat: Categoria, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.size(100.dp).clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(cat.icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(cat.nombre, fontSize = 12.sp, color = TextGray)
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController) {
    val amarillo = Color(0xFFFFB300)
    val gris = Color(0xFF9E9E9E)
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route?.substringBefore("/") ?: "inicio"

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_inicio),
                    contentDescription = "Inicio",
                    modifier = Modifier.size(24.dp),
                    tint = if (currentRoute == "inicio") amarillo else gris
                )
            },
            label = { Text("Inicio", color = if (currentRoute == "inicio") amarillo else gris, fontSize = 12.sp) },
            selected = currentRoute == "inicio",
            onClick = {
                navController.navigate("inicio") {
                    popUpTo("inicio") { inclusive = true }
                }
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_servicios),
                    contentDescription = "Servicios",
                    modifier = Modifier.size(24.dp),
                    tint = if (currentRoute == "servicios_tab") amarillo else gris
                )
            },
            label = { Text("Servicios", color = if (currentRoute == "servicios_tab") amarillo else gris, fontSize = 12.sp) },
            selected = currentRoute == "servicios_tab",
            onClick = { navController.navigate("servicios_tab") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_perfil),
                    contentDescription = "Perfil",
                    modifier = Modifier.size(24.dp),
                    tint = if (currentRoute == "perfil") amarillo else gris
                )
            },
            label = { Text("Perfil", color = if (currentRoute == "perfil") amarillo else gris, fontSize = 12.sp) },
            selected = currentRoute == "perfil",
            onClick = { navController.navigate("perfil") },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}