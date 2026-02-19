package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.OrangePrimary
import com.example.fixnow.OrangeLight
import com.example.fixnow.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun PantallaPerfil(navController: NavController) {
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val user = session?.user
    val nombreUsuario = user?.userMetadata?.get("nombre")?.toString()?.trim('"')
        ?: user?.email?.substringBefore("@")
        ?: "Usuario"
    val emailUsuario = user?.email ?: ""
    val inicial = nombreUsuario.firstOrNull()?.uppercaseChar() ?: 'U'
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(brush = Brush.verticalGradient(colors = listOf(OrangePrimary, OrangeLight)))
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = inicial.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangePrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(nombreUsuario, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(emailUsuario, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                Text(
                    "Cuenta",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    OpcionPerfil(
                        icon = Icons.Default.Settings,
                        titulo = "Ajustes",
                        subtitulo = "Notificaciones, idioma",
                        onClick = { }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color(0xFFF0F0F0)
                    )
                    OpcionPerfil(
                        icon = Icons.Default.Star,
                        titulo = "Conviértete en Socio",
                        subtitulo = "Ofrece tus servicios en FixNow",
                        iconColor = OrangePrimary,
                        habilitado = false,
                        onClick = { }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Información",
                    fontSize = 13.sp,
                    color = Color(0xFF9E9E9E),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    OpcionPerfil(
                        icon = Icons.Default.Info,
                        titulo = "Legal",
                        subtitulo = "Términos y privacidad",
                        onClick = { }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                SupabaseClient.client.auth.signOut()
                            } catch (e: Exception) {
                                // AppNavigation detecta el cambio de sesión y navega al login
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEE0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFE53935))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión", color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun OpcionPerfil(
    icon: ImageVector,
    titulo: String,
    subtitulo: String,
    iconColor: Color = Color(0xFF757575),
    habilitado: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitado) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (habilitado) Color(0xFFF5F5F5) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                if (!habilitado) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFEEE0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Próximamente", fontSize = 9.sp, color = OrangePrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Text(subtitulo, fontSize = 12.sp, color = Color(0xFF9E9E9E))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(20.dp)
        )
    }
}