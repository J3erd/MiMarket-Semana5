package com.example.mimarket.data.remote.api

import com.example.mimarket.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Configuración centralizada de Retrofit: URL base, tiempos de espera y conversor JSON (Gson).
 * La URL base sale de BuildConfig.API_BASE_URL (app/build.gradle.kts).
 *  - Emulador de Android Studio: http://127.0.0.1:3000/  (10.0.2.2 = la PC que ejecuta el emulador)
 *  - Celular real: cambiar por la IP de la PC, ej. http://192.168.1.20:3000/
 */
object RetrofitClient {

    /** Crea un servicio para cualquier URL (las pruebas lo usan con un servidor falso). */
    fun crearServicio(baseUrl: String): ProductoApiService {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // tiempo máximo para conectar
            .readTimeout(8, TimeUnit.SECONDS)    // tiempo máximo esperando respuesta -> Timeout
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProductoApiService::class.java)
    }

    /** Servicio de productos que usa la app (se crea una sola vez). */
    val productoApi: ProductoApiService by lazy { crearServicio(BuildConfig.API_BASE_URL) }
}
