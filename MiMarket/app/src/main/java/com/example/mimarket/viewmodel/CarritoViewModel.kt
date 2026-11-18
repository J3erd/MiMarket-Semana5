package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.repository.CarritoRepository
import com.example.mimarket.data.repository.UsuarioRepository
import com.example.mimarket.model.ItemCarritoConProducto
import com.example.mimarket.util.CalculadoraCarrito
import com.example.mimarket.util.OperacionInvalidaException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel del resumen de compra.
 * Funciones: exponer el carrito con subtotal por producto, subtotal general, costo de envío y total
 * (datos derivados), y procesar modificar cantidades / eliminar productos. Todo se guarda en Room.
 */
class CarritoViewModel(
    private val carritoRepository: CarritoRepository = MiMarketApplication.instancia.carritoRepository,
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CarritoUiState())
    /** Estado observable por la pantalla (StateFlow). */
    val uiState: StateFlow<CarritoUiState> = _uiState.asStateFlow()

    private val estadoActual: CarritoUiState
        get() = _uiState.value

    init {
        observarCarrito()
        // Dirección de entrega guardada en Firestore (se elige en el mapa)
        viewModelScope.launch {
            usuarioRepository.observarPerfil().collect { u ->
                _uiState.value = estadoActual.copy(direccionEntrega = u?.direccionEntrega.orEmpty())
            }
        }
    }

    /** Escucha el carrito y el costo de envío; ante cualquier cambio recalcula todos los importes. */
    private fun observarCarrito() {
        viewModelScope.launch {
            combine(
                carritoRepository.observarItems(),
                carritoRepository.observarCostoEnvio()
            ) { items, envioConfigurado -> items to envioConfigurado }
                .catch { e ->
                    _uiState.value = estadoActual.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "No se pudo cargar el carrito"
                    )
                }
                .collect { (items, envioConfigurado) -> _uiState.value = construirEstado(items, envioConfigurado) }
        }
    }

    /** Calcula la información derivada: subtotal general, costo de envío y total. */
    private fun construirEstado(items: List<ItemCarritoConProducto>, envioConfigurado: Double): CarritoUiState {
        val subtotal = CalculadoraCarrito.subtotal(items)                       // suma de cantidad x precio
        val envio = CalculadoraCarrito.costoEnvio(subtotal, envioConfigurado)   // 0 si el carrito está vacío
        return estadoActual.copy(
            items = items,
            cantidadTotal = CalculadoraCarrito.cantidadTotal(items),
            subtotal = subtotal,
            costoEnvio = envio,
            total = CalculadoraCarrito.total(subtotal, envio),                  // subtotal + envío
            isLoading = false,
            errorMessage = null
        )
    }

    /** Botón "+": suma una unidad al renglón. */
    fun incrementar(item: ItemCarritoConProducto) = ejecutar { carritoRepository.incrementar(item.item.id) }

    /** Botón "−": resta una unidad (con 1 unidad, el renglón se elimina). */
    fun disminuir(item: ItemCarritoConProducto) = ejecutar { carritoRepository.disminuir(item.item.id) }

    /** Icono de papelera: elimina el producto del carrito. */
    fun eliminar(item: ItemCarritoConProducto) = ejecutar { carritoRepository.eliminarItem(item.item.id) }

    /** La vista avisa que ya mostró el mensaje. */
    fun mensajeMostrado() {
        _uiState.value = estadoActual.copy(mensaje = null)
    }

    /** Ejecuta una acción y muestra los errores de negocio (ej. stock insuficiente) como mensaje. */
    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                accion()
            } catch (e: OperacionInvalidaException) {
                _uiState.value = estadoActual.copy(mensaje = e.message)
            } catch (e: Exception) {
                _uiState.value = estadoActual.copy(mensaje = "Ocurrió un problema. Inténtalo de nuevo")
            }
        }
    }
}
