package com.example.mimarket.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mimarket.util.CalculadoraCarrito
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Crea (una sola vez por proceso) el DataStore llamado "mimarket_preferencias".
// Se declara como propiedad de extensión de Context para poder usar contexto.almacenDatos.
private val Context.almacenDatos by preferencesDataStore(name = "mimarket_preferencias")

/**
 * Persistencia de datos SENCILLOS (clave-valor) con Jetpack DataStore. Guarda:
 *  - Último correo usado (solo para precargarlo en el login; nunca la contraseña).
 *  - Preferencia del usuario: categoría elegida en el catálogo.
 *  - Configuración de la app: costo de envío.
 *  - Semana 5: consentimiento del aviso de privacidad para usar la ubicación.
 * La sesión ya no se guarda aquí: la conserva Firebase Authentication.
 */
class PreferenciasDataStore(private val contexto: Context) {

    private companion object {
        // Claves del archivo de preferencias (nombre + tipo de dato).
        val EMAIL_USUARIO = stringPreferencesKey("email_usuario")
        val CATEGORIA_FILTRO = stringPreferencesKey("categoria_filtro")
        val COSTO_ENVIO = doublePreferencesKey("costo_envio")
        val CONSENTIMIENTO_UBICACION = booleanPreferencesKey("consentimiento_ubicacion")
    }

    /** Emite el correo del último usuario que inició sesión ("" si no hay). */
    val flujoEmail: Flow<String> =
        contexto.almacenDatos.data.map { it[EMAIL_USUARIO] ?: "" }

    /** Categoría elegida en el catálogo; cadena vacía = "Todas". */
    val flujoCategoriaFiltro: Flow<String> =
        contexto.almacenDatos.data.map { it[CATEGORIA_FILTRO] ?: "" }

    /** Costo de envío configurado; si nunca se guardó, usa el valor por defecto (S/ 2.00). */
    val flujoCostoEnvio: Flow<Double> =
        contexto.almacenDatos.data.map { it[COSTO_ENVIO] ?: CalculadoraCarrito.COSTO_ENVIO_POR_DEFECTO }

    /** true si el usuario aceptó el aviso de privacidad para el uso de su ubicación. */
    val flujoConsentimientoUbicacion: Flow<Boolean> =
        contexto.almacenDatos.data.map { it[CONSENTIMIENTO_UBICACION] ?: false }

    /** Recuerda el correo para precargarlo en el próximo inicio de sesión. */
    suspend fun guardarUltimoEmail(email: String) {
        // edit { } modifica el archivo de forma atómica en segundo plano.
        contexto.almacenDatos.edit { it[EMAIL_USUARIO] = email }
    }

    /** Guarda la categoría elegida (preferencia del usuario) para recordarla entre usos. */
    suspend fun guardarCategoriaFiltro(categoria: String) {
        contexto.almacenDatos.edit { it[CATEGORIA_FILTRO] = categoria }
    }

    /** Cambia el costo de envío (configuración de la app). */
    suspend fun guardarCostoEnvio(costo: Double) {
        contexto.almacenDatos.edit { it[COSTO_ENVIO] = costo }
    }

    /** Registra (o revoca) el consentimiento para usar la ubicación. */
    suspend fun guardarConsentimientoUbicacion(aceptado: Boolean) {
        contexto.almacenDatos.edit { it[CONSENTIMIENTO_UBICACION] = aceptado }
    }

    /** Borra los datos personales guardados en el dispositivo (al cerrar sesión o eliminar la cuenta). */
    suspend fun borrarDatosPersonales(conservarEmail: Boolean) {
        contexto.almacenDatos.edit {
            if (!conservarEmail) it.remove(EMAIL_USUARIO)
            it.remove(CONSENTIMIENTO_UBICACION)
            it.remove(CATEGORIA_FILTRO)
        }
    }
}
