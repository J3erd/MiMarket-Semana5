package com.example.mimarket.data.repository

import android.net.Uri
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.local.datastore.PreferenciasDataStore
import com.example.mimarket.data.local.room.DatosIniciales
import com.example.mimarket.data.local.room.ProductoDao
import com.example.mimarket.data.local.room.aEntidad
import com.example.mimarket.data.local.room.aModelo
import com.example.mimarket.data.remote.api.ProductoApiService
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.remote.api.RetrofitClient
import com.example.mimarket.data.remote.firebase.FirebaseAuthManager
import com.example.mimarket.data.remote.firebase.FirestoreRepository
import com.example.mimarket.data.remote.firebase.RecursoNoEncontradoException
import com.example.mimarket.data.remote.firebase.SesionNoIniciadaException
import com.example.mimarket.data.remote.firebase.StorageRepository
import com.example.mimarket.data.remote.firebase.llamarFirebase
import com.example.mimarket.data.remote.model.ProductoRequest
import com.example.mimarket.model.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repositorio del catálogo (FACHADA).
 * Los ViewModel solo conocen esta clase. Por dentro coordina:
 *  - Cloud Firestore: CRUD de productos y escucha en tiempo real (fuente de verdad).
 *  - Firebase Storage: subida de la imagen elegida en la galería.
 *  - Room: copia local del catálogo (la necesita el carrito) que se actualiza con cada Snapshot.
 *  - Retrofit (backend Node.js de la semana 4): solo para importar el catálogo inicial a Firestore.
 *  - DataStore: categoría elegida.
 */
class ProductoRepository(
    private val productoDao: ProductoDao,
    private val preferencias: PreferenciasDataStore,
    private val firestore: FirestoreRepository = FirestoreRepository(),
    private val storage: StorageRepository = StorageRepository(MiMarketApplication.instancia),
    private val auth: FirebaseAuthManager = FirebaseAuthManager(),
    private val api: ProductoApiService = RetrofitClient.productoApi
) {
    /** UID del usuario autenticado (para marcar en pantalla los productos propios). */
    val uidActual: String?
        get() = auth.uidActual

    // ---- Copia local (Room) ----

    /** Read: catálogo local reactivo. Emite una lista nueva cada vez que cambia la tabla productos. */
    fun observarProductos(): Flow<List<Producto>> =
        productoDao.observarTodos().map { lista -> lista.map { it.aModelo() } }

    /** Read: busca un producto en la copia local (null si no existe). */
    suspend fun obtenerProducto(id: Long): Producto? = productoDao.obtenerPorId(id)?.aModelo()

    /** Create local: inserta un producto y devuelve el id generado (lo usan las pruebas de Room). */
    suspend fun crearProducto(producto: Producto): Long = productoDao.insertar(producto.aEntidad())

    /** Update local: cambia solo el precio de un producto. */
    suspend fun actualizarPrecio(id: Long, nuevoPrecio: Double) = productoDao.actualizarPrecio(id, nuevoPrecio)

    /** Delete local: elimina el producto; también lo retira del carrito (clave foránea con CASCADE). */
    suspend fun eliminarProducto(id: Long) = productoDao.eliminarPorId(id)

    // ---- Cloud Firestore (fuente de verdad) ----

    /**
     * TIEMPO REAL: escucha la colección "productos" (Snapshots). Cada vez que cualquier usuario
     * agrega, edita o elimina un producto, Firestore envía la lista nueva; aquí se refleja en Room
     * y, como la pantalla observa Room, el catálogo se redibuja solo (sin refrescar).
     * Emite la cantidad de productos recibidos. Si las reglas bloquean la lectura, el flujo falla.
     */
    fun observarCatalogoEnTiempoReal(): Flow<Int> = firestore.observarProductos().map { lista ->
        productoDao.upsertar(lista.map { it.aEntidad() })
        productoDao.eliminarNoIncluidos(lista.map { it.id })
        lista.size
    }

    /** Read: producto actual según Firestore (NoEncontrado si ya no existe). */
    suspend fun obtenerProductoRemoto(id: Long): ResultadoApi<Producto> = llamarFirebase {
        firestore.obtenerProducto(id) ?: throw RecursoNoEncontradoException()
    }

    /**
     * Create: si se eligió una imagen de la galería ([imagenLocal]) primero se sube a Firebase Storage
     * y su URL de descarga se guarda en el documento. El producto queda marcado con el UID del creador.
     */
    suspend fun registrarProducto(datos: ProductoRequest, imagenLocal: Uri?): ResultadoApi<Producto> =
        llamarFirebase(tiempoMaximoMs = TIEMPO_CON_IMAGEN_MS) {
            val uid = auth.uidActual ?: throw SesionNoIniciadaException()
            val urlImagen = if (imagenLocal != null) storage.subirImagenProducto(uid, imagenLocal) else datos.imagen
            val producto = Producto(
                id = System.currentTimeMillis(), // id único; también es el id del documento
                nombre = datos.nombre,
                descripcion = datos.descripcion,
                precio = datos.precio,
                imagen = urlImagen,
                categoria = datos.categoria,
                stock = datos.stock,
                creadorUid = uid
            )
            try {
                firestore.crearProducto(producto)
            } catch (e: Exception) {
                if (imagenLocal != null) storage.eliminarImagen(urlImagen) // no dejar imágenes huérfanas
                throw e
            }
            producto
        }

    /**
     * Update: si el usuario no es el creador ni administrador, las reglas de seguridad de Firestore
     * rechazan la operación (PERMISSION_DENIED) y se devuelve ErrorApi.PermisoDenegado.
     */
    suspend fun modificarProducto(id: Long, datos: ProductoRequest, imagenLocal: Uri?): ResultadoApi<Producto> =
        llamarFirebase(tiempoMaximoMs = TIEMPO_CON_IMAGEN_MS) {
            val uid = auth.uidActual ?: throw SesionNoIniciadaException()
            val anterior = firestore.obtenerProducto(id) ?: throw RecursoNoEncontradoException()
            val urlImagen = if (imagenLocal != null) storage.subirImagenProducto(uid, imagenLocal) else datos.imagen
            val actualizado = anterior.copy(
                nombre = datos.nombre,
                descripcion = datos.descripcion,
                precio = datos.precio,
                imagen = urlImagen,
                categoria = datos.categoria,
                stock = datos.stock
            )
            try {
                firestore.actualizarProducto(actualizado)
            } catch (e: Exception) {
                if (imagenLocal != null) storage.eliminarImagen(urlImagen)
                throw e
            }
            // La imagen anterior ya no se usa: se borra de Storage.
            if (imagenLocal != null && anterior.imagen != urlImagen) storage.eliminarImagen(anterior.imagen)
            actualizado
        }

    /** Delete: elimina en Firestore (creador o admin), borra su imagen de Storage y la copia local. */
    suspend fun borrarProducto(id: Long): ResultadoApi<Unit> = llamarFirebase {
        val imagen = productoDao.obtenerPorId(id)?.imagen.orEmpty()
        firestore.eliminarProducto(id)
        storage.eliminarImagen(imagen)
        productoDao.eliminarPorId(id)
    }

    /**
     * Migración del catálogo de Node.js a Firestore: lee los productos del backend de la semana 4
     * (GET /api/productos) y los escribe en la colección "productos". Si el servidor Node no está
     * encendido se usan los productos de ejemplo. Devuelve cuántos importó y de dónde.
     */
    suspend fun importarCatalogoInicial(): ResultadoApi<Pair<Int, String>> = llamarFirebase(tiempoMaximoMs = 40_000) {
        val uid = auth.uidActual ?: throw SesionNoIniciadaException()
        val delServidor = runCatching { api.listar().map { it.aModelo() } }.getOrNull().orEmpty()
        val origen = if (delServidor.isNotEmpty()) "el servidor Node.js" else "los datos de ejemplo"
        val lista = delServidor.ifEmpty { DatosIniciales.productos() }
        lista.forEachIndexed { indice, p ->
            val id = if (p.id > 0) p.id else (indice + 1).toLong()
            firestore.crearProducto(p.copy(id = id, creadorUid = uid))
        }
        lista.size to origen
    }

    // ---- Preferencia del usuario (DataStore) ----

    /** Categoría seleccionada en el catálogo (flujo reactivo). */
    val flujoCategoriaFiltro: Flow<String> = preferencias.flujoCategoriaFiltro

    /** Guarda la categoría seleccionada ("" = todas). */
    suspend fun guardarCategoriaFiltro(categoria: String) = preferencias.guardarCategoriaFiltro(categoria)

    private companion object {
        // Subir una imagen puede tardar más que una escritura normal.
        const val TIEMPO_CON_IMAGEN_MS = 60_000L
    }
}
