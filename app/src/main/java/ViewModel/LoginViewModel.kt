package com.example.fixnow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

class LoginViewModel : ViewModel() {
    private val _usuario = mutableStateOf("")
    val usuario: State<String> = _usuario

    fun onUsuarioChange(nuevoValor: String) {
        _usuario.value = nuevoValor
    }
}