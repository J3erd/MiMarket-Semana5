package com.example.mimarket.viewmodel

import com.example.mimarket.model.Tienda
import com.example.mimarket.model.UbicacionUsuario

/**
 * Estados de la ubicación del usuario en la pantalla del mapa.
 * Es una sealed interface: la pantalla está obligada a contemplar TODOS los casos.
 */
sealed interface EstadoUbicacion {
    /** Aún no se pidió nada: primero debe aceptarse el aviso de privacidad. */
    data object SinSolicitar : EstadoUbicacion
    /** Cargando ubicación. */
    data object Cargando : EstadoUbicacion
    /** Ubicación obtenida. */
    data class Obtenida(val ubicacion: UbicacionUsuario) : EstadoUbicacion
    /** El usuario no aceptó el aviso de privacidad: no se pide el permiso ni se lee la ubicación. */
    data object AvisoRechazado : EstadoUbicacion
    /** Permiso denegado. [definitivo] = Android ya no volverá a mostrar el diálogo (hay que ir a Ajustes). */
    data class PermisoDenegado(val definitivo: Boolean) : EstadoUbicacion
    /** El permiso está concedido pero la ubicación del dispositivo (GPS) está apagada. */
    data object GpsDesactivado : EstadoUbicacion
    /** No se pudo obtener la ubicación por otro motivo. */
    data class Error(val mensaje: String) : EstadoUbicacion
}

/**
 * Estado de la pantalla "Mapa de tiendas".
 *
 * @property tiendas Tiendas a mostrar como marcadores (Firestore, o las de ejemplo como respaldo).
 * @property cargandoTiendas true hasta recibir la primera lista de tiendas.
 * @property tiendaSeleccionada Tienda elegida en la lista o tocando su marcador.
 * @property ubicacion Estado de la ubicación del usuario.
 * @property consentimiento true si ya aceptó el aviso de privacidad de ubicación.
 * @property mostrarAvisoPrivacidad true mientras se muestra el aviso (ANTES de pedir el permiso).
 * @property solicitarPermiso true cuando la pantalla debe lanzar el diálogo de permisos de Android.
 * @property tiendaMasCercana Tienda más próxima al usuario y su distancia en km (solo con ubicación).
 * @property guardandoDireccion true mientras se guarda la dirección de entrega.
 * @property mensaje Aviso puntual (snackbar).
 */
data class MapaTiendasUiState(
    val tiendas: List<Tienda> = emptyList(),
    val cargandoTiendas: Boolean = true,
    val tiendaSeleccionada: Tienda? = null,
    val ubicacion: EstadoUbicacion = EstadoUbicacion.SinSolicitar,
    val consentimiento: Boolean = false,
    val mostrarAvisoPrivacidad: Boolean = false,
    val solicitarPermiso: Boolean = false,
    val tiendaMasCercana: Pair<Tienda, Double>? = null,
    val guardandoDireccion: Boolean = false,
    val mensaje: String? = null
)
