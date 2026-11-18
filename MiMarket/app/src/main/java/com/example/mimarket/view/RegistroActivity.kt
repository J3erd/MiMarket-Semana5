package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.util.TextosLegales
import com.example.mimarket.view.componentes.DialogoAvisoPrivacidad
import com.example.mimarket.viewmodel.RegistroUiState
import com.example.mimarket.viewmodel.RegistroViewModel

/**
 * Pantalla de registro (semana 5): crea la cuenta con Firebase Authentication (correo y contraseña).
 * Exige aceptar el aviso de privacidad (consentimiento explícito) antes de crear la cuenta.
 * Solo dibuja el estado y envía las acciones al ViewModel; no contiene lógica de negocio.
 */
class RegistroActivity : ComponentActivity() {

    private val viewModel: RegistroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiMarketTheme {
                val uiState by viewModel.uiState.collectAsState()

                // Cuenta creada: Firebase deja la sesión iniciada -> pantalla 4 (Aviso de Privacidad) y se limpia la pila.
                LaunchedEffect(uiState.registrado) {
                    if (uiState.registrado) {
                        startActivity(
                            Intent(this@RegistroActivity, AvisoPrivacidadActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                }

                RegistroContenido(
                    uiState = uiState,
                    alCambiarNombre = viewModel::cambiarNombre,
                    alCambiarEmail = viewModel::cambiarEmail,
                    alCambiarPassword = viewModel::cambiarPassword,
                    alCambiarConfirmacion = viewModel::cambiarConfirmacion,
                    alCambiarAcepta = viewModel::cambiarAceptaPrivacidad,
                    alVerAviso = viewModel::verAviso,
                    alRegistrar = viewModel::registrar,
                    alVolver = { finish() }
                )
            }
        }
    }
}

/** Versión sin estado (stateless): recibe todo por parámetros, ideal para previsualizar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroContenido(
    uiState: RegistroUiState,
    alCambiarNombre: (String) -> Unit,
    alCambiarEmail: (String) -> Unit,
    alCambiarPassword: (String) -> Unit,
    alCambiarConfirmacion: (String) -> Unit,
    alCambiarAcepta: (Boolean) -> Unit,
    alVerAviso: (Boolean) -> Unit,
    alRegistrar: () -> Unit,
    alVolver: () -> Unit
) {
    // Texto completo del aviso de privacidad (informativo; la aceptación es la casilla)
    if (uiState.mostrarAviso) {
        DialogoAvisoPrivacidad(
            titulo = "Aviso de Privacidad",
            texto = TextosLegales.AVISO_PRIVACIDAD,
            textoAceptar = "Entendido",
            alAceptar = { alVerAviso(false) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al inicio de sesión")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = uiState.nombre, onValueChange = alCambiarNombre,
                label = { Text("Nombre completo") }, singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.errorNombre != null,
                supportingText = { uiState.errorNombre?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.email, onValueChange = alCambiarEmail,
                label = { Text("Correo electrónico") }, singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.errorEmail != null,
                supportingText = { uiState.errorEmail?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            // Las contraseñas se ocultan al escribir y nunca se guardan en el dispositivo
            OutlinedTextField(
                value = uiState.password, onValueChange = alCambiarPassword,
                label = { Text("Contraseña (mínimo 6 caracteres)") }, singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.errorPassword != null,
                supportingText = { uiState.errorPassword?.let { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.confirmacion, onValueChange = alCambiarConfirmacion,
                label = { Text("Confirmar contraseña") }, singleLine = true,
                enabled = !uiState.isLoading,
                isError = uiState.errorConfirmacion != null,
                supportingText = { uiState.errorConfirmacion?.let { Text(it) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            // Consentimiento explícito: casilla desmarcada por defecto + enlace al aviso completo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = uiState.aceptaPrivacidad,
                    onCheckedChange = alCambiarAcepta,
                    enabled = !uiState.isLoading
                )
                Text("Acepto el Aviso de Privacidad", modifier = Modifier.weight(1f))
                TextButton(onClick = { alVerAviso(true) }) { Text("Leer") }
            }
            if (uiState.errorPrivacidad != null) {
                Text(uiState.errorPrivacidad, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            // Error de Firebase ya traducido (ej. "Ya existe una cuenta registrada con ese correo")
            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = alRegistrar,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Registrarme", fontSize = 16.sp)
                }
            }
            TextButton(onClick = alVolver, enabled = !uiState.isLoading, modifier = Modifier.fillMaxWidth()) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegistroPreview() {
    MiMarketTheme {
        RegistroContenido(
            uiState = RegistroUiState(),
            alCambiarNombre = {}, alCambiarEmail = {}, alCambiarPassword = {}, alCambiarConfirmacion = {},
            alCambiarAcepta = {}, alVerAviso = {}, alRegistrar = {}, alVolver = {}
        )
    }
}
