package com.example.mimarket.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mimarket.model.DetalleCompra

/**
 * Tabla "detalle_compras". Relación N:1 con [CompraEntity] (Compra 1 -> N DetalleCompra).
 * productoId NO es clave foránea a propósito: el detalle conserva nombre y precio como copia histórica.
 */
@Entity(
    tableName = "detalle_compras",
    foreignKeys = [
        ForeignKey(
            entity = CompraEntity::class,
            parentColumns = ["id"],
            childColumns = ["compraId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["compraId"])]
)
data class DetalleCompraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val compraId: Long,
    val productoId: Long,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

// Entity -> Model
fun DetalleCompraEntity.aModelo(): DetalleCompra =
    DetalleCompra(id, compraId, productoId, nombreProducto, cantidad, precioUnitario, subtotal)

// Model -> Entity
fun DetalleCompra.aEntidad(): DetalleCompraEntity =
    DetalleCompraEntity(id, compraId, productoId, nombreProducto, cantidad, precioUnitario, subtotal)
