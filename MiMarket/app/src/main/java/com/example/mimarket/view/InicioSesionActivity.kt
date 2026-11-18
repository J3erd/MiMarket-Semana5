package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.ui.theme.NaranjaAcento
import com.example.mimarket.viewmodel.InicioSesionUiState
import com.example.mimarket.viewmodel.InicioSesionViewModel

/**
 * Pantalla 1 - Inicio de sesión (Firebase Authentication, con sesión persistente).
 * Solo dibuja el estado y envía las acciones al ViewModel; no contiene lógica de negocio.
 */
class InicioSesionActivity : ComponentActivity() {

    // ViewModel asociado a esta pantalla; sobrevive a giros de pantalla (by viewModels())
    private val viewModel: InicioSesionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent: define la interfaz con Jetpack Compose (no hay XML)
        setContent {
            MiMarketTheme {
                InicioSesionScreen(
                    viewModel = viewModel,
                    alIniciarSesionExitoso = { requiereAviso ->
                        // Login correcto: Aviso de Privacidad (si aún no lo aceptó) o Home. El login no queda en la pila.
                        val destino = if (requiereAviso) AvisoPrivacidadActivity::class.java else HomeActivity::class.java
                        startActivity(Intent(this, destino))
                        finish()
                    },
                    // Abre la pantalla de registro (crear cuenta con Firebase Authentication)
                    alIrARegistro = { startActivity(Intent(this, RegistroActivity::class.java)) }
                )
            }
        }
    }
}

/** Versión con estado: observa el ViewModel y navega cuando [InicioSesionUiState.isLoggedIn] es `true`. */
@Composable
fun InicioSesionScreen(
    viewModel: InicioSesionViewModel,
    alIniciarSesionExitoso: (requiereAviso: Boolean) -> Unit,
    alIrARegistro: () -> Unit
) {
    // Convierte el StateFlow del ViewModel en un State de Compose: al cambiar, la pantalla se redibuja
    val uiState by viewModel.uiState.collectAsState()

    // Efecto: cuando isLoggedIn pasa a true (login nuevo o sesión recordada) se navega al Home
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) alIniciarSesionExitoso(uiState.requiereAviso)
    }

    InicioSesionContenido(
        uiState = uiState,
        alCambiarEmail = viewModel::onEmailChange,
        alCambiarPassword = viewModel::onPasswordChange,
        alIngresar = viewModel::iniciarSesion,
        alIrARegistro = alIrARegistro,
        alRecuperarClave = viewModel::recuperarClave
    )
}

/** Versión sin estado (stateless): recibe todo por parámetros, ideal para previsualizar. */
@Composable
fun InicioSesionContenido(
    uiState: InicioSesionUiState,
    alCambiarEmail: (String) -> Unit,
    alCambiarPassword: (String) -> Unit,
    alIngresar: () -> Unit,
    alIrARegistro: () -> Unit = {},
    alRecuperarClave: () -> Unit = {}
) {
    Scaffold { padding ->
        // Mientras se consulta la sesión guardada (o ya se autenticó) se muestra solo un indicador de carga.
        if (uiState.verificandoSesion || uiState.isLoggedIn) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logotipo: icono de carrito y nombre "Mi Market" con dos colores
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Mi ") }
                    withStyle(SpanStyle(color = NaranjaAcento)) { append("Market") }
                },
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(24.dp))

            // Tarjeta con el formulario de inicio de sesión
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Iniciar sesión", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                    // Campo correo: el texto vive en el UiState; isError/supportingText muestran la validación
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = alCambiarEmail,
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        isError = uiState.errorEmail != null,
                        supportingText = { uiState.errorEmail?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Campo contraseña: PasswordVisualTransformation oculta los caracteres
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = alCambiarPassword,
                        label = { Text("Contraseña") },
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        isError = uiState.errorPassword != null,
                        supportingText = { uiState.errorPassword?.let { Text(it) } },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // "¿Olvidaste tu contraseña?": Firebase envía un correo para restablecerla
                    TextButton(onClick = alRecuperarClave, enabled = !uiState.isLoading) {
                        Text("¿Olvidaste tu contraseña?")
                    }

                    // Error general (credenciales incorrectas): el usuario permanece en el login
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Aviso informativo (correo de recuperación enviado)
                    if (uiState.mensajeInfo != null) {
                        Text(text = uiState.mensajeInfo, color = MaterialTheme.colorScheme.primary)
                    }

                    // Botón Ingresar: solo avisa al ViewModel; se deshabilita y muestra un spinner mientras valida
                    Button(
                        onClick = alIngresar,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Ingresar", fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // Semana 5: las cuentas se crean con Firebase Authentication desde la pantalla de registro
            TextButton(onClick = alIrARegistro, enabled = !uiState.isLoading) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InicioSesionPreview() {
    MiMarketTheme {
        InicioSesionContenido(
            uiState = InicioSesionUiState(verificandoSesion = false),
            alCambiarEmail = {},
            alCambiarPassword = {},
            alIngresar = {}
        )
    }
}
