package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Importamos tus colores
import com.example.fixnow.OrangePrimary

@Composable
fun PantallaLogin(navController: NavController) {
    // Variables para guardar lo que escribe el usuario
    var usuario by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // El degradado de Naranja a Blanco
                brush = Brush.verticalGradient(
                    colors = listOf(OrangePrimary, Color.White),
                    startY = 0f,
                    endY = 3000f // Ajusta esto para que el blanco empiece más abajo
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. LOGO (Icono + Texto)
            Icon(
                imageVector = Icons.Default.Build, // Usamos una llave como placeholder
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FixNow",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 2. CAMPO USUARIO
            Text(
                text = "Usuario",
                color = Color.White,
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 4.dp)
            )
            CampoTextoPersonalizado(
                value = usuario,
                onValueChange = { usuario = it },
                placeholder = ""
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. CAMPO CONTRASEÑA
            Text(
                text = "Contraseña",
                color = Color.White,
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 4.dp)
            )
            CampoTextoPersonalizado(
                value = password,
                onValueChange = { password = it },
                placeholder = "",
                esPassword = true
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 4. BOTÓN INGRESAR
            Button(
                onClick = {
                    // Al hacer click, navegamos al Inicio y borramos el Login del historial
                    navController.navigate("inicio") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCC8E00) // Un naranja un poco más oscuro para el botón
                ),
                shape = RoundedCornerShape(50), // Bordes muy redondos
                modifier = Modifier
                    .fillMaxWidth(0.6f) // Que ocupe el 60% del ancho
                    .height(50.dp)
                    .shadow(8.dp, RoundedCornerShape(50))
            ) {
                Text("Ingresar", fontSize = 18.sp, fontStyle = FontStyle.Italic)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. TEXTO CREAR CUENTA
            Row {
                Text("¿No tienes cuenta? ", fontSize = 14.sp, color = Color.Black)
                Text(
                    "Crea una cuenta",
                    fontSize = 14.sp,
                    color = Color(0xFF5E35B1), // Color moradito/azul
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { /* Aquí iría a registro */ }
                )
            }
        }
    }
}

// COMPONENTE PERSONALIZADO PARA LOS CAMPOS BLANCOS "GORDITOS"
@Composable
fun CampoTextoPersonalizado(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    esPassword: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .shadow(8.dp, RoundedCornerShape(50)), // Sombra suave
        color = Color.White,
        shape = RoundedCornerShape(50) // Forma de cápsula
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (esPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = if (esPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent, // Quitamos la línea de abajo
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}