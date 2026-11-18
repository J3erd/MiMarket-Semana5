package com.example.mimarket.view.componentes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Muestra la imagen de un producto: una URL de Firebase Storage (se descarga con Coil) o el nombre
 * de un drawable local (ej. "prod_manzana").
 * Si el recurso no existe, dibuja un ícono de carrito como marcador.
 */
@Composable
fun ImagenProducto(
    nombreRecurso: String,
    descripcion: String?,
    modifier: Modifier = Modifier
) {
    // Si el servidor guarda una URL (http/https) la imagen se descarga con Coil.
    if (nombreRecurso.startsWith("http")) {
        AsyncImage(
            model = nombreRecurso,
            contentDescription = descripcion,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
        return
    }
    val contexto = LocalContext.current
    // getIdentifier convierte el nombre guardado en Room ("prod_manzana") en el id real del drawable
    val recursoId = remember(nombreRecurso) {
        contexto.resources.getIdentifier(nombreRecurso, "drawable", contexto.packageName)
    }
    // 0 significa que el recurso no existe
    if (recursoId != 0) {
        Image(
            painter = painterResource(id = recursoId),
            contentDescription = descripcion,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        // Marcador de posición si falta la imagen
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = descripcion)
        }
    }
}

/** Indicador de carga centrado en toda la pantalla (estado Loading). */
@Composable
fun PantallaCargando(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** Mensaje centrado con botón opcional (estados Empty y Error). */
@Composable
fun MensajeCentrado(
    titulo: String,
    detalle: String? = null,
    textoBoton: String? = null,
    alPulsarBoton: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = titulo,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        if (detalle != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = detalle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (textoBoton != null && alPulsarBoton != null) {
            Spacer(Modifier.height(20.dp))
            Button(onClick = alPulsarBoton) { Text(textoBoton) }
        }
    }
}

/** Control "- cantidad +" para modificar unidades. */
@Composable
fun SelectorCantidad(
    cantidad: Int,
    alDisminuir: () -> Unit,
    alAumentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalIconButton(onClick = alDisminuir, modifier = Modifier.size(34.dp)) {
            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = cantidad.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )
        FilledTonalIconButton(onClick = alAumentar, modifier = Modifier.size(34.dp)) {
            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Semana 5 - Aviso de privacidad / consentimiento explícito.
 * Se usa en el registro, en el perfil y, sobre todo, ANTES de pedir el permiso de ubicación.
 * Si [alRechazar] es null el diálogo es solo informativo (un único botón "Entendido").
 */
@Composable
fun DialogoAvisoPrivacidad(
    titulo: String,
    texto: String,
    textoAceptar: String,
    alAceptar: () -> Unit,
    textoRechazar: String? = null,
    alRechazar: (() -> Unit)? = null
) {
    AlertDialog(
        // Tocar fuera equivale a no aceptar (o a cerrar, si es informativo).
        onDismissRequest = alRechazar ?: alAceptar,
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                Text(texto, fontSize = 14.sp)
            }
        },
        confirmButton = { Button(onClick = alAceptar) { Text(textoAceptar) } },
        dismissButton = {
            if (textoRechazar != null && alRechazar != null) {
                TextButton(onClick = alRechazar) { Text(textoRechazar) }
            }
        }
    )
}
