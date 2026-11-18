package com.example.mimarket.viewmodel

import com.example.mimarket.model.ItemCarritoConProducto

/**
 * Estado del resumen de compra (carrito).
 * Los importes (subtotal, envío, total) son datos derivados que calcula el ViewModel.
 */
data class CarritoUiState(
    val items: List<ItemCarritoConProducto> = emptyList(),
    val cantidadTotal: Int = 0,
    val subtotal: Double = 0.0,
    val costoEnvio: Double = 0.0,
    val total: Double = 0.0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val mensaje: String? = null,
    val direccionEntrega: String = "" // Semana 5: dirección elegida en el mapa (pantalla Confirmar pedido)
) {
    /** No se puede confirmar una compra con el carrito vacío. */
    val puedeConfirmar: Boolean
        get() = items.isNotEmpty() && !isLoading

    /** Estado genérico: cargando, error, carrito vacío o con productos. */
    val estado: EstadoUi
        get() = when {
            isLoading -> EstadoUi.CARGANDO
            errorMessage != null -> EstadoUi.ERROR
            items.isEmpty() -> EstadoUi.VACIO
            else -> EstadoUi.EXITO
        }
}
