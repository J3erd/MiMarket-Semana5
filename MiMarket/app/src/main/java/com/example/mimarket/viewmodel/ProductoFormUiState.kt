package com.example.mimarket.viewmodel

import android.net.Uri

/**
 * Estado del formulario de producto (registrar / editar).
 * Los campos numéricos se guardan como texto para poder validarlos y mostrar errores por campo.
 *
 * @property imagen Imagen actual del producto (URL de Firebase Storage o nombre de drawable).
 * @property imagenLocal Imagen recién elegida en la galería; se sube a Storage al pulsar "Guardar".
 */
data class ProductoFormUiState(
    val id: Long = 0, // 0 = registrando; >0 = editando
    val nombre: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val stock: String = "",
    val imagen: String = "",
    val imagenLocal: Uri? = null,
    val categoria: String = "",
    val errorNombre: String? = null,
    val errorPrecio: String? = null,
    val errorStock: String? = null,
    val cargando: Boolean = false,      // cargando datos del producto a editar
    val enviando: Boolean = false,      // subida/escritura en curso (bloquea el botón: sin envíos duplicados)
    val errorMessage: String? = null,   // error de Firebase (mensaje comprensible)
    val sinConexion: Boolean = false,
    val permisoDenegado: Boolean = false,  // las reglas de seguridad bloquearon la edición (producto ajeno)
    val productoNoExiste: Boolean = false, // el producto ya fue eliminado
    val mensajeExito: String? = null    // si no es null, Firestore confirmó la operación
) {
    val esEdicion: Boolean get() = id > 0
}
