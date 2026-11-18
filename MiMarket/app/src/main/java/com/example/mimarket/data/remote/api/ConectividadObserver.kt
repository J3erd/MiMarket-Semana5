package com.example.mimarket.data.remote.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observa la conexión a internet con ConnectivityManager + NetworkCallback.
 * [enLinea] emite true/false cada vez que la red disponible cambia.
 * Requiere el permiso ACCESS_NETWORK_STATE (declarado en el AndroidManifest).
 */
class ConectividadObserver(contexto: Context) {

    private val manager = contexto.applicationContext
        .getSystemService(ConnectivityManager::class.java)

    val enLinea: Flow<Boolean> = callbackFlow {
        // Redes con internet que están disponibles ahora mismo
        val redes = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                redes.add(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                redes.remove(network)
                trySend(redes.isNotEmpty())
            }
        }

        val peticion = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Estado inicial, antes de que llegue el primer evento del callback
        trySend(hayInternetAhora())
        manager.registerNetworkCallback(peticion, callback)

        // Al dejar de observar se libera el callback (evita fugas de memoria)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun hayInternetAhora(): Boolean {
        val capacidades = manager.getNetworkCapabilities(manager.activeNetwork)
        return capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
