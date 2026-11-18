package com.example.mimarket.data.remote.firebase

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Fuente de datos remota: Firebase Authentication (correo y contraseña).
 * Es la ÚNICA clase que habla con FirebaseAuth; los ViewModel la usan a través de UsuarioRepository.
 *
 * Privacidad: la contraseña solo se entrega a Firebase (que la guarda cifrada con hash + sal en sus
 * servidores). La app nunca la almacena en Room, DataStore ni Firestore.
 * Persistencia: Firebase conserva la sesión en el dispositivo, por lo que al cerrar y abrir la app
 * [haySesion] sigue devolviendo true hasta que se llame a [cerrarSesion].
 */
class FirebaseAuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    /** UID del usuario autenticado (null si no hay sesión). */
    val uidActual: String?
        get() = auth.currentUser?.uid

    /** Correo del usuario autenticado ("" si no hay sesión). */
    val emailActual: String
        get() = auth.currentUser?.email.orEmpty()

    /** Nombre guardado en el perfil de Firebase Auth. */
    val nombreActual: String
        get() = auth.currentUser?.displayName.orEmpty()

    /** true si hay una sesión guardada (persistencia de sesión). */
    fun haySesion(): Boolean = auth.currentUser != null

    /** Crea la cuenta con correo y contraseña y guarda el nombre en el perfil. Devuelve el UID. */
    suspend fun registrar(email: String, clave: String, nombre: String): String {
        val resultado = auth.createUserWithEmailAndPassword(email.trim(), clave).await()
        val usuario = resultado.user ?: throw IllegalStateException("No se pudo crear la cuenta")
        usuario.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nombre.trim()).build()).await()
        return usuario.uid
    }

    /** Inicia sesión con correo y contraseña. Devuelve el UID. */
    suspend fun iniciarSesion(email: String, clave: String): String {
        val resultado = auth.signInWithEmailAndPassword(email.trim(), clave).await()
        return resultado.user?.uid ?: throw IllegalStateException("No se pudo iniciar sesión")
    }

    /** "¿Olvidaste tu contraseña?": Firebase envía un correo con un enlace para crear una nueva. */
    suspend fun enviarCorreoRecuperacion(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /** Cierra la sesión en este dispositivo. */
    fun cerrarSesion() = auth.signOut()

    /**
     * Vuelve a comprobar la contraseña. Firebase lo exige antes de operaciones sensibles
     * (como eliminar la cuenta) para confirmar que quien la pide es el dueño.
     */
    suspend fun reautenticar(clave: String) {
        val usuario = auth.currentUser ?: throw SesionNoIniciadaException()
        val credencial = EmailAuthProvider.getCredential(usuario.email.orEmpty(), clave)
        usuario.reauthenticate(credencial).await()
    }

    /** Elimina definitivamente la cuenta de Firebase Authentication. */
    suspend fun eliminarCuenta() {
        val usuario = auth.currentUser ?: throw SesionNoIniciadaException()
        usuario.delete().await()
    }
}
