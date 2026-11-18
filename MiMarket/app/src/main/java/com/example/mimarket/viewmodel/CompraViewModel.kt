package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.remote.firebase.errorDeFirebase
import com.example.mimarket.data.repository.CompraRepository
import com.example.mimarket.model.CompraConDetalles
import com.example.mimarket.util.OperacionInvalidaException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel de compras.
 * Funciones: confirmar la compra (validando carrito vacío y stock), cargar la compra realizada
 * para mostrar su resumen y exponer el historial de compras guardado en Room.
 */
class CompraViewModel(
    private val compraRepository: CompraRepository = MiMarketApplication.instancia.compraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompraUiState())
    /** Estado observable por la pantalla (StateFlow). */
    val uiState: StateFlow<CompraUiState> = _uiState.asStateFlow()

    private val estadoActual: CompraUiState
        get() = _uiState.value

    // Referencia a la escucha del historial, para no iniciarla dos veces.
    private var trabajoHistorial: Job? = null

    /**
     * Botón "Confirmar compra". Si el carrito está vacío o falta stock, publica el error y no registra nada;
     * si todo va bien, deja compraRealizada = true y la pantalla navega a "Compra realizada".
     */
    fun confirmarCompra() {
        // Evita confirmar dos veces (doble toque o compra ya hecha).
        if (estadoActual.isLoading || estadoActual.compraRealizada) return
        _uiState.value = estadoActual.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val compra = compraRepository.confirmarCompra()
                _uiState.value = aEstadoCompra(compra)
                // Semana 5: guarda el pedido en Firestore (usuarios/{uid}/pedidos) y descuenta el stock en la nube.
                // Se lanza en el ámbito de la aplicación para que no se cancele al cerrar esta pantalla.
                MiMarketApplication.instancia.ambitoApp.launch {
                    compraRepository.registrarPedidoEnNube(compra)
                }
            } catch (e: OperacionInvalidaException) {
                // Error de regla de negocio (carrito vacío, stock insuficiente).
                _uiState.value = estadoActual.copy(isLoading = false, errorMessage = e.message)
            } catch (e: Exception) {
                _uiState.value = estadoActual.copy(isLoading = false, errorMessage = "No se pudo registrar la compra")
            }
        }
    }

    /** Carga desde Room una compra ya registrada (pantalla "Compra realizada") usando su id. */
    fun cargarCompra(compraId: Long) {
        // Si ya está cargada (por ejemplo tras girar la pantalla) no se vuelve a pedir.
        if (estadoActual.compraRealizada && estadoActual.compraId == compraId) return
        _uiState.value = estadoActual.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val compra = compraRepository.obtenerCompra(compraId)
                _uiState.value = if (compra != null) {
                    aEstadoCompra(compra)
                } else {
                    estadoActual.copy(isLoading = false, errorMessage = "No se encontró la compra")
                }
            } catch (e: Exception) {
                _uiState.value = estadoActual.copy(isLoading = false, errorMessage = "No se pudo cargar la compra")
            }
        }
    }

    /** Observa el historial de compras; se actualiza solo cuando se registra una nueva compra. */
    fun cargarHistorial() {
        if (trabajoHistorial?.isActive == true) return
        _uiState.value = estadoActual.copy(isLoading = true)
        trabajoHistorial = viewModelScope.launch {
            // Semana 5: el historial se lee en tiempo real de Firestore (usuarios/{uid}/pedidos).
            compraRepository.observarPedidos()
                .catch { e ->
                    _uiState.value = estadoActual.copy(
                        isLoading = false,
                        // Mensaje comprensible (p. ej. PERMISSION_DENIED -> "No tienes permiso...")
                        errorMessage = errorDeFirebase(e).mensaje
                    )
                }
                .collect { lista ->
                    _uiState.value = estadoActual.copy(historial = lista, isLoading = false, errorMessage = null)
                }
        }
    }

    /** La vista avisa que ya mostró el error para no repetirlo. */
    fun errorMostrado() {
        _uiState.value = estadoActual.copy(errorMessage = null)
    }

    /** Convierte una compra de dominio en el UiState (número de pedido, cantidad de productos, total). */
    private fun aEstadoCompra(compra: CompraConDetalles) = estadoActual.copy(
        compraRealizada = true,
        compraId = compra.compra.id,
        numeroPedido = compra.compra.numeroPedido,
        cantidadProductos = compra.cantidadProductos,
        total = compra.compra.total,
        isLoading = false,
        errorMessage = null
    )
}
