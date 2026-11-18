package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.util.Formato
import com.example.mimarket.util.TextosLegales
import com.example.mimarket.view.componentes.DialogoAvisoPrivacidad
import com.example.mimarket.view.componentes.PantallaCargando
import com.example.mimarket.viewmodel.PerfilUiState
import com.example.mimarket.viewmodel.PerfilViewModel

/**
 * Pantalla "Perfil de usuario" (semana 5): datos personales leídos de Firestore y opciones de
 * privacidad: ver el aviso, revocar el consentimiento de ubicación, cerrar sesión y eliminar la
 * cuenta con todos los datos personales.
 */
class PerfilActivity : ComponentActivity() {

    private val viewModel: PerfilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        setContent {
            MiMarketTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Sesión cerrada o cuenta eliminada: vuelve al login y borra la pila de pantallas (CLEAR_TASK)
                LaunchedEffect(uiState.sesionTerminada) {
                    if (uiState.sesionTerminada) {
                        if (uiState.cuentaEliminada) {
                            Toast.makeText(this@PerfilActivity, "Tu cuenta y tus datos personales fueron eliminados", Toast.LENGTH_LONG).show()
                        }
                        startActivity(
                            Intent(this@PerfilActivity, InicioSesionActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                }

                PerfilContenido(
                    uiState = uiState,
                    alVerHistorial = { startActivity(Intent(this, HistorialComprasActivity::class.java)) },
                    alVerAviso = viewModel::verAviso,
                    alRevocarUbicacion = viewModel::revocarConsentimientoUbicacion,
                    alPublicarTiendas = viewModel::publicarTiendas,
                    alCerrarSesion = viewModel::cerrarSesion,
                    alSolicitarEliminar = viewModel::solicitarEliminarCuenta,
                    alCambiarClave = viewModel::cambiarClaveConfirmacion,
                    alConfirmarEliminar = viewModel::confirmarEliminarCuenta,
                    alCancelarEliminar = viewModel::cancelarEliminarCuenta,
                    alMensajeMostrado = viewModel::mensajeMostrado,
                    alVolver = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilContenido(
    uiState: PerfilUiState,
    alVerHistorial: () -> Unit,
    alVerAviso: (Boolean) -> Unit,
    alRevocarUbicacion: () -> Unit,
    alPublicarTiendas: () -> Unit,
    alCerrarSesion: () -> Unit,
    alSolicitarEliminar: () -> Unit,
    alCambiarClave: (String) -> Unit,
    alConfirmarEliminar: () -> Unit,
    alCancelarEliminar: () -> Unit,
    alMensajeMostrado: () -> Unit,
    alVolver: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let { snackbar.showSnackbar(it); alMensajeMostrado() }
    }

    if (uiState.mostrarAviso) {
        DialogoAvisoPrivacidad(
            titulo = "Aviso de Privacidad",
            texto = TextosLegales.AVISO_PRIVACIDAD,
            textoAceptar = "Entendido",
            alAceptar = { alVerAviso(false) }
        )
    }

    // Diálogo "Eliminar cuenta": explica qué se borra y exige la contraseña para confirmar
    if (uiState.confirmandoEliminar) {
        AlertDialog(
            onDismissRequest = { if (!uiState.operando) alCancelarEliminar() },
            title = { Text("Eliminar cuenta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se eliminarán de forma permanente tu cuenta, tus datos personales y tu historial de pedidos. " +
                            "Esta acción no se puede deshacer."
                    )
                    OutlinedTextField(
                        value = uiState.claveConfirmacion,
                        onValueChange = alCambiarClave,
                        label = { Text("Escribe tu contraseña") },
                        singleLine = true,
                        enabled = !uiState.operando,
                        isError = uiState.errorEliminar != null,
                        supportingText = { uiState.errorEliminar?.let { Text(it) } },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    if (uiState.operando) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = alConfirmarEliminar, enabled = !uiState.operando) {
                    Text("Eliminar definitivamente", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = alCancelarEliminar, enabled = !uiState.operando) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil", fontWeight = FontWeight.Bold) },
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
        val usuario = uiState.usuario
        if (uiState.cargando || usuario == null) {
            PantallaCargando(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.operando && !uiState.confirmandoEliminar) LinearProgressIndicator(Modifier.fillMaxWidth())

            // ---- Datos personales (solo el propio usuario puede leerlos: reglas de Firestore) ----
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(usuario.nombre.ifBlank { "Usuario" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(usuario.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))
                    FilaPerfil("Rol", if (usuario.esAdmin) "Administrador" else "Cliente")
                    FilaPerfil("Dirección de entrega", usuario.direccionEntrega.ifBlank { "Sin definir (elígela desde el mapa)" })
                    if (usuario.fechaRegistro > 0) FilaPerfil("Miembro desde", Formato.fecha(usuario.fechaRegistro))
                }
            }

            OutlinedButton(onClick = alVerHistorial, modifier = Modifier.fillMaxWidth()) { Text("Mis pedidos") }

            // ---- Privacidad ----
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacidad y protección de datos", fontWeight = FontWeight.Bold)
                    FilaPerfil(
                        "Uso de mi ubicación",
                        if (uiState.consentimientoUbicacion) "Aceptado" else "No aceptado"
                    )
                    TextButton(onClick = { alVerAviso(true) }) { Text("Ver aviso de privacidad") }
                    if (uiState.consentimientoUbicacion) {
                        // Derecho a retirar el consentimiento: el mapa volverá a preguntar antes de usar la ubicación
                        TextButton(onClick = alRevocarUbicacion) { Text("Revocar el uso de mi ubicación") }
                    }
                }
            }

            // ---- Solo administradores ----
            if (usuario.esAdmin) {
                OutlinedButton(onClick = alPublicarTiendas, enabled = !uiState.operando, modifier = Modifier.fillMaxWidth()) {
                    Text("Publicar tiendas de ejemplo en Firestore")
                }
            }

            Spacer(Modifier.height(4.dp))
            Button(onClick = alCerrarSesion, enabled = !uiState.operando, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
            // Derecho de eliminación: borra la cuenta y los datos personales desde la propia app
            Button(
                onClick = alSolicitarEliminar,
                enabled = !uiState.operando,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Eliminar mi cuenta y mis datos") }
        }
    }
}

/** Una línea "etiqueta: valor" de la tarjeta de datos. */
@Composable
private fun FilaPerfil(etiqueta: String, valor: String) {
    Column {
        Text(etiqueta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor)
    }
}
