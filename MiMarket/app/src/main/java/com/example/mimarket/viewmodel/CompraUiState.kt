package com.example.mimarket.viewmodel

import com.example.mimarket.model.CompraConDetalles

/**
 * Estado de las pantallas de compra (confirmación, compra realizada e historial).
 *
 * @property compraRealizada `true` cuando la compra se confirmó y registró en Room.
 * @property compraId Id de la compra registrada (se envía a la pantalla "Compra realizada").
 * @property numeroPedido Número de pedido mostrado al cliente.
 * @property cantidadProductos Unidades compradas.
 * @property total Total pagado.
 * @property historial Todas las compras registradas, de la más reciente a la más antigua.
 */
data class CompraUiState(
    val compraRealizada: Boolean = false,
    val compraId: Long = 0,
    val numeroPedido: String = "",
    val cantidadProductos: Int = 0,
    val total: Double = 0.0,
    val historial: List<CompraConDetalles> = emptyList(),
    val errorMessage: String? = null,
    val isLoading: Boolean = false
) {
    /** Estado genérico: cargando, error, sin datos (vacío) o con datos (éxito). */
    val estado: EstadoUi
        get() = when {
            isLoading -> EstadoUi.CARGANDO
            errorMessage != null -> EstadoUi.ERROR
            compraRealizada || historial.isNotEmpty() -> EstadoUi.EXITO
            else -> EstadoUi.VACIO
        }
}
