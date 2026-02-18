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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Importes de Supabase y de tu proyecto
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.model.Categoria
import com.example.fixnow.OrangePrimary
import com.example.fixnow.OrangeLight
import com.example.fixnow.BackgroundWhite
import com.example.fixnow.TextGray
import io.github.jan.supabase.auth.auth

@Composable
fun PantallaInicio(navController: NavController) {

    // CAMBIO: Obtener el usuario desde Supabase en lugar de Firebase
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val user = session?.user

    // En Supabase, el nombre suele venir en user_metadata
    val nombreUsuario = user?.userMetadata?.get("nombre")?.toString()
        ?: user?.email?.substringBefore("@")
        ?: "Usuario"

    Scaffold(
        bottomBar = { BottomNavBar() }
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
                    Text("Calle Alcatraz #1515, Tecate", color = Color.White, modifier = Modifier.padding(16.dp), fontSize = 14.sp)
                    Text(
                        text = "Hola, $nombreUsuario 👋",
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
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ingresa tu ubicación", color = TextGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("¿Qué servicio buscas?", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
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
fun BottomNavBar() {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, selected = true, onClick = { })
        NavigationBarItem(icon = { Icon(Icons.Outlined.Person, contentDescription = "Perfil") }, selected = false, onClick = { })
    }
}