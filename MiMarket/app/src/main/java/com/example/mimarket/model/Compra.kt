package com.example.mimarket.model

/** Estados posibles de una compra. */
object EstadoCompra {
    const val COMPLETADA = "COMPLETADA"
    const val CANCELADA = "CANCELADA"
}

/**
 * Cabecera de una compra realizada.
 *
 * @property numeroPedido Código visible para el cliente (ej. "#0000012").
 * @property fecha Marca de tiempo en milisegundos (epoch).
 */
data class Compra(
    val id: Long = 0,
    val numeroPedido: String,
    val fecha: Long,
    val subtotal: Double,
    val costoEnvio: Double,
    val total: Double,
    val estado: String
)

/**
 * Línea de una compra. Guarda una copia del nombre y del precio para conservar el historial
 * aunque el producto cambie o se elimine del catálogo.
 */
data class DetalleCompra(
    val id: Long = 0,
    val compraId: Long,
    val productoId: Long,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

/** Compra con todas sus líneas (relación Compra 1 -> N DetalleCompra). */
data class CompraConDetalles(
    val compra: Compra,
    val detalles: List<DetalleCompra>
) {
    /** Total de unidades compradas. */
    val cantidadProductos: Int
        get() = detalles.sumOf { it.cantidad }
}
