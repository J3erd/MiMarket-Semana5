package com.example.mimarket.data.remote.api

import com.example.mimarket.data.remote.model.ErrorRespuesta
import com.google.gson.Gson
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

/**
 * Tipos de error de red que la app distingue. Cada uno trae un mensaje comprensible
 * (nunca trazas técnicas ni detalles internos del servidor).
 */
sealed class ErrorApi(val mensaje: String) {
    /** 400: datos inválidos o incompletos (usa el mensaje de validación del servidor). */
    class SolicitudInvalida(detalle: String?) : ErrorApi(detalle ?: "Los datos enviados no son válidos")
    /** 401: credenciales incorrectas. */
    object NoAutorizado : ErrorApi("Credenciales incorrectas o sesión no autorizada")
    /** 404: el producto no existe. */
    object NoEncontrado : ErrorApi("El producto no existe o ya fue eliminado")
    /** 500 y similares. */
    object ErrorServidor : ErrorApi("El servidor tuvo un problema. Inténtalo nuevamente más tarde")
    /** El servidor tardó demasiado. */
    object Timeout : ErrorApi("El servidor tardó demasiado en responder")
    /** No hay red o el backend está apagado. */
    object SinConexion : ErrorApi("No es posible comunicarse con el servidor. Verifica tu conexión a internet e inténtalo de nuevo")
    /** Semana 5 - Firestore PERMISSION_DENIED: las reglas de seguridad bloquearon la operación. */
    object PermisoDenegado : ErrorApi("No tienes permiso para realizar esta acción. Solo el creador del producto o un administrador puede modificarlo o eliminarlo")
    /** Semana 5 - Error de Firebase con un mensaje ya redactado para el usuario. */
    class Mensaje(texto: String) : ErrorApi(texto)
    /** Cualquier otra excepción. */
    object Inesperado : ErrorApi("Ocurrió un error inesperado")
}

/** Resultado de una llamada a la API: o trae datos (Exito) o trae un error clasificado (Fallo). */
sealed class ResultadoApi<out T> {
    data class Exito<T>(val datos: T) : ResultadoApi<T>()
    data class Fallo(val error: ErrorApi) : ResultadoApi<Nothing>()
}

/**
 * Ejecuta una llamada de red y traduce cualquier excepción al error correspondiente.
 * Se usa dentro de corrutinas (suspend), por lo que no bloquea el hilo principal.
 */
suspend fun <T> llamarApi(bloque: suspend () -> T): ResultadoApi<T> = try {
    ResultadoApi.Exito(bloque())
} catch (e: CancellationException) {
    throw e // la cancelación de la corrutina no es un error
} catch (e: HttpException) {
    ResultadoApi.Fallo(errorDeHttp(e))
} catch (e: SocketTimeoutException) {
    ResultadoApi.Fallo(ErrorApi.Timeout) // debe ir antes que IOException (es subclase)
} catch (e: IOException) {
    ResultadoApi.Fallo(ErrorApi.SinConexion)
} catch (e: Exception) {
    ResultadoApi.Fallo(ErrorApi.Inesperado)
}

/** Clasifica la respuesta HTTP de error según su código. */
private fun errorDeHttp(e: HttpException): ErrorApi = when (e.code()) {
    400, 409 -> ErrorApi.SolicitudInvalida(leerMensajeServidor(e)) // 409: regla de negocio (ej. eliminar con stock)
    401 -> ErrorApi.NoAutorizado
    404 -> ErrorApi.NoEncontrado
    in 500..599 -> ErrorApi.ErrorServidor
    else -> ErrorApi.Inesperado
}

/** Lee {"error": "..."} del cuerpo de la respuesta (solo se muestra en errores 400). */
private fun leerMensajeServidor(e: HttpException): String? = try {
    Gson().fromJson(e.response()?.errorBody()?.string(), ErrorRespuesta::class.java)?.error
} catch (_: Exception) {
    null
}
