package com.example.fixnow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.screens.PantallaInicio
import com.example.fixnow.screens.PantallaListaServicios
import com.example.fixnow.screens.PantallaLogin
import com.example.fixnow.screens.PantallaRegistro
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val sessionStatus = SupabaseClient.client.auth.sessionStatus.collectAsState()

    val startDestination = when (sessionStatus.value) {
        is SessionStatus.Authenticated -> "inicio"
        is SessionStatus.NotAuthenticated -> "login"
        else -> null
    }

    if (startDestination == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("login") { PantallaLogin(navController) }
            composable("registro") { PantallaRegistro(navController) }
            composable("inicio") { PantallaInicio(navController) }
            composable("servicios/{categoria}") { backStackEntry ->
                val categoria =
                    backStackEntry.arguments?.getString("categoria") ?: "Servicio"
                PantallaListaServicios(navController, categoria)
            }
        }
    }
}

