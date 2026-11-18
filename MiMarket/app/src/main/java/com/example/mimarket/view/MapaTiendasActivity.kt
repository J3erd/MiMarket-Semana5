package com.example.mimarket.view

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.mimarket.BuildConfig
import com.example.mimarket.model.Tienda
import com.example.mimarket.model.UbicacionUsuario
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.util.TextosLegales
import com.example.mimarket.view.componentes.DialogoAvisoPrivacidad
import com.example.mimarket.viewmodel.EstadoUbicacion
import com.example.mimarket.viewmodel.MapaTiendasUiState
import com.example.mimarket.viewmodel.MapaTiendasViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Pantalla "Mapa de tiendas" (semana 5): Google Maps en Jetpack Compose con un marcador por tienda
 * y la ubicación actual del usuario.
 *
 * Orden del flujo (privacidad primero):
 *   1) Aviso de privacidad -> 2) permiso de ubicación en tiempo de ejecución -> 3) ubicación.
 * Si el usuario no acepta el aviso o deniega el permiso, la pantalla SIGUE funcionando: muestra el
 * mapa con las tiendas y permite elegir una tienda de la lista (flujo alternativo).
 *
 * La vista solo dibuja el estado y lanza los diálogos del sistema; las decisiones las toma el ViewModel.
 */
class MapaTiendasActivity : ComponentActivity() {

    private val viewModel: MapaTiendasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        setContent {
            MiMarketTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Permisos en tiempo de ejecución: se piden ubicación precisa y aproximada a la vez;
                // el usuario decide cuál concede (o ninguna).
                val lanzadorPermisos = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { resultado ->
                    val concedido = resultado.values.any { it }
                    // Si Android ya no mostrará el diálogo ("No volver a preguntar"), solo queda ir a Ajustes.
                    val puedeVolverAPreguntar = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    viewModel.alResponderPermiso(concedido, puedeVolverAPreguntar)
                }

                // El ViewModel decide CUÁNDO pedir el permiso (solo después de aceptar el aviso de privacidad).
                LaunchedEffect(uiState.solicitarPermiso) {
                    if (uiState.solicitarPermiso) {
                        lanzadorPermisos.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                MapaTiendasContenido(
                    uiState = uiState,
                    alAceptarAviso = viewModel::aceptarAvisoPrivacidad,
                    alRechazarAviso = viewModel::rechazarAvisoPrivacidad,
                    alSolicitarUbicacion = viewModel::solicitarUbicacion,
                    alReintentarUbicacion = viewModel::obtenerUbicacion,
                    alSeleccionarTienda = viewModel::seleccionarTienda,
                    alUsarDireccion = viewModel::usarComoDireccionDeEntrega,
                    alMensajeMostrado = viewModel::mensajeMostrado,
                    alAbrirAjustesApp = {
                        // Ajustes de la app: ahí el usuario puede conceder el permiso manualmente
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                        )
                    },
                    alAbrirAjustesUbicacion = { startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
                    // "Cómo llegar": abre la ruta en Google Maps hacia la tienda elegida
                    alComoLlegar = { tienda ->
                        val ruta = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${tienda.latitud},${tienda.longitud}")
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, ruta)) }
                    },
                    alVolver = { finish() }
                )
            }
        }
    }

    /** Al volver de Ajustes se comprueba si ya se concedió el permiso o se encendió el GPS. */
    override fun onResume() {
        super.onResume()
        viewModel.alVolverAPantalla()
    }
}

// Centro de Lima: posición inicial del mapa cuando aún no hay ubicación ni tienda elegida.
private val LIMA = LatLng(-12.0464, -77.0428)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaTiendasContenido(
    uiState: MapaTiendasUiState,
    alAceptarAviso: () -> Unit,
    alRechazarAviso: () -> Unit,
    alSolicitarUbicacion: () -> Unit,
    alReintentarUbicacion: () -> Unit,
    alSeleccionarTienda: (Tienda?) -> Unit,
    alUsarDireccion: (UbicacionUsuario) -> Unit,
    alMensajeMostrado: () -> Unit,
    alAbrirAjustesApp: () -> Unit,
    alAbrirAjustesUbicacion: () -> Unit,
    alComoLlegar: (Tienda) -> Unit = {},
    alVolver: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let { snackbar.showSnackbar(it); alMensajeMostrado() }
    }

    // AVISO DE PRIVACIDAD: se muestra ANTES de pedir el permiso y de leer la ubicación (consentimiento explícito).
    if (uiState.mostrarAvisoPrivacidad) {
        DialogoAvisoPrivacidad(
            titulo = "Aviso de privacidad: uso de tu ubicación",
            texto = TextosLegales.AVISO_UBICACION,
            textoAceptar = "Aceptar y continuar",
            alAceptar = alAceptarAviso,
            textoRechazar = "No acepto",
            alRechazar = alRechazarAviso
        )
    }

    val ubicacion = (uiState.ubicacion as? EstadoUbicacion.Obtenida)?.ubicacion
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LIMA, 11f) }

    // Al obtener la ubicación, el mapa se centra en el usuario.
    LaunchedEffect(ubicacion) {
        if (ubicacion != null) {
            camara.animate(CameraUpdateFactory.newLatLngZoom(LatLng(ubicacion.latitud, ubicacion.longitud), 13f))
        }
    }
    // Al elegir una tienda (lista o marcador), el mapa se centra en ella.
    LaunchedEffect(uiState.tiendaSeleccionada) {
        uiState.tiendaSeleccionada?.let {
            camara.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitud, it.longitud), 15f))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tiendas Mi Market", fontWeight = FontWeight.Bold) },
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
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // ---- Mapa (ocupa el espacio disponible) ----
            Box(Modifier.weight(1f).fillMaxWidth()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camara,
                    uiSettings = MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = false)
                ) {
                    // Un marcador por cada tienda física (coordenadas leídas de Firestore)
                    uiState.tiendas.forEach { tienda ->
                        Marker(
                            state = remember(tienda.id, tienda.latitud, tienda.longitud) {
                                MarkerState(position = LatLng(tienda.latitud, tienda.longitud))
                            },
                            title = tienda.nombre,
                            snippet = tienda.direccion,
                            onClick = {
                                alSeleccionarTienda(tienda)
                                false // false = además muestra la ventana con el nombre y la dirección
                            }
                        )
                    }
                    // Marcador azul con la ubicación actual del usuario (solo si la concedió)
                    if (ubicacion != null) {
                        Marker(
                            state = remember(ubicacion.latitud, ubicacion.longitud) {
                                MarkerState(position = LatLng(ubicacion.latitud, ubicacion.longitud))
                            },
                            title = "Tu ubicación actual",
                            snippet = ubicacion.direccion,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        )
                    }
                }
                if (uiState.cargandoTiendas) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                if (!BuildConfig.MAPS_KEY_CONFIGURADA) {
                    // Ayuda para quien compila el proyecto: sin clave, Google no entrega las imágenes del mapa
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                    ) {
                        Text(
                            "Falta configurar la clave de Google Maps (línea MAPS_API_KEY en local.properties). " +
                                "Las tiendas, tu ubicación y \"Cómo llegar\" sí funcionan.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // ---- Panel inferior: estado de la ubicación + lista de tiendas ----
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    PanelUbicacion(
                        uiState = uiState,
                        alSolicitarUbicacion = alSolicitarUbicacion,
                        alReintentarUbicacion = alReintentarUbicacion,
                        alUsarDireccion = alUsarDireccion,
                        alAbrirAjustesApp = alAbrirAjustesApp,
                        alAbrirAjustesUbicacion = alAbrirAjustesUbicacion,
                        alBuscarManual = { alSeleccionarTienda(uiState.tiendas.firstOrNull()) }
                    )
                    Text(
                        "Nuestras tiendas (toca una para verla en el mapa)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)
                    )
                    // Lista de tiendas: funciona siempre, con o sin permiso de ubicación (flujo alternativo)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.tiendas, key = { it.id }) { tienda ->
                            TarjetaTienda(
                                tienda = tienda,
                                seleccionada = tienda.id == uiState.tiendaSeleccionada?.id,
                                alPulsar = { alSeleccionarTienda(tienda) }
                            )
                        }
                    }
                    // Pantalla 13 - "Cómo llegar" a la tienda elegida (en la lista o tocando su marcador)
                    uiState.tiendaSeleccionada?.let { tienda ->
                        Button(
                            onClick = { alComoLlegar(tienda) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)
                        ) { Text("Cómo llegar a ${tienda.nombre}") }
                    }
                }
            }
        }
    }
}

/**
 * Dibuja un caso distinto por cada estado de la ubicación:
 * sin solicitar / cargando / obtenida / aviso rechazado / permiso denegado / GPS desactivado / error.
 */
@Composable
private fun PanelUbicacion(
    uiState: MapaTiendasUiState,
    alSolicitarUbicacion: () -> Unit,
    alReintentarUbicacion: () -> Unit,
    alUsarDireccion: (UbicacionUsuario) -> Unit,
    alAbrirAjustesApp: () -> Unit,
    alAbrirAjustesUbicacion: () -> Unit,
    alBuscarManual: () -> Unit = {}
) {
    Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (val estado = uiState.ubicacion) {
            EstadoUbicacion.SinSolicitar -> {
                Text("Ubicación", fontWeight = FontWeight.Bold)
                Text("Activa tu ubicación para ver la tienda más cercana.", fontSize = 13.sp)
                Button(onClick = alSolicitarUbicacion) { Text("Usar mi ubicación") }
            }

            EstadoUbicacion.Cargando -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Obteniendo tu ubicación...", modifier = Modifier.padding(start = 10.dp))
            }

            is EstadoUbicacion.Obtenida -> {
                Text("Tu ubicación actual", fontWeight = FontWeight.Bold)
                // Geocodificación inversa: dirección aproximada a partir de las coordenadas
                Text(
                    estado.ubicacion.direccion
                        ?: "Lat ${"%.5f".format(estado.ubicacion.latitud)}, Lng ${"%.5f".format(estado.ubicacion.longitud)}",
                    fontSize = 13.sp
                )
                uiState.tiendaMasCercana?.let { (tienda, km) ->
                    Text(
                        "Tienda más cercana: ${tienda.nombre} (${"%.1f".format(km)} km)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                OutlinedButton(
                    onClick = { alUsarDireccion(estado.ubicacion) },
                    enabled = !uiState.guardandoDireccion
                ) { Text("Usar como dirección de entrega") }
            }

            EstadoUbicacion.AvisoRechazado -> {
                Text("No usaremos tu ubicación", fontWeight = FontWeight.Bold)
                Text(
                    "No aceptaste el aviso de privacidad. Puedes ver las tiendas en el mapa y elegir una de la lista.",
                    fontSize = 13.sp
                )
                OutlinedButton(onClick = alSolicitarUbicacion) { Text("Ver el aviso de nuevo") }
            }

            is EstadoUbicacion.PermisoDenegado -> {
                Text("No tenemos permiso para acceder a tu ubicación", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(
                    "No pasa nada: puedes ver todas las tiendas en el mapa y elegir una manualmente de la lista. " +
                        "Si cambias de opinión, puedes conceder el permiso.",
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Si Android ya no muestra el diálogo, la única vía es la pantalla de Ajustes de la app
                    if (!estado.definitivo) {
                        OutlinedButton(onClick = alSolicitarUbicacion) { Text("Volver a intentar") }
                    }
                    OutlinedButton(onClick = alAbrirAjustesApp) { Text("Abrir configuración") }
                }
                // Flujo alternativo sin ubicación (pantalla 15)
                TextButton(onClick = alBuscarManual) { Text("Buscar tiendas manualmente") }
            }

            EstadoUbicacion.GpsDesactivado -> {
                Text("La ubicación del dispositivo está apagada", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text("Enciende el GPS para ver tu posición. Mientras tanto puedes elegir una tienda de la lista.", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = alAbrirAjustesUbicacion) { Text("Activar ubicación") }
                    OutlinedButton(onClick = alReintentarUbicacion) { Text("Reintentar") }
                }
            }

            is EstadoUbicacion.Error -> {
                Text("No se pudo obtener tu ubicación", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(estado.mensaje, fontSize = 13.sp)
                OutlinedButton(onClick = alReintentarUbicacion) { Text("Reintentar") }
            }
        }
    }
}

/** Tarjeta de una tienda en la lista horizontal. */
@Composable
private fun TarjetaTienda(tienda: Tienda, seleccionada: Boolean, alPulsar: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.width(220.dp).clickable(onClick = alPulsar),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (seleccionada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(tienda.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(tienda.direccion, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(tienda.horario, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
