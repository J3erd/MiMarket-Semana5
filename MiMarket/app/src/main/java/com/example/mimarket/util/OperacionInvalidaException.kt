package com.example.mimarket.util

/**
 * Error de regla de negocio (stock insuficiente, carrito vacío, etc.).
 * Su mensaje es apto para mostrarse al usuario.
 */
class OperacionInvalidaException(mensaje: String) : Exception(mensaje)
