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
 * ViewModel de la pantalla de inicio de sesión.
 * Funciones: (1) comprobar si Firebase conserva una sesión (persistencia), (2) guardar lo que el
 * usuario escribe, (3) validar el formulario, (4) pedir al repositorio que inicie sesión con
 * Firebase Authentication y (5) publicar el resultado en un StateFlow para que la pantalla lo dibuje.
 * La vista nunca valida ni consulta datos: solo muestra el estado y avisa de las acciones.
 */
class InicioSesionViewModel(
    // Por defecto toma el repositorio compartido de la Application; en pruebas se puede reemplazar.
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository
) : ViewModel() {

    // Estado MUTABLE: solo el ViewModel puede modificarlo (privado).
    private val _uiState = MutableStateFlow(InicioSesionUiState())

    // Estado de SOLO LECTURA que observa la pantalla (StateFlow).
    val uiState: StateFlow<InicioSesionUiState> = _uiState.asStateFlow()

    // Al crearse el ViewModel (al abrir la app) se verifica la sesión guardada.
    init {
        verificarSesionGuardada()
    }

    /**
     * Persistencia de sesión: si Firebase Auth ya tiene un usuario, marca isLoggedIn = true
     * (la pantalla navegará al catálogo); si no, muestra el formulario con el último correo usado.
     */
    private fun verificarSesionGuardada() {
        viewModelScope.launch {
            val activa = runCatching { usuarioRepository.haySesionActiva() }.getOrDefault(false)
            val ultimoEmail = if (activa) "" else runCatching { usuarioRepository.ultimoEmail() }.getOrDefault("")
            _uiState.update {
                it.copy(
                    email = it.email.ifEmpty { ultimoEmail },
                    verificandoSesion = false,
                    isLoggedIn = activa
                )
            }
        }
    }

    /** Guarda en el estado lo que el usuario escribe en el correo y limpia sus errores. */
    fun onEmailChange(valor: String) {
        _uiState.update { it.copy(email = valor, errorEmail = null, errorMessage = null, mensajeInfo = null) }
    }

    /** "¿Olvidaste tu contraseña?": usa el correo escrito y pide a Firebase el correo de restablecimiento. */
    fun recuperarClave() {
        val actual = _uiState.value
        if (actual.isLoading) return
        val errorEmail = Validadores.validarEmail(actual.email)
        if (errorEmail != null) {
            _uiState.update { it.copy(errorEmail = "Escribe tu correo para recuperar la contraseña", errorMessage = null) }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null, mensajeInfo = null) }
        viewModelScope.launch {
            val resultado = usuarioRepository.recuperarClave(actual.email)
            _uiState.update {
                when (resultado) {
                    // Por seguridad no se revela si el correo existe o no.
                    is ResultadoApi.Exito -> it.copy(isLoading = false, mensajeInfo = "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña")
                    is ResultadoApi.Fallo -> it.copy(isLoading = false, errorMessage = resultado.error.mensaje)
                }
            }
        }
    }

    /** Guarda en el estado lo que el usuario escribe en la contraseña y limpia sus errores. */
    fun onPasswordChange(valor: String) {
        _uiState.update { it.copy(password = valor, errorPassword = null, errorMessage = null) }
    }

    /** Valida el formulario y, si es correcto, inicia sesión con Firebase Authentication. */
    fun iniciarSesion() {
        val actual = _uiState.value
        // Evita doble pulsación mientras ya se está validando.
        if (actual.isLoading) return

        // 1) Validaciones de formulario (correo obligatorio/formato, contraseña obligatoria).
        val errorEmail = Validadores.validarEmail(actual.email)
        val errorPassword = Validadores.validarClave(actual.password)
        if (errorEmail != null || errorPassword != null) {
            // Se queda en el login mostrando los errores bajo cada campo.
            _uiState.update { it.copy(errorEmail = errorEmail, errorPassword = errorPassword, errorMessage = null) }
            return
        }

        // 2) Muestra el indicador de carga y consulta a Firebase en segundo plano.
        _uiState.update { it.copy(isLoading = true, errorEmail = null, errorPassword = null, errorMessage = null) }
        viewModelScope.launch {
            val resultado = usuarioRepository.iniciarSesion(actual.email, actual.password)
            // Si aún no aceptó el aviso de privacidad, la pantalla lo mostrará antes del Home.
            val requiereAviso = resultado is ResultadoApi.Exito && !usuarioRepository.aceptoAvisoPrivacidad()
            _uiState.update {
                when (resultado) {
                    // Credenciales válidas: isLoggedIn = true -> la pantalla navega al Home (o al aviso).
                    // La contraseña se borra del estado: no se conserva en memoria ni en disco.
                    is ResultadoApi.Exito -> it.copy(isLoading = false, isLoggedIn = true, requiereAviso = requiereAviso, password = "")
                    // Error de Firebase ya traducido (ej. "Correo o contraseña incorrectos").
                    is ResultadoApi.Fallo -> it.copy(isLoading = false, errorMessage = resultado.error.mensaje)
                }
            }
        }
    }
}
