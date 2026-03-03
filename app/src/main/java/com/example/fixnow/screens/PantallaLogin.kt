package com.example.fixnow.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.fixnow.ui.theme.OrangePrimary
import com.example.fixnow.ui.theme.OrangeDark
import com.example.fixnow.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun PantallaLogin(navController: NavController) {

    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Build,
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

            Text(
                text = "Correo electrónico",
                color = Color.White,
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, bottom = 4.dp)
            )

            CampoTextoPersonalizado(
                value = usuario,
                onValueChange = { usuario = it },
                placeholder = "ejemplo@email.com"
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                placeholder = "********",
                esPassword = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (cargando) {
                CircularProgressIndicator(color = OrangePrimary)
            } else {
                Button(
                    onClick = {
                        val emailLimpio = usuario.trim().lowercase()
                        val passLimpia = password.trim()

                        if (emailLimpio.isBlank() || passLimpia.isBlank()) {
                            mensajeError = "Completa todos los campos"
                        } else {
                            mensajeError = ""
                            cargando = true
                            scope.launch {
                                try {
                                    SupabaseClient.client.auth.signInWith(Email) {
                                        this.email = emailLimpio
                                        this.password = passLimpia
                                    }
                                    Log.d("LOGIN", "Sesión iniciada con éxito")
                                    // La navegación se maneja en el Main/AppNavigation
                                } catch (e: Exception) {
                                    Log.e("LOGIN_ERROR", "Error: ${e.message}")
                                    val errorMsg = e.message ?: ""
                                    mensajeError = when {
                                        errorMsg.contains("Email not confirmed", ignoreCase = true) -> 
                                            "Debes confirmar tu correo electrónico"
                                        errorMsg.contains("Invalid login credentials", ignoreCase = true) -> 
                                            "Correo o contraseña incorrectos"
                                        else -> "Error: ${e.localizedMessage}"
                                    }
                                } finally {
                                    cargando = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeDark),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
                ) {
                    Text("Ingresar")
                }
            }

            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = mensajeError,
                    color = Color.Red,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row {
                Text("¿No tienes cuenta? ", fontSize = 14.sp, color = Color.Black)
                Text(
                    "Crear una cuenta",
                    fontSize = 14.sp,
                    color = Color(0xFF5E35B1),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { navController.navigate("registro") }
                )
            }
        }
    }
}

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
            .shadow(8.dp, RoundedCornerShape(50)),
        color = Color.White,
        shape = RoundedCornerShape(50)
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = Color.Gray) },
            visualTransformation = if (esPassword)
                PasswordVisualTransformation()
            else
                androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = if (esPassword)
                KeyboardOptions(keyboardType = KeyboardType.Password)
            else
                KeyboardOptions.Default,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxSize()
        )
    }
}
