package com.example.mimarket.util

/**
 * Validaciones de formularios (usadas por InicioSesionViewModel).
 * Cada función devuelve el MENSAJE de error, o `null` si el dato es válido.
 */
object Validadores {

    // Patrón de correo: texto@dominio.extensión (la extensión tiene al menos 2 letras).
    private val PATRON_EMAIL =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    /** `true` si el texto tiene formato de correo válido (se ignoran espacios en los extremos). */
    fun esEmailValido(email: String): Boolean = PATRON_EMAIL.matches(email.trim())

    /** Valida el correo: obligatorio y con formato válido. */
    fun validarEmail(email: String): String? = when {
        email.isBlank() -> "El correo es obligatorio"
        !esEmailValido(email) -> "Ingresa un correo con formato válido"
        else -> null
    }

    /** Valida la contraseña: solo exige que no esté vacía. */
    fun validarClave(clave: String): String? =
        if (clave.isBlank()) "La contraseña es obligatoria" else null

    /** Semana 5 - registro: el nombre es obligatorio. */
    fun validarNombre(nombre: String): String? =
        if (nombre.isBlank()) "El nombre es obligatorio" else null

    /** Semana 5 - registro: Firebase Authentication exige contraseñas de al menos 6 caracteres. */
    fun validarClaveNueva(clave: String): String? = when {
        clave.isBlank() -> "La contraseña es obligatoria"
        clave.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> null
    }
}