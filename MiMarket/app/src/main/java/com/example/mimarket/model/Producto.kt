package com.example.mimarket.model

/**
 * Modelo de dominio de un producto del minimarket (mapeado al documento productos/{id} de Firestore).
 *
 * @property id Identificador único. En Firestore es también el id del documento (como texto).
 * @property nombre Nombre del producto.
 * @property descripcion Descripción corta.
 * @property precio Precio unitario en soles (S/).
 * @property imagen URL de descarga de Firebase Storage, o nombre de un drawable local (ej. "prod_manzana").
 * @property categoria Categoría para filtrar el catálogo (ej. "Frutas").
 * @property stock Unidades disponibles.
 * @property creadorUid UID (Firebase Auth) del usuario que registró el producto. Las reglas de
 *   seguridad de Firestore lo usan para permitir editar/eliminar solo al creador o a un administrador.
 */
// Data class: Kotlin genera automáticamente equals, hashCode, toString y copy().
data class Producto(
    val id: Long = 0,                // 0 = aún no guardado
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val imagen: String,
    val categoria: String,
    val stock: Int,
    val creadorUid: String = ""
)
