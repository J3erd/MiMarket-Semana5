package com.example.mimarket.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Operaciones sobre la tabla "detalle_compras". */
@Dao
interface DetalleCompraDao {

    // Inserta todas las líneas de una compra de una sola vez
    @Insert
    suspend fun insertarTodos(detalles: List<DetalleCompraEntity>)

    // Líneas de una compra concreta
    @Query("SELECT * FROM detalle_compras WHERE compraId = :compraId ORDER BY id")
    suspend fun obtenerPorCompra(compraId: Long): List<DetalleCompraEntity>
}
