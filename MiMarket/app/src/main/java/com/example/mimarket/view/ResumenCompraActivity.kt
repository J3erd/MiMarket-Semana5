package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.model.ItemCarrito
import com.example.mimarket.model.ItemCarritoConProducto
import com.example.mimarket.model.Producto
import com.example.mimarket.util.Formato
import com.example.mimarket.view.componentes.ImagenProducto
import com.example.mimarket.view.componentes.MensajeCentrado
import com.example.mimarket.view.componentes.PantallaCargando
import com.example.mimarket.view.componentes.SelectorCantidad
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.viewmodel.CarritoUiState
import com.example.mimarket.viewmodel.CarritoViewModel
import com.example.mimarket.viewmodel.CompraUiState
import com.example.mimarket.viewmodel.CompraViewModel
import com.example.mimarket.viewmodel.EstadoUi

/** Pantalla 18 - Mi carrito: modificar cantidades, eliminar productos y continuar a "Confirmar pedido". */
class ResumenCompraActivity : ComponentActivity() {

    // Dos ViewModels: uno gestiona el carrito (cantidades, importes) y otro la confirmación de la compra
    private val carritoViewModel: CarritoViewModel by viewModels()
    private val compraViewModel: CompraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        setContent {
            MiMarketTheme {
                ResumenCompraScreen(
                    carritoViewModel = carritoViewModel,
                    compraViewModel = compraViewModel,
                    // Volver: cierra esta pantalla y reaparece el catálogo
                    alVolver = { finish() },
                    // Continuar -> pantalla 19 Confirmar pedido
                    alContinuar = { startActivity(Intent(this, ConfirmarPedidoActivity::class.java)) },
                    // Compra confirmada: abre "Compra realizada" enviando el id y cierra el resumen
                    alCompraRealizada = { compraId ->
                        startActivity(
                            Intent(this, CompraRealizadaActivity::class.java)
                                .putExtra(CompraRealizadaActivity.EXTRA_COMPRA_ID, compraId)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ResumenCompraScreen(
    carritoViewModel: CarritoViewModel,
    compraViewModel: CompraViewModel,
    alVolver: () -> Unit,
    alContinuar: () -> Unit,
    alCompraRealizada: (Long) -> Unit
) {
    // Estados observados de ambos ViewModels
    val carrito by carritoViewModel.uiState.collectAsState()
    val compra by compraViewModel.uiState.collectAsState()

    // Cuando la compra queda registrada (compraRealizada = true) se navega a la pantalla final
    LaunchedEffect(compra.compraRealizada) {
        if (compra.compraRealizada) alCompraRealizada(compra.compraId)
    }

    ResumenCompraContenido(
        carrito = carrito,
        compra = compra,
        alIncrementar = carritoViewModel::incrementar,
        alDisminuir = carritoViewModel::disminuir,
        alEliminar = carritoViewModel::eliminar,
        alConfirmar = alContinuar,
        alVolver = alVolver,
        alMensajeMostrado = carritoViewModel::mensajeMostrado,
        alErrorMostrado = compraViewModel::errorMostrado
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenCompraContenido(
    carrito: CarritoUiState,
    compra: CompraUiState,
    alIncrementar: (ItemCarritoConProducto) -> Unit,
    alDisminuir: (ItemCarritoConProducto) -> Unit,
    alEliminar: (ItemCarritoConProducto) -> Unit,
    alConfirmar: () -> Unit,
    alVolver: () -> Unit,
    alMensajeMostrado: () -> Unit,
    alErrorMostrado: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(carrito.mensaje) {
        carrito.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            alMensajeMostrado()
        }
    }
    LaunchedEffect(compra.errorMessage) {
        compra.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            alErrorMostrado()
        }
    }

    // Mientras se registra la compra se bloquean los botones y se muestra un indicador
    val procesando = compra.isLoading || compra.compraRealizada

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi carrito", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al catálogo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        // Panel inferior fijo: subtotal, envío, total y botones Volver / Confirmar
        bottomBar = {
            PanelTotales(
                carrito = carrito,
                procesando = procesando,
                alConfirmar = alConfirmar,
                alVolver = alVolver
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (carrito.estado) {
                EstadoUi.CARGANDO -> PantallaCargando()
                EstadoUi.ERROR, EstadoUi.SIN_CONEXION -> MensajeCentrado(
                    titulo = "No se pudo cargar el carrito",
                    detalle = carrito.errorMessage
                )
                // Carrito vacío: mensaje y botón para volver al catálogo (no se puede confirmar)
                EstadoUi.VACIO -> MensajeCentrado(
                    titulo = "Tu carrito está vacío",
                    detalle = "Agrega productos desde el catálogo para poder confirmar una compra.",
                    textoBoton = "Ir al catálogo",
                    alPulsarBoton = alVolver
                )
                EstadoUi.EXITO -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(carrito.items, key = { it.item.id }) { fila ->
                        FilaCarrito(
                            fila = fila,
                            alIncrementar = { alIncrementar(fila) },
                            alDisminuir = { alDisminuir(fila) },
                            alEliminar = { alEliminar(fila) }
                        )
                    }
                }
            }
            if (procesando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Renglón del carrito: imagen, nombre, "Cant × precio", selector de cantidad,
 * subtotal del producto y papelera para eliminarlo.
 */
@Composable
private fun FilaCarrito(
    fila: ItemCarritoConProducto,
    alIncrementar: () -> Unit,
    alDisminuir: () -> Unit,
    alEliminar: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImagenProducto(
                nombreRecurso = fila.producto.imagen,
                descripcion = fila.producto.nombre,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(fila.producto.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Cant: ${fila.item.cantidad} × ${Formato.moneda(fila.item.precioUnitario)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectorCantidad(
                    cantidad = fila.item.cantidad,
                    alDisminuir = alDisminuir,
                    alAumentar = alIncrementar
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Formato.moneda(fila.subtotal), fontWeight = FontWeight.Bold)
                IconButton(onClick = alEliminar) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar ${fila.producto.nombre}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/** Resumen de importes (subtotal, envío, total) y botones de acción. */
@Composable
private fun PanelTotales(
    carrito: CarritoUiState,
    procesando: Boolean,
    alConfirmar: () -> Unit,
    alVolver: () -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column(Modifier.padding(16.dp)) {
            FilaImporte("Subtotal", Formato.moneda(carrito.subtotal))
            FilaImporte("Costo de envío", Formato.moneda(carrito.costoEnvio))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            FilaImporte("Total", Formato.moneda(carrito.total), destacado = true)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = alVolver,
                    enabled = !procesando,
                    modifier = Modifier.weight(1f)
                ) { Text("Volver") }
                // Con el carrito vacío el botón queda deshabilitado: no se puede confirmar.
                Button(
                    onClick = alConfirmar,
                    enabled = carrito.puedeConfirmar && !procesando,
                    modifier = Modifier.weight(1f)
                ) { Text("Continuar") }
            }
        }
    }
}

/** Una línea "etiqueta ........ valor" del panel de totales. */
@Composable
private fun FilaImporte(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            etiqueta,
            fontWeight = if (destacado) FontWeight.ExtraBold else FontWeight.Normal,
            fontSize = if (destacado) 18.sp else 14.sp
        )
        Text(
            valor,
            fontWeight = if (destacado) FontWeight.ExtraBold else FontWeight.Medium,
            fontSize = if (destacado) 18.sp else 14.sp,
            color = if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResumenCompraPreview() {
    val manzana = Producto(1, "Manzana", "Manzana roja", 2.5, "prod_manzana", "Frutas", 10)
    MiMarketTheme {
        ResumenCompraContenido(
            carrito = CarritoUiState(
                items = listOf(ItemCarritoConProducto(ItemCarrito(1, 1, 2, 2.5), manzana)),
                cantidadTotal = 2, subtotal = 5.0, costoEnvio = 2.0, total = 7.0, isLoading = false
            ),
            compra = CompraUiState(),
            alIncrementar = {}, alDisminuir = {}, alEliminar = {}, alConfirmar = {},
            alVolver = {}, alMensajeMostrado = {}, alErrorMostrado = {}
        )
    }
}
