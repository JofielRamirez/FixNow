package com.example.fixnow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.screens.PantallaInicio
import com.example.fixnow.screens.PantallaListaServicios
import com.example.fixnow.screens.PantallaLogin
import com.example.fixnow.screens.PantallaRegistro
import com.example.fixnow.screens.PantallaServicios
import com.example.fixnow.screens.PantallaPerfil
import com.example.fixnow.screens.PantallaDetalleSocio
import com.example.fixnow.screens.PantallaChat
import com.example.fixnow.screens.PantallaListaChats
import com.example.fixnow.screens.PantallaTesting
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CICLO_VIDA", "onCreate → App iniciada por primera vez o recreada")
        SupabaseClient.client.handleDeeplinks(intent)
        setContent {
            AppNavigation()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("CICLO_VIDA", "onStart → App visible para el usuario")
    }

    override fun onResume() {
        super.onResume()
        Log.d("CICLO_VIDA", "onResume → App en primer plano, lista para interactuar")
    }

    override fun onPause() {
        super.onPause()
        Log.d("CICLO_VIDA", "onPause → App perdió el foco")
    }

    override fun onStop() {
        super.onStop()
        Log.d("CICLO_VIDA", "onStop → App ya no es visible para el usuario")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("CICLO_VIDA", "onRestart → App vuelve de estar detenida")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CICLO_VIDA", "onDestroy → App destruida completamente")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        SupabaseClient.client.handleDeeplinks(intent)
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val sessionStatus by SupabaseClient.client.auth.sessionStatus.collectAsState()

    LaunchedEffect(sessionStatus) {
        when (sessionStatus) {
            is SessionStatus.Authenticated -> {
                if (navController.currentDestination?.route == "login" ||
                    navController.currentDestination?.route == "registro") {
                    navController.navigate("inicio") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is SessionStatus.NotAuthenticated -> {
                if (navController.currentDestination?.route != "login") {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { PantallaLogin(navController) }
        composable("registro") { PantallaRegistro(navController) }
        composable("inicio") { PantallaInicio(navController) }
        composable("servicios") { PantallaServicios(navController) }
        composable("perfil") { PantallaPerfil(navController) }
        composable("mensajes") { PantallaListaChats(navController) }
        composable("detalle_socio/{socioId}") { backStackEntry ->
            val socioId = backStackEntry.arguments?.getString("socioId") ?: ""
            PantallaDetalleSocio(navController, socioId)
        }
        composable("chat/{socioId}/{nombre}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("socioId") ?: ""
            val nombre = backStackEntry.arguments?.getString("nombre") ?: "Socio"
            PantallaChat(navController, id, nombre)
        }
        composable("servicios/{categoria}") { backStackEntry ->
            val categoria = backStackEntry.arguments?.getString("categoria") ?: "Servicio"
            PantallaListaServicios(navController, categoria)
        }
        composable("testing") { PantallaTesting(navController) }
    }
}_