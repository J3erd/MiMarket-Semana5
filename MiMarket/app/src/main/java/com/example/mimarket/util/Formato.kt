package com.example.mimarket.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Utilidades de presentación: convierten importes y fechas en texto para mostrar en pantalla. */
object Formato {

    /** Importe en soles con 2 decimales y punto decimal. Ej.: 12.5 -> "S/ 12.50" */
    fun moneda(valor: Double): String = "S/ " + String.format(Locale.US, "%.2f", valor)

    /** Fecha y hora legibles a partir de milisegundos. Ej.: -> "27/10/2026 03:33" */
    fun fecha(milisegundos: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE")).format(Date(milisegundos))
}
