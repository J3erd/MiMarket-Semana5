package com.example.mimarket.data.remote.api

import com.example.mimarket.data.remote.model.DescuentoStockRequest
import com.example.mimarket.data.remote.model.EliminarRespuesta
import com.example.mimarket.data.remote.model.ProductoDto
import com.example.mimarket.data.remote.model.ProductoRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Contrato REST de productos (Retrofit). Todos los métodos son `suspend`: se ejecutan en una
 * corrutina y NO bloquean el hilo principal. Si el servidor responde 4xx/5xx lanzan HttpException.
 */
interface ProductoApiService {

    /** Read: GET /api/productos -> lista completa. */
    @GET("api/productos")
    suspend fun listar(): List<ProductoDto>

    /** Read: GET /api/productos/:id -> un producto (404 si no existe). */
    @GET("api/productos/{id}")
    suspend fun obtener(@Path("id") id: Long): ProductoDto

    /** Create: POST /api/productos -> 201 con el producto creado (400 si los datos son inválidos). */
    @POST("api/productos")
    suspend fun crear(@Body producto: ProductoRequest): ProductoDto

    /** Update: PUT /api/productos/:id -> 200 con el producto actualizado (404 si no existe). */
    @PUT("api/productos/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body producto: ProductoRequest): ProductoDto

    /** Delete: DELETE /api/productos/:id -> 200 (404 si no existe). */
    @DELETE("api/productos/{id}")
    suspend fun eliminar(@Path("id") id: Long): EliminarRespuesta

    /** Descuenta stock después de una compra: PATCH /api/productos/:id/stock. */
    @PATCH("api/productos/{id}/stock")
    suspend fun descontarStock(@Path("id") id: Long, @Body cuerpo: DescuentoStockRequest): ProductoDto
}
