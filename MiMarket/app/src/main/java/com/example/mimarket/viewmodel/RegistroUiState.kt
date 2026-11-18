package com.example.mimarket.viewmodel

/**
 * Estado de la pantalla de registro de cuenta.
 *
 * @property aceptaPrivacidad true si marcó la casilla del aviso de privacidad (obligatoria).
 * @property mostrarAviso true mientras se muestra el texto completo del aviso de privacidad.
 * @property registrado true cuando Firebase creó la cuenta -> la vista navega al catálogo.
 */
data class RegistroUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val confirmacion: String = "",
    val aceptaPrivacidad: Boolean = false,
    val mostrarAviso: Boolean = false,
    val errorNombre: String? = null,
    val errorEmail: String? = null,
    val errorPassword: String? = null,
    val errorConfirmacion: String? = null,
    val errorPrivacidad: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrado: Boolean = false
)
