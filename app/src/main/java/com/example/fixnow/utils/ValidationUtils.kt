package com.example.fixnow.utils

object ValidationUtils {

    fun validarNombre(nombre: String): String? {
        if (nombre.isBlank()) return "Ingresa tu nombre"
        if (nombre.trim().length < 2) return "El nombre es muy corto"
        return null
    }

    fun validarEmail(email: String): String? {
        if (email.isBlank()) return "Ingresa tu correo"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())
            return "El correo no es válido"
        return null
    }

    fun validarPassword(password: String): String? {
        if (password.isBlank()) return "Ingresa una contraseña"
        if (password.length < 8) return "Mínimo 8 caracteres"
        if (!password.any { it.isDigit() }) return "Debe contener al menos un número"
        if (!password.any { it.isLetter() }) return "Debe contener al menos una letra"
        return null
    }
}
