package com.example.mimarket.data.repository

import androidx.room.withTransaction
import com.example.mimarket.data.local.datastore.PreferenciasDataStore
import com.example.mimarket.data.local.room.AppDatabase
import com.example.mimarket.data.local.room.CarritoDao
import com.example.mimarket.data.local.room.ItemCarritoEntity
import com.example.mimarket.data.local.room.ProductoDao
import com.example.mimarket.data.local.room.aModelo
import com.example.mimarket.model.ItemCarritoConProducto
import com.example.mimarket.util.OperacionInvalidaException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio del carrito.
 * Función: aplicar las reglas del carrito (stock, un renglón por producto, cantidad mínima 1)
 * y guardar CADA cambio de inmediato en Room, por lo que el carrito sobrevive al cierre de la app.
 * Las operaciones que leen y luego escriben usan una transacción para que no queden datos a medias.
 */
class CarritoRepository(
    private val baseDeDatos: AppDatabase,
    private val carritoDao: CarritoDao,
    private val productoDao: ProductoDao,
    private val preferencias: PreferenciasDataStore
) {
    /** Read: contenido del carrito con los datos de cada producto; se actualiza solo al cambiar la tabla. */
    fun observarItems(): Flow<List<ItemCarritoConProducto>> =
        carritoDao.observarItemsConProducto().map { lista -> lista.map { it.aModelo() } }

    /** Costo de envío configurado en DataStore. */
    fun observarCostoEnvio(): Flow<Double> = preferencias.flujoCostoEnvio

    /**
     * Create / Update: agrega el producto al carrito o suma una unidad si ya estaba.
     * Lanza OperacionInvalidaException si se supera el stock disponible.
     */
    suspend fun agregarProducto(productoId: Long) {
        // withTransaction: o se aplica todo o no se aplica nada.
        baseDeDatos.withTransaction {
            // 1) Verifica que el producto exista.
            val producto = productoDao.obtenerPorId(productoId)
                ?: throw OperacionInvalidaException("El producto ya no está disponible")
            // 2) Busca si ya está en el carrito y calcula la nueva cantidad.
            val existente = carritoDao.obtenerPorProductoId(productoId)
            val nuevaCantidad = (existente?.cantidad ?: 0) + 1
            // 3) Regla de negocio: no se puede pedir más de lo que hay en stock.
            if (nuevaCantidad > producto.stock) {
                throw OperacionInvalidaException("Stock insuficiente de ${producto.nombre} (disponible: ${producto.stock})")
            }
            if (existente == null) {
                // Primera vez: crea el renglón guardando el precio vigente (precioUnitario).
                carritoDao.insertar(
                    ItemCarritoEntity(productoId = productoId, cantidad = 1, precioUnitario = producto.precio)
                )
            } else {
                // Ya estaba: solo actualiza la cantidad.
                carritoDao.actualizarCantidad(existente.id, nuevaCantidad)
            }
        }
    }

    /**
     * Update: fija la cantidad de un renglón. Con 0 o menos lo elimina. Valida contra el stock.
     */
    suspend fun cambiarCantidad(itemId: Long, nuevaCantidad: Int) {
        baseDeDatos.withTransaction {
            // Si el renglón ya no existe, no hay nada que hacer.
            val item = carritoDao.obtenerPorId(itemId) ?: return@withTransaction
            if (nuevaCantidad <= 0) {
                // Cantidad 0: el producto se retira del carrito.
                carritoDao.eliminarPorId(itemId)
                return@withTransaction
            }
            val producto = productoDao.obtenerPorId(item.productoId)
                ?: throw OperacionInvalidaException("El producto ya no está disponible")
            if (nuevaCantidad > producto.stock) {
                throw OperacionInvalidaException("Stock insuficiente de ${producto.nombre} (disponible: ${producto.stock})")
            }
            carritoDao.actualizarCantidad(itemId, nuevaCantidad)
        }
    }

    /** Update: suma una unidad a un renglón (botón "+"). */
    suspend fun incrementar(itemId: Long) {
        val item = carritoDao.obtenerPorId(itemId) ?: return
        cambiarCantidad(itemId, item.cantidad + 1)
    }

    /** Update: resta una unidad a un renglón (botón "−"); si llega a 0 se elimina. */
    suspend fun disminuir(itemId: Long) {
        val item = carritoDao.obtenerPorId(itemId) ?: return
        cambiarCantidad(itemId, item.cantidad - 1)
    }

    /** Update: resta una unidad buscando por producto (lo usa el catálogo, que conoce el producto y no el renglón). */
    suspend fun disminuirPorProducto(productoId: Long) {
        val item = carritoDao.obtenerPorProductoId(productoId) ?: return
        cambiarCantidad(item.id, item.cantidad - 1)
    }

    /** Delete: retira un renglón completo del carrito (icono de papelera). */
    suspend fun eliminarItem(itemId: Long) = carritoDao.eliminarPorId(itemId)

    /** Delete: vacía el carrito por completo. */
    suspend fun vaciarCarrito() = carritoDao.vaciar()
}
