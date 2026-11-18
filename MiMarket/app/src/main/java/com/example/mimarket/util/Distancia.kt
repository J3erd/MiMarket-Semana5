package com.example.mimarket.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Cálculo de distancias entre coordenadas (lógica pura, sin Android: se puede probar con tests). */
object Distancia {

    private const val RADIO_TIERRA_KM = 6371.0

    /** Distancia en kilómetros entre dos puntos (fórmula de Haversine). */
    fun kilometros(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * RADIO_TIERRA_KM * asin(sqrt(a))
    }
}
