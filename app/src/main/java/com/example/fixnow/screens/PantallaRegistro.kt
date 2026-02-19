package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.OrangePrimary
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
    var mensajeError by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(OrangePrimary, Color.White),
                    startY = 0f,
                    endY = 3000f
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Build, contentDescription = "Logo", tint = Color.White, modifier = Modifier.size(60.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Registro FixNow", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, fontStyle = FontStyle.Italic)
            Spacer(modifier = Modifier.height(32.dp))

            // Campo Nombre
            Text("Nombre completo", color = Color.White, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 4.dp))
            CampoTextoPersonalizado(value = nombre, onValueChange = { nombre = it }, placeholder = "Tu nombre")
            Spacer(modifier = Modifier.height(16.dp))

            // Campo Email
            Text("Correo electrónico", color = Color.White, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 4.dp))
            CampoTextoPersonalizado(value = email, onValueChange = { email = it }, placeholder = "ejemplo@email.com")
            Spacer(modifier = Modifier.height(16.dp))

            // Campo Contraseña
            Text("Contraseña", color = Color.White, fontSize = 16.sp, modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 4.dp))
            CampoTextoPersonalizado(value = password, onValueChange = { password = it }, placeholder = "Mínimo 8 caracteres", esPassword = true)

            // Hint de requisitos
            Text(
                "Mínimo 8 caracteres, una letra y un número",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 12.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val emailLimpio = email.trim()
                    val passwordLimpio = password
                    val nombreRegistro = nombre.trim()

                    when {
                        nombreRegistro.isBlank() || emailLimpio.isBlank() || passwordLimpio.isBlank() -> {
                            mensajeError = "Completa todos los campos"
                        }
                        passwordLimpio.length < 8 -> {
                            mensajeError = "La contraseña debe tener al menos 8 caracteres"
                        }
                        !passwordLimpio.any { it.isDigit() } -> {
                            mensajeError = "La contraseña debe contener al menos un número"
                        }
                        !passwordLimpio.any { it.isLetter() } -> {
                            mensajeError = "La contraseña debe contener al menos una letra"
                        }
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
                                            UsuarioRepository.guardarUsuario(
                                                uid = uid,
                                                email = emailLimpio,
                                                nombre = nombreRegistro
                                            )
                                        } catch (dbError: Exception) {
                                            // Auth exitoso pero fallo DB, igual navega
                                        }
                                    } else {
                                        mensajeExito = "¡Revisa tu correo para confirmar tu cuenta!"
                                        cargando = false
                                    }

                                } catch (e: Exception) {
                                    val msg = e.message ?: e.localizedMessage ?: ""
                                    mensajeError = when {
                                        msg.contains("already registered", ignoreCase = true) ||
                                                msg.contains("User already registered", ignoreCase = true) ->
                                            "Este correo ya está registrado"
                                        msg.contains("rate limit", ignoreCase = true) ||
                                                msg.contains("seconds", ignoreCase = true) ->
                                            "Espera un momento antes de intentarlo de nuevo"
                                        msg.contains("invalid email", ignoreCase = true) ->
                                            "El correo no es válido"
                                        else -> "Error: $msg"
                                    }
                                    cargando = false
                                }
                            }
                        }
                    }
                },
                enabled = !cargando,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC8E00)),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Crear Cuenta")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (mensajeError.isNotEmpty()) {
                Text(text = mensajeError, color = Color.Red, fontSize = 14.sp)
            }
            if (mensajeExito.isNotEmpty()) {
                Text(text = mensajeExito, color = Color(0xFF2E7D32), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Text("¿Ya tienes cuenta? ", fontSize = 14.sp, color = Color.Black)
                Text(
                    "Inicia sesión",
                    fontSize = 14.sp,
                    color = Color(0xFF5E35B1),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.popBackStack() }
                )
            }
        }
    }
}