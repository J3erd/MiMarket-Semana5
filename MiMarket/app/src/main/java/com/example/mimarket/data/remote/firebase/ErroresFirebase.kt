package com.example.mimarket.data.remote.firebase

import com.example.mimarket.data.remote.api.ErrorApi
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.util.OperacionInvalidaException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/** No hay un usuario autenticado para realizar la operación. */
class SesionNoIniciadaException : Exception("Tu sesión no está activa. Inicia sesión nuevamente")

/** El documento pedido no existe en Firestore. */
class RecursoNoEncontradoException : Exception("El recurso no existe")

/**
 * Ejecuta una operación de Firebase y traduce cualquier excepción a un [ErrorApi] con un mensaje
 * comprensible (nunca se muestran códigos técnicos como ERROR_WRONG_PASSWORD o PERMISSION_DENIED).
 * Si Firebase no responde en [tiempoMaximoMs] (sin internet, las escrituras quedan en espera)
 * se informa "sin conexión".
 */
suspend fun <T> llamarFirebase(tiempoMaximoMs: Long = 15_000, bloque: suspend () -> T): ResultadoApi<T> = try {
    ResultadoApi.Exito(withTimeout(tiempoMaximoMs) { bloque() })
} catch (e: TimeoutCancellationException) {
    ResultadoApi.Fallo(ErrorApi.SinConexion) // debe ir antes que CancellationException (es subclase)
} catch (e: CancellationException) {
    throw e // la cancelación de la corrutina no es un error
} catch (e: Exception) {
    ResultadoApi.Fallo(errorDeFirebase(e))
}

/** Clasifica una excepción de Firebase Auth, Firestore o Storage. */
fun errorDeFirebase(e: Throwable): ErrorApi = when (e) {
    is SesionNoIniciadaException -> ErrorApi.Mensaje(e.message ?: "Inicia sesión nuevamente")
    is RecursoNoEncontradoException -> ErrorApi.NoEncontrado
    is OperacionInvalidaException -> ErrorApi.Mensaje(e.message ?: "La operación no es válida")
    is FirebaseNetworkException -> ErrorApi.SinConexion
    is FirebaseTooManyRequestsException ->
        ErrorApi.Mensaje("Demasiados intentos seguidos. Espera unos minutos e inténtalo de nuevo")
    is FirebaseAuthException -> errorDeAuth(e.errorCode)
    is FirebaseFirestoreException -> errorDeFirestore(e.code)
    is StorageException -> errorDeStorage(e.errorCode)
    is IOException -> ErrorApi.SinConexion
    // Cualquier otro error de Firebase (p. ej. proyecto mal configurado o google-services.json de ejemplo)
    is FirebaseException -> ErrorApi.Mensaje("No se pudo conectar con el servicio de Mi Market. Inténtalo de nuevo más tarde")
    else -> ErrorApi.Inesperado
}

/** Firebase Authentication: cada código técnico se convierte en una frase para el usuario. */
private fun errorDeAuth(codigo: String): ErrorApi = ErrorApi.Mensaje(
    when (codigo) {
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_USER_NOT_FOUND" ->
            "Correo o contraseña incorrectos"
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Ya existe una cuenta registrada con ese correo"
        "ERROR_INVALID_EMAIL" -> "El correo no tiene un formato válido"
        "ERROR_WEAK_PASSWORD" -> "La contraseña es muy débil. Usa al menos 6 caracteres"
        "ERROR_USER_DISABLED" -> "Esta cuenta fue deshabilitada. Comunícate con soporte"
        "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos seguidos. Espera unos minutos e inténtalo de nuevo"
        "ERROR_REQUIRES_RECENT_LOGIN" -> "Por seguridad, vuelve a iniciar sesión antes de realizar esta acción"
        "ERROR_OPERATION_NOT_ALLOWED" -> "El inicio de sesión con correo no está habilitado en el servidor"
        "ERROR_NETWORK_REQUEST_FAILED" -> "No hay conexión a internet. Verifica tu red e inténtalo de nuevo"
        else -> "No se pudo completar la autenticación. Inténtalo de nuevo"
    }
)

/** Cloud Firestore: PERMISSION_DENIED aparece cuando las reglas de seguridad bloquean la operación. */
private fun errorDeFirestore(codigo: FirebaseFirestoreException.Code): ErrorApi = when (codigo) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED -> ErrorApi.PermisoDenegado
    FirebaseFirestoreException.Code.UNAUTHENTICATED -> ErrorApi.NoAutorizado
    FirebaseFirestoreException.Code.NOT_FOUND -> ErrorApi.NoEncontrado
    FirebaseFirestoreException.Code.UNAVAILABLE,
    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> ErrorApi.SinConexion
    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> ErrorApi.ErrorServidor
    else -> ErrorApi.Inesperado
}

/** Firebase Storage (subida de imágenes). */
private fun errorDeStorage(codigo: Int): ErrorApi = when (codigo) {
    StorageException.ERROR_NOT_AUTHORIZED ->
        ErrorApi.Mensaje("No tienes permiso para subir esta imagen (debe ser una imagen de hasta 5 MB)")
    StorageException.ERROR_NOT_AUTHENTICATED -> ErrorApi.NoAutorizado
    StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> ErrorApi.SinConexion
    StorageException.ERROR_QUOTA_EXCEEDED,
    StorageException.ERROR_BUCKET_NOT_FOUND,
    StorageException.ERROR_PROJECT_NOT_FOUND ->
        ErrorApi.Mensaje("El almacenamiento de imágenes no está disponible en este momento")
    StorageException.ERROR_OBJECT_NOT_FOUND -> ErrorApi.Mensaje("No se encontró la imagen seleccionada")
    else -> ErrorApi.Mensaje("No se pudo subir la imagen. Inténtalo de nuevo")
}
