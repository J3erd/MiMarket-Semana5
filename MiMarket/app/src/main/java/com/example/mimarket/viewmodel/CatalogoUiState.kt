package com.example.mimarket.viewmodel

import com.example.mimarket.model.Producto

/**
 * Estado del catálogo.
 *
 * @property productos Productos visibles (ya filtrados por la categoría elegida).
 * @property cantidades Unidades de cada producto que están en el carrito (clave = id de producto).
 * @property cantidadCarrito Total de unidades en el carrito (se muestra en el acceso al carrito).
 * @property categorias Categorías disponibles para filtrar.
 * @property categoriaSeleccionada Categoría activa; cadena vacía = todas.
 * @property busqueda Texto del buscador (filtra por nombre o descripción).
 * @property isLoading true hasta recibir el primer Snapshot de Firestore.
 * @property sinConexion true si el servidor no se pudo alcanzar (apagado, sin red o timeout).
 * @property sinRed true si el dispositivo no tiene internet (se muestra un aviso y, al volver la red, se reintenta solo).
 * @property errorMessage Error de la consulta (400/404/500/inesperado), ya redactado para el usuario.
 * @property operando true mientras se ejecuta una operación (eliminar o importar; evita envíos duplicados).
 * @property productoAEliminar Producto pendiente de confirmar en el diálogo de eliminación.
 * @property avisoEliminar Mensaje formal cuando la eliminación no está permitida.
 * @property mensaje Aviso puntual para el usuario (resultado de una operación).
 */
data class CatalogoUiState(
    val productos: List<Producto> = emptyList(),
    val cantidades: Map<Long, Int> = emptyMap(),
    val cantidadCarrito: Int = 0,
    val categorias: List<String> = emptyList(),
    val categoriaSeleccionada: String = "",
    val busqueda: String = "",
    val emailUsuario: String = "",
    val uidUsuario: String = "", // UID del usuario autenticado: marca en pantalla los productos propios
    val isLoading: Boolean = true,
    val sinConexion: Boolean = false,
    val sinRed: Boolean = false, // el dispositivo perdió internet (aviso en pantalla; lo informa ConectividadObserver)
    val errorMessage: String? = null,
    val operando: Boolean = false,
    val productoAEliminar: Producto? = null,
    val avisoEliminar: String? = null, // motivo por el que NO se permite eliminar (se muestra en un diálogo)
    val mensaje: String? = null
) {
    /** Estado genérico: cargando, sin conexión, error, lista vacía o éxito (con productos). */
    val estado: EstadoUi
        get() = when {
            isLoading -> EstadoUi.CARGANDO
            sinConexion -> EstadoUi.SIN_CONEXION
            errorMessage != null -> EstadoUi.ERROR
            productos.isEmpty() -> EstadoUi.VACIO
            else -> EstadoUi.EXITO
        }
}
