package com.example.fixnow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fixnow.screens.PantallaInicio
import com.example.fixnow.screens.PantallaListaServicios
import com.example.fixnow.screens.PantallaLogin

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
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { PantallaLogin(navController) }
        composable("inicio") { PantallaInicio(navController) }
        composable("servicios/{categoria}") { backStackEntry ->
            val categoria = backStackEntry.arguments?.getString("categoria") ?: "Servicio"
            PantallaListaServicios(navController, categoria)
        }
    }
}