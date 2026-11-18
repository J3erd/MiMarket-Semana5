package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.repository.CarritoRepository
import com.example.mimarket.data.repository.ProductoRepository
import com.example.mimarket.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del Home: saludo con el nombre del usuario, accesos rápidos y ofertas destacadas.
 * También mantiene encendida la escucha en tiempo real del catálogo (Firestore -> Room).
 */
class HomeViewModel(
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository,
    private val productoRepository: ProductoRepository = MiMarketApplication.instancia.productoRepository,
    private val carritoRepository: CarritoRepository = MiMarketApplication.instancia.carritoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Nombre del usuario (datos personales en Firestore)
        viewModelScope.launch {
            usuarioRepository.observarPerfil().collect { u ->
                _uiState.update { it.copy(nombreUsuario = u?.nombre?.substringBefore(' ').orEmpty()) }
            }
        }
        // Escucha en tiempo real del catálogo: actualiza la copia local que leen las ofertas
        viewModelScope.launch {
            productoRepository.observarCatalogoEnTiempoReal()
                .catch { _uiState.update { it.copy(cargando = false) } }
                .collect { }
        }
        // Ofertas destacadas (4 productos con stock) + cantidad del carrito
        viewModelScope.launch {
            combine(productoRepository.observarProductos(), carritoRepository.observarItems()) { productos, items ->
                productos.filter { it.stock > 0 }.take(4) to items.sumOf { it.item.cantidad }
            }.collect { (ofertas, cantidad) ->
                _uiState.update { it.copy(ofertasDestacadas = ofertas, cantidadCarrito = cantidad, cargando = false) }
            }
        }
    }
}
