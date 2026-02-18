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
import com.example.fixnow.OrangePrimary
import com.example.fixnow.data.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email




import kotlinx.coroutines.launch

@Composable
fun PantallaLogin(navController: NavController) {

    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var recordarUsuario by rememberSaveable { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // NUEVO: Observar cambios en la sesión
    LaunchedEffect(Unit) {
        SupabaseClient.client.auth.sessionStatus.collect { status ->
            if (status is io.github.jan.supabase.auth.status.SessionStatus.Authenticated) {
                // Si el usuario se loguea (ya sea por Google o Email), lo mandamos al inicio
                navController.navigate("inicio") {
                    popUpTo("login") {
                        inclusive = true
                    } // Evita que regrese al login con el botón de atrás
                }
            }
        }
    }

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
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 8.dp, bottom = 4.dp)
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
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 8.dp, bottom = 4.dp)
            )

            CampoTextoPersonalizado(
                value = password,
                onValueChange = { password = it },
                placeholder = "",
                esPassword = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            ) {
                Checkbox(
                    checked = recordarUsuario,
                    onCheckedChange = { recordarUsuario = it }
                )
                Text(
                    text = "Recordar mi sesión",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val emailLimpio = usuario.trim()
                    val passLimpia = password

                    if (emailLimpio.isBlank() || passLimpia.isBlank()) {
                        mensajeError = "Completa todos los campos"
                        return@Button
                    }

                    // Inicio de sesión con Supabase
                    scope.launch {
                        try {
                            SupabaseClient.client.auth.signInWith(Email) {
                                email = emailLimpio
                                password = passLimpia
                            }
                            // Si no lanzó excepción, el login fue exitoso:
                            navController.navigate("inicio") {
                                popUpTo("login") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            mensajeError = "Correo o contraseña incorrectos"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCC8E00)
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp)
            ) {
                Text("Ingresar")
            }
            Spacer(modifier = Modifier.height(20.dp))


            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            SupabaseClient.client.auth.signInWith(
                                provider = Google,
                                redirectUrl = "fixnow://login"
                            )


                        } catch (e: Exception) {
                            mensajeError = "Error al conectar con Google"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.6f).height(50.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Continuar con Google")
            }
            if (mensajeError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mensajeError,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row {
                Text("¿No tienes cuenta? ", fontSize = 14.sp, color = Color.Black)

                Text(
                    "Crear una cuenta",
                    fontSize = 14.sp,
                    color = Color(0xFF5E35B1),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        navController.navigate("registro")
                    }
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