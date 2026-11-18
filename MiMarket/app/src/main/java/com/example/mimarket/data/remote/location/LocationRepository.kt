package com.example.mimarket.data.remote.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fuente de datos de geolocalización. Es la ÚNICA clase que habla con los servicios de ubicación
 * de Google (FusedLocationProviderClient) y con el Geocoder de Android.
 * No guarda la ubicación en ningún lado: solo la entrega a quien la pide.
 */
class LocationRepository(private val contexto: Context) {

    private val cliente = LocationServices.getFusedLocationProviderClient(contexto)

    /** true si el usuario concedió ubicación precisa (FINE) o aproximada (COARSE). */
    fun tienePermiso(): Boolean = tienePermisoPreciso() ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun tienePermisoPreciso(): Boolean =
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** true si la ubicación (GPS / red) está encendida en los ajustes del dispositivo. */
    fun gpsActivado(): Boolean {
        val gestor = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(gestor)
    }

    /**
     * Ubicación actual con FusedLocationProviderClient. Devuelve null si no se pudo obtener.
     * Solo debe llamarse cuando [tienePermiso] es true (por eso se suprime el aviso de lint).
     */
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionActual(): Location? {
        val prioridad = if (tienePermisoPreciso()) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        // Pide una lectura nueva; si tarda demasiado se usa la última conocida.
        val actual = withTimeoutOrNull(12_000) {
            cliente.getCurrentLocation(prioridad, CancellationTokenSource().token).await()
        }
        return actual ?: withTimeoutOrNull(4_000) { cliente.lastLocation.await() }
    }

    /**
     * Geocodificación inversa (opcional): convierte coordenadas en una dirección legible.
     * Devuelve null si no hay internet o el dispositivo no tiene Geocoder.
     */
    @Suppress("DEPRECATION")
    suspend fun obtenerDireccion(latitud: Double, longitud: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            Geocoder(contexto, Locale.forLanguageTag("es-PE"))
                .getFromLocation(latitud, longitud, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        }.getOrNull()
    }
}
