package com.example.mimarket.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.remote.api.ErrorApi
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.remote.model.ProductoRequest
import com.example.mimarket.data.repository.ProductoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del formulario: valida los datos, sube la imagen elegida a Firebase Storage y guarda
 * el producto en Cloud Firestore (crear o actualizar) mediante corrutinas.
 * Publica el resultado REAL: nunca informa éxito si Firestore no lo confirmó. Si las reglas de
 * seguridad bloquean la edición de un producto ajeno, muestra un mensaje claro.
 */
class ProductoFormViewModel(
    private val repositorio: ProductoRepository = MiMarketApplication.instancia.productoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoFormUiState())
    val uiState: StateFlow<ProductoFormUiState> = _uiState.asStateFlow()

    private var iniciado = false

    /** Prepara el formulario. Si [id] > 0 lee el producto de Firestore para mostrar sus datos actuales. */
    fun iniciar(id: Long) {
        if (iniciado) return
        iniciado = true
        if (id <= 0) return
        _uiState.update { it.copy(id = id) }
        cargarProducto()
    }

    /** Lee el producto de Firestore (también es el botón "Reintentar" de la pantalla de edición). */
    fun cargarProducto() {
        val id = _uiState.value.id
        _uiState.update { it.copy(cargando = true, errorMessage = null, sinConexion = false, productoNoExiste = false) }
        viewModelScope.launch {
            when (val r = repositorio.obtenerProductoRemoto(id)) {
                is ResultadoApi.Exito -> _uiState.update {
                    val p = r.datos
                    it.copy(
                        cargando = false,
                        nombre = p.nombre, descripcion = p.descripcion,
                        precio = p.precio.toString(), stock = p.stock.toString(),
                        imagen = p.imagen, categoria = p.categoria
                    )
                }
                is ResultadoApi.Fallo -> _uiState.update {
                    it.copy(
                        cargando = false,
                        errorMessage = r.error.mensaje,
                        sinConexion = r.error is ErrorApi.SinConexion || r.error is ErrorApi.Timeout,
                        productoNoExiste = r.error is ErrorApi.NoEncontrado
                    )
                }
            }
        }
    }

    fun cambiarNombre(v: String) = _uiState.update { it.copy(nombre = v, errorNombre = null) }
    fun cambiarDescripcion(v: String) = _uiState.update { it.copy(descripcion = v) }
    fun cambiarPrecio(v: String) = _uiState.update { it.copy(precio = v, errorPrecio = null) }
    fun cambiarStock(v: String) = _uiState.update { it.copy(stock = v, errorStock = null) }
    fun cambiarCategoria(v: String) = _uiState.update { it.copy(categoria = v) }

    /** El usuario eligió una imagen en la galería (null si canceló: se conserva la anterior). */
    fun seleccionarImagen(uri: Uri?) {
        if (uri != null) _uiState.update { it.copy(imagenLocal = uri) }
    }

    /** Quita la imagen recién elegida (vuelve a la imagen que ya tenía el producto). */
    fun quitarImagenSeleccionada() = _uiState.update { it.copy(imagenLocal = null) }

    /**
     * Botón "Guardar". Valida (nombre no vacío, precio > 0, stock >= 0); si es válido sube la imagen
     * (si se eligió una) y crea o actualiza el documento. Mientras envía, [ProductoFormUiState.enviando]
     * bloquea nuevos envíos.
     */
    fun guardar() {
        val s = _uiState.value
        if (s.enviando || s.cargando) return
        val errorNombre = if (s.nombre.isBlank()) "El nombre no puede estar vacío" else null
        val precio = s.precio.trim().replace(',', '.').toDoubleOrNull()
        val errorPrecio = if (precio == null || precio <= 0.0) "El precio debe ser mayor que cero" else null
        val stock = s.stock.trim().toIntOrNull()
        val errorStock = if (stock == null || stock < 0) "El stock no puede ser negativo" else null
        if (errorNombre != null || errorPrecio != null || errorStock != null) {
            _uiState.update { it.copy(errorNombre = errorNombre, errorPrecio = errorPrecio, errorStock = errorStock) }
            return
        }
        val datos = ProductoRequest(
            nombre = s.nombre.trim(),
            descripcion = s.descripcion.trim(),
            precio = precio!!,
            stock = stock!!,
            imagen = s.imagen.trim(),
            categoria = s.categoria.trim().ifEmpty { "General" }
        )
        _uiState.update { it.copy(enviando = true, errorMessage = null, sinConexion = false, permisoDenegado = false) }
        viewModelScope.launch {
            val resultado = if (s.esEdicion) repositorio.modificarProducto(s.id, datos, s.imagenLocal)
            else repositorio.registrarProducto(datos, s.imagenLocal)
            _uiState.update {
                when (resultado) {
                    is ResultadoApi.Exito -> it.copy(
                        enviando = false,
                        mensajeExito = if (s.esEdicion) "Producto actualizado correctamente" else "Producto registrado correctamente"
                    )
                    is ResultadoApi.Fallo -> it.copy(
                        enviando = false,
                        errorMessage = resultado.error.mensaje,
                        sinConexion = resultado.error is ErrorApi.SinConexion || resultado.error is ErrorApi.Timeout,
                        permisoDenegado = resultado.error is ErrorApi.PermisoDenegado,
                        productoNoExiste = resultado.error is ErrorApi.NoEncontrado
                    )
                }
            }
        }
    }
}
