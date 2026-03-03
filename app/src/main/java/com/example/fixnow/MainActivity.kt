package com.example.fixnow

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.screens.*
import com.example.fixnow.ui.theme.FixNowTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus

// Estado global del tema — accesible desde cualquier screen
object TemaApp {
    var oscuro by mutableStateOf<Boolean?>(null) // null = seguir sistema
}

class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        Log.d("CICLO_VIDA", "onCreate")
        SupabaseClient.client.handleDeeplinks(intent)

        setContent {
            val sistemaOscuro = isSystemInDarkTheme()
            val usarOscuro = TemaApp.oscuro ?: sistemaOscuro

            FixNowTheme(darkTheme = usarOscuro) {
                AppNavigation()
            }
        }
    }

    override fun onStart()   { super.onStart();   Log.d("CICLO_VIDA", "onStart") }
    override fun onResume()  { super.onResume();  Log.d("CICLO_VIDA", "onResume") }
    override fun onPause()   { super.onPause();   Log.d("CICLO_VIDA", "onPause") }
    override fun onStop()    { super.onStop();    Log.d("CICLO_VIDA", "onStop") }
    override fun onRestart() { super.onRestart(); Log.d("CICLO_VIDA", "onRestart") }
    override fun onDestroy() { super.onDestroy(); Log.d("CICLO_VIDA", "onDestroy") }

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
                    navController.navigate("inicio") { popUpTo(0) { inclusive = true } }
                }
            }
            is SessionStatus.NotAuthenticated -> {
                if (navController.currentDestination?.route != "login") {
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login")    { PantallaLogin(navController) }
        composable("registro") { PantallaRegistro(navController) }
        composable("inicio")   { PantallaInicio(navController) }
        composable("servicios") { PantallaServicios(navController) }
        composable("perfil")   { PantallaPerfil(navController) }
        composable("mensajes") { PantallaListaChats(navController) }
        composable("detalle_socio/{socioId}") { backStackEntry ->
            PantallaDetalleSocio(navController, backStackEntry.arguments?.getString("socioId") ?: "")
        }
        composable("chat/{socioId}/{nombre}") { backStackEntry ->
            PantallaChat(
                navController,
                backStackEntry.arguments?.getString("socioId") ?: "",
                backStackEntry.arguments?.getString("nombre") ?: "Socio"
            )
        }
        composable("servicios/{categoria}") { backStackEntry ->
            PantallaListaServicios(navController, backStackEntry.arguments?.getString("categoria") ?: "Servicio")
        }
        composable("testing") { PantallaTesting(navController) }
    }
}
