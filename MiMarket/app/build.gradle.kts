import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose")
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services) // Semana 5: lee app/google-services.json (Firebase)
}

// Semana 5 - Protección de API Keys: la clave de Google Maps NO se escribe en el código ni en el
// AndroidManifest. Se lee de local.properties (archivo ignorado por git) con la línea:
//   MAPS_API_KEY=AIza...
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val claveMaps: String = propiedadesLocales.getProperty("MAPS_API_KEY", "").trim()

android {
    namespace = "com.example.mimarket"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mimarket"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2.0"

        // URL base del backend Node.js (solo se usa para importar el catálogo inicial a Firestore).
        // Con "adb reverse tcp:3000 tcp:3000" (lo hace Servidor/iniciar_servidor.bat) sirve para emulador y celular.
        buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:3000/\"")

        // La clave de Maps viaja al AndroidManifest como marcador (${MAPS_API_KEY}).
        manifestPlaceholders["MAPS_API_KEY"] = claveMaps.ifEmpty { "CLAVE_NO_CONFIGURADA" }
        // Permite a la app avisar si todavía no se configuró la clave.
        buildConfigField("boolean", "MAPS_KEY_CONFIGURADA", claveMaps.isNotEmpty().toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
        buildConfig = true // genera BuildConfig.API_BASE_URL y BuildConfig.MAPS_KEY_CONFIGURADA
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.runtime.livedata)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Room (copia local del catálogo + carrito)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Retrofit + OkHttp + Gson (backend Node.js de la semana 4) y Coroutines
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
    // Coil: carga de imágenes por URL (también las de Firebase Storage)
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.datastore.preferences)

    // ---- Semana 5: Firebase (Authentication, Cloud Firestore y Storage) ----
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.coroutines.play.services) // .await() para las tareas de Firebase

    // ---- Semana 5: Google Maps y geolocalización ----
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location) // FusedLocationProviderClient
    implementation(libs.maps.compose)           // GoogleMap en Jetpack Compose

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
