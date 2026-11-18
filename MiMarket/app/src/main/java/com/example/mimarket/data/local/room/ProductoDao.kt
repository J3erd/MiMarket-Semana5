package com.example.mimarket.data.local.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Operaciones CRUD sobre la tabla "productos". */
@Dao
interface ProductoDao {

    // ---- READ ----
    /** Catálogo reactivo: se vuelve a emitir cada vez que cambia la tabla. */
    // Devuelve un Flow: Room vuelve a emitir la lista cuando cambia la tabla (UI siempre actualizada)
    @Query("SELECT * FROM productos ORDER BY nombre")
    fun observarTodos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Long): ProductoEntity?

    // Cantidad de productos registrados
    @Query("SELECT COUNT(*) FROM productos")
    suspend fun contar(): Int

    // ---- CREATE ----
    // INSERT: devuelve el id generado
    @Insert
    suspend fun insertar(producto: ProductoEntity): Long

    // ---- UPDATE ----
    // UPDATE por clave primaria con el objeto completo
    @Update
    suspend fun actualizar(producto: ProductoEntity)

    // Cambia solo el precio
    @Query("UPDATE productos SET precio = :precio WHERE id = :id")
    suspend fun actualizarPrecio(id: Long, precio: Double)

    // Resta unidades al stock (se usa al confirmar una compra)
    @Query("UPDATE productos SET stock = stock - :cantidad WHERE id = :id")
    suspend fun descontarStock(id: Long, cantidad: Int)

    // ---- DELETE ----
    // DELETE del objeto indicado
    @Delete
    suspend fun eliminar(producto: ProductoEntity)

    // DELETE por id
    @Query("DELETE FROM productos WHERE id = :id")
    suspend fun eliminarPorId(id: Long)

    /** Sincronización con el servidor: inserta los productos nuevos y actualiza los existentes (sin borrar el carrito). */
    @Upsert
    suspend fun upsertar(productos: List<ProductoEntity>)

    /** Sincronización: quita de la copia local los productos que el servidor ya no tiene. */
    @Query("DELETE FROM productos WHERE id NOT IN (:idsVigentes)")
    suspend fun eliminarNoIncluidos(idsVigentes: List<Long>)
}
