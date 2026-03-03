package com.example.fixnow.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import com.example.fixnow.TemaApp
import com.example.fixnow.ui.theme.*
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

@Composable
fun PantallaPerfil(navController: NavController) {
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val user = session?.user
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var mostrarDialogo by remember { mutableStateOf(false) }
    var tipoSeleccionado by remember { mutableStateOf("") }
    var perfil by remember { mutableStateOf<UsuarioPerfil?>(null) }
    val categorias = listOf("Carpinteria", "Cerrajeria", "Mecanica", "Plomeria", "Electricidad")

    val nombreUsuario = user?.userMetadata?.get("nombre")?.toString()?.trim('"')
        ?: user?.email?.substringBefore("@") ?: "Usuario"
    val emailUsuario = user?.email ?: ""
    val inicial = nombreUsuario.firstOrNull()?.uppercaseChar() ?: 'U'

    // Colores que cambian con el tema
    val fondo         = MaterialTheme.colorScheme.background
    val superficie    = MaterialTheme.colorScheme.surface
    val sobreSup      = MaterialTheme.colorScheme.onSurface
    val sobreSupVar   = MaterialTheme.colorScheme.onSurfaceVariant
    val supVar        = MaterialTheme.colorScheme.surfaceVariant
    val divider       = MaterialTheme.colorScheme.outlineVariant

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    val uid = user?.id?.replace("\"", "")?.trim() ?: ""
                    if (bytes != null && uid.isNotEmpty()) {
                        UsuarioRepository.subirFotoTrabajo(uid, bytes)
                        Toast.makeText(context, "¡Foto publicada!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) { Log.e("SUBIDA_FOTO", "Error: ${e.message}") }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        user?.id?.let { uid ->
            try {
                val uidLimpio = uid.replace("\"", "").trim()
                val datos = SupabaseClient.client.postgrest["Usuarios"]
                    .select { filter { eq("id", uidLimpio) } }
                    .decodeSingleOrNull<UsuarioPerfil>()
                if (datos != null) perfil = datos
            } catch (e: Exception) { Log.e("PERFIL", "Error: ${e.message}") }
        }
    }

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
                            RadioButton(
                                selected = tipoSeleccionado == cat,
                                onClick = { tipoSeleccionado = cat },
                                colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                            )
                            Text(cat, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val id = user?.id ?: ""
                                if (id.isNotEmpty() && tipoSeleccionado.isNotEmpty()) {
                                    UsuarioRepository.convertirseEnPrestador(id, tipoSeleccionado)
                                    perfil = perfil?.copy(es_prestador = true, tipo_servicio = tipoSeleccionado)
                                    mostrarDialogo = false
                                    Toast.makeText(context, "¡Felicidades, ahora eres socio!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = tipoSeleccionado.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { mostrarDialogo = false }) { Text("Cancelar", color = OrangePrimary) } }
        )
    }

    Scaffold(bottomBar = { BottomNavBar(navController) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fondo)                          // ← tema
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header naranja (siempre naranja, no cambia) ──────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(brush = Brush.verticalGradient(colors = listOf(OrangeDark, OrangePrimary)))
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(CircleShape).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFFFF3E0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(inicial.toString(), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = OrangePrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(nombreUsuario, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (perfil?.es_prestador == true) {
                        Surface(color = Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(20.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF69F0AE), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Socio · ${perfil?.tipo_servicio}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Text(emailUsuario, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                // ── CUENTA ───────────────────────────────────────
                Text("CUENTA", fontSize = 11.sp, color = sobreSupVar, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = superficie),   // ← tema
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column {
                        OpcionPerfil(Icons.Default.Settings, "Ajustes", "Notificaciones, idioma", sobreSup, sobreSupVar, supVar) {}
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = divider)
                        if (perfil?.es_prestador == true) {
                            OpcionPerfil(Icons.Default.Build, "Configurar mi Servicio", "Gestionar ${perfil?.tipo_servicio}", sobreSup, sobreSupVar, supVar, iconColor = OrangePrimary) {}
                        } else {
                            OpcionPerfil(Icons.Default.Star, "Conviértete en Socio", "Ofrece tus servicios en FixNow", sobreSup, sobreSupVar, supVar, iconColor = OrangePrimary) { mostrarDialogo = true }
                        }
                    }
                }

                // ── HERRAMIENTAS DE SOCIO ─────────────────────────
                if (perfil?.es_prestador == true) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("HERRAMIENTAS DE SOCIO", fontSize = 11.sp, color = sobreSupVar, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = superficie),   // ← tema
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        OpcionPerfil(Icons.Default.AddCircle, "Subir fotos de mi trabajo", "Publica tus trabajos realizados", sobreSup, sobreSupVar, supVar, iconColor = OrangePrimary) {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                }

                // ── APARIENCIA ────────────────────────────────────
                Spacer(modifier = Modifier.height(20.dp))
                Text("APARIENCIA", fontSize = 11.sp, color = sobreSupVar, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = superficie),       // ← tema
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).background(supVar, RoundedCornerShape(12.dp)), // ← tema
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema oscuro", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = sobreSup)
                            Text(
                                when (TemaApp.oscuro) {
                                    true  -> "Activado manualmente"
                                    false -> "Desactivado manualmente"
                                    null  -> "Según el sistema"
                                },
                                fontSize = 12.sp, color = sobreSupVar
                            )
                        }
                        Switch(
                            checked = TemaApp.oscuro ?: isSystemInDarkTheme(),
                            onCheckedChange = { TemaApp.oscuro = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangePrimary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = supVar
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── CERRAR SESIÓN ─────────────────────────────────
                OutlinedButton(
                    onClick = { scope.launch { SupabaseClient.client.auth.signOut() } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ColorError.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.ExitToApp, null, tint = ColorError, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión", color = ColorError, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun OpcionPerfil(
    icon: ImageVector,
    titulo: String,
    subtitulo: String,
    colorTitulo: Color,
    colorSubtitulo: Color,
    colorFondoIcono: Color,
    iconColor: Color = Color(0xFF9E9E9E),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(colorFondoIcono, RoundedCornerShape(12.dp)), // ← tema
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colorTitulo)       // ← tema
            Text(subtitulo, fontSize = 12.sp, color = colorSubtitulo)                                 // ← tema
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colorSubtitulo, modifier = Modifier.size(20.dp))
    }
}
