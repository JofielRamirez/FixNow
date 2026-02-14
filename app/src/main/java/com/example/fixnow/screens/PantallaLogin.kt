package com.example.fixnow.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.OrangePrimary

@Composable
fun PantallaLogin(navController: NavController) {

    var usuario by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var recordarUsuario by rememberSaveable { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    // 🔥 CONFIGURACIÓN GOOGLE
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("305295534074-k3boufslm1l9c5l80l9q8lhja3fblrpt.apps.googleusercontent.com")
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            navController.navigate("inicio") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            mensajeError = "Error con Firebase Google"
                        }
                    }

            } catch (e: ApiException) {
                mensajeError = "Error Google: ${e.localizedMessage}"
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
                    if (usuario.isBlank() || password.isBlank()) {
                        mensajeError = "Completa todos los campos"
                        return@Button
                    }

                    auth.signInWithEmailAndPassword(usuario.trim(), password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                navController.navigate("inicio") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                mensajeError =
                                    task.exception?.localizedMessage ?: "Error al iniciar sesión"
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

            Spacer(modifier = Modifier.height(16.dp))

            // 🔥 BOTÓN GOOGLE FUNCIONANDO
            Button(
                onClick = {
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Iniciar sesión con Google")
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
                    "Crea una cuenta",
                    fontSize = 14.sp,
                    color = Color(0xFF5E35B1),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {

                        if (usuario.isBlank() || password.isBlank()) {
                            mensajeError = "Completa todos los campos"
                            return@clickable
                        }

                        auth.createUserWithEmailAndPassword(usuario.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    navController.navigate("inicio") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                } else {
                                    mensajeError =
                                        task.exception?.localizedMessage ?: "Error al registrar"
                                }
                            }
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
