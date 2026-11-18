package com.example.mimarket.util

import com.example.mimarket.model.ItemCarritoConProducto
import kotlin.math.round

/**
 * Cálculos derivados del carrito (subtotal, envío y total).
 * Es lógica pura (sin Android), por lo que se puede probar con tests unitarios (LogicaTest).
 */
object CalculadoraCarrito {

    /** Costo de envío por defecto en soles; puede cambiarse desde DataStore (configuración de la app). */
    const val COSTO_ENVIO_POR_DEFECTO = 2.0

    /** Redondea a 2 decimales para evitar errores de coma flotante (ej. 0.1 + 0.2). */
    fun redondear(valor: Double): Double = round(valor * 100.0) / 100.0

    /** Subtotal general: suma de (cantidad x precio unitario) de todos los renglones. */
    fun subtotal(items: List<ItemCarritoConProducto>): Double =
        redondear(items.sumOf { it.item.cantidad * it.item.precioUnitario })

    /** Cantidad total de unidades en el carrito (es el número que se ve en el icono del carrito). */
    fun cantidadTotal(items: List<ItemCarritoConProducto>): Int = items.sumOf { it.item.cantidad }

    /** El envío solo se cobra si hay productos: con subtotal 0 el costo es 0. */
    fun costoEnvio(subtotal: Double, costoBase: Double = COSTO_ENVIO_POR_DEFECTO): Double =
        if (subtotal <= 0.0) 0.0 else redondear(costoBase)

    /** Total a pagar = subtotal + costo de envío. */
    fun total(subtotal: Double, costoEnvio: Double): Double = redondear(subtotal + costoEnvio)
}
