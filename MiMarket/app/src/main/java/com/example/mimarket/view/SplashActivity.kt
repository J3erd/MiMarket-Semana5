package com.example.mimarket.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mimarket.ui.theme.MiMarketTheme
import com.example.mimarket.ui.theme.NaranjaAcento
import com.example.mimarket.viewmodel.DestinoSplash
import com.example.mimarket.viewmodel.SplashViewModel

/** Pantalla 1 - Splash (pantalla de arranque de la app). */
class SplashActivity : ComponentActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiMarketTheme {
                val destino by viewModel.destino.collectAsState()
                LaunchedEffect(destino) {
                    val pantalla = when (destino) {
                        DestinoSplash.LOGIN -> InicioSesionActivity::class.java
                        DestinoSplash.AVISO_PRIVACIDAD -> AvisoPrivacidadActivity::class.java
                        DestinoSplash.HOME -> HomeActivity::class.java
                        null -> return@LaunchedEffect
                    }
                    startActivity(Intent(this@SplashActivity, pantalla))
                    finish() // el Splash no queda en la pila de atrás
                }
                SplashContenido()
            }
        }
    }
}

@Composable
fun SplashContenido() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Mi ") }
                    withStyle(SpanStyle(color = NaranjaAcento)) { append("Market") }
                },
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text("Tu minimarket cerca de ti", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashPreview() {
    MiMarketTheme { SplashContenido() }
}
