package com.example.mimarket.model

import com.example.mimarket.util.CalculadoraCarrito

/**
 * Elemento del carrito: referencia a un producto con su cantidad y el precio unitario vigente al agregarlo.
 */
// Un renglón del carrito: producto + cantidad + precio al momento de agregarlo.
data class ItemCarrito(
    val id: Long = 0,
    val productoId: Long,
    val cantidad: Int,
    val precioUnitario: Double
)

/**
 * Elemento del carrito junto con los datos del producto (relación ItemCarrito -> Producto).
 * Es el modelo que consumen las pantallas para dibujar nombre, imagen y subtotal.
 */
data class ItemCarritoConProducto(
    val item: ItemCarrito,
    val producto: Producto
) {
    /** Subtotal del renglón = cantidad x precio unitario (dato derivado, no se guarda en la BD). */
    val subtotal: Double
        get() = CalculadoraCarrito.redondear(item.cantidad * item.precioUnitario)
}
