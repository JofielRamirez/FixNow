package com.example.fixnow.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.ui.theme.OrangePrimary
import com.example.fixnow.ui.theme.OrangeDark
import com.example.fixnow.ui.theme.OrangeLight
import com.example.fixnow.ui.theme.ColorSuccess
import com.example.fixnow.ui.theme.ErrorRed
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun PantallaRegistro(navController: NavController) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(brush = Brush.verticalGradient(colors = listOf(OrangeDark, OrangePrimary)))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .align(Alignment.BottomCenter)
                .background(Color(0xFFF5F5F5))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 52.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FixNow",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text("Crea tu cuenta gratis", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 80 })
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Registro", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                        Text(
                            "Completa tus datos",
                            fontSize = 13.sp,
                            color = Color(0xFF9E9E9E),
                            modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
                        )

                        CampoTextoIcono(
                            value = nombre,
                            onValueChange = { nombre = it },
                            placeholder = "Nombre completo",
                            leadingIcon = {
                                Icon(Icons.Default.Person, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CampoTextoIcono(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Correo electrónico",
                            leadingIcon = {
                                Icon(Icons.Default.Email, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CampoTextoIcono(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Contraseña (mín. 8 caracteres)",
                            esPassword = !passwordVisible,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFF9E9E9E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )

                        // Indicador de fuerza de contraseña
                        if (password.isNotEmpty()) {
                            val fuerza = when {
                                password.length >= 8 && password.any { it.isDigit() } && password.any { it.isLetter() } -> 3
                                password.length >= 6 -> 2
                                else -> 1
                            }
                            val (color, label) = when (fuerza) {
                                3 -> Pair(ColorSuccess, "Contraseña segura")
                                2 -> Pair(OrangePrimary, "Contraseña regular")
                                else -> Pair(ErrorRed, "Contraseña débil")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                repeat(3) { i ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (i < fuerza) color else Color(0xFFE0E0E0))
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
                            }
                        }

                        AnimatedVisibility(visible = mensajeError.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ErrorRed.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(mensajeError, color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        AnimatedVisibility(visible = mensajeExito.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ColorSuccess.copy(alpha = 0.1f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(mensajeExito, color = ColorSuccess, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val emailLimpio = email.trim().lowercase()
                                val passwordLimpio = password
                                val nombreRegistro = nombre.trim()
                                when {
                                    nombreRegistro.isBlank() || emailLimpio.isBlank() || passwordLimpio.isBlank() ->
                                        mensajeError = "Completa todos los campos"
                                    passwordLimpio.length < 8 ->
                                        mensajeError = "La contraseña debe tener al menos 8 caracteres"
                                    !passwordLimpio.any { it.isDigit() } ->
                                        mensajeError = "La contraseña debe contener al menos un número"
                                    !passwordLimpio.any { it.isLetter() } ->
                                        mensajeError = "La contraseña debe contener al menos una letra"
                                    else -> {
                                        mensajeError = ""
                                        mensajeExito = ""
                                        cargando = true
                                        scope.launch {
                                            try {
                                                SupabaseClient.client.auth.signUpWith(Email) {
                                                    this.email = emailLimpio
                                                    this.password = passwordLimpio
                                                }
                                                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
                                                if (uid != null) {
                                                    try {
                                                        UsuarioRepository.guardarUsuario(uid = uid, email = emailLimpio, nombre = nombreRegistro)
                                                    } catch (_: Exception) {}
                                                } else {
                                                    mensajeExito = "¡Revisa tu correo para confirmar tu cuenta!"
                                                    cargando = false
                                                }
                                            } catch (e: Exception) {
                                                val msg = e.message ?: e.localizedMessage ?: ""
                                                mensajeError = when {
                                                    msg.contains("already registered", ignoreCase = true) -> "Este correo ya está registrado"
                                                    msg.contains("rate limit", ignoreCase = true) || msg.contains("seconds", ignoreCase = true) -> "Espera un momento antes de intentarlo"
                                                    msg.contains("invalid email", ignoreCase = true) -> "El correo no es válido"
                                                    else -> "Error: $msg"
                                                }
                                                cargando = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !cargando,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                disabledContainerColor = OrangeLight
                            )
                        ) {
                            if (cargando) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            } else {
                                Text("Crear Cuenta", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Text("¿Ya tienes cuenta? ", fontSize = 13.sp, color = Color(0xFF9E9E9E))
                            Text(
                                "Inicia sesión",
                                fontSize = 13.sp,
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
