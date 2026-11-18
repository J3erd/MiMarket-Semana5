package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.repository.TiendaRepository
import com.example.mimarket.data.repository.UsuarioRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del perfil: muestra los datos personales del usuario y concentra sus derechos de
 * privacidad: ver el aviso, revocar el consentimiento de ubicación, cerrar sesión y
 * ELIMINAR la cuenta junto con sus datos personales.
 */
class PerfilViewModel(
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository,
    private val tiendaRepository: TiendaRepository = MiMarketApplication.instancia.tiendaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PerfilUiState())
    val uiState: StateFlow<PerfilUiState> = _uiState.asStateFlow()

    // Escuchas activas (se cancelan antes de cerrar sesión para no leer Firestore sin autenticación).
    private var escuchaPerfil: Job? = null

    init {
        escuchaPerfil = viewModelScope.launch {
            usuarioRepository.observarPerfil().collect { usuario ->
                _uiState.update { it.copy(usuario = usuario, cargando = false) }
            }
        }
        viewModelScope.launch {
            usuarioRepository.flujoConsentimientoUbicacion.collect { aceptado ->
                _uiState.update { it.copy(consentimientoUbicacion = aceptado) }
            }
        }
    }

    /** Abre / cierra el aviso de privacidad completo. */
    fun verAviso(visible: Boolean) = _uiState.update { it.copy(mostrarAviso = visible) }

    /** Revoca el consentimiento de ubicación: el mapa volverá a mostrar el aviso antes de usarla. */
    fun revocarConsentimientoUbicacion() {
        viewModelScope.launch {
            usuarioRepository.guardarConsentimientoUbicacion(false)
            _uiState.update { it.copy(mensaje = "Consentimiento de ubicación revocado") }
        }
    }

    /** Solo administradores: publica las tiendas de ejemplo en la colección "tiendas" de Firestore. */
    fun publicarTiendas() {
        if (_uiState.value.operando) return
        _uiState.update { it.copy(operando = true) }
        viewModelScope.launch {
            val resultado = tiendaRepository.publicarTiendasDeEjemplo()
            _uiState.update {
                it.copy(
                    operando = false,
                    mensaje = when (resultado) {
                        is ResultadoApi.Exito -> "Se publicaron ${resultado.datos} tiendas en Firestore"
                        is ResultadoApi.Fallo -> resultado.error.mensaje
                    }
                )
            }
        }
    }

    /** Cierra la sesión y limpia los datos locales; la vista vuelve al login. */
    fun cerrarSesion() {
        if (_uiState.value.operando) return
        _uiState.update { it.copy(operando = true) }
        viewModelScope.launch {
            escuchaPerfil?.cancel()
            runCatching { usuarioRepository.cerrarSesion() }
            _uiState.update { it.copy(operando = false, sesionTerminada = true) }
        }
    }

    // ---- Eliminar cuenta y datos personales ----

    /** Abre el diálogo de confirmación de "Eliminar cuenta". */
    fun solicitarEliminarCuenta() =
        _uiState.update { it.copy(confirmandoEliminar = true, claveConfirmacion = "", errorEliminar = null) }

    fun cambiarClaveConfirmacion(v: String) = _uiState.update { it.copy(claveConfirmacion = v, errorEliminar = null) }

    fun cancelarEliminarCuenta() =
        _uiState.update { it.copy(confirmandoEliminar = false, claveConfirmacion = "", errorEliminar = null) }

    /**
     * Confirma la eliminación: exige la contraseña (reautenticación) y borra el historial de pedidos,
     * el documento del usuario, la cuenta de Firebase Authentication y los datos del dispositivo.
     */
    fun confirmarEliminarCuenta() {
        val s = _uiState.value
        if (s.operando) return
        if (s.claveConfirmacion.isBlank()) {
            _uiState.update { it.copy(errorEliminar = "Escribe tu contraseña para confirmar") }
            return
        }
        _uiState.update { it.copy(operando = true, errorEliminar = null) }
        viewModelScope.launch {
            when (val resultado = usuarioRepository.eliminarCuenta(s.claveConfirmacion)) {
                is ResultadoApi.Exito -> {
                    escuchaPerfil?.cancel()
                    _uiState.update {
                        it.copy(operando = false, confirmandoEliminar = false, claveConfirmacion = "", sesionTerminada = true, cuentaEliminada = true)
                    }
                }
                is ResultadoApi.Fallo ->
                    _uiState.update { it.copy(operando = false, claveConfirmacion = "", errorEliminar = resultado.error.mensaje) }
            }
        }
    }

    /** La vista avisa que ya mostró el mensaje. */
    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }
}
