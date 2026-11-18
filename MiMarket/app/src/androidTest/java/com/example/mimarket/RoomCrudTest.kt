package com.example.mimarket

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.mimarket.data.local.datastore.PreferenciasDataStore
import com.example.mimarket.data.local.room.AppDatabase
import com.example.mimarket.data.repository.CarritoRepository
import com.example.mimarket.data.repository.CompraRepository
import com.example.mimarket.data.repository.ProductoRepository
import com.example.mimarket.model.Producto
import com.example.mimarket.util.OperacionInvalidaException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Evidencia de CRUD sobre Room (base en memoria) a través de los Repository. */
@RunWith(AndroidJUnit4::class)
class RoomCrudTest {

    private lateinit var bd: AppDatabase
    private lateinit var productos: ProductoRepository
    private lateinit var carrito: CarritoRepository
    private lateinit var compras: CompraRepository

    @Before
    fun preparar() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        bd = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        val prefs = PreferenciasDataStore(contexto)
        productos = ProductoRepository(bd.productoDao(), prefs)
        carrito = CarritoRepository(bd, bd.carritoDao(), bd.productoDao(), prefs)
        compras = CompraRepository(bd, bd.carritoDao(), bd.productoDao(), bd.compraDao(), bd.detalleCompraDao(), prefs)
    }

    @After
    fun cerrar() = bd.close()

    private fun nuevo(nombre: String, precio: Double, stock: Int = 10) =
        Producto(nombre = nombre, descripcion = "d", precio = precio, imagen = "prod_x", categoria = "Test", stock = stock)

    @Test
    fun crudProductos() = runBlocking {
        val id = productos.crearProducto(nuevo("Pera", 3.0))                       // Create
        assertEquals("Pera", productos.obtenerProducto(id)?.nombre)                 // Read
        productos.actualizarPrecio(id, 3.5)                                         // Update
        assertEquals(3.5, productos.obtenerProducto(id)!!.precio, 0.001)
        productos.eliminarProducto(id)                                              // Delete
        assertNull(productos.obtenerProducto(id))
    }

    @Test
    fun crudCarrito() = runBlocking {
        val id = productos.crearProducto(nuevo("Pera", 3.0))
        carrito.agregarProducto(id)                                                  // Create
        carrito.agregarProducto(id)                                                  // Update (suma)
        var items = carrito.observarItems().first()                                  // Read
        assertEquals(2, items.single().item.cantidad)
        carrito.disminuir(items.single().item.id)                                    // Update
        items = carrito.observarItems().first()
        assertEquals(1, items.single().item.cantidad)
        carrito.eliminarItem(items.single().item.id)                                 // Delete
        assertTrue(carrito.observarItems().first().isEmpty())
    }

    @Test
    fun stockInsuficiente_lanzaError() = runBlocking {
        val id = productos.crearProducto(nuevo("Pera", 3.0, stock = 1))
        carrito.agregarProducto(id)
        try {
            carrito.agregarProducto(id)
            fail("Debió lanzar OperacionInvalidaException")
        } catch (e: OperacionInvalidaException) {
            assertEquals(1, carrito.observarItems().first().single().item.cantidad)
        }
    }

    @Test
    fun carritoVacio_noSePuedeConfirmar() = runBlocking {
        try {
            compras.confirmarCompra()
            fail("Debió lanzar OperacionInvalidaException")
        } catch (e: OperacionInvalidaException) {
            assertTrue(compras.observarHistorial().first().isEmpty())
        }
    }

    @Test
    fun confirmarCompra_registraHistorial_vaciaCarrito_yDescuentaStock() = runBlocking {
        val id = productos.crearProducto(nuevo("Pera", 3.0, stock = 5))
        carrito.agregarProducto(id)
        carrito.agregarProducto(id)

        val compra = compras.confirmarCompra()

        assertEquals(6.0, compra.compra.subtotal, 0.001)
        assertEquals(2.0, compra.compra.costoEnvio, 0.001)
        assertEquals(8.0, compra.compra.total, 0.001)
        assertEquals(2, compra.cantidadProductos)
        assertTrue(compra.compra.numeroPedido.startsWith("#"))
        assertTrue(carrito.observarItems().first().isEmpty())                        // carrito vacío
        assertEquals(3, productos.obtenerProducto(id)!!.stock)                       // stock descontado
        assertEquals(1, compras.observarHistorial().first().size)                    // historial conservado

        // El historial NO se borra al vaciar el carrito ni al eliminar el producto.
        productos.eliminarProducto(id)
        assertEquals("Pera", compras.observarHistorial().first().single().detalles.single().nombreProducto)
    }
}
