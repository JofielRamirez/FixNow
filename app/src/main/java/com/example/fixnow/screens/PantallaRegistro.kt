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

    // Scope para manejar las funciones suspendidas de Supabase
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
            CampoTextoPersonalizado(value = password, onValueChange = { password = it }, placeholder = "******", esPassword = true)
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // Extraemos los valores a variables locales para evitar conflictos con 'this' en el bloque de Supabase
                    val emailLimpio = email.trim()
                    val passwordLimpio = password
                    val nombreRegistro = nombre

                    if (nombreRegistro.isBlank() || emailLimpio.isBlank() || passwordLimpio.isBlank()) {
                        mensajeError = "Completa todos los campos"
                        return@Button
                    }

                    scope.launch {
                        try {
                            // 1. Registro en Supabase Auth
                            SupabaseClient.client.auth.signUpWith(Email) {
                                this.email = emailLimpio
                                this.password = passwordLimpio
                            }

                            // 2. Obtener el UID generado
                            val uid = SupabaseClient.client.auth.currentUserOrNull()?.id

                            if (uid != null) {
                                // 3. Guardar en la tabla 'usuarios' de tu base de datos
                                UsuarioRepository.guardarUsuario(
                                    uid = uid,
                                    email = emailLimpio,
                                    nombre = nombreRegistro
                                )

                                navController.navigate("inicio") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            mensajeError = e.localizedMessage ?: "Error al registrar"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC8E00)),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
            ) {
                Text("Crear Cuenta")
            }

            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = mensajeError, color = Color.Red, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Text("¿Ya tienes cuenta? ", fontSize = 14.sp, color = Color.Black)
                Text("Inicia sesión", fontSize = 14.sp, color = Color(0xFF5E35B1), fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                    navController.popBackStack()
                })
            }
        }
    }
}