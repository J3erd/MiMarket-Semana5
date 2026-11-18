package com.example.mimarket.view.componentes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Pestañas de la barra inferior (igual que el flujo de pantallas: Inicio, Catálogo, Carrito, Perfil). */
enum class Pestana { INICIO, CATALOGO, CARRITO, PERFIL }

/**
 * Barra de navegación inferior compartida por Home y Catálogo.
 * Solo dibuja; la navegación real (Intents) la hace la Activity con [alNavegar].
 */
@Suppress("DEPRECATION")
@Composable
fun BarraInferior(
    seleccionada: Pestana,
    cantidadCarrito: Int,
    alNavegar: (Pestana) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = seleccionada == Pestana.INICIO,
            onClick = { alNavegar(Pestana.INICIO) },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = seleccionada == Pestana.CATALOGO,
            onClick = { alNavegar(Pestana.CATALOGO) },
            icon = { Icon(Icons.Default.List, contentDescription = null) },
            label = { Text("Catálogo") }
        )
        // Carrito con la cantidad de unidades como insignia
        NavigationBarItem(
            selected = seleccionada == Pestana.CARRITO,
            onClick = { alNavegar(Pestana.CARRITO) },
            icon = {
                BadgedBox(badge = { if (cantidadCarrito > 0) Badge { Text(cantidadCarrito.toString()) } }) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                }
            },
            label = { Text("Carrito") }
        )
        NavigationBarItem(
            selected = seleccionada == Pestana.PERFIL,
            onClick = { alNavegar(Pestana.PERFIL) },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Perfil") }
        )
    }
}
