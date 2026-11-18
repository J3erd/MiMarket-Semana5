package com.example.mimarket.data.repository

import com.example.mimarket.data.local.datastore.PreferenciasDataStore
import com.example.mimarket.data.local.room.AppDatabase
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.remote.firebase.FirebaseAuthManager
import com.example.mimarket.data.remote.firebase.FirestoreRepository
import com.example.mimarket.data.remote.firebase.SesionNoIniciadaException
import com.example.mimarket.data.remote.firebase.llamarFirebase
import com.example.mimarket.model.RolUsuario
import com.example.mimarket.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositorio de usuarios y sesión (FACHADA).
 * Los ViewModel solo conocen esta clase: no saben que por dentro se usa Firebase Authentication
 * (cuentas y sesión), Cloud Firestore (datos personales en usuarios/{uid}), DataStore
 * (último correo y consentimiento) y Room (datos locales que se limpian al salir).
 */
class UsuarioRepository(
    private val auth: FirebaseAuthManager,
    private val firestore: FirestoreRepository,
    private val preferencias: PreferenciasDataStore,
    private val baseDeDatos: AppDatabase
) {
    /** Último correo usado (se precarga en el login y se muestra en el catálogo). */
    val flujoEmail: Flow<String> = preferencias.flujoEmail

    /** true si el usuario aceptó el aviso de privacidad para usar su ubicación. */
    val flujoConsentimientoUbicacion: Flow<Boolean> = preferencias.flujoConsentimientoUbicacion

    /** UID del usuario autenticado (null si no hay sesión). */
    val uidActual: String?
        get() = auth.uidActual

    /** Persistencia de sesión: true si Firebase Auth conserva una sesión de un uso anterior. */
    fun haySesionActiva(): Boolean = auth.haySesion()

    /** Devuelve el último correo guardado ("" si no hay). */
    suspend fun ultimoEmail(): String = preferencias.flujoEmail.first()

    /** Inicia sesión con Firebase Authentication (correo y contraseña). */
    suspend fun iniciarSesion(email: String, clave: String): ResultadoApi<Unit> = llamarFirebase {
        val uid = auth.iniciarSesion(email, clave)
        preferencias.guardarUltimoEmail(auth.emailActual)
        // Si la cuenta aún no tiene documento en Firestore (p. ej. creada desde la consola), se crea.
        if (firestore.obtenerUsuario(uid) == null) {
            firestore.guardarUsuario(usuarioNuevo(uid, auth.emailActual, auth.nombreActual, aceptoPrivacidad = false))
        }
    }

    /**
     * Registro: crea la cuenta en Firebase Authentication y guarda los datos personales en
     * Firestore (usuarios/{uid}). El registro exige haber aceptado el aviso de privacidad.
     */
    suspend fun registrarUsuario(nombre: String, email: String, clave: String): ResultadoApi<Unit> = llamarFirebase {
        val uid = auth.registrar(email, clave, nombre)
        // aceptoPrivacidad = false: el consentimiento se registra en la pantalla "Aviso de Privacidad" que sigue
        firestore.guardarUsuario(usuarioNuevo(uid, email.trim(), nombre.trim(), aceptoPrivacidad = false))
        preferencias.guardarUltimoEmail(email.trim())
    }

    private fun usuarioNuevo(uid: String, email: String, nombre: String, aceptoPrivacidad: Boolean) = Usuario(
        uid = uid,
        email = email,
        nombre = nombre,
        rol = RolUsuario.CLIENTE, // el rol admin solo se asigna desde la consola de Firebase
        aceptoPrivacidad = aceptoPrivacidad,
        fechaRegistro = System.currentTimeMillis()
    )

    /** Datos del usuario en tiempo real (pantalla de perfil). Si Firestore falla, usa los de Auth. */
    fun observarPerfil(): Flow<Usuario?> {
        val uid = auth.uidActual ?: return flowOf(null)
        val respaldo = Usuario(uid = uid, email = auth.emailActual, nombre = auth.nombreActual)
        return firestore.observarUsuario(uid)
            .map { it ?: respaldo }
            .catch { emit(respaldo) }
    }

    /** "¿Olvidaste tu contraseña?": envía el correo de restablecimiento de Firebase. */
    suspend fun recuperarClave(email: String): ResultadoApi<Unit> = llamarFirebase {
        auth.enviarCorreoRecuperacion(email)
    }

    /** true si el usuario ya aceptó el aviso de privacidad (campo aceptoPrivacidad de usuarios/{uid}). */
    suspend fun aceptoAvisoPrivacidad(): Boolean {
        val uid = auth.uidActual ?: return false
        return runCatching { firestore.obtenerUsuario(uid)?.aceptoPrivacidad == true }.getOrDefault(false)
    }

    /** Pantalla "Aviso de Privacidad" -> "Aceptar y continuar": guarda el consentimiento en Firestore. */
    suspend fun aceptarAvisoPrivacidad(): ResultadoApi<Unit> = llamarFirebase {
        val uid = auth.uidActual ?: throw SesionNoIniciadaException()
        firestore.marcarAvisoAceptado(uid)
    }

    /** Guarda la dirección de entrega elegida desde el mapa. */
    suspend fun guardarDireccionEntrega(direccion: String): ResultadoApi<Unit> = llamarFirebase {
        val uid = auth.uidActual ?: throw SesionNoIniciadaException()
        firestore.actualizarDireccionEntrega(uid, direccion)
    }

    /** Registra o revoca el consentimiento del aviso de privacidad de ubicación. */
    suspend fun guardarConsentimientoUbicacion(aceptado: Boolean) =
        preferencias.guardarConsentimientoUbicacion(aceptado)

    /** Cierra la sesión y limpia los datos locales del usuario (carrito, compras, consentimiento). */
    suspend fun cerrarSesion() {
        limpiarDatosLocales(conservarEmail = true)
        auth.cerrarSesion()
    }

    /**
     * Derecho de eliminación: borra la cuenta y los datos personales.
     *  1) Reautentica con la contraseña (si es incorrecta no se borra nada).
     *  2) Borra de Firestore el historial de pedidos y el documento del usuario.
     *  3) Borra la cuenta de Firebase Authentication.
     *  4) Borra los datos guardados en el dispositivo.
     */
    suspend fun eliminarCuenta(clave: String): ResultadoApi<Unit> = llamarFirebase(tiempoMaximoMs = 30_000) {
        val uid = auth.uidActual ?: throw SesionNoIniciadaException()
        auth.reautenticar(clave)
        firestore.eliminarDatosUsuario(uid)
        auth.eliminarCuenta()
        limpiarDatosLocales(conservarEmail = false)
    }

    private suspend fun limpiarDatosLocales(conservarEmail: Boolean) {
        // clearAllTables es bloqueante: se ejecuta fuera del hilo principal.
        withContext(Dispatchers.IO) { baseDeDatos.clearAllTables() }
        preferencias.borrarDatosPersonales(conservarEmail)
    }
}
