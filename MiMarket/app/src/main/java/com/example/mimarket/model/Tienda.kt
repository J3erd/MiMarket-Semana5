package com.example.mimarket.model

/**
 * Tienda física de Mi Market (mapeada al documento tiendas/{id} de Firestore).
 * Sus coordenadas se muestran como marcadores en el mapa.
 */
data class Tienda(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val horario: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)

/**
 * Ubicación actual del usuario obtenida con FusedLocationProviderClient.
 * Solo vive en memoria mientras la pantalla del mapa está abierta: no se guarda en ningún lado,
 * salvo que el usuario pulse expresamente "Usar como dirección de entrega".
 *
 * @property direccion Dirección aproximada (geocodificación inversa); null si no se pudo obtener.
 */
data class UbicacionUsuario(
    val latitud: Double,
    val longitud: Double,
    val direccion: String? = null
)
