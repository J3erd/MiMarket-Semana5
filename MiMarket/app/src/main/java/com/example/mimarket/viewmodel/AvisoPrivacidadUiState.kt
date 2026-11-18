package com.example.mimarket.viewmodel

/**
 * Estado de la pantalla 4 - Aviso de Privacidad.
 *
 * @property mostrarCompleto true mientras se muestra el aviso completo ("Leer aviso completo").
 * @property aceptado true cuando Firestore guardó el consentimiento -> la vista navega al Home.
 */
data class AvisoPrivacidadUiState(
    val enviando: Boolean = false,
    val mostrarCompleto: Boolean = false,
    val aceptado: Boolean = false,
    val errorMessage: String? = null
)
