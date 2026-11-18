package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.repository.UsuarioRepository
import com.example.mimarket.util.Validadores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del registro de cuenta: valida el formulario, exige aceptar el aviso de privacidad
 * y crea la cuenta con Firebase Authentication a través del repositorio.
 */
class RegistroViewModel(
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun cambiarNombre(v: String) = _uiState.update { it.copy(nombre = v, errorNombre = null, errorMessage = null) }
    fun cambiarEmail(v: String) = _uiState.update { it.copy(email = v, errorEmail = null, errorMessage = null) }
    fun cambiarPassword(v: String) = _uiState.update { it.copy(password = v, errorPassword = null, errorMessage = null) }
    fun cambiarConfirmacion(v: String) = _uiState.update { it.copy(confirmacion = v, errorConfirmacion = null, errorMessage = null) }
    fun cambiarAceptaPrivacidad(v: Boolean) = _uiState.update { it.copy(aceptaPrivacidad = v, errorPrivacidad = null) }

    /** Abre / cierra el texto completo del aviso de privacidad. */
    fun verAviso(visible: Boolean) = _uiState.update { it.copy(mostrarAviso = visible) }

    /** Botón "Registrarme": valida todo y, si es correcto, crea la cuenta. */
    fun registrar() {
        val s = _uiState.value
        if (s.isLoading) return

        val errorNombre = Validadores.validarNombre(s.nombre)
        val errorEmail = Validadores.validarEmail(s.email)
        val errorPassword = Validadores.validarClaveNueva(s.password)
        val errorConfirmacion = if (s.confirmacion != s.password) "Las contraseñas no coinciden" else null
        // Consentimiento explícito: sin aceptar el aviso de privacidad no se crea la cuenta.
        val errorPrivacidad = if (!s.aceptaPrivacidad) "Debes aceptar el aviso de privacidad para registrarte" else null
        if (listOf(errorNombre, errorEmail, errorPassword, errorConfirmacion, errorPrivacidad).any { it != null }) {
            _uiState.update {
                it.copy(
                    errorNombre = errorNombre, errorEmail = errorEmail, errorPassword = errorPassword,
                    errorConfirmacion = errorConfirmacion, errorPrivacidad = errorPrivacidad
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val resultado = usuarioRepository.registrarUsuario(s.nombre, s.email, s.password)
            _uiState.update {
                when (resultado) {
                    // Las contraseñas se borran del estado apenas termina el registro.
                    is ResultadoApi.Exito -> it.copy(isLoading = false, registrado = true, password = "", confirmacion = "")
                    is ResultadoApi.Fallo -> it.copy(isLoading = false, errorMessage = resultado.error.mensaje)
                }
            }
        }
    }
}
