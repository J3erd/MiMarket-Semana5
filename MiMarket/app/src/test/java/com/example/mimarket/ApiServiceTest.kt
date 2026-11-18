package com.example.mimarket

import com.example.mimarket.data.remote.api.ErrorApi
import com.example.mimarket.data.remote.model.ProductoRequest
import com.example.mimarket.data.remote.api.ResultadoApi
import com.example.mimarket.data.remote.api.RetrofitClient
import com.example.mimarket.data.remote.api.llamarApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pruebas de Retrofit contra un servidor falso: URL base, métodos HTTP, JSON y clasificación de errores. */
class ApiServiceTest {

    private lateinit var servidor: MockWebServer

    @Before fun iniciar() { servidor = MockWebServer(); servidor.start() }
    @After fun detener() { servidor.shutdown() }

    private fun api() = RetrofitClient.crearServicio(servidor.url("/").toString())

    @Test fun listar_usaGetYConvierteJson() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""[{"id":1,"nombre":"Leche","descripcion":"d","precio":4.5,"stock":3,"imagen":"x"}]"""))
        val r = llamarApi { api().listar() }
        val req = servidor.takeRequest()
        assertEquals("GET", req.method)
        assertEquals("/api/productos", req.path)
        assertEquals("Leche", (r as ResultadoApi.Exito).datos[0].aModelo().nombre)
        assertEquals("General", r.datos[0].aModelo().categoria) // categoría ausente -> General
    }

    @Test fun crear_usaPostConCuerpoJson() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":9,"nombre":"Leche evaporada","precio":4.5,"stock":25}"""))
        val r = llamarApi { api().crear(ProductoRequest("Leche evaporada", "Lata", 4.5, 25, "", "General")) }
        val req = servidor.takeRequest()
        assertEquals("POST", req.method)
        assertTrue(req.body.readUtf8().contains("\"nombre\":\"Leche evaporada\""))
        assertEquals(9L, (r as ResultadoApi.Exito).datos.id)
    }

    @Test fun actualizarYEliminar_usanPutYDelete() = runBlocking {
        servidor.enqueue(MockResponse().setBody("""{"id":2,"nombre":"A","precio":1,"stock":1}"""))
        servidor.enqueue(MockResponse().setBody("""{"mensaje":"ok"}"""))
        llamarApi { api().actualizar(2, ProductoRequest("A", "", 1.0, 1, "", "General")) }
        llamarApi { api().eliminar(2) }
        assertEquals("PUT /api/productos/2", servidor.takeRequest().let { "${it.method} ${it.path}" })
        assertEquals("DELETE /api/productos/2", servidor.takeRequest().let { "${it.method} ${it.path}" })
    }

    private fun errorPara(codigo: Int, cuerpo: String = """{"error":"detalle"}""") = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(codigo).setBody(cuerpo))
        (llamarApi { api().obtener(1) } as ResultadoApi.Fallo).error
    }

    @Test fun codigo400_muestraMensajeDeValidacion() {
        val e = errorPara(400, """{"error":"El precio debe ser mayor que cero"}""")
        assertTrue(e is ErrorApi.SolicitudInvalida)
        assertEquals("El precio debe ser mayor que cero", e.mensaje)
    }
    @Test fun codigo401() = assertTrue(errorPara(401) is ErrorApi.NoAutorizado)
    @Test fun codigo404() = assertTrue(errorPara(404) is ErrorApi.NoEncontrado)
    @Test fun codigo500_noMuestraDetallesInternos() {
        val e = errorPara(500, "Stacktrace secreto")
        assertTrue(e is ErrorApi.ErrorServidor)
        assertTrue(!e.mensaje.contains("Stacktrace"))
    }

    @Test fun servidorApagado_esSinConexion() = runBlocking {
        val api = api(); servidor.shutdown()
        assertTrue((llamarApi { api.listar() } as ResultadoApi.Fallo).error is ErrorApi.SinConexion)
    }

    @Test fun servidorLento_esTimeout() = runBlocking {
        servidor.enqueue(MockResponse().setBody("[]").setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val cliente = okhttp3.OkHttpClient.Builder().readTimeout(300, java.util.concurrent.TimeUnit.MILLISECONDS).build()
        val api = retrofit2.Retrofit.Builder().baseUrl(servidor.url("/")).client(cliente)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build()
            .create(com.example.mimarket.data.remote.api.ProductoApiService::class.java)
        assertTrue((llamarApi { api.listar() } as ResultadoApi.Fallo).error is ErrorApi.Timeout)
    }
}
