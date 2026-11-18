package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.remote.api.ConectividadObserver
import com.example.mimarket.data.remote.api.ErrorApi
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.remote.firebase.errorDeFirebase
import com.example.mimarket.data.repository.CarritoRepository
import com.example.mimarket.data.repository.ProductoRepository
import com.example.mimarket.data.repository.UsuarioRepository
import com.example.mimarket.model.ItemCarritoConProducto
import com.example.mimarket.model.Producto
import com.example.mimarket.util.OperacionInvalidaException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del catálogo.
 * Funciones:
 *  - Escuchar el catálogo de Cloud Firestore EN TIEMPO REAL (Snapshots): si otro usuario agrega,
 *    edita o elimina un producto, la lista se actualiza sola, sin refrescar.
 *  - Combinar productos, carrito y preferencias en un único StateFlow<CatalogoUiState>.
 *  - Eliminar productos con confirmación; si las reglas de seguridad lo impiden, mostrar el motivo.
 *  - Agregar/quitar unidades del carrito y filtrar por categoría.
 */
class CatalogoViewModel(
    private val productoRepository: ProductoRepository = MiMarketApplication.instancia.productoRepository,
    private val carritoRepository: CarritoRepository = MiMarketApplication.instancia.carritoRepository,
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository,
    private val conectividad: ConectividadObserver = MiMarketApplication.instancia.conectividad
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogoUiState(uidUsuario = productoRepository.uidActual.orEmpty()))
    /** Estado observable por la pantalla (StateFlow). */
    val uiState: StateFlow<CatalogoUiState> = _uiState.asStateFlow()

    // Paquete interno que agrupa los 4 orígenes de datos locales antes de convertirlos en UiState.
    private class Datos(
        val productos: List<Producto>,
        val items: List<ItemCarritoConProducto>,
        val filtro: String,
        val email: String
    )

    // Últimos datos locales recibidos (para recalcular la lista al escribir en el buscador).
    private var ultimosDatos: Datos? = null

    // Escucha activa de Firestore (se guarda para poder reiniciarla con "Reintentar").
    private var escuchaCatalogo: Job? = null

    init {
        observarDatos()
        observarConexion()
        cargarCatalogo() // empieza la escucha en tiempo real al entrar al catálogo
    }

    /**
     * Escucha productos (copia local), carrito, categoría elegida y correo. Cuando cualquiera cambia
     * se recalculan los campos derivados, sin tocar los indicadores de carga/error.
     */
    private fun observarDatos() {
        viewModelScope.launch {
            combine(
                productoRepository.observarProductos(),
                carritoRepository.observarItems(),
                productoRepository.flujoCategoriaFiltro,
                usuarioRepository.flujoEmail
            ) { productos, items, filtro, email -> Datos(productos, items, filtro, email) }
                .catch { _uiState.update { s -> s.copy(errorMessage = "No se pudo leer la información guardada") } }
                .collect { datos ->
                    ultimosDatos = datos
                    recalcular()
                }
        }
    }

    /** Recalcula la lista visible aplicando categoría y texto de búsqueda. */
    private fun recalcular() {
        val datos = ultimosDatos ?: return
        val categorias = datos.productos.map { it.categoria }.distinct().sorted()
        val filtro = if (datos.filtro in categorias) datos.filtro else ""
        _uiState.update { s ->
            val texto = s.busqueda.trim().lowercase()
            s.copy(
                productos = datos.productos
                    .filter { filtro.isEmpty() || it.categoria == filtro }
                    .filter { texto.isEmpty() || it.nombre.lowercase().contains(texto) || it.descripcion.lowercase().contains(texto) },
                cantidades = datos.items.associate { it.producto.id to it.item.cantidad },
                cantidadCarrito = datos.items.sumOf { it.item.cantidad },
                categorias = categorias,
                categoriaSeleccionada = filtro,
                emailUsuario = datos.email
            )
        }
    }

    /** Buscador del catálogo: filtra la lista ya cargada. */
    fun buscar(texto: String) {
        _uiState.update { it.copy(busqueda = texto) }
        recalcular()
    }

    /**
     * Vigila la conexión del dispositivo. Muestra el aviso "Sin conexión" y, cuando vuelve internet
     * y la escucha había fallado, la reinicia automáticamente.
     */
    private fun observarConexion() {
        viewModelScope.launch {
            conectividad.enLinea.collect { enLinea ->
                _uiState.update { it.copy(sinRed = !enLinea) }
                val s = _uiState.value
                if (enLinea && !s.isLoading && s.errorMessage != null) cargarCatalogo()
            }
        }
    }

    /**
     * Read en tiempo real. Inicia (o reinicia) la escucha de la colección "productos" de Firestore.
     * El primer Snapshot quita el indicador de carga; los siguientes llegan solos cuando alguien
     * cambia el catálogo. Si la escucha falla (sin permiso, sin conexión) se muestra el error con
     * un botón "Reintentar" que vuelve a llamar a esta función.
     */
    fun cargarCatalogo() {
        escuchaCatalogo?.cancel()
        _uiState.update { it.copy(isLoading = true, sinConexion = false, errorMessage = null) }
        escuchaCatalogo = viewModelScope.launch {
            productoRepository.observarCatalogoEnTiempoReal()
                .catch { e -> _uiState.update { s -> aplicarError(s.copy(isLoading = false), errorDeFirebase(e)) } }
                .collect { _uiState.update { s -> s.copy(isLoading = false, sinConexion = false, errorMessage = null) } }
        }
    }

    // Convierte un ErrorApi en los campos de error del estado.
    private fun aplicarError(s: CatalogoUiState, error: ErrorApi): CatalogoUiState =
        if (error is ErrorApi.SinConexion || error is ErrorApi.Timeout)
            s.copy(sinConexion = true, errorMessage = error.mensaje)
        else
            s.copy(errorMessage = error.mensaje)

    /**
     * Migración Node.js -> Firestore: cuando el catálogo está vacío, importa los productos del
     * backend de la semana 4 (o los de ejemplo si el servidor está apagado) a la colección "productos".
     */
    fun importarCatalogoInicial() {
        if (_uiState.value.operando) return
        _uiState.update { it.copy(operando = true) }
        viewModelScope.launch {
            val resultado = productoRepository.importarCatalogoInicial()
            _uiState.update { s ->
                s.copy(
                    operando = false,
                    mensaje = when (resultado) {
                        is ResultadoApi.Exito -> "Se importaron ${resultado.datos.first} productos a Firestore desde ${resultado.datos.second}"
                        is ResultadoApi.Fallo -> resultado.error.mensaje
                    }
                )
            }
        }
    }

    /**
     * Botón "Eliminar" de una tarjeta. Reglas de negocio: solo se elimina si su stock es 0; además, si el producto está en el carrito
     * NO se puede eliminar; se pide retirarlo primero. Si no, se abre el diálogo de confirmación.
     */
    fun solicitarEliminar(producto: Producto) {
        val enCarrito = _uiState.value.cantidades[producto.id] ?: 0
        if (producto.stock > 0) {
            // Regla de negocio: solo se eliminan productos sin stock
            _uiState.update { it.copy(avisoEliminar = "El producto \"${producto.nombre}\" cuenta con ${producto.stock} unidades en stock, por lo que no puede ser eliminado.") }
        } else if (enCarrito > 0) {
            _uiState.update { it.copy(avisoEliminar = "El producto \"${producto.nombre}\" se encuentra en su carrito de compras. Debe retirarlo del carrito antes de eliminarlo.") }
        } else {
            _uiState.update { it.copy(productoAEliminar = producto) }
        }
    }

    /** El usuario cerró el aviso de eliminación no permitida. */
    fun cerrarAvisoEliminar() {
        _uiState.update { it.copy(avisoEliminar = null) }
    }

    /** El usuario canceló el diálogo: no se envía nada a Firestore. */
    fun cancelarEliminar() {
        _uiState.update { it.copy(productoAEliminar = null) }
    }

    /**
     * Confirmó el diálogo: elimina el documento en Firestore. Si el usuario no es el creador ni
     * administrador, las reglas responden PERMISSION_DENIED y se muestra un mensaje claro.
     */
    fun confirmarEliminar() {
        val producto = _uiState.value.productoAEliminar ?: return
        if (_uiState.value.operando) return // evita doble envío
        _uiState.update { it.copy(operando = true, productoAEliminar = null) }
        viewModelScope.launch {
            val resultado = productoRepository.borrarProducto(producto.id)
            _uiState.update { s ->
                s.copy(
                    operando = false,
                    mensaje = when (resultado) {
                        is ResultadoApi.Exito -> "Producto \"${producto.nombre}\" eliminado correctamente"
                        is ResultadoApi.Fallo -> resultado.error.mensaje
                    }
                )
            }
        }
    }

    /** Lo llama la pantalla cuando vuelve del formulario o del detalle: muestra el mensaje del resultado. */
    fun alVolverDelFormulario(mensaje: String?) {
        // No hace falta recargar: el Snapshot de Firestore ya actualizó la lista.
        if (mensaje != null) _uiState.update { it.copy(mensaje = mensaje) }
    }

    /** Botón "Agregar" / "+": suma una unidad del producto al carrito. */
    fun agregarAlCarrito(producto: Producto) = ejecutar { carritoRepository.agregarProducto(producto.id) }

    /** Botón "−": resta una unidad (si llega a 0, el producto sale del carrito). */
    fun quitarUnidad(producto: Producto) = ejecutar { carritoRepository.disminuirPorProducto(producto.id) }

    /** Chip de categoría: guarda la elección en DataStore (el flujo actualiza la lista solo). */
    fun seleccionarCategoria(categoria: String) = ejecutar { productoRepository.guardarCategoriaFiltro(categoria) }

    /** La vista avisa que ya mostró el mensaje (snackbar) para no repetirlo. */
    fun mensajeMostrado() {
        _uiState.update { it.copy(mensaje = null) }
    }

    /** Ejecuta una acción local (carrito/preferencias) y convierte los errores en un mensaje. */
    private fun ejecutar(accion: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                accion()
            } catch (e: OperacionInvalidaException) {
                _uiState.update { it.copy(mensaje = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(mensaje = "Ocurrió un problema. Inténtalo de nuevo") }
            }
        }
    }
}
