package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.util.Formato
import com.example.mimarket.view.componentes.MensajeCentrado
import com.example.mimarket.view.componentes.PantallaCargando
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.ui.theme.VerdeExito
import com.example.mimarket.viewmodel.CompraUiState
import com.example.mimarket.viewmodel.CompraViewModel
import com.example.mimarket.viewmodel.EstadoUi

/** Pantalla 4 - Compra realizada: los datos se leen de Room con el id recibido. */
class CompraRealizadaActivity : ComponentActivity() {

    private val viewModel: CompraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        // Lee el id de la compra enviado por el Resumen y pide al ViewModel que la cargue desde Room
        viewModel.cargarCompra(intent.getLongExtra(EXTRA_COMPRA_ID, 0L))

        // El botón "atrás" también regresa al catálogo (no al resumen, que ya no existe).
        onBackPressedDispatcher.addCallback(this) { volverAlCatalogo() }

        setContent {
            MiMarketTheme {
                val uiState by viewModel.uiState.collectAsState()
                CompraRealizadaContenido(
                    uiState = uiState,
                    alVolverAlCatalogo = { volverAlCatalogo() },
                    alVerPedidos = {
                        volverAlCatalogo()
                        startActivity(Intent(this, HistorialComprasActivity::class.java))
                    }
                )
            }
        }
    }

    /** Regresa al catálogo reutilizando la pantalla existente (CLEAR_TOP) y cierra esta. */
    private fun volverAlCatalogo() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    companion object {
        /** Clave del dato (id de la compra) que viaja en el Intent entre pantallas. */
        const val EXTRA_COMPRA_ID = "extra_compra_id"
    }
}

@Composable
fun CompraRealizadaContenido(
    uiState: CompraUiState,
    alVolverAlCatalogo: () -> Unit,
    alVerPedidos: () -> Unit = {}
) {
    Scaffold { padding ->
        when (uiState.estado) {
            EstadoUi.CARGANDO -> PantallaCargando(Modifier.padding(padding))
            EstadoUi.ERROR, EstadoUi.VACIO, EstadoUi.SIN_CONEXION -> MensajeCentrado(
                titulo = "No se pudo mostrar la compra",
                detalle = uiState.errorMessage,
                textoBoton = "Ir al inicio",
                alPulsarBoton = alVolverAlCatalogo,
                modifier = Modifier.padding(padding)
            )
            // Caso de éxito: mensaje de confirmación, número de pedido, cantidad de productos y total
            EstadoUi.EXITO -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = VerdeExito,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("¡Pedido realizado!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Tu pedido ha sido procesado correctamente.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilaDato("Nº de pedido", uiState.numeroPedido)
                        FilaDato("Productos", uiState.cantidadProductos.toString())
                        FilaDato("Total", Formato.moneda(uiState.total))
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = alVerPedidos,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) { Text("Ver mis pedidos") }
                TextButton(onClick = alVolverAlCatalogo) { Text("Ir al inicio") }
            }
        }
    }
}

/** Una línea "etiqueta ........ valor" de la tarjeta de resumen. */
@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun CompraRealizadaPreview() {
    MiMarketTheme {
        CompraRealizadaContenido(
            uiState = CompraUiState(compraRealizada = true, numeroPedido = "#0000012", cantidadProductos = 3, total = 12.5),
            alVolverAlCatalogo = {}
        )
    }
}
