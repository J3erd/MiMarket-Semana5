package com.example.mimarket.data.remote.firebase

import com.example.mimarket.model.EstadoCompra
import com.example.mimarket.model.Pedido
import com.example.mimarket.model.PedidoItem
import com.example.mimarket.model.Producto
import com.example.mimarket.model.RolUsuario
import com.example.mimarket.model.Tienda
import com.example.mimarket.model.Usuario
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Fuente de datos remota: Cloud Firestore. Es la ÚNICA clase que habla con FirebaseFirestore.
 *
 * Estructura de la base de datos:
 *   productos/{id}                 catálogo (lo leen todos los usuarios autenticados)
 *   tiendas/{id}                   tiendas físicas con sus coordenadas
 *   usuarios/{uid}                 datos personales de cada usuario
 *   usuarios/{uid}/pedidos/{id}    historial de compras (subcolección privada del usuario)
 *
 * Las funciones "observar..." usan addSnapshotListener: Firestore avisa en TIEMPO REAL cada vez que
 * otro usuario agrega, edita o elimina un documento, sin que la app tenga que refrescar.
 */
class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val productos get() = db.collection("productos")
    private val tiendas get() = db.collection("tiendas")
    private fun usuario(uid: String) = db.collection("usuarios").document(uid)
    private fun pedidos(uid: String) = usuario(uid).collection("pedidos")

    // ------------------------------------------------------------------ Productos (CRUD)

    /** Read en tiempo real: emite el catálogo completo cada vez que cambia la colección (Snapshots). */
    fun observarProductos(): Flow<List<Producto>> = callbackFlow {
        val registro = productos.addSnapshotListener { instantanea, error ->
            if (error != null) {
                close(error) // p. ej. PERMISSION_DENIED si las reglas bloquean la lectura
                return@addSnapshotListener
            }
            trySend(instantanea?.documents.orEmpty().mapNotNull { it.aProducto() })
        }
        // Al dejar de observar se quita el listener (evita fugas y lecturas innecesarias).
        awaitClose { registro.remove() }
    }

    /** Read: un producto por id (null si no existe). */
    suspend fun obtenerProducto(id: Long): Producto? =
        productos.document(id.toString()).get().await().aProducto()

    /** Create: guarda el producto en productos/{id}. */
    suspend fun crearProducto(producto: Producto) {
        productos.document(producto.id.toString()).set(producto.aMapa()).await()
    }

    /**
     * Update: modifica los datos editables. No toca creadorUid.
     * Si el usuario no es el creador ni administrador, las reglas responden PERMISSION_DENIED.
     */
    suspend fun actualizarProducto(producto: Producto) {
        productos.document(producto.id.toString()).update(
            mapOf(
                "nombre" to producto.nombre,
                "descripcion" to producto.descripcion,
                "precio" to producto.precio,
                "stock" to producto.stock,
                "imagen" to producto.imagen,
                "categoria" to producto.categoria
            )
        ).await()
    }

    /** Delete: elimina el documento (solo el creador o un administrador, según las reglas). */
    suspend fun eliminarProducto(id: Long) {
        productos.document(id.toString()).delete().await()
    }

    /** Resta unidades al stock después de una compra (operación atómica en el servidor). */
    suspend fun descontarStock(id: Long, cantidad: Int) {
        productos.document(id.toString()).update("stock", FieldValue.increment(-cantidad.toLong())).await()
    }

    // ------------------------------------------------------------------ Usuarios (datos personales)

    /** Crea o reemplaza el documento usuarios/{uid}. */
    suspend fun guardarUsuario(u: Usuario) {
        usuario(u.uid).set(
            mapOf(
                "uid" to u.uid,
                "email" to u.email,
                "nombre" to u.nombre,
                "rol" to u.rol,
                "direccionEntrega" to u.direccionEntrega,
                "aceptoPrivacidad" to u.aceptoPrivacidad,
                "fechaRegistro" to u.fechaRegistro
            )
        ).await()
    }

    /** Lee los datos del usuario (null si aún no tiene documento). */
    suspend fun obtenerUsuario(uid: String): Usuario? = usuario(uid).get().await().aUsuario()

    /** Datos del usuario en tiempo real (pantalla de perfil). */
    fun observarUsuario(uid: String): Flow<Usuario?> = callbackFlow {
        val registro = usuario(uid).addSnapshotListener { doc, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(doc?.aUsuario())
        }
        awaitClose { registro.remove() }
    }

    /** Registra que el usuario aceptó el aviso de privacidad (pantalla "Aviso de Privacidad"). */
    suspend fun marcarAvisoAceptado(uid: String) {
        usuario(uid).update(mapOf("aceptoPrivacidad" to true, "fechaAceptacionPrivacidad" to System.currentTimeMillis())).await()
    }

    /** Guarda la dirección de entrega elegida en el mapa. */
    suspend fun actualizarDireccionEntrega(uid: String, direccion: String) {
        usuario(uid).update("direccionEntrega", direccion).await()
    }

    /**
     * Derecho de eliminación: borra TODOS los datos personales del usuario en Firestore
     * (cada pedido de su historial y su documento de usuario).
     */
    suspend fun eliminarDatosUsuario(uid: String) {
        val documentos = pedidos(uid).get().await().documents
        // Un lote admite hasta 500 operaciones: se borra por grupos.
        documentos.chunked(400).forEach { grupo ->
            val lote = db.batch()
            grupo.forEach { lote.delete(it.reference) }
            lote.commit().await()
        }
        usuario(uid).delete().await()
    }

    // ------------------------------------------------------------------ Pedidos (subcolección)

    /** Guarda un pedido en usuarios/{uid}/pedidos/{id}. */
    suspend fun guardarPedido(uid: String, pedido: Pedido) {
        pedidos(uid).document(pedido.id).set(
            mapOf(
                "numeroPedido" to pedido.numeroPedido,
                "fecha" to pedido.fecha,
                "subtotal" to pedido.subtotal,
                "costoEnvio" to pedido.costoEnvio,
                "total" to pedido.total,
                "estado" to pedido.estado,
                "items" to pedido.items.map {
                    mapOf(
                        "productoId" to it.productoId,
                        "nombreProducto" to it.nombreProducto,
                        "cantidad" to it.cantidad,
                        "precioUnitario" to it.precioUnitario,
                        "subtotal" to it.subtotal
                    )
                }
            )
        ).await()
    }

    /** Historial del usuario en tiempo real, del pedido más reciente al más antiguo. */
    fun observarPedidos(uid: String): Flow<List<Pedido>> = callbackFlow {
        val registro = pedidos(uid).orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { instantanea, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(instantanea?.documents.orEmpty().map { it.aPedido() })
            }
        awaitClose { registro.remove() }
    }

    // ------------------------------------------------------------------ Tiendas (mapa)

    /** Tiendas físicas en tiempo real. */
    fun observarTiendas(): Flow<List<Tienda>> = callbackFlow {
        val registro = tiendas.addSnapshotListener { instantanea, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(instantanea?.documents.orEmpty().map { it.aTienda() })
        }
        awaitClose { registro.remove() }
    }

    /** Publica (o reemplaza) las tiendas. Solo lo permite la regla de seguridad a un administrador. */
    suspend fun guardarTiendas(lista: List<Tienda>) {
        val lote = db.batch()
        lista.forEach { t ->
            lote.set(
                tiendas.document(t.id),
                mapOf(
                    "nombre" to t.nombre,
                    "direccion" to t.direccion,
                    "horario" to t.horario,
                    "latitud" to t.latitud,
                    "longitud" to t.longitud
                )
            )
        }
        lote.commit().await()
    }
}

// ---------------------------------------------------------------------- Mapeadores Firestore <-> Model
// Se convierten a mano (en vez de toObject) para controlar los tipos numéricos y los valores ausentes.

private fun DocumentSnapshot.numero(campo: String): Number? = get(campo) as? Number

private fun Producto.aMapa(): Map<String, Any> = mapOf(
    "id" to id,
    "nombre" to nombre,
    "descripcion" to descripcion,
    "precio" to precio,
    "stock" to stock,
    "imagen" to imagen,
    "categoria" to categoria,
    "creadorUid" to creadorUid
)

private fun DocumentSnapshot.aProducto(): Producto? {
    if (!exists()) return null
    val id = numero("id")?.toLong() ?: this.id.toLongOrNull() ?: return null
    return Producto(
        id = id,
        nombre = getString("nombre").orEmpty(),
        descripcion = getString("descripcion").orEmpty(),
        precio = numero("precio")?.toDouble() ?: 0.0,
        imagen = getString("imagen").orEmpty(),
        categoria = getString("categoria")?.takeIf { it.isNotBlank() } ?: "General",
        stock = numero("stock")?.toInt() ?: 0,
        creadorUid = getString("creadorUid").orEmpty()
    )
}

private fun DocumentSnapshot.aUsuario(): Usuario? {
    if (!exists()) return null
    return Usuario(
        uid = id,
        email = getString("email").orEmpty(),
        nombre = getString("nombre").orEmpty(),
        rol = getString("rol") ?: RolUsuario.CLIENTE,
        direccionEntrega = getString("direccionEntrega").orEmpty(),
        aceptoPrivacidad = getBoolean("aceptoPrivacidad") ?: false,
        fechaRegistro = numero("fechaRegistro")?.toLong() ?: 0
    )
}

private fun DocumentSnapshot.aPedido(): Pedido {
    val filas = (get("items") as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
    return Pedido(
        id = id,
        numeroPedido = getString("numeroPedido").orEmpty(),
        fecha = numero("fecha")?.toLong() ?: 0,
        subtotal = numero("subtotal")?.toDouble() ?: 0.0,
        costoEnvio = numero("costoEnvio")?.toDouble() ?: 0.0,
        total = numero("total")?.toDouble() ?: 0.0,
        estado = getString("estado") ?: EstadoCompra.COMPLETADA,
        items = filas.map { fila ->
            PedidoItem(
                productoId = (fila["productoId"] as? Number)?.toLong() ?: 0,
                nombreProducto = fila["nombreProducto"] as? String ?: "",
                cantidad = (fila["cantidad"] as? Number)?.toInt() ?: 0,
                precioUnitario = (fila["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (fila["subtotal"] as? Number)?.toDouble() ?: 0.0
            )
        }
    )
}

private fun DocumentSnapshot.aTienda(): Tienda = Tienda(
    id = id,
    nombre = getString("nombre").orEmpty(),
    direccion = getString("direccion").orEmpty(),
    horario = getString("horario").orEmpty(),
    latitud = numero("latitud")?.toDouble() ?: 0.0,
    longitud = numero("longitud")?.toDouble() ?: 0.0
)
