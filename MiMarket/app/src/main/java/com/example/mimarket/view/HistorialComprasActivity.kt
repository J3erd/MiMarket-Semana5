package com.example.mimarket.view

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.model.CompraConDetalles
import com.example.mimarket.util.Formato
import com.example.mimarket.view.componentes.MensajeCentrado
import com.example.mimarket.view.componentes.PantallaCargando
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.viewmodel.CompraUiState
import com.example.mimarket.viewmodel.CompraViewModel
import com.example.mimarket.viewmodel.EstadoUi

/** Pantalla 21 - Mis pedidos (pedidos del usuario leídos de Firestore: usuarios/{uid}/pedidos). */
class HistorialComprasActivity : ComponentActivity() {

    private val viewModel: CompraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        setContent {
            MiMarketTheme {
                val uiState by viewModel.uiState.collectAsState()
                // Al abrir la pantalla se empieza a observar el historial del usuario en Firestore (tiempo real)
                LaunchedEffect(Unit) { viewModel.cargarHistorial() }
                HistorialContenido(uiState = uiState, alVolver = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialContenido(uiState: CompraUiState, alVolver: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis pedidos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Error / cargando / sin compras / lista de compras
            when {
                uiState.errorMessage != null -> MensajeCentrado("No se pudo cargar el historial", uiState.errorMessage)
                uiState.isLoading -> PantallaCargando()
                uiState.historial.isEmpty() -> MensajeCentrado(
                    titulo = "Aún no tienes compras",
                    detalle = "Cuando confirmes una compra aparecerá aquí."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.historial, key = { it.compra.id }) { TarjetaCompra(it) }
                }
            }
        }
    }
}

/** Tarjeta de una compra: pedido, fecha, estado, líneas de detalle, envío y total. */
@Composable
private fun TarjetaCompra(compra: CompraConDetalles) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(compra.compra.numeroPedido, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text(compra.compra.estado, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            Text(
                Formato.fecha(compra.compra.fecha),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            compra.detalles.forEach { d ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${d.cantidad} × ${d.nombreProducto}", fontSize = 14.sp)
                    Text(Formato.moneda(d.subtotal), fontSize = 14.sp)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Envío", fontSize = 13.sp)
                Text(Formato.moneda(compra.compra.costoEnvio), fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold)
                Text(Formato.moneda(compra.compra.total), fontWeight = FontWeight.Bold)
            }
        }
    }
}
