package com.example.fixnow.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.OrangeLight
import com.example.fixnow.OrangePrimary
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue

@Composable
fun PantallaPerfil(navController: NavController) {
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val user = session?.user
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- ESTADOS ---
    var mostrarDialogo by remember { mutableStateOf(false) }
    var tipoSeleccionado by remember { mutableStateOf("") }
    var perfil by remember { mutableStateOf<UsuarioPerfil?>(null) }
    val categorias = listOf("Carpintería", "Cerrajería", "Mecánica", "Plomería", "Electricidad")

    val nombreUsuario = user?.userMetadata?.get("nombre")?.toString()?.trim('"')
        ?: user?.email?.substringBefore("@")
        ?: "Usuario"
    val emailUsuario = user?.email ?: ""
    val inicial = nombreUsuario.firstOrNull()?.uppercaseChar() ?: 'U'

    // --- LAUNCHER PARA GALERÍA ---
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    val uidLimpio = user?.id?.replace("\"", "")?.trim() ?: ""

                    if (bytes != null && uidLimpio.isNotEmpty()) {
                        UsuarioRepository.subirFotoTrabajo(uidLimpio, bytes)
                        Toast.makeText(context, "¡Foto publicada!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("SUBIDA_FOTO", "Error: ${e.message}")
                }
            }
        }
    }



    // --- RECARGA DINÁMICA (Única fuente de verdad) ---
    // Dentro de PantallaPerfil, usa esto para recargar siempre que regreses a la pestaña
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(navBackStackEntry) {
        user?.id?.let { uid ->
            try {
                val uidLimpio = uid.replace("\"", "").trim()
                val datos = SupabaseClient.client.postgrest["Usuarios"]
                    .select { filter { eq("id", uidLimpio) } }
                    .decodeSingleOrNull<UsuarioPerfil>()

                if (datos != null) {
                    perfil = datos // Aquí se actualiza la UI con los datos REALES de la DB
                }
            } catch (e: Exception) {
                Log.e("PERFIL", "Error al recargar: ${e.message}")
            }
        }
    }

    // --- DIÁLOGO DE REGISTRO ---
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text("¿Qué servicio ofreces?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    categorias.forEach { cat ->
                        Row(
                            Modifier.fillMaxWidth().clickable { tipoSeleccionado = cat }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (tipoSeleccionado == cat), onClick = { tipoSeleccionado = cat })
                            Text(text = cat, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val idUsuario = user?.id ?: ""
                                if (idUsuario.isNotEmpty() && tipoSeleccionado.isNotEmpty()) {
                                    UsuarioRepository.convertirseEnPrestador(idUsuario, tipoSeleccionado)

                                    // Actualización local para feedback inmediato
                                    perfil = perfil?.copy(es_prestador = true, tipo_servicio = tipoSeleccionado)

                                    val nombreExito = perfil?.nombre ?: "Usuario"
                                    mostrarDialogo = false
                                    Toast.makeText(context, "¡Felicidades $nombreExito, ahora eres socio!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("UI_ERROR", "Error: ${e.message}")
                                Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = tipoSeleccionado.isNotEmpty()
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header con degradado
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
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = inicial.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(nombreUsuario, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    if (perfil?.es_prestador == true) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Socio: ${perfil?.tipo_servicio}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(emailUsuario, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // Sección Cuenta
                Text("Cuenta", fontSize = 13.sp, color = Color(0xFF9E9E9E), fontWeight = FontWeight.Medium)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        OpcionPerfil(icon = Icons.Default.Settings, titulo = "Ajustes", subtitulo = "Notificaciones, idioma") {}
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))

                        if (perfil?.es_prestador == true) {
                            OpcionPerfil(
                                icon = Icons.Default.Build,
                                titulo = "Configurar mi Servicio",
                                subtitulo = "Gestionar mi perfil de ${perfil?.tipo_servicio}",
                                iconColor = OrangePrimary
                            ) {}
                        } else {
                            OpcionPerfil(
                                icon = Icons.Default.Star,
                                titulo = "Conviértete en Socio",
                                subtitulo = "Ofrece tus servicios en FixNow",
                                iconColor = OrangePrimary
                            ) { mostrarDialogo = true }
                        }
                    }
                }

                // Sección Herramientas Socio
                if (perfil?.es_prestador == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Herramientas de Socio", fontSize = 13.sp, color = Color(0xFF9E9E9E), fontWeight = FontWeight.Medium)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.padding(vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        OpcionPerfil(
                            icon = Icons.Default.AddCircle,
                            titulo = "Subir fotos de mi trabajo",
                            subtitulo = "Publica tus trabajos realizados",
                            iconColor = OrangePrimary
                        ) {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { scope.launch { SupabaseClient.client.auth.signOut() } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEEE0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFE53935))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión", color = Color(0xFFE53935), fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(20.dp))
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
            Text(subtitulo, fontSize = 12.sp, color = Color(0xFF9E9E9E))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(20.dp))
    }
}