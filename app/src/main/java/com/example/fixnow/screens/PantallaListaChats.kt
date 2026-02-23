package com.example.fixnow.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fixnow.OrangePrimary
import com.example.fixnow.data.ChatRepository
import com.example.fixnow.data.SupabaseClient
import com.example.fixnow.data.UsuarioPerfil
import com.example.fixnow.data.UsuarioRepository
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaChats(navController: NavController) {
    val miId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: ""
    var listaSociosConChat by remember { mutableStateOf<List<UsuarioPerfil>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var textoBusqueda by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        cargando = true
        val ids = ChatRepository.obtenerConversaciones(miId)
        val perfiles = mutableListOf<UsuarioPerfil>()
        for (id in ids) {
            UsuarioRepository.obtenerSocioPorId(id)?.let { perfiles.add(it) }
        }
        listaSociosConChat = perfiles
        cargando = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color.White)) {
            // Buscador tipo Messenger
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { textoBusqueda = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar contacto...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OrangePrimary)
                }
            } else if (listaSociosConChat.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(80.dp), tint = Color(0xFFEEEEEE))
                    Spacer(Modifier.height(16.dp))
                    Text("No hay mensajes todavía", color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            } else {
                LazyColumn {
                    items(listaSociosConChat.filter { it.nombre?.contains(textoBusqueda, true) == true }) { socio ->
                        ItemChatMessenger(socio) {
                            navController.navigate("chat/${socio.id}/${socio.nombre}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemChatMessenger(socio: UsuarioPerfil, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(60.dp).background(Color(0xFFFFEEE0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                socio.nombre?.take(1)?.uppercase() ?: "S",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary
            )
            // Círculo de estado "En línea"
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(socio.nombre ?: "Socio", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF222222))
            Text("Toca para chatear con el socio", color = Color.Gray, fontSize = 14.sp, maxLines = 1)
        }
        Text("12:45", color = Color.Gray, fontSize = 12.sp)
    }
}
