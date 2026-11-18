package com.example.mimarket.data.repository

import androidx.room.withTransaction
import com.example.mimarket.data.local.datastore.PreferenciasDataStore
import com.example.mimarket.data.local.room.AppDatabase
import com.example.mimarket.data.local.room.CarritoDao
import com.example.mimarket.data.local.room.CompraDao
import com.example.mimarket.data.local.room.CompraEntity
import com.example.mimarket.data.local.room.DetalleCompraDao
import com.example.mimarket.data.local.room.DetalleCompraEntity
import com.example.mimarket.data.local.room.ProductoDao
import com.example.mimarket.data.local.room.aModelo
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.remote.firebase.FirebaseAuthManager
import com.example.mimarket.data.remote.firebase.FirestoreRepository
import com.example.mimarket.data.remote.firebase.SesionNoIniciadaException
import com.example.mimarket.data.remote.firebase.llamarFirebase
import com.example.mimarket.model.CompraConDetalles
import com.example.mimarket.model.EstadoCompra
import com.example.mimarket.model.aCompraConDetalles
import com.example.mimarket.model.aPedido
import com.example.mimarket.util.CalculadoraCarrito
import com.example.mimarket.util.OperacionInvalidaException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repositorio de compras.
 * Función principal: convertir el carrito en una compra registrada. Esa operación es atómica
 * (transacción de Room): registra compra y detalle, descuenta stock y vacía el carrito;
 * si algo falla, no se aplica NADA.
 * Semana 5: después de registrar la compra en el dispositivo, el pedido se guarda en la subcolección
 * usuarios/{uid}/pedidos de Firestore (historial privado de cada usuario) y se descuenta el stock en la nube.
 */
class CompraRepository(
    private val baseDeDatos: AppDatabase,
    private val carritoDao: CarritoDao,
    private val productoDao: ProductoDao,
    private val compraDao: CompraDao,
    private val detalleCompraDao: DetalleCompraDao,
    private val preferencias: PreferenciasDataStore,
    private val firestore: FirestoreRepository = FirestoreRepository(),
    private val auth: FirebaseAuthManager = FirebaseAuthManager()
) {
    /**
     * Create: confirma la compra con el contenido actual del carrito.
     * @throws OperacionInvalidaException si el carrito está vacío o no hay stock suficiente.
     * @return la compra registrada con su detalle.
     */
    suspend fun confirmarCompra(): CompraConDetalles {
        // El costo de envío se lee ANTES de la transacción (DataStore no forma parte de la transacción de Room).
        val costoEnvioBase = preferencias.flujoCostoEnvio.first()

        val compraId = baseDeDatos.withTransaction {
            // 1) Lee el carrito con los datos de cada producto.
            val items = carritoDao.obtenerItemsConProducto().map { it.aModelo() }
            // 2) Regla: no se confirma una compra con el carrito vacío.
            if (items.isEmpty()) {
                throw OperacionInvalidaException("No se puede confirmar una compra con el carrito vacío")
            }
            // 3) Regla: ningún renglón puede superar el stock actual.
            items.firstOrNull { it.item.cantidad > it.producto.stock }?.let {
                throw OperacionInvalidaException("Stock insuficiente de ${it.producto.nombre} (disponible: ${it.producto.stock})")
            }

            // 4) Calcula los importes (subtotal, envío, total).
            val subtotal = CalculadoraCarrito.subtotal(items)
            val costoEnvio = CalculadoraCarrito.costoEnvio(subtotal, costoEnvioBase)
            val total = CalculadoraCarrito.total(subtotal, costoEnvio)

            // 5) Inserta la cabecera de la compra (el número de pedido se completa enseguida con el id).
            val id = compraDao.insertar(
                CompraEntity(
                    numeroPedido = "",
                    fecha = System.currentTimeMillis(),
                    subtotal = subtotal,
                    costoEnvio = costoEnvio,
                    total = total,
                    estado = EstadoCompra.COMPLETADA
                )
            )
            // Número de pedido visible: "#" + id rellenado con ceros (ej. #0000012).
            compraDao.actualizarNumeroPedido(id, "#" + id.toString().padStart(7, '0'))

            // 6) Inserta una línea de detalle por cada producto, copiando nombre y precio (historial inalterable).
            detalleCompraDao.insertarTodos(
                items.map {
                    DetalleCompraEntity(
                        compraId = id,
                        productoId = it.producto.id,
                        nombreProducto = it.producto.nombre,
                        cantidad = it.item.cantidad,
                        precioUnitario = it.item.precioUnitario,
                        subtotal = it.subtotal
                    )
                }
            )
            // 7) Descuenta del stock las unidades vendidas.
            items.forEach { productoDao.descontarStock(it.producto.id, it.item.cantidad) }
            // 8) Vacía el carrito (el historial de compras NO se toca).
            carritoDao.vaciar()
            id
        }
        // Devuelve la compra ya guardada (leída de Room) con su detalle.
        return obtenerCompra(compraId) ?: throw OperacionInvalidaException("No se pudo registrar la compra")
    }

    /** Read: una compra con todas sus líneas (null si no existe). */
    suspend fun obtenerCompra(id: Long): CompraConDetalles? = compraDao.obtenerConDetalles(id)?.aModelo()

    /** Read: historial completo, de la más reciente a la más antigua; se actualiza solo. */
    fun observarHistorial(): Flow<List<CompraConDetalles>> =
        compraDao.observarHistorial().map { lista -> lista.map { it.aModelo() } }

    /** Update: cambia el estado de una compra (por ejemplo a CANCELADA). */
    suspend fun actualizarEstado(compraId: Long, estado: String) = compraDao.actualizarEstado(compraId, estado)

    // ---- Semana 5: historial en Cloud Firestore (usuarios/{uid}/pedidos) ----

    /**
     * Guarda la compra como pedido en la subcolección del usuario y descuenta el stock de cada
     * producto en Firestore (los demás dispositivos lo ven en tiempo real).
     */
    suspend fun registrarPedidoEnNube(compra: CompraConDetalles): ResultadoApi<Unit> = llamarFirebase {
        val uid = auth.uidActual ?: throw SesionNoIniciadaException()
        firestore.guardarPedido(uid, compra.aPedido())
        // Si un producto ya fue eliminado por su dueño, el descuento de ese producto se omite.
        compra.detalles.forEach { runCatching { firestore.descontarStock(it.productoId, it.cantidad) } }
    }

    /** Historial de pedidos del usuario autenticado, en tiempo real (del más reciente al más antiguo). */
    fun observarPedidos(): Flow<List<CompraConDetalles>> {
        val uid = auth.uidActual ?: return flowOf(emptyList())
        return firestore.observarPedidos(uid).map { lista -> lista.map { it.aCompraConDetalles() } }
    }
}