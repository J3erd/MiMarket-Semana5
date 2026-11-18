package com.example.mimarket.viewmodel

/** Estados genéricos que puede mostrar cualquier pantalla (Loading / Success / Error / Empty). */
enum class EstadoUi {
    CARGANDO, // Loading: la información se está cargando
    EXITO,    // Success: hay datos para mostrar
    ERROR,    // Error: ocurrió un problema
    VACIO,    // Empty: no hay datos que mostrar
    SIN_CONEXION // Offline: no se puede comunicar con el servidor
}
