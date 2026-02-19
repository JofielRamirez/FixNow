package com.example.fixnow.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Agregado para la lista de socios
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items // Agregado para el LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.OrangePrimary
import com.example.fixnow.OrangeLight
import com.example.fixnow.BackgroundWhite
import com.example.fixnow.TextGray
import com.example.fixnow.data.UsuarioPerfil // Importación directa del modelo
import com.example.fixnow.data.UsuarioRepository // Importación del repositorio
import kotlinx.coroutines.launch

data class CategoriaExtra(val nombre: String, val icon: ImageVector, val descripcion: String)

@Composable
fun PantallaServicios(navController: NavController) {
    val scope = rememberCoroutineScope()
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }
    var listaSocios by remember { mutableStateOf<List<UsuarioPerfil>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }

    val categorias = listOf(
        CategoriaExtra("Plomería", Icons.Default.Build, "Tuberías y más"),
        CategoriaExtra("Cerrajería", Icons.Default.Lock, "Llaves y cerraduras"),
        CategoriaExtra("Electricidad", Icons.Default.Star, "Instalaciones"),
        CategoriaExtra("Mecánica", Icons.Default.Settings, "Autos y motores"),
        CategoriaExtra("Carpintería", Icons.Default.Home, "Muebles y madera"),
        CategoriaExtra("Limpieza", Icons.Default.Delete, "Hogar y oficina"),
        CategoriaExtra("Pintura", Icons.Default.Create, "Interior y exterior"),
        CategoriaExtra("Jardinería", Icons.Default.Face, "Jardines y plantas"),
        CategoriaExtra("Mudanzas", Icons.Default.ShoppingCart, "Carga y traslado")
    )

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(padding)
        ) {
            // Header Dinámico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(brush = Brush.verticalGradient(colors = listOf(OrangePrimary, OrangeLight)))
            ) {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (categoriaSeleccionada != null) {
                        IconButton(onClick = { categoriaSeleccionada = null }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    }
                    Column {
                        Text(categoriaSeleccionada ?: "Servicios", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(if (categoriaSeleccionada == null) "¿En qué te podemos ayudar?" else "Socios disponibles", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buscador (Código original restaurado)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buscar servicio...", color = TextGray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (categoriaSeleccionada == null) {
                Text(
                    "Servicios básicos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categorias) { cat ->
                        CardServicio(cat) {
                            categoriaSeleccionada = cat.nombre
                            Log.d("FILTRO_SOCIOS", "Buscando a: ${cat.nombre}") // Agrega esta línea
                            scope.launch {
                                cargando = true
                                listaSocios = UsuarioRepository.obtenerSociosPorCategoria(cat.nombre)
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
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (listaSocios.isEmpty()) {
                            item {
                                Text(
                                    "No se encontraron socios en esta categoría.",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color.Gray
                                )
                            }
                        } else {
                            items(listaSocios) { socio ->
                                CardSocioSimple(socio)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardServicio(cat: CategoriaExtra, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    cat.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = OrangePrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                cat.nombre,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
        }
    }
}

@Composable
fun CardSocioSimple(socio: com.example.fixnow.data.UsuarioPerfil) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(0xFFFFEEE0), CircleShape), contentAlignment = Alignment.Center) {
                Text(socio.nombre.take(1).uppercase(), fontWeight = FontWeight.Bold, color = OrangePrimary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(socio.nombre, fontWeight = FontWeight.Bold)
                Text(socio.tipo_servicio ?: "", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}