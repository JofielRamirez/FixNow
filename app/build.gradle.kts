import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.fixnow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.fixnow"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        addManifestPlaceholders(
            mapOf(
                "supabaseDeepLinkScheme" to "fixnow",
                "MAPS_API_KEY" to (Properties().apply {
                    load(rootProject.file("local.properties").inputStream())
                }["MAPS_API_KEY"] ?: "")
            )
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // ---------- BASE ANDROID ----------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.core:core-splashscreen:1.2.0")

    // ---------- COMPOSE ----------
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // ICONOS EXTENDIDOS
    implementation("androidx.compose.material:material-icons-extended")

    // ---------- NAVIGATION ----------
    implementation("androidx.navigation:navigation-compose:2.8.6")

    // ---------- SUPABASE v3 ----------
    val supabase_version = "3.0.3"
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabase_version")
    implementation("io.github.jan-tennert.supabase:auth-kt:$supabase_version")
    implementation("io.github.jan-tennert.supabase:storage-kt:$supabase_version")
    implementation("io.github.jan-tennert.supabase:realtime-kt:$supabase_version")

    // ---------- KTOR 3 ----------
    val ktor_version = "3.0.3"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-android:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-utils:$ktor_version")

    // ---------- IMÁGENES ----------
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ---------- MAPS Y LOCATION ----------
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ---------- SERIALIZATION ----------
    implementation(libs.kotlinx.serialization.json)

    // ---------- TEST ----------
    testImplementation(libs.junit)
}