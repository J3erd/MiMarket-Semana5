package com.example.mimarket.data.local.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mimarket.model.ItemCarrito

/**
 * Tabla "carrito_items". Relación N:1 con [ProductoEntity] (un producto aparece una sola vez en el carrito,
 * por eso el índice único en productoId). Si el producto se elimina, su renglón del carrito también.
 */
@Entity(
    tableName = "carrito_items",
    foreignKeys = [ // clave foránea: productoId debe existir en productos
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["productoId"], unique = true)]
)
data class ItemCarritoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productoId: Long,
    val cantidad: Int,
    val precioUnitario: Double
)

// Entity -> Model
fun ItemCarritoEntity.aModelo(): ItemCarrito = ItemCarrito(id, productoId, cantidad, precioUnitario)

// Model -> Entity
fun ItemCarrito.aEntidad(): ItemCarritoEntity = ItemCarritoEntity(id, productoId, cantidad, precioUnitario)
