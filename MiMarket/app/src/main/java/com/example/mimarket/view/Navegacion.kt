package com.example.mimarket.view

import android.content.Intent
import androidx.activity.ComponentActivity
import com.example.mimarket.MiMarketApplication
import com.example.mimarket.view.componentes.Pestana

/**
 * "Todas las pantallas protegidas requieren autenticación" (flujo de pantallas).
 * Se llama al inicio de onCreate de cada pantalla protegida: si no hay sesión de Firebase,
 * envía al login y cierra la pantalla. Devuelve false en ese caso.
 */
fun ComponentActivity.requerirSesion(): Boolean {
    if (MiMarketApplication.instancia.usuarioRepository.haySesionActiva()) return true
    startActivity(
        Intent(this, InicioSesionActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
    finish()
    return false
}

/** Navegación de la barra inferior. CLEAR_TOP evita apilar varias veces la misma pantalla. */
fun ComponentActivity.irAPestana(pestana: Pestana) {
    val destino = when (pestana) {
        Pestana.INICIO -> HomeActivity::class.java
        Pestana.CATALOGO -> CatalogoActivity::class.java
        Pestana.CARRITO -> ResumenCompraActivity::class.java
        Pestana.PERFIL -> PerfilActivity::class.java
    }
    if (destino == this::class.java) return
    startActivity(Intent(this, destino).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
}
