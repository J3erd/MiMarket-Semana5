package com.example.mimarket.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.mimarket.model.Producto

/** Tabla "productos": copia local del catálogo de Firestore (permite usar el carrito con relaciones de Room). */
@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // clave primaria autoincremental
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val imagen: String,
    val categoria: String,
    val stock: Int,
    val creadorUid: String = "" // UID del usuario que registró el producto (viene de Firestore)
)

// Mapeadores: separan la capa de base de datos (Entity) del resto de la app (Model).
// Entity -> Model
fun ProductoEntity.aModelo(): Producto = Producto(id, nombre, descripcion, precio, imagen, categoria, stock, creadorUid)

// Model -> Entity
fun Producto.aEntidad(): ProductoEntity = ProductoEntity(id, nombre, descripcion, precio, imagen, categoria, stock, creadorUid)
