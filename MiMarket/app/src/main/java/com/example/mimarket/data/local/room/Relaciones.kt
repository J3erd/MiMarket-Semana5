package com.example.mimarket.data.local.room

import androidx.room.Embedded
import androidx.room.Relation
import com.example.mimarket.model.CompraConDetalles
import com.example.mimarket.model.ItemCarritoConProducto

/** Resultado de consulta: renglón del carrito + su producto (ItemCarrito N:1 Producto). */
data class ItemCarritoConProductoRelacion(
    @Embedded val item: ItemCarritoEntity, // incrusta las columnas del renglón del carrito
    // Room hace el JOIN: busca el producto cuyo id == item.productoId
    @Relation(parentColumn = "productoId", entityColumn = "id")
    val producto: ProductoEntity
)

/** Resultado de consulta: compra + sus detalles (Compra 1:N DetalleCompra). */
data class CompraConDetallesRelacion(
    @Embedded val compra: CompraEntity, // incrusta las columnas de la compra
    // Room trae todos los detalles cuyo compraId == compra.id
    @Relation(parentColumn = "id", entityColumn = "compraId")
    val detalles: List<DetalleCompraEntity>
)

// Convierten las relaciones de Room en modelos de dominio
fun ItemCarritoConProductoRelacion.aModelo(): ItemCarritoConProducto =
    ItemCarritoConProducto(item = item.aModelo(), producto = producto.aModelo())

fun CompraConDetallesRelacion.aModelo(): CompraConDetalles =
    CompraConDetalles(compra = compra.aModelo(), detalles = detalles.map { it.aModelo() })
