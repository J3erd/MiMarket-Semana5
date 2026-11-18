package com.example.mimarket.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Operaciones CRUD sobre la tabla "carrito_items". */
@Dao
interface CarritoDao {

    // ---- READ ----
    // @Transaction: la consulta del renglón y la de su producto se hacen de forma consistente
    @Transaction
    @Query("SELECT * FROM carrito_items ORDER BY id")
    fun observarItemsConProducto(): Flow<List<ItemCarritoConProductoRelacion>>

    @Transaction
    @Query("SELECT * FROM carrito_items ORDER BY id")
    // Versión de una sola lectura (no reactiva): la usa la confirmación de compra
    suspend fun obtenerItemsConProducto(): List<ItemCarritoConProductoRelacion>

    // Busca un renglón por su id
    @Query("SELECT * FROM carrito_items WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): ItemCarritoEntity?

    // Busca el renglón de un producto (hay como máximo uno)
    @Query("SELECT * FROM carrito_items WHERE productoId = :productoId LIMIT 1")
    suspend fun obtenerPorProductoId(productoId: Long): ItemCarritoEntity?

    // ---- CREATE ----
    // Agrega un renglón nuevo
    @Insert
    suspend fun insertar(item: ItemCarritoEntity): Long

    // ---- UPDATE ----
    // Actualiza un renglón completo
    @Update
    suspend fun actualizar(item: ItemCarritoEntity)

    // Cambia solo la cantidad
    @Query("UPDATE carrito_items SET cantidad = :cantidad WHERE id = :id")
    suspend fun actualizarCantidad(id: Long, cantidad: Int)

    // ---- DELETE ----
    // Elimina un renglón
    @Delete
    suspend fun eliminar(item: ItemCarritoEntity)

    // Elimina un renglón por id (papelera)
    @Query("DELETE FROM carrito_items WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    // Vacía todo el carrito
    @Query("DELETE FROM carrito_items")
    suspend fun vaciar()
}
