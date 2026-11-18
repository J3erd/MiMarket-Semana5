package com.example.mimarket.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.util.Formato
import com.example.mimarket.view.componentes.ImagenProducto
import com.example.mimarket.view.componentes.MensajeCentrado
import com.example.mimarket.view.componentes.PantallaCargando
import com.example.mimarket.view.componentes.SelectorCantidad
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.viewmodel.ProductoDetalleViewModel

/**
 * Pantalla "Detalle de producto": se abre al tocar la imagen de un producto del catálogo.
 * Muestra imagen, nombre, precio, stock y descripción; permite elegir cantidad, agregar al carrito,
 * editar (formulario) y eliminar (con confirmación). Devuelve RESULT_OK si algo cambió en el servidor.
 */
class ProductoDetalleActivity : ComponentActivity() {

    private val viewModel: ProductoDetalleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        viewModel.iniciar(intent.getLongExtra(EXTRA_PRODUCTO_ID, 0L))
        setContent {
            MiMarketTheme { PantallaDetalle() }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PantallaDetalle() {
        val estado by viewModel.uiState.collectAsState()
        val contexto = LocalContext.current
        val snackbar = remember { SnackbarHostState() }

        // Formulario de edición: al guardar con éxito se avisa al catálogo (RESULT_OK) y se muestra el mensaje
        val lanzadorEditar = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == Activity.RESULT_OK) setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_MENSAJE, r.data?.getStringExtra(ProductoFormActivity.EXTRA_MENSAJE)))
        }
        LaunchedEffect(estado.mensaje) {
            estado.mensaje?.let { snackbar.showSnackbar(it); viewModel.mensajeMostrado() }
        }
        // Eliminación confirmada por el servidor: devuelve el mensaje y cierra
        LaunchedEffect(estado.eliminado) {
            if (estado.eliminado) {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_MENSAJE, "Producto eliminado correctamente"))
                finish()
            }
        }
        estado.avisoEliminar?.let { texto ->
            AlertDialog(
                onDismissRequest = viewModel::cerrarAvisoEliminar,
                title = { Text("Eliminación no permitida") },
                text = { Text(texto) },
                confirmButton = { TextButton(onClick = viewModel::cerrarAvisoEliminar) { Text("Entendido") } }
            )
        }
        if (estado.confirmandoEliminar) {
            AlertDialog(
                onDismissRequest = viewModel::cancelarEliminar,
                title = { Text("Eliminar producto") },
                text = { Text("¿Deseas eliminar \"${estado.producto?.nombre}\"? Esta acción no se puede deshacer.") },
                confirmButton = { TextButton(onClick = viewModel::confirmarEliminar) { Text("Eliminar") } },
                dismissButton = { TextButton(onClick = viewModel::cancelarEliminar) { Text("Cancelar") } }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detalle del producto") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                    },
                    actions = {
                        val p = estado.producto
                        if (p != null) {
                            IconButton(onClick = {
                                lanzadorEditar.launch(Intent(contexto, ProductoFormActivity::class.java).putExtra(ProductoFormActivity.EXTRA_PRODUCTO_ID, p.id))
                            }) { Icon(Icons.Default.Edit, contentDescription = "Editar producto") }
                            IconButton(onClick = viewModel::solicitarEliminar, enabled = !estado.operando) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar producto")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                if (estado.operando) LinearProgressIndicator(Modifier.fillMaxWidth())
                val p = estado.producto
                when {
                    estado.cargando -> PantallaCargando()
                    p == null -> MensajeCentrado(
                        titulo = "Producto no disponible",
                        detalle = "El producto ya no existe.",
                        textoBoton = "Volver",
                        alPulsarBoton = { finish() }
                    )
                    else -> Column(
                        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ImagenProducto(
                            nombreRecurso = p.imagen,
                            descripcion = p.nombre,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(p.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(Formato.moneda(p.precio), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text("Stock: ${p.stock}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Categoría: ${p.categoria}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (p.descripcion.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(p.descripcion)
                        }
                        if (estado.cantidadEnCarrito > 0) Text("En tu carrito: ${estado.cantidadEnCarrito}", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        if (p.stock <= 0) {
                            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Agotado") }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                SelectorCantidad(estado.cantidadSeleccionada, viewModel::disminuir, viewModel::aumentar)
                            }
                            Spacer(Modifier.height(4.dp))
                            Button(onClick = viewModel::agregarAlCarrito, modifier = Modifier.fillMaxWidth()) { Text("Agregar al carrito") }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_PRODUCTO_ID = "producto_id"
        const val EXTRA_MENSAJE = "mensaje"
    }
}
