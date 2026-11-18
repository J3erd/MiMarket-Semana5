package com.example.mimarket.viewmodel

import com.example.mimarket.model.Usuario

/**
 * Estado de la pantalla "Perfil de usuario".
 *
 * @property usuario Datos personales leídos de Firestore (usuarios/{uid}).
 * @property consentimientoUbicacion true si aceptó el aviso de privacidad de ubicación.
 * @property mostrarAviso true mientras se muestra el aviso de privacidad completo.
 * @property confirmandoEliminar true mientras se muestra el diálogo "Eliminar cuenta".
 * @property claveConfirmacion Contraseña escrita para confirmar la eliminación (no se guarda).
 * @property operando true mientras se ejecuta una operación (cerrar sesión, eliminar, publicar tiendas).
 * @property sesionTerminada true al cerrar sesión o eliminar la cuenta -> la vista vuelve al login.
 */
data class PerfilUiState(
    val usuario: Usuario? = null,
    val cargando: Boolean = true,
    val consentimientoUbicacion: Boolean = false,
    val mostrarAviso: Boolean = false,
    val confirmandoEliminar: Boolean = false,
    val claveConfirmacion: String = "",
    val errorEliminar: String? = null,
    val operando: Boolean = false,
    val sesionTerminada: Boolean = false,
    val cuentaEliminada: Boolean = false,
    val mensaje: String? = null
)
