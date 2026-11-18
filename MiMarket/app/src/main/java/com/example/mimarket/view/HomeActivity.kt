package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.model.Producto
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.util.Formato
import com.example.mimarket.view.componentes.BarraInferior
import com.example.mimarket.view.componentes.ImagenProducto
import com.example.mimarket.view.componentes.PantallaCargando
import com.example.mimarket.view.componentes.Pestana
import com.example.mimarket.viewmodel.HomeUiState
import com.example.mimarket.viewmodel.HomeViewModel

/** Pantalla 5 - Home (sesión iniciada): saludo, accesos rápidos y ofertas destacadas. */
class HomeActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        setContent {
            MiMarketTheme {
                val uiState by viewModel.uiState.collectAsState()
                HomeContenido(
                    uiState = uiState,
                    alNavegar = { irAPestana(it) },
                    alAbrirTiendas = { startActivity(Intent(this, MapaTiendasActivity::class.java)) },
                    alAbrirPedidos = { startActivity(Intent(this, HistorialComprasActivity::class.java)) },
                    alAbrirProducto = { p ->
                        startActivity(
                            Intent(this, ProductoDetalleActivity::class.java)
                                .putExtra(ProductoDetalleActivity.EXTRA_PRODUCTO_ID, p.id)
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContenido(
    uiState: HomeUiState,
    alNavegar: (Pestana) -> Unit,
    alAbrirTiendas: () -> Unit,
    alAbrirPedidos: () -> Unit,
    alAbrirProducto: (Producto) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Market", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = { BarraInferior(Pestana.INICIO, uiState.cantidadCarrito, alNavegar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (uiState.nombreUsuario.isNotBlank()) "¡Hola, ${uiState.nombreUsuario}!" else "¡Hola!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // ---- Accesos rápidos ----
            Text("Accesos rápidos", fontWeight = FontWeight.Medium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AccesoRapido("Catálogo", Icons.Default.List) { alNavegar(Pestana.CATALOGO) }
                AccesoRapido("Tiendas", Icons.Default.Place, alAbrirTiendas)
                AccesoRapido("Mis pedidos", Icons.Default.DateRange, alAbrirPedidos)
                AccesoRapido("Mi perfil", Icons.Default.Person) { alNavegar(Pestana.PERFIL) }
            }

            // ---- Ofertas destacadas (productos del catálogo en tiempo real) ----
            Text("Ofertas destacadas", fontWeight = FontWeight.Medium)
            when {
                uiState.cargando -> PantallaCargando(Modifier.height(160.dp))
                uiState.ofertasDestacadas.isEmpty() -> Text(
                    "Aún no hay productos. Entra al Catálogo para cargarlos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> uiState.ofertasDestacadas.chunked(2).forEach { fila ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        fila.forEach { p -> TarjetaOferta(p, Modifier.weight(1f)) { alAbrirProducto(p) } }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AccesoRapido(texto: String, icono: ImageVector, alPulsar: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = alPulsar, modifier = Modifier.size(56.dp)) {
            Icon(icono, contentDescription = texto)
        }
        Text(texto, fontSize = 12.sp)
    }
}

@Composable
private fun TarjetaOferta(producto: Producto, modifier: Modifier, alPulsar: () -> Unit) {
    ElevatedCard(modifier.clickable(onClick = alPulsar)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ImagenProducto(
                nombreRecurso = producto.imagen,
                descripcion = producto.nombre,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))
            )
            Text(producto.nombre, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Formato.moneda(producto.precio), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    MiMarketTheme {
        HomeContenido(
            uiState = HomeUiState(
                nombreUsuario = "Ana",
                ofertasDestacadas = listOf(Producto(1, "Leche", "1 L", 4.2, "prod_leche", "Lácteos", 10)),
                cargando = false
            ),
            alNavegar = {}, alAbrirTiendas = {}, alAbrirPedidos = {}, alAbrirProducto = {}
        )
    }
}
