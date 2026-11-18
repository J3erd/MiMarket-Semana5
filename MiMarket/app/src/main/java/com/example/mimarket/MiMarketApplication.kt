package com.example.mimarket

import android.app.Application
import com.example.mimarket.data.local.datastore.PreferenciasDataStore
import com.example.mimarket.data.local.room.AppDatabase
import com.example.mimarket.data.remote.api.ConectividadObserver
import com.example.mimarket.data.remote.firebase.FirebaseAuthManager
import com.example.mimarket.data.remote.firebase.FirestoreRepository
import com.example.mimarket.data.remote.firebase.StorageRepository
import com.example.mimarket.data.remote.location.LocationRepository
import com.example.mimarket.data.repository.CarritoRepository
import com.example.mimarket.data.repository.CompraRepository
import com.example.mimarket.data.repository.ProductoRepository
import com.example.mimarket.data.repository.TiendaRepository
import com.example.mimarket.data.repository.UsuarioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Clase Application: es lo PRIMERO que Android crea al abrir la app (antes que cualquier pantalla).
 * Su función es construir UNA sola vez las fuentes de datos (Room, DataStore, Firebase y ubicación)
 * y los repositorios, para que todos los ViewModel compartan las mismas instancias.
 * Está registrada en el AndroidManifest con android:name=".MiMarketApplication".
 */
class MiMarketApplication : Application() {

    // Base de datos Room (SQLite). "private set": solo esta clase puede asignarla.
    lateinit var baseDeDatos: AppDatabase
        private set

    // Almacén DataStore: último correo, categoría elegida, costo de envío y consentimiento de ubicación.
    lateinit var preferencias: PreferenciasDataStore
        private set

    /** Ámbito de corrutinas con la vida de la app: para trabajos que no deben cancelarse al cerrar una pantalla. */
    val ambitoApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Observa si el dispositivo tiene conexión a internet (ConnectivityManager + NetworkCallback). */
    val conectividad: ConectividadObserver by lazy { ConectividadObserver(this) }

    // ---- Semana 5: fuentes de datos remotas (capa data/remote). Solo las usan los repositorios. ----

    /** Firebase Authentication. */
    private val authManager: FirebaseAuthManager by lazy { FirebaseAuthManager() }

    /** Cloud Firestore. */
    private val firestoreRepository: FirestoreRepository by lazy { FirestoreRepository() }

    /** Firebase Storage. */
    private val storageRepository: StorageRepository by lazy { StorageRepository(this) }

    /** Ubicación del dispositivo (FusedLocationProviderClient). */
    private val locationRepository: LocationRepository by lazy { LocationRepository(this) }

    // ---- Repositorios (FACHADA): lo único que conocen los ViewModel. ----
    // Se crean de forma perezosa (lazy): solo cuando alguien los usa por primera vez.
    // Cada uno recibe las fuentes de datos que necesita (inyección por constructor).

    /** Repositorio de usuarios y sesión (registro, login, perfil, eliminar cuenta). */
    val usuarioRepository: UsuarioRepository by lazy {
        UsuarioRepository(authManager, firestoreRepository, preferencias, baseDeDatos)
    }

    /** Repositorio del catálogo (CRUD en Firestore, imágenes en Storage, copia local en Room). */
    val productoRepository: ProductoRepository by lazy {
        ProductoRepository(baseDeDatos.productoDao(), preferencias, firestoreRepository, storageRepository, authManager)
    }

    /** Repositorio del carrito (agregar, cambiar cantidad, eliminar). */
    val carritoRepository: CarritoRepository by lazy {
        CarritoRepository(baseDeDatos, baseDeDatos.carritoDao(), baseDeDatos.productoDao(), preferencias)
    }

    /** Repositorio de compras (confirmar compra e historial de pedidos en Firestore). */
    val compraRepository: CompraRepository by lazy {
        CompraRepository(
            baseDeDatos,
            baseDeDatos.carritoDao(),
            baseDeDatos.productoDao(),
            baseDeDatos.compraDao(),
            baseDeDatos.detalleCompraDao(),
            preferencias,
            firestoreRepository,
            authManager
        )
    }

    /** Repositorio de tiendas y ubicación (mapa). */
    val tiendaRepository: TiendaRepository by lazy {
        TiendaRepository(firestoreRepository, locationRepository)
    }

    /** Se ejecuta al iniciar el proceso de la app: inicializa Room y DataStore. */
    override fun onCreate() {
        super.onCreate()
        // Guarda la referencia global para que los ViewModel puedan obtener los repositorios.
        instancia = this
        // Crea (o abre, si ya existe) el archivo de base de datos "mimarket_db".
        baseDeDatos = AppDatabase.obtenerBaseDeDatos(this)
        // Prepara el acceso al archivo de preferencias de DataStore.
        preferencias = PreferenciasDataStore(this)
    }

    companion object {
        /** Acceso global a la instancia de Application (usado por los valores por defecto de los ViewModel). */
        lateinit var instancia: MiMarketApplication
            private set
    }
}
