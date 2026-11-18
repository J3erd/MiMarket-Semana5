package com.example.mimarket.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local (SQLite mediante Room).
 * Desde la semana 5 la fuente de verdad es Cloud Firestore; Room conserva:
 *  - productos: copia local del catálogo (se actualiza sola con los Snapshots de Firestore).
 *  - carrito_items: carrito del dispositivo (relación con productos).
 *  - compras / detalle_compras: comprobante de la última compra hecha en este dispositivo.
 * Ya NO existe la tabla de usuarios ni se guardan contraseñas: las cuentas las gestiona Firebase Authentication.
 */
@Database(
    entities = [
        ProductoEntity::class,      // tabla productos
        ItemCarritoEntity::class,   // tabla carrito_items
        CompraEntity::class,        // tabla compras
        DetalleCompraEntity::class  // tabla detalle_compras
    ],
    version = 2,          // v2 (semana 5): productos.creadorUid y se elimina la tabla usuarios
    exportSchema = false  // no genera el archivo JSON del esquema
)
abstract class AppDatabase : RoomDatabase() {

    // Room genera la implementación de cada DAO automáticamente.
    abstract fun productoDao(): ProductoDao
    abstract fun carritoDao(): CarritoDao
    abstract fun compraDao(): CompraDao
    abstract fun detalleCompraDao(): DetalleCompraDao

    companion object {
        // @Volatile: el cambio de INSTANCIA es visible de inmediato para todos los hilos.
        @Volatile
        private var INSTANCIA: AppDatabase? = null

        // Nombre del archivo SQLite dentro del almacenamiento privado de la app.
        private const val NOMBRE_BD = "mimarket_db"

        /**
         * Patrón Singleton: devuelve la única instancia de la base de datos.
         * Si no existe, la crea dentro de un bloque synchronized (seguro entre hilos).
         */
        fun obtenerBaseDeDatos(contexto: Context): AppDatabase {
            return INSTANCIA ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    contexto.applicationContext, // contexto de aplicación (evita fugas de memoria)
                    AppDatabase::class.java,
                    NOMBRE_BD
                )
                    // Al pasar de la versión 1 (semana 4) a la 2 se recrean las tablas: los datos
                    // locales son solo una copia; el catálogo se vuelve a descargar de Firestore.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCIA = instancia
                instancia
            }
        }
    }
}
