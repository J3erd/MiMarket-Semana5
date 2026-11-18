package com.example.mimarket.viewmodel

import com.example.mimarket.model.Producto

/**
 * Estado de la pantalla "Detalle de producto".
 * @property producto Producto mostrado (null si ya no existe, por ejemplo tras eliminarlo).
 * @property cantidadEnCarrito Unidades de este producto que ya están en el carrito.
 * @property cantidadSeleccionada Unidades que se agregarán al pulsar "Agregar al carrito".
 * @property confirmandoEliminar true mientras se muestra el diálogo de confirmación.
 * @property operando true mientras se ejecuta la eliminación (evita envíos duplicados).
 * @property eliminado true cuando el servidor confirmó la eliminación (la pantalla se cierra).
 */
data class ProductoDetalleUiState(
    val producto: Producto? = null,
    val cargando: Boolean = true,
    val cantidadEnCarrito: Int = 0,
    val cantidadSeleccionada: Int = 1,
    val confirmandoEliminar: Boolean = false,
    val operando: Boolean = false,
    val eliminado: Boolean = false,
    val avisoEliminar: String? = null,
    val mensaje: String? = null
)
