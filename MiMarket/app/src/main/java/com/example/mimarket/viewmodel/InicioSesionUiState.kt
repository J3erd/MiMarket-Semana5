package com.example.mimarket.viewmodel

/**
 * Estado de la pantalla de inicio de sesión (equivale a LoginUiState).
 *
 * @property email Texto del campo correo.
 * @property password Texto del campo contraseña.
 * @property errorEmail Error de validación del correo (null si es válido).
 * @property errorPassword Error de validación de la contraseña.
 * @property isLoading `true` mientras se validan las credenciales.
 * @property verificandoSesion `true` al abrir la app, mientras se consulta si hay una sesión guardada.
 * @property errorMessage Error general (ej. credenciales incorrectas).
 * @property isLoggedIn `true` cuando el usuario quedó autenticado -> la vista navega al catálogo.
 */
data class InicioSesionUiState(
    val email: String = "",
    val password: String = "",
    val errorEmail: String? = null,
    val errorPassword: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val verificandoSesion: Boolean = true,
    val requiereAviso: Boolean = false,   // true si aún no aceptó el aviso de privacidad -> pantalla 4
    val mensajeInfo: String? = null       // aviso informativo (ej. "te enviamos un correo para restablecer...")
) {
    /** Estado genérico que la pantalla usa para decidir qué dibujar (cargando / error / éxito / vacío). */
    val estado: EstadoUi
        get() = when {
            isLoading || verificandoSesion -> EstadoUi.CARGANDO
            errorMessage != null || errorEmail != null || errorPassword != null -> EstadoUi.ERROR
            isLoggedIn -> EstadoUi.EXITO
            else -> EstadoUi.VACIO
        }
}
