package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.util.TextosLegales
import com.example.mimarket.view.componentes.DialogoAvisoPrivacidad
import com.example.mimarket.viewmodel.AvisoPrivacidadUiState
import com.example.mimarket.viewmodel.AvisoPrivacidadViewModel

/**
 * Pantalla 4 - Aviso de Privacidad. Aparece después del registro (y al iniciar sesión si el usuario
 * aún no lo aceptó). El consentimiento queda guardado en Firestore (usuarios/{uid}.aceptoPrivacidad).
 */
class AvisoPrivacidadActivity : ComponentActivity() {

    private val viewModel: AvisoPrivacidadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!requerirSesion()) return
        setContent {
            MiMarketTheme {
                val uiState by viewModel.uiState.collectAsState()
                // Consentimiento guardado -> Home (sin volver atrás al aviso)
                LaunchedEffect(uiState.aceptado) {
                    if (uiState.aceptado) {
                        startActivity(
                            Intent(this@AvisoPrivacidadActivity, HomeActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                }
                AvisoPrivacidadContenido(
                    uiState = uiState,
                    alAceptar = viewModel::aceptar,
                    alVerCompleto = viewModel::verAvisoCompleto
                )
            }
        }
    }
}

@Composable
fun AvisoPrivacidadContenido(
    uiState: AvisoPrivacidadUiState,
    alAceptar: () -> Unit,
    alVerCompleto: (Boolean) -> Unit
) {
    if (uiState.mostrarCompleto) {
        DialogoAvisoPrivacidad(
            titulo = "Aviso de Privacidad",
            texto = TextosLegales.AVISO_PRIVACIDAD,
            textoAceptar = "Entendido",
            alAceptar = { alVerCompleto(false) }
        )
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text("Aviso de Privacidad", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Text(TextosLegales.AVISO_RESUMEN, modifier = Modifier.padding(16.dp))
            }
            if (uiState.errorMessage != null) {
                Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = alAceptar,
                enabled = !uiState.enviando,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState.enviando) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Aceptar y continuar")
            }
            TextButton(onClick = { alVerCompleto(true) }) { Text("Leer aviso completo") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AvisoPrivacidadPreview() {
    MiMarketTheme { AvisoPrivacidadContenido(AvisoPrivacidadUiState(), {}, {}) }
}
