package com.example.mimarket.model

/** Roles posibles de un usuario. El rol "admin" solo se asigna desde la consola de Firebase. */
object RolUsuario {
    const val CLIENTE = "cliente"
    const val ADMIN = "admin"
}

/**
 * Usuario de Mi Market (mapeado al documento usuarios/{uid} de Firestore).
 * La contraseña NO forma parte del modelo: la gestiona Firebase Authentication y nunca
 * se guarda en Firestore, en Room ni en DataStore.
 *
 * @property uid Identificador que asigna Firebase Authentication.
 * @property email Correo con el que inicia sesión.
 * @property nombre Nombre completo.
 * @property rol "cliente" o "admin".
 * @property direccionEntrega Dirección elegida desde el mapa (dato personal, opcional).
 * @property aceptoPrivacidad true si aceptó el aviso de privacidad al registrarse.
 * @property fechaRegistro Marca de tiempo (milisegundos) del registro.
 */
data class Usuario(
    val uid: String = "",
    val email: String = "",
    val nombre: String = "",
    val rol: String = RolUsuario.CLIENTE,
    val direccionEntrega: String = "",
    val aceptoPrivacidad: Boolean = false,
    val fechaRegistro: Long = 0
) {
    /** true si el usuario puede editar/eliminar cualquier producto y publicar tiendas. */
    val esAdmin: Boolean
        get() = rol == RolUsuario.ADMIN
}
