package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Registra el consentimiento explícito del usuario antes de entrar al Home. */
class AvisoPrivacidadViewModel(
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvisoPrivacidadUiState())
    val uiState: StateFlow<AvisoPrivacidadUiState> = _uiState.asStateFlow()

    fun verAvisoCompleto(visible: Boolean) = _uiState.update { it.copy(mostrarCompleto = visible) }

    /** "Aceptar y continuar": guarda el consentimiento en Firestore y deja pasar al Home. */
    fun aceptar() {
        if (_uiState.value.enviando) return
        _uiState.update { it.copy(enviando = true, errorMessage = null) }
        viewModelScope.launch {
            val resultado = usuarioRepository.aceptarAvisoPrivacidad()
            _uiState.update {
                when (resultado) {
                    is ResultadoApi.Exito -> it.copy(enviando = false, aceptado = true)
                    is ResultadoApi.Fallo -> it.copy(enviando = false, errorMessage = resultado.error.mensaje)
                }
            }
        }
    }
}
