package com.example.mimarket.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Operaciones sobre la tabla "compras". El historial se conserva siempre. */
@Dao
interface CompraDao {

    // Registra la cabecera de una compra y devuelve su id
    @Insert
    suspend fun insertar(compra: CompraEntity): Long

    // Cabecera de una compra
    @Query("SELECT * FROM compras WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): CompraEntity?

    // Compra con todos sus detalles (relación 1:N)
    @Transaction
    @Query("SELECT * FROM compras WHERE id = :id LIMIT 1")
    suspend fun obtenerConDetalles(id: Long): CompraConDetallesRelacion?

    // Historial reactivo: la más reciente primero
    @Transaction
    @Query("SELECT * FROM compras ORDER BY fecha DESC, id DESC")
    fun observarHistorial(): Flow<List<CompraConDetallesRelacion>>

    // Número de compras registradas
    @Query("SELECT COUNT(*) FROM compras")
    suspend fun contar(): Int

    // Asigna el número de pedido una vez conocido el id
    @Query("UPDATE compras SET numeroPedido = :numeroPedido WHERE id = :id")
    suspend fun actualizarNumeroPedido(id: Long, numeroPedido: String)

    // Cambia el estado (COMPLETADA / CANCELADA)
    @Query("UPDATE compras SET estado = :estado WHERE id = :id")
    suspend fun actualizarEstado(id: Long, estado: String)

    // Semana 5: borra las compras guardadas en el dispositivo (al cerrar sesión o eliminar la cuenta)
    @Query("DELETE FROM compras")
    suspend fun borrarTodo()
}
