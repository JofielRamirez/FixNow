package com.example.fixnow.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://nodxqqtyrxvgeyvezpps.supabase.co",
        supabaseKey = "sb_publishable_Gd5B7AKCg4OcVGepIDrhUg_K1BaV3xP"
    ) {
        install(Auth)
        install(Postgrest)
    }
}