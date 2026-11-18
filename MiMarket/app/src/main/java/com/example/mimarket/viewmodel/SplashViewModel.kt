package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.repository.UsuarioRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A dónde ir después del Splash. */
enum class DestinoSplash { LOGIN, AVISO_PRIVACIDAD, HOME }

/**
 * Pantalla 1 - Splash. Muestra el logo un momento y decide el destino:
 * sin sesión -> Login; con sesión pero sin aceptar el aviso -> Aviso de Privacidad; si no -> Home.
 * (Persistencia de sesión: Firebase Auth recuerda al usuario aunque se cierre la app.)
 */
class SplashViewModel(
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository
) : ViewModel() {

    private val _destino = MutableStateFlow<DestinoSplash?>(null)
    val destino: StateFlow<DestinoSplash?> = _destino.asStateFlow()

    init {
        viewModelScope.launch {
            delay(1200) // tiempo para ver el logo
            _destino.value = when {
                !usuarioRepository.haySesionActiva() -> DestinoSplash.LOGIN
                !usuarioRepository.aceptoAvisoPrivacidad() -> DestinoSplash.AVISO_PRIVACIDAD
                else -> DestinoSplash.HOME
            }
        }
    }
}
