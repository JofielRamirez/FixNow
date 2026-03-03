package com.example.fixnow.data

import android.content.Context
import android.content.SharedPreferences

object AppEstadoPrefs {
    private const val PREFS_NAME = "fixnow_estado"
    private const val KEY_ULTIMA_CATEGORIA = "ultima_categoria"
    private const val KEY_ULTIMO_ACCESO = "ultimo_acceso"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun guardarUltimaCategoria(context: Context, categoria: String) {
        prefs(context).edit().putString(KEY_ULTIMA_CATEGORIA, categoria).apply()
    }

    fun obtenerUltimaCategoria(context: Context): String =
        prefs(context).getString(KEY_ULTIMA_CATEGORIA, "") ?: ""

    fun guardarUltimoAcceso(context: Context) {
        prefs(context).edit().putLong(KEY_ULTIMO_ACCESO, System.currentTimeMillis()).apply()
    }

    fun obtenerUltimoAcceso(context: Context): Long =
        prefs(context).getLong(KEY_ULTIMO_ACCESO, 0L)
}
