package com.example.fixnow.data

import com.google.firebase.firestore.FirebaseFirestore

object UsuarioRepository {
    private val db = FirebaseFirestore.getInstance()

    // Esta función recibe los datos y funciones de éxito/error
    fun guardarUsuario(
        uid: String,
        email: String,
        nombre: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val usuarioData = hashMapOf(
            "uid" to uid,
            "email" to email,
            "nombre" to nombre,
            "fechaRegistro" to System.currentTimeMillis()
        )

        // Guarda en la colección "usuarios" usando el UID
        db.collection("usuarios").document(uid)
            .set(usuarioData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }
}