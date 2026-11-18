package com.example.mimarket.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.mimarket.ui.theme.VerdeExito
import com.example.mimarket.view.componentes.BarraInferior
import com.example.mimarket.view.componentes.Pestana
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.model.Producto
import com.example.mimarket.util.Formato
import com.example.mimarket.view.componentes.ImagenProducto
import com.example.mimarket.view.componentes.MensajeCentrado
import com.example.mimarket.view.componentes.PantallaCargando
import com.example.mimarket.view.componentes.SelectorCantidad
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.viewmodel.CatalogoUiState
import com.example.mimarket.viewmodel.CatalogoViewModel
import com.example.mimarket.viewmodel.EstadoUi

/**
 * Pantalla 2 - Catálogo de productos. Semana 5: se lee de Cloud Firestore EN TIEMPO REAL (Snapshots),
 * con registrar, editar y eliminar. Desde aquí se abre el mapa de tiendas y el perfil.
 */
class CatalogoActivity : ComponentActivity() {

    // ViewModel del catálogo (sobrevive a giros de pantalla)
    private val viewModel: CatalogoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        setContent {
            MiMarketTheme {
                CatalogoScreen(
                    viewModel = viewModel,
                    // Navegación: carrito -> Resumen de compra; icono historial -> Historial de compras
                    alIrAlCarrito = { startActivity(Intent(this, ResumenCompraActivity::class.java)) },
                    alIrAlHistorial = { startActivity(Intent(this, HistorialComprasActivity::class.java)) },
                    // Semana 5: mapa de tiendas y perfil del usuario (cerrar sesión / eliminar cuenta están en el perfil)
                    alIrAlMapa = { startActivity(Intent(this, MapaTiendasActivity::class.java)) },
                    alIrAlPerfil = { startActivity(Intent(this, PerfilActivity::class.java)) },
                    // Barra inferior compartida: Inicio, Catálogo, Carrito, Perfil
                    alNavegar = { irAPestana(it) }
                )
            }
        }
    }
}

@Composable
fun CatalogoScreen(
    viewModel: CatalogoViewModel,
    alIrAlCarrito: () -> Unit,
    alIrAlHistorial: () -> Unit,
    alIrAlMapa: () -> Unit,
    alIrAlPerfil: () -> Unit,
    alNavegar: (Pestana) -> Unit
) {
    // Observa el estado del ViewModel; cada cambio redibuja la pantalla
    val uiState by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current

    // Abre el formulario; al volver con RESULT_OK muestra el mensaje.
    // No hace falta recargar: el Snapshot de Firestore actualiza la lista por sí solo.
    val lanzadorFormulario = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            viewModel.alVolverDelFormulario(r.data?.getStringExtra(ProductoFormActivity.EXTRA_MENSAJE))
        }
    }
    // Detalle del producto: al volver (editó o eliminó) se muestra el mensaje
    val lanzadorDetalle = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) viewModel.alVolverDelFormulario(r.data?.getStringExtra(ProductoDetalleActivity.EXTRA_MENSAJE))
    }
    fun abrirDetalle(id: Long) = lanzadorDetalle.launch(
        Intent(contexto, ProductoDetalleActivity::class.java).putExtra(ProductoDetalleActivity.EXTRA_PRODUCTO_ID, id)
    )
    fun abrirFormulario(id: Long) = lanzadorFormulario.launch(
        Intent(contexto, ProductoFormActivity::class.java).putExtra(ProductoFormActivity.EXTRA_PRODUCTO_ID, id)
    )

    CatalogoContenido(
        uiState = uiState,
        alAgregar = viewModel::agregarAlCarrito,
        alQuitar = viewModel::quitarUnidad,
        alSeleccionarCategoria = viewModel::seleccionarCategoria,
        alIrAlCarrito = alIrAlCarrito,
        alIrAlHistorial = alIrAlHistorial,
        alIrAlMapa = alIrAlMapa,
        alIrAlPerfil = alIrAlPerfil,
        alNavegar = alNavegar,
        alImportar = viewModel::importarCatalogoInicial,
        alMensajeMostrado = viewModel::mensajeMostrado,
        alRegistrar = { abrirFormulario(0L) },
        alEditar = { abrirFormulario(it.id) },
        alEliminar = viewModel::solicitarEliminar,
        alConfirmarEliminar = viewModel::confirmarEliminar,
        alCancelarEliminar = viewModel::cancelarEliminar,
        alCerrarAviso = viewModel::cerrarAvisoEliminar,
        alRecargar = viewModel::cargarCatalogo,
        alBuscar = viewModel::buscar,
        alAbrirDetalle = { abrirDetalle(it.id) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoContenido(
    uiState: CatalogoUiState,
    alAgregar: (Producto) -> Unit,
    alQuitar: (Producto) -> Unit,
    alSeleccionarCategoria: (String) -> Unit,
    alIrAlCarrito: () -> Unit,
    alIrAlHistorial: () -> Unit,
    alMensajeMostrado: () -> Unit,
    alIrAlMapa: () -> Unit = {},
    alIrAlPerfil: () -> Unit = {},
    alNavegar: (Pestana) -> Unit = {},
    alImportar: () -> Unit = {},
    alRegistrar: () -> Unit = {},
    alEditar: (Producto) -> Unit = {},
    alEliminar: (Producto) -> Unit = {},
    alConfirmarEliminar: () -> Unit = {},
    alCancelarEliminar: () -> Unit = {},
    alCerrarAviso: () -> Unit = {},
    alRecargar: () -> Unit = {},
    alBuscar: (String) -> Unit = {},
    alAbrirDetalle: (Producto) -> Unit = {}
) {
    // Controla los avisos tipo "snackbar" (ej. stock insuficiente)
    val snackbarHostState = remember { SnackbarHostState() }

    // Cuando llega un mensaje nuevo se muestra y luego se avisa al ViewModel para limpiarlo
    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            alMensajeMostrado()
        }
    }

    // Aviso formal cuando la eliminación no está permitida (producto con stock o dentro del carrito)
    uiState.avisoEliminar?.let { texto ->
        AlertDialog(
            onDismissRequest = alCerrarAviso,
            title = { Text("Eliminación no permitida") },
            text = { Text(texto) },
            confirmButton = { TextButton(onClick = alCerrarAviso) { Text("Entendido") } }
        )
    }

    // Diálogo de confirmación (solo visible cuando hay un producto pendiente de eliminar)
    uiState.productoAEliminar?.let { DialogoEliminar(it, alConfirmarEliminar, alCancelarEliminar) }

    Scaffold(
        // Barra superior: nombre de la tienda, correo del usuario, registrar producto e historial
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mi Market", fontWeight = FontWeight.Bold)
                        if (uiState.emailUsuario.isNotEmpty()) {
                            Text(uiState.emailUsuario, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Registrar un producto nuevo (Create)
                    IconButton(onClick = alRegistrar) {
                        Icon(Icons.Default.Add, contentDescription = "Registrar producto")
                    }
                    IconButton(onClick = alIrAlHistorial) {
                        Icon(Icons.Default.DateRange, contentDescription = "Historial de compras")
                    }
                }
            )
        },
        // Barra inferior compartida (flujo de pantallas): Inicio, Catálogo, Carrito y Perfil
        bottomBar = { BarraInferior(Pestana.CATALOGO, uiState.cantidadCarrito, alNavegar) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Aviso permanente mientras el dispositivo no tiene internet
            if (uiState.sinRed) {
                Text(
                    text = "Sin conexión a internet",
                    color = MaterialTheme.colorScheme.onError,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .padding(6.dp)
                )
            }
            // Barra de progreso fina mientras se ejecuta una operación (eliminar o importar)
            if (uiState.operando) LinearProgressIndicator(Modifier.fillMaxWidth())
            // Buscador por nombre o descripción (visible también cuando la búsqueda no encuentra nada)
            if (uiState.estado == EstadoUi.EXITO || uiState.estado == EstadoUi.VACIO) {
                OutlinedTextField(
                    value = uiState.busqueda,
                    onValueChange = alBuscar,
                    placeholder = { Text("Buscar productos...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            // Dibuja según el estado: cargando / sin conexión / error / vacío / éxito (lista de productos)
            when (uiState.estado) {
                EstadoUi.CARGANDO -> PantallaCargando()
                EstadoUi.SIN_CONEXION -> MensajeCentrado(
                    titulo = "Sin conexión",
                    detalle = uiState.errorMessage,
                    textoBoton = "Reintentar",
                    alPulsarBoton = alRecargar
                )
                EstadoUi.ERROR -> MensajeCentrado(
                    titulo = "No se pudo cargar el catálogo",
                    detalle = uiState.errorMessage,
                    textoBoton = "Reintentar",
                    alPulsarBoton = alRecargar
                )
                EstadoUi.VACIO -> if (uiState.busqueda.isNotBlank()) {
                    MensajeCentrado(
                        titulo = "No hay productos disponibles",
                        detalle = "Ningún producto coincide con la búsqueda."
                    )
                } else {
                    // Catálogo vacío en Firestore: se puede migrar el catálogo del backend Node.js
                    MensajeCentrado(
                        titulo = "El catálogo está vacío",
                        detalle = "Registra un producto con el botón + o importa a Firestore el catálogo inicial (del servidor Node.js o de ejemplo).",
                        textoBoton = "Cargar productos iniciales",
                        alPulsarBoton = alImportar
                    )
                }
                EstadoUi.EXITO -> {
                    // Chips de categoría y rejilla de 2 columnas con los productos (copia local sincronizada con Firestore)
                    FiltroCategorias(uiState, alSeleccionarCategoria)
                    // Ya no hay botón "Recargar": Firestore envía los cambios en tiempo real
                    Text(
                        text = "Catálogo en tiempo real · se actualiza solo",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.productos, key = { it.id }) { producto ->
                            TarjetaProducto(
                                producto = producto,
                                cantidadEnCarrito = uiState.cantidades[producto.id] ?: 0,
                                esPropio = producto.creadorUid.isNotEmpty() && producto.creadorUid == uiState.uidUsuario,
                                alAgregar = { alAgregar(producto) },
                                alQuitar = { alQuitar(producto) },
                                alEditar = { alEditar(producto) },
                                alAbrirDetalle = { alAbrirDetalle(producto) },
                                alEliminar = { alEliminar(producto) },
                                habilitado = !uiState.operando
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Diálogo de confirmación de eliminación. Cancelar no envía nada al servidor. */
@Composable
private fun DialogoEliminar(producto: Producto, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Eliminar producto") },
        text = { Text("¿Seguro que deseas eliminar \"${producto.nombre}\"? Esta acción no se puede deshacer.") },
        confirmButton = { TextButton(onClick = alConfirmar) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } }
    )
}

/** Los productos registrados desde la app usan como id la hora de creación (milisegundos). */
private fun esNuevo(producto: Producto): Boolean {
    val ahora = System.currentTimeMillis()
    return producto.id > 1_000_000_000_000L && ahora - producto.id in 0..10 * 60 * 1000L
}

/** Pantalla 11 - Acciones del producto (editar / eliminar). */
@Composable
private fun DialogoAcciones(producto: Producto, alEditar: () -> Unit, alEliminar: () -> Unit, alCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Acciones") },
        text = {
            Column {
                TextButton(onClick = alEditar) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text("  Editar producto")
                }
                TextButton(onClick = alEliminar) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("  Eliminar producto", color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Solo el creador o un administrador puede realizar esta acción.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } }
    )
}

/** Fila de chips para filtrar por categoría; la elección se recuerda con DataStore. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltroCategorias(uiState: CatalogoUiState, alSeleccionar: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = uiState.categoriaSeleccionada.isEmpty(),
                onClick = { alSeleccionar("") },
                label = { Text("Todas") }
            )
        }
        items(uiState.categorias) { categoria ->
            FilterChip(
                selected = uiState.categoriaSeleccionada == categoria,
                onClick = { alSeleccionar(categoria) },
                label = { Text(categoria) }
            )
        }
    }
}

/**
 * Tarjeta de un producto: imagen, nombre, descripción, precio y control de carrito.
 * Si el producto no está en el carrito muestra "Agregar"; si ya está, el selector − cantidad +.
 */
@Composable
private fun TarjetaProducto(
    producto: Producto,
    cantidadEnCarrito: Int,
    esPropio: Boolean,
    alAgregar: () -> Unit,
    alQuitar: () -> Unit,
    alEditar: () -> Unit,
    alAbrirDetalle: () -> Unit,
    alEliminar: () -> Unit,
    habilitado: Boolean
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ImagenProducto(
                nombreRecurso = producto.imagen,
                descripcion = producto.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = alAbrirDetalle) // tocar la imagen abre el detalle
            )
            Spacer(Modifier.height(8.dp))
            // Pantalla 12 - etiqueta "NUEVO": producto creado hace menos de 10 minutos (llegó por tiempo real)
            if (esNuevo(producto)) {
                Badge(containerColor = VerdeExito) { Text("NUEVO", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp)) }
            }
            Text(
                text = producto.nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = producto.descripcion,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = Formato.moneda(producto.precio),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.primary
            )
            // Stock disponible según Firestore
            Text(
                text = "Stock: ${producto.stock}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Indica de quién es el producto: las reglas de Firestore solo dejan editar/eliminar al creador o a un admin
            Text(
                text = if (esPropio) "Publicado por ti" else "De otro usuario",
                fontSize = 11.sp,
                color = if (esPropio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Pantalla 11 - "Acciones": editar (Update) o eliminar (Delete). Solo el creador o un admin puede.
            var mostrarAcciones by remember { mutableStateOf(false) }
            TextButton(onClick = { mostrarAcciones = true }, enabled = habilitado) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
                Text("Acciones")
            }
            if (mostrarAcciones) {
                DialogoAcciones(
                    producto = producto,
                    alEditar = { mostrarAcciones = false; alEditar() },
                    alEliminar = { mostrarAcciones = false; alEliminar() },
                    alCerrar = { mostrarAcciones = false }
                )
            }
            // Tres casos: sin stock, aún no está en el carrito, o ya está (se puede cambiar la cantidad)
            when {
                producto.stock <= 0 -> Button(onClick = {}, enabled = false) { Text("Agotado") }
                cantidadEnCarrito == 0 -> Button(
                    onClick = alAgregar,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Agregar") }
                else -> SelectorCantidad(
                    cantidad = cantidadEnCarrito,
                    alDisminuir = alQuitar,
                    alAumentar = alAgregar
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CatalogoPreview() {
    MiMarketTheme {
        CatalogoContenido(
            uiState = CatalogoUiState(
                productos = listOf(
                    Producto(1, "Manzana", "Manzana roja fresca, 1 kg", 2.5, "prod_manzana", "Frutas", 10),
                    Producto(2, "Leche", "Leche entera en caja, 1 L", 4.0, "prod_leche", "Lácteos", 10)
                ),
                cantidades = mapOf(1L to 2),
                cantidadCarrito = 2,
                categorias = listOf("Frutas", "Lácteos"),
                emailUsuario = "cliente@mimarket.com",
                isLoading = false
            ),
            alAgregar = {}, alQuitar = {}, alSeleccionarCategoria = {},
            alIrAlCarrito = {}, alIrAlHistorial = {}, alMensajeMostrado = {}
        )
    }
}
