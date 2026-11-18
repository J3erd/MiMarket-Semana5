package com.example.mimarket.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mimarket.model.Compra

/** Tabla "compras": cabecera de cada compra confirmada. El historial nunca se borra al vaciar el carrito. */
@Entity(tableName = "compras")
data class CompraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numeroPedido: String,
    val fecha: Long,
    val subtotal: Double,
    val costoEnvio: Double,
    val total: Double,
    val estado: String
)

// Entity -> Model
fun CompraEntity.aModelo(): Compra = Compra(id, numeroPedido, fecha, subtotal, costoEnvio, total, estado)

// Model -> Entity
fun Compra.aEntidad(): CompraEntity = CompraEntity(id, numeroPedido, fecha, subtotal, costoEnvio, total, estado)
