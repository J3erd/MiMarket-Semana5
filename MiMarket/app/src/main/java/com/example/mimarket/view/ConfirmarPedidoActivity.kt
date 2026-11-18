package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.util.Formato
import com.example.mimarket.view.componentes.MensajeCentrado
import com.example.mimarket.viewmodel.CarritoUiState
import com.example.mimarket.viewmodel.CarritoViewModel
import com.example.mimarket.viewmodel.CompraUiState
import com.example.mimarket.viewmodel.CompraViewModel

/**
 * Pantalla 19 - Confirmar pedido: entrega a domicilio, dirección (elegida en el mapa), fecha y resumen.
 * "Confirmar pedido" registra la compra (Room + Firestore usuarios/{uid}/pedidos) y abre "Pedido realizado".
 */
class ConfirmarPedidoActivity : ComponentActivity() {

    private val carritoViewModel: CarritoViewModel by viewModels()
    private val compraViewModel: CompraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        setContent {
            MiMarketTheme {
                val carrito by carritoViewModel.uiState.collectAsState()
                val compra by compraViewModel.uiState.collectAsState()
                // Compra registrada -> pantalla 20 "Pedido realizado"
                LaunchedEffect(compra.compraRealizada) {
                    if (compra.compraRealizada) {
                        startActivity(
                            Intent(this@ConfirmarPedidoActivity, CompraRealizadaActivity::class.java)
                                .putExtra(CompraRealizadaActivity.EXTRA_COMPRA_ID, compra.compraId)
                        )
                        finish()
                    }
                }
                ConfirmarPedidoContenido(
                    carrito = carrito,
                    compra = compra,
                    alConfirmar = compraViewModel::confirmarCompra,
                    alElegirDireccion = { startActivity(Intent(this, MapaTiendasActivity::class.java)) },
                    alErrorMostrado = compraViewModel::errorMostrado,
                    alVolver = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmarPedidoContenido(
    carrito: CarritoUiState,
    compra: CompraUiState,
    alConfirmar: () -> Unit,
    alElegirDireccion: () -> Unit,
    alErrorMostrado: () -> Unit,
    alVolver: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(compra.errorMessage) {
        compra.errorMessage?.let { snackbar.showSnackbar(it); alErrorMostrado() }
    }
    val procesando = compra.isLoading || compra.compraRealizada

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmar pedido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al carrito") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (carrito.items.isEmpty() && !carrito.isLoading && !procesando) {
            MensajeCentrado("Tu carrito está vacío", "Agrega productos para confirmar un pedido.", "Volver", alVolver, Modifier.padding(padding))
            return@Scaffold
        }
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Entrega a domicilio", fontWeight = FontWeight.Bold)
                        HorizontalDivider()
                        Text("Dirección", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (carrito.direccionEntrega.isNotBlank()) {
                            Text(carrito.direccionEntrega)
                        } else {
                            Text("Aún no elegiste una dirección de entrega.")
                        }
                        // Pantalla 17: la dirección se elige en el mapa ("Usar como dirección de entrega")
                        TextButton(onClick = alElegirDireccion) {
                            Text(if (carrito.direccionEntrega.isBlank()) "Elegir en el mapa" else "Cambiar dirección")
                        }
                        Text("Fecha y hora", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Formato.fecha(System.currentTimeMillis()))
                    }
                }
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Resumen", fontWeight = FontWeight.Bold)
                        Fila("${carrito.cantidadTotal} productos", Formato.moneda(carrito.subtotal))
                        Fila("Costo de envío", Formato.moneda(carrito.costoEnvio))
                        HorizontalDivider()
                        Fila("Total", Formato.moneda(carrito.total), destacado = true)
                    }
                }
                Button(
                    onClick = alConfirmar,
                    enabled = carrito.puedeConfirmar && !procesando,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Confirmar pedido") }
            }
            if (procesando) CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal)
        Text(valor, fontWeight = if (destacado) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
