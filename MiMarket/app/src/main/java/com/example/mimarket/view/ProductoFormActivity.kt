package com.example.mimarket.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import com.example.mimarket.ui.theme.VerdeExito
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.view.componentes.ImagenProducto
import com.example.mimarket.view.componentes.MensajeCentrado
import com.example.mimarket.view.componentes.PantallaCargando
import com.example.mimarket.viewmodel.ProductoFormUiState
import com.example.mimarket.viewmodel.ProductoFormViewModel

/**
 * Formulario para registrar (Create) o editar (Update) un producto en Cloud Firestore.
 * Si recibe EXTRA_PRODUCTO_ID > 0 funciona como edición; si no, como registro.
 * Semana 5: permite elegir una imagen de la galería; al guardar se sube a Firebase Storage y su URL
 * de descarga se guarda en el documento. Al terminar con éxito devuelve RESULT_OK + el mensaje.
 */
class ProductoFormActivity : ComponentActivity() {

    private val viewModel: ProductoFormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        viewModel.iniciar(intent.getLongExtra(EXTRA_PRODUCTO_ID, 0L))
        setContent {
            MiMarketTheme {
                val estado by viewModel.uiState.collectAsState()

                // Selector de fotos del sistema (Photo Picker): el usuario elige UNA imagen y la app solo
                // recibe esa imagen. No requiere pedir permiso de almacenamiento (mínimo privilegio).
                val selectorImagen = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
                    viewModel.seleccionarImagen(uri)
                }

                // Cuando Firestore confirma la operación se muestra la pantalla 10 "¡Producto guardado!";
                // su botón "Ver en catálogo" avisa al catálogo y cierra el formulario.
                FormularioProducto(
                    estado = estado,
                    alCambiarNombre = viewModel::cambiarNombre,
                    alCambiarDescripcion = viewModel::cambiarDescripcion,
                    alCambiarPrecio = viewModel::cambiarPrecio,
                    alCambiarStock = viewModel::cambiarStock,
                    alCambiarCategoria = viewModel::cambiarCategoria,
                    alElegirImagen = {
                        selectorImagen.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    alQuitarImagen = viewModel::quitarImagenSeleccionada,
                    alGuardar = viewModel::guardar,
                    alReintentarCarga = viewModel::cargarProducto,
                    alVolver = { finish() },
                    alVerEnCatalogo = {
                        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_MENSAJE, estado.mensajeExito))
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PRODUCTO_ID = "producto_id"
        const val EXTRA_MENSAJE = "mensaje"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioProducto(
    estado: ProductoFormUiState,
    alCambiarNombre: (String) -> Unit,
    alCambiarDescripcion: (String) -> Unit,
    alCambiarPrecio: (String) -> Unit,
    alCambiarStock: (String) -> Unit,
    alCambiarCategoria: (String) -> Unit,
    alElegirImagen: () -> Unit,
    alQuitarImagen: () -> Unit,
    alGuardar: () -> Unit,
    alReintentarCarga: () -> Unit,
    alVolver: () -> Unit,
    alVerEnCatalogo: () -> Unit = alVolver
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.esEdicion) "Editar producto" else "Registrar producto") },
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
            when {
                // Pantalla 10 - Producto guardado (imagen subida)
                estado.mensajeExito != null -> ProductoGuardado(estado, alVerEnCatalogo)
                // Cargando los datos actuales del producto a editar
                estado.cargando -> PantallaCargando()
                // No se pudo obtener el producto a editar (no existe / sin conexión / error): mensaje + reintentar
                estado.esEdicion && estado.errorMessage != null && estado.nombre.isEmpty() -> MensajeCentrado(
                    titulo = if (estado.productoNoExiste) "Producto no encontrado"
                    else if (estado.sinConexion) "Sin conexión" else "No se pudo cargar el producto",
                    detalle = estado.errorMessage,
                    textoBoton = if (estado.productoNoExiste) "Volver" else "Reintentar",
                    alPulsarBoton = if (estado.productoNoExiste) alVolver else alReintentarCarga
                )
                else -> Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ---- Imagen del producto (galería -> Firebase Storage) ----
                    Text("Imagen del producto", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    val formaImagen = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(12.dp))
                    if (estado.imagenLocal != null) {
                        // Vista previa de la imagen elegida (todavía no se subió)
                        AsyncImage(
                            model = estado.imagenLocal,
                            contentDescription = "Imagen seleccionada",
                            contentScale = ContentScale.Crop,
                            modifier = formaImagen
                        )
                        Text(
                            "Se subirá a Firebase Storage al guardar.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Imagen actual del producto (o un marcador si aún no tiene)
                        ImagenProducto(nombreRecurso = estado.imagen, descripcion = "Imagen actual", modifier = formaImagen)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = alElegirImagen, enabled = !estado.enviando) {
                            Text(if (estado.imagenLocal != null || estado.imagen.isNotBlank()) "Cambiar imagen" else "Seleccionar imagen de la galería")
                        }
                        if (estado.imagenLocal != null) {
                            TextButton(onClick = alQuitarImagen, enabled = !estado.enviando) { Text("Quitar") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = estado.nombre, onValueChange = alCambiarNombre,
                        label = { Text("Nombre *") }, singleLine = true,
                        isError = estado.errorNombre != null,
                        supportingText = { estado.errorNombre?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = estado.descripcion, onValueChange = alCambiarDescripcion,
                        label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = estado.precio, onValueChange = alCambiarPrecio,
                        label = { Text("Precio (S/) *") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = estado.errorPrecio != null,
                        supportingText = { estado.errorPrecio?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = estado.stock, onValueChange = alCambiarStock,
                        label = { Text("Stock *") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = estado.errorStock != null,
                        supportingText = { estado.errorStock?.let { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = estado.categoria, onValueChange = alCambiarCategoria,
                        label = { Text("Categoría (opcional)") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Error de Firebase: mensaje comprensible; la pantalla sigue activa
                    if (estado.errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        if (estado.permisoDenegado) {
                            // Las reglas de seguridad de Firestore bloquearon la edición de un producto ajeno
                            Text("Acción bloqueada por seguridad", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                        Text(estado.errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(16.dp))
                    // Mientras envía, el botón se deshabilita y muestra progreso (evita envíos duplicados)
                    Button(onClick = alGuardar, enabled = !estado.enviando, modifier = Modifier.fillMaxWidth()) {
                        if (estado.enviando) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (estado.errorMessage != null && !estado.permisoDenegado) "Reintentar" else "Guardar")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = alVolver, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                }
            }
        }
    }
}

/** Pantalla 10 - "¡Producto guardado!": confirma que la imagen se subió a Storage y el producto se guardó en Firestore. */
@Composable
private fun ProductoGuardado(estado: ProductoFormUiState, alVerEnCatalogo: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VerdeExito, modifier = Modifier.size(96.dp))
        Text("¡Producto guardado!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            if (estado.imagenLocal != null) "La imagen se subió a Firebase Storage y el producto se guardó en Cloud Firestore."
            else "El producto se guardó en Cloud Firestore.",
            textAlign = TextAlign.Center
        )
        Button(onClick = alVerEnCatalogo, modifier = Modifier.fillMaxWidth()) { Text("Ver en catálogo") }
    }
}
