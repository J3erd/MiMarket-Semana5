package com.example.mimarket.data.remote.model

import com.example.mimarket.model.Producto

/**
 * Producto tal como lo envía/recibe el backend (JSON). Todos los campos tienen valor por defecto
 * para que Gson no deje nulos si el servidor omite alguno.
 */
data class ProductoDto(
    val id: Long = 0,
    val nombre: String? = null,
    val descripcion: String? = null,
    val precio: Double = 0.0,
    val stock: Int = 0,
    val imagen: String? = null,
    val categoria: String? = null
) {
    /** Convierte el JSON del servidor en el modelo de la app. */
    fun aModelo() = Producto(
        id = id,
        nombre = nombre.orEmpty(),
        descripcion = descripcion.orEmpty(),
        precio = precio,
        imagen = imagen.orEmpty(),
        categoria = categoria?.takeIf { it.isNotBlank() } ?: "General",
        stock = stock
    )
}

/** Cuerpo JSON de POST /api/productos y PUT /api/productos/:id. */
data class ProductoRequest(
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val stock: Int,
    val imagen: String,
    val categoria: String
)

/** Cuerpo de PATCH /api/productos/:id/stock. */
data class DescuentoStockRequest(val cantidad: Int)

/** Respuesta de DELETE /api/productos/:id. */
data class EliminarRespuesta(val mensaje: String? = null)

/** Forma de los errores del backend: {"error": "texto"}. */
data class ErrorRespuesta(val error: String? = null)
