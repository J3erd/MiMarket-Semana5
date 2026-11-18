package com.example.mimarket.viewmodel

import com.example.mimarket.model.Producto

/**
 * Estado de la pantalla 5 - Home (sesión iniciada).
 *
 * @property nombreUsuario Nombre para el saludo "¡Hola, ...!" (Firestore usuarios/{uid}).
 * @property ofertasDestacadas Algunos productos del catálogo con stock (tiempo real).
 * @property cantidadCarrito Unidades en el carrito (insignia de la barra inferior).
 */
data class HomeUiState(
    val nombreUsuario: String = "",
    val ofertasDestacadas: List<Producto> = emptyList(),
    val cantidadCarrito: Int = 0,
    val cargando: Boolean = true
)
