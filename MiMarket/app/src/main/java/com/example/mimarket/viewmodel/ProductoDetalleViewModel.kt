package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.repository.CarritoRepository
import com.example.mimarket.data.repository.ProductoRepository
import com.example.mimarket.model.Producto
import com.example.mimarket.util.OperacionInvalidaException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ViewModel del detalle: observa el producto y el carrito, agrega unidades y elimina el producto. */
class ProductoDetalleViewModel(
    private val productoRepository: ProductoRepository = MiMarketApplication.instancia.productoRepository,
    private val carritoRepository: CarritoRepository = MiMarketApplication.instancia.carritoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoDetalleUiState())
    val uiState: StateFlow<ProductoDetalleUiState> = _uiState.asStateFlow()

    private var iniciado = false

    /** Empieza a observar el producto [id] (copia local sincronizada con el servidor) y el carrito. */
    fun iniciar(id: Long) {
        if (iniciado) return
        iniciado = true
        viewModelScope.launch {
            combine(productoRepository.observarProductos(), carritoRepository.observarItems()) { lista, items ->
                lista.firstOrNull { it.id == id } to (items.firstOrNull { it.producto.id == id }?.item?.cantidad ?: 0)
            }.collect { (producto, enCarrito) ->
                _uiState.update { it.copy(producto = producto, cantidadEnCarrito = enCarrito, cargando = false) }
            }
        }
    }

    /** Botón "+" del selector (no supera el stock disponible). */
    fun aumentar() = _uiState.update {
        val max = it.producto?.stock ?: 1
        it.copy(cantidadSeleccionada = (it.cantidadSeleccionada + 1).coerceAtMost(maxOf(max, 1)))
    }

    /** Botón "−" del selector (mínimo 1). */
    fun disminuir() = _uiState.update { it.copy(cantidadSeleccionada = (it.cantidadSeleccionada - 1).coerceAtLeast(1)) }

    /** "Agregar al carrito": suma las unidades elegidas; la regla de stock la valida el repositorio. */
    fun agregarAlCarrito() {
        val s = _uiState.value
        val producto = s.producto ?: return
        viewModelScope.launch {
            try {
                repeat(s.cantidadSeleccionada) { carritoRepository.agregarProducto(producto.id) }
                _uiState.update { it.copy(mensaje = "Agregado al carrito", cantidadSeleccionada = 1) }
            } catch (e: OperacionInvalidaException) {
                _uiState.update { it.copy(mensaje = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(mensaje = "Ocurrió un problema. Inténtalo de nuevo") }
            }
        }
    }

    /** Botón "Eliminar": si el producto está en el carrito no se puede eliminar (misma regla del catálogo). */
    fun solicitarEliminar() {
        val s = _uiState.value
        val producto = s.producto ?: return
        if (producto.stock > 0) {
            // Regla de negocio: solo se eliminan productos sin stock
            _uiState.update { it.copy(avisoEliminar = "El producto \"${producto.nombre}\" cuenta con ${producto.stock} unidades en stock, por lo que no puede ser eliminado.") }
        } else if (s.cantidadEnCarrito > 0) {
            _uiState.update { it.copy(avisoEliminar = "El producto \"${producto.nombre}\" se encuentra en su carrito de compras. Debe retirarlo del carrito antes de eliminarlo.") }
        } else {
            _uiState.update { it.copy(confirmandoEliminar = true) }
        }
    }

    fun cerrarAvisoEliminar() = _uiState.update { it.copy(avisoEliminar = null) }

    fun cancelarEliminar() = _uiState.update { it.copy(confirmandoEliminar = false) }

    /** Confirma: elimina en Firestore; si las reglas lo impiden (PERMISSION_DENIED) se muestra el motivo. */
    fun confirmarEliminar() {
        val producto = _uiState.value.producto ?: return
        if (_uiState.value.operando) return
        _uiState.update { it.copy(confirmandoEliminar = false, operando = true) }
        viewModelScope.launch {
            when (val r = productoRepository.borrarProducto(producto.id)) {
                is ResultadoApi.Exito -> _uiState.update { it.copy(operando = false, eliminado = true) }
                is ResultadoApi.Fallo -> _uiState.update { it.copy(operando = false, mensaje = r.error.mensaje) }
            }
        }
    }

    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }
}
