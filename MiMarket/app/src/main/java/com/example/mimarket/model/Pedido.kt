package com.example.mimarket.model

/**
 * Pedido del historial de compras (mapeado al documento usuarios/{uid}/pedidos/{id} de Firestore).
 * Cada usuario solo puede leer y escribir sus propios pedidos (reglas de seguridad).
 *
 * @property id Id del documento (se usa la marca de tiempo de la compra).
 * @property numeroPedido Código visible para el cliente (ej. "#0000012").
 * @property fecha Marca de tiempo en milisegundos.
 * @property items Productos comprados (copia de nombre y precio: el historial no cambia aunque el producto sí).
 */
data class Pedido(
    val id: String = "",
    val numeroPedido: String = "",
    val fecha: Long = 0,
    val subtotal: Double = 0.0,
    val costoEnvio: Double = 0.0,
    val total: Double = 0.0,
    val estado: String = EstadoCompra.COMPLETADA,
    val items: List<PedidoItem> = emptyList()
)

/** Línea de un pedido. */
data class PedidoItem(
    val productoId: Long = 0,
    val nombreProducto: String = "",
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0,
    val subtotal: Double = 0.0
)

/** Compra local (Room) -> Pedido (Firestore). */
fun CompraConDetalles.aPedido(): Pedido = Pedido(
    id = compra.fecha.toString(),
    numeroPedido = compra.numeroPedido,
    fecha = compra.fecha,
    subtotal = compra.subtotal,
    costoEnvio = compra.costoEnvio,
    total = compra.total,
    estado = compra.estado,
    items = detalles.map { PedidoItem(it.productoId, it.nombreProducto, it.cantidad, it.precioUnitario, it.subtotal) }
)

/** Pedido (Firestore) -> modelo que dibuja la pantalla de historial. */
fun Pedido.aCompraConDetalles(): CompraConDetalles = CompraConDetalles(
    compra = Compra(
        id = fecha,
        numeroPedido = numeroPedido,
        fecha = fecha,
        subtotal = subtotal,
        costoEnvio = costoEnvio,
        total = total,
        estado = estado
    ),
    detalles = items.mapIndexed { indice, it ->
        DetalleCompra(
            id = indice.toLong(),
            compraId = fecha,
            productoId = it.productoId,
            nombreProducto = it.nombreProducto,
            cantidad = it.cantidad,
            precioUnitario = it.precioUnitario,
            subtotal = it.subtotal
        )
    }
)
