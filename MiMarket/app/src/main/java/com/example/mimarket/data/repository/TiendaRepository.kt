package com.example.mimarket.data.repository

import com.example.mimarket.data.local.room.DatosIniciales
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.remote.firebase.FirestoreRepository
import com.example.mimarket.data.remote.firebase.llamarFirebase
import com.example.mimarket.data.remote.location.LocationRepository
import com.example.mimarket.model.Tienda
import com.example.mimarket.model.UbicacionUsuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Repositorio de tiendas y ubicación (FACHADA).
 * El ViewModel del mapa solo conoce esta clase: no depende de Firestore, de
 * FusedLocationProviderClient ni del Geocoder.
 */
class TiendaRepository(
    private val firestore: FirestoreRepository,
    private val ubicacion: LocationRepository
) {
    /**
     * Tiendas físicas en tiempo real desde Firestore (colección "tiendas").
     * Si la colección está vacía o no se puede leer, se usan las tiendas de ejemplo.
     */
    fun observarTiendas(): Flow<List<Tienda>> = firestore.observarTiendas()
        .map { lista -> lista.ifEmpty { DatosIniciales.tiendas() } }
        .catch { emit(DatosIniciales.tiendas()) }

    /** Publica las tiendas de ejemplo en Firestore (las reglas solo lo permiten a un administrador). */
    suspend fun publicarTiendasDeEjemplo(): ResultadoApi<Int> = llamarFirebase {
        val lista = DatosIniciales.tiendas()
        firestore.guardarTiendas(lista)
        lista.size
    }

    /** true si la app tiene permiso de ubicación (precisa o aproximada). */
    fun tienePermisoUbicacion(): Boolean = ubicacion.tienePermiso()

    /** true si la ubicación del dispositivo está encendida. */
    fun gpsActivado(): Boolean = ubicacion.gpsActivado()

    /**
     * Ubicación actual del usuario y su dirección aproximada (geocodificación inversa).
     * Devuelve null si no se pudo obtener. No se guarda en ningún lado.
     */
    suspend fun obtenerUbicacionActual(): UbicacionUsuario? {
        val punto = ubicacion.obtenerUbicacionActual() ?: return null
        return UbicacionUsuario(
            latitud = punto.latitude,
            longitud = punto.longitude,
            direccion = ubicacion.obtenerDireccion(punto.latitude, punto.longitude)
        )
    }
}
