package com.example.fixnow.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioRepository
import com.example.fixnow.ui.theme.OrangeDark
import com.example.fixnow.ui.theme.OrangePrimary
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRegistroSocio(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SupabaseClient.client.auth.currentSessionOrNull()
    val uid = session?.user?.id ?: ""

    var pasoActual by remember { mutableIntStateOf(1) }
    
    // Datos del registro
    var categoriaSeleccionada by remember { mutableStateOf("") }
    var uriIdentificacion by remember { mutableStateOf<Uri?>(null) }
    var uriAntecedentes by remember { mutableStateOf<Uri?>(null) }
    var subiendo by remember { mutableStateOf(false) }

    val categorias = listOf("Carpinteria", "Cerrajeria", "Mecanica", "Plomeria", "Electricidad")

    // Launchers
    val pickId = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) uriIdentificacion = uri
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uriAntecedentes = uri
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Registro de Socio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (pasoActual > 1) pasoActual-- else navController.popBackStack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicador de pasos
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PasoIndicador(1, pasoActual >= 1)
                    PasoIndicador(2, pasoActual >= 2)
                    PasoIndicador(3, pasoActual >= 3)
                }
                
                Spacer(modifier = Modifier.height(30.dp))

                when (pasoActual) {
                    1 -> {
                        Text("¿Cuál es tu especialidad?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        categorias.forEach { cat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { categoriaSeleccionada = cat },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (categoriaSeleccionada == cat) OrangePrimary.copy(alpha = 0.1f) 
                                                     else MaterialTheme.colorScheme.surface
                                ),
                                border = if (categoriaSeleccionada == cat) 
                                         androidx.compose.foundation.BorderStroke(2.dp, OrangePrimary) else null
                            ) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = categoriaSeleccionada == cat,
                                        onClick = { categoriaSeleccionada = cat },
                                        colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                                    )
                                    Text(cat, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    2 -> {
                        Text("Identificación Oficial", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Escanea o sube una foto de tu ID (INE/Pasaporte)", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .border(2.dp, if (uriIdentificacion != null) OrangePrimary else Color.LightGray, RoundedCornerShape(16.dp))
                                .clickable { pickId.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uriIdentificacion != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, null, tint = OrangePrimary, modifier = Modifier.size(48.dp))
                                    Text("Identificación cargada", color = OrangePrimary)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Text("Tocar para subir", color = Color.Gray)
                                }
                            }
                        }
                    }
                    3 -> {
                        Text("Antecedentes No Penales", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Sube tu carta de antecedentes en formato PDF o imagen", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .border(2.dp, if (uriAntecedentes != null) OrangePrimary else Color.LightGray, RoundedCornerShape(16.dp))
                                .clickable { pickPdf.launch("*/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (uriAntecedentes != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Description, null, tint = OrangePrimary, modifier = Modifier.size(48.dp))
                                    Text("Documento cargado", color = OrangePrimary)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.UploadFile, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Text("Seleccionar archivo", color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (subiendo) {
                    CircularProgressIndicator(color = OrangePrimary)
                } else {
                    Button(
                        onClick = {
                            if (pasoActual < 3) {
                                pasoActual++
                            } else {
                                // Finalizar proceso
                                subiendo = true
                                scope.launch {
                                    try {
                                        var urlId: String? = null
                                        var urlAnt: String? = null

                                        // Subir archivos si existen
                                        uriIdentificacion?.let { uri ->
                                            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                                            if (bytes != null) {
                                                urlId = UsuarioRepository.subirDocumentoSocio(uid, "identificacion.jpg", bytes, "documentos_socios")
                                            }
                                        }

                                        uriAntecedentes?.let { uri ->
                                            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                                            if (bytes != null) {
                                                urlAnt = UsuarioRepository.subirDocumentoSocio(uid, "antecedentes.pdf", bytes, "documentos_socios")
                                            }
                                        }

                                        // Actualizar perfil
                                        UsuarioRepository.convertirseEnPrestador(uid, categoriaSeleccionada, urlId, urlAnt)
                                        
                                        Toast.makeText(context, "¡Registro completado con éxito!", Toast.LENGTH_LONG).show()
                                        navController.navigate("inicio") { popUpTo(0) }
                                    } catch (e: Exception) {
                                        Log.e("REGISTRO_SOCIO", "Error: ${e.message}")
                                        Toast.makeText(context, "Error al registrar: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        subiendo = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        enabled = when(pasoActual) {
                            1 -> categoriaSeleccionada.isNotEmpty()
                            2 -> uriIdentificacion != null
                            3 -> uriAntecedentes != null
                            else -> false
                        }
                    ) {
                        Text(if (pasoActual == 3) "Finalizar Registro" else "Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PasoIndicador(paso: Int, activo: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(if (activo) OrangePrimary else Color.LightGray, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(paso.toString(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}
