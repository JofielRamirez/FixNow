package com.example.fixnow.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.ui.theme.OrangePrimary
import com.example.fixnow.ui.theme.OrangeDark
import com.example.fixnow.ui.theme.OrangeLight
import com.example.fixnow.ui.theme.ErrorRed
import com.example.fixnow.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

@Composable
fun PantallaLogin(navController: NavController) {
    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize()) {

        // Fondo superior naranja
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(brush = Brush.verticalGradient(colors = listOf(OrangeDark, OrangePrimary)))
        )

        // Fondo inferior gris claro
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(Color(0xFFF5F5F5))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zona logo
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -40 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("FN", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "FixNow",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Servicios a un toque",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Card flotante con formulario
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 80 })
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Iniciar sesión", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                        Text(
                            "Bienvenido de nuevo",
                            fontSize = 13.sp,
                            color = Color(0xFF9E9E9E),
                            modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
                        )

                        CampoTextoIcono(
                            value = usuario,
                            onValueChange = { usuario = it },
                            placeholder = "Correo electrónico",
                            leadingIcon = {
                                Icon(Icons.Default.Email, null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CampoTextoIcono(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Contraseña",
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

                        Spacer(modifier = Modifier.height(24.dp))

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
                                        } catch (e: Exception) {
                                            Log.e("LOGIN_ERROR", "Error: ${e.message}")
                                            val errorMsg = e.message ?: ""
                                            mensajeError = when {
                                                errorMsg.contains("Email not confirmed", ignoreCase = true) ->
                                                    "Confirma tu correo electrónico"
                                                errorMsg.contains("Invalid login credentials", ignoreCase = true) ->
                                                    "Correo o contraseña incorrectos"
                                                else -> "Error al iniciar sesión"
                                            }
                                        } finally {
                                            cargando = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                disabledContainerColor = OrangeLight
                            ),
                            enabled = !cargando
                        ) {
                            if (cargando) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            } else {
                                Text("Ingresar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                            Text("¿No tienes cuenta? ", fontSize = 14.sp, color = Color.Black)
                            Text(
                                "Regístrate",
                                fontSize = 15.sp,
                                color = OrangePrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { navController.navigate("registro") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CampoTextoIcono(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    esPassword: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(placeholder, fontSize = 14.sp, color = Color(0xFFBDBDBD)) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = if (esPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = if (esPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangePrimary,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFFAFAFA),
            cursorColor = OrangePrimary,
            focusedTextColor = Color(0xFF1A1A1A),    // ← NUEVO
            unfocusedTextColor = Color(0xFF1A1A1A)   // ← NUEVO
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
