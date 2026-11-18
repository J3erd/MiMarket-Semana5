package com.example.mimarket.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.repository.TiendaRepository
import com.example.mimarket.data.repository.UsuarioRepository
import com.example.mimarket.model.Tienda
import com.example.mimarket.model.UbicacionUsuario
import com.example.mimarket.util.Distancia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel del mapa de tiendas. No conoce Google Maps ni los servicios de ubicación: solo habla
 * con los repositorios (fachada) y publica un StateFlow<MapaTiendasUiState>.
 *
 * Flujo de privacidad y permisos:
 *   1) Se muestra el AVISO DE PRIVACIDAD (consentimiento explícito) antes de recopilar la ubicación.
 *   2) Si acepta, la pantalla pide el permiso de ubicación en tiempo de ejecución.
 *   3) Permiso concedido -> se obtiene la ubicación. Permiso denegado -> flujo alternativo:
 *      el mapa y la lista de tiendas siguen funcionando sin ubicación.
 */
class MapaTiendasViewModel(
    private val tiendaRepository: TiendaRepository = MiMarketApplication.instancia.tiendaRepository,
    private val usuarioRepository: UsuarioRepository = MiMarketApplication.instancia.usuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapaTiendasUiState())
    val uiState: StateFlow<MapaTiendasUiState> = _uiState.asStateFlow()

    init {
        observarTiendas()
        iniciarFlujoDeUbicacion()
    }

    /** Tiendas en tiempo real (Firestore, con las de ejemplo como respaldo). */
    private fun observarTiendas() {
        viewModelScope.launch {
            tiendaRepository.observarTiendas().collect { lista ->
                _uiState.update { it.copy(tiendas = lista, cargandoTiendas = false) }
                recalcularTiendaMasCercana()
            }
        }
    }

    /** Al abrir la pantalla: si aún no aceptó el aviso de privacidad se le muestra; si ya aceptó, se continúa. */
    private fun iniciarFlujoDeUbicacion() {
        viewModelScope.launch {
            val consentimiento = usuarioRepository.flujoConsentimientoUbicacion.first()
            _uiState.update { it.copy(consentimiento = consentimiento) }
            if (consentimiento) continuarConPermiso() else _uiState.update { it.copy(mostrarAvisoPrivacidad = true) }
        }
    }

    /** "Aceptar y continuar" del aviso de privacidad: guarda el consentimiento y recién entonces pide el permiso. */
    fun aceptarAvisoPrivacidad() {
        viewModelScope.launch {
            usuarioRepository.guardarConsentimientoUbicacion(true)
            _uiState.update { it.copy(consentimiento = true, mostrarAvisoPrivacidad = false) }
            continuarConPermiso()
        }
    }

    /** "No acepto": no se pide el permiso ni se lee la ubicación; el mapa sigue disponible. */
    fun rechazarAvisoPrivacidad() {
        _uiState.update { it.copy(mostrarAvisoPrivacidad = false, ubicacion = EstadoUbicacion.AvisoRechazado) }
    }

    /** Botón "Usar mi ubicación": vuelve a empezar el flujo (aviso -> permiso -> ubicación). */
    fun solicitarUbicacion() {
        if (_uiState.value.consentimiento) continuarConPermiso()
        else _uiState.update { it.copy(mostrarAvisoPrivacidad = true) }
    }

    // Con el aviso ya aceptado: si hay permiso se lee la ubicación; si no, se pide a la pantalla que lo solicite.
    private fun continuarConPermiso() {
        if (tiendaRepository.tienePermisoUbicacion()) obtenerUbicacion()
        else _uiState.update { it.copy(solicitarPermiso = true) }
    }

    /**
     * Resultado del diálogo de permisos de Android.
     * @param concedido true si se concedió ubicación precisa o aproximada.
     * @param puedeVolverAPreguntar false si el usuario marcó "No volver a preguntar" (denegación definitiva).
     */
    fun alResponderPermiso(concedido: Boolean, puedeVolverAPreguntar: Boolean) {
        _uiState.update { it.copy(solicitarPermiso = false) }
        if (concedido) obtenerUbicacion()
        else _uiState.update { it.copy(ubicacion = EstadoUbicacion.PermisoDenegado(definitivo = !puedeVolverAPreguntar)) }
    }

    /** Al volver de Ajustes: si el usuario concedió el permiso o encendió el GPS, se reintenta. */
    fun alVolverAPantalla() {
        val estado = _uiState.value.ubicacion
        val pendiente = estado is EstadoUbicacion.PermisoDenegado || estado is EstadoUbicacion.GpsDesactivado
        if (pendiente && _uiState.value.consentimiento && tiendaRepository.tienePermisoUbicacion()) obtenerUbicacion()
    }

    /** Obtiene la ubicación actual (FusedLocationProviderClient) y su dirección aproximada. */
    fun obtenerUbicacion() {
        if (_uiState.value.ubicacion is EstadoUbicacion.Cargando) return
        if (!tiendaRepository.gpsActivado()) {
            _uiState.update { it.copy(ubicacion = EstadoUbicacion.GpsDesactivado) }
            return
        }
        _uiState.update { it.copy(ubicacion = EstadoUbicacion.Cargando) }
        viewModelScope.launch {
            val ubicacion = runCatching { tiendaRepository.obtenerUbicacionActual() }.getOrNull()
            _uiState.update {
                it.copy(
                    ubicacion = if (ubicacion != null) EstadoUbicacion.Obtenida(ubicacion)
                    else EstadoUbicacion.Error("No pudimos obtener tu ubicación. Verifica que el GPS esté encendido e inténtalo de nuevo")
                )
            }
            recalcularTiendaMasCercana()
        }
    }

    /** Calcula la tienda más cercana al usuario (solo cuando hay ubicación). */
    private fun recalcularTiendaMasCercana() {
        _uiState.update { s ->
            val ubicacion = (s.ubicacion as? EstadoUbicacion.Obtenida)?.ubicacion
            s.copy(
                tiendaMasCercana = ubicacion?.let { u ->
                    s.tiendas
                        .map { t -> t to Distancia.kilometros(u.latitud, u.longitud, t.latitud, t.longitud) }
                        .minByOrNull { it.second }
                }
            )
        }
    }

    /** Flujo alternativo sin ubicación: el usuario elige la tienda en la lista (o toca su marcador). */
    fun seleccionarTienda(tienda: Tienda?) = _uiState.update { it.copy(tiendaSeleccionada = tienda) }

    /** Guarda la dirección actual como dirección de entrega (dato personal protegido por las reglas). */
    fun usarComoDireccionDeEntrega(ubicacion: UbicacionUsuario) {
        if (_uiState.value.guardandoDireccion) return
        val direccion = ubicacion.direccion
            ?: "Lat ${"%.5f".format(ubicacion.latitud)}, Lng ${"%.5f".format(ubicacion.longitud)}"
        _uiState.update { it.copy(guardandoDireccion = true) }
        viewModelScope.launch {
            val resultado = usuarioRepository.guardarDireccionEntrega(direccion)
            _uiState.update {
                it.copy(
                    guardandoDireccion = false,
                    mensaje = when (resultado) {
                        is ResultadoApi.Exito -> "Dirección de entrega guardada"
                        is ResultadoApi.Fallo -> resultado.error.mensaje
                    }
                )
            }
        }
    }

    /** La vista avisa que ya mostró el mensaje. */
    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }
}
