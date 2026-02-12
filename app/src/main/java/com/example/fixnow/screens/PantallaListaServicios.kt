package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.OrangePrimary
import com.example.fixnow.OrangeLight
import com.example.fixnow.BackgroundWhite
import com.example.fixnow.TextGray

@Composable
fun PantallaListaServicios(navController: NavController, categoria: String) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWhite)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(brush = Brush.verticalGradient(colors = listOf(OrangePrimary, OrangeLight)))
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
                Text(text = categoria, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("¡Encuentra a la persona Indicada!", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(3) { index -> CardTrabajador(index) }
            }
        }
    }
}

@Composable
fun CardTrabajador(index: Int) {
    val nombres = listOf("Rodolfo", "Jofiel", "Maria")
    val nombre = nombres[index % nombres.size]
    Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(6.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF7E57C2), Color(0xFF512DA8))))) {
                Text(nombre.take(1), color = Color.White, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("25 Reseñas", fontSize = 10.sp, color = TextGray)
                }
                Text("⭐⭐⭐⭐☆", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Green))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Disponible", fontSize = 10.sp, color = Color.Green)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DetalleIcono(Icons.Default.LocationOn, "2.3 k")
                    DetalleIcono(Icons.Default.DateRange, "15 min")
                    DetalleIcono(Icons.Default.ShoppingCart, "2 Años Exp")
                }
            }
        }
    }
}

@Composable
fun DetalleIcono(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextGray)
        Spacer(modifier = Modifier.width(2.dp))
        Text(text, fontSize = 10.sp, color = TextGray)
    }
}