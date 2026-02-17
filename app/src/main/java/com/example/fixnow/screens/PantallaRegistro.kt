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
import com.example.fixnow.data.UsuarioRepository
import com.google.firebase.auth.FirebaseAuth

@Composable
fun PantallaRegistro(navController: NavController) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()

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
                    if (nombre.isBlank() || email.isBlank() || password.isBlank()) {
                        mensajeError = "Completa todos los campos"
                        return@Button
                    }
                    // 1. Creamos el usuario en Firebase Auth
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    // 2. 🚀 LLAMAMOS A NUESTRO ARCHIVO SEPARADO PARA GUARDAR EN BASE DE DATOS
                                    UsuarioRepository.guardarUsuario(
                                        uid = user.uid,
                                        email = user.email ?: "",
                                        nombre = nombre,
                                        onSuccess = {
                                            navController.navigate("inicio") { popUpTo("login") { inclusive = true } }
                                        },
                                        onFailure = { e ->
                                            mensajeError = "Error BD: ${e.message}"
                                        }
                                    )
                                }
                            } else {
                                mensajeError = task.exception?.localizedMessage ?: "Error al registrar"
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
                    navController.popBackStack() // Regresa a la pantalla de login
                })
            }
        }
    }
}