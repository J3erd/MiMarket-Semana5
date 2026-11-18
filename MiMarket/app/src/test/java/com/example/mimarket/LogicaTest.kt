package com.example.mimarket

import com.example.mimarket.model.ItemCarrito
import com.example.mimarket.model.ItemCarritoConProducto
import com.example.mimarket.model.Producto
import com.example.mimarket.util.CalculadoraCarrito
import com.example.mimarket.util.Formato
import com.example.mimarket.util.Validadores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de la lógica pura: validaciones y cálculos del carrito. */
class LogicaTest {

    private fun fila(id: Long, cantidad: Int, precio: Double) = ItemCarritoConProducto(
        ItemCarrito(id, id, cantidad, precio),
        Producto(id, "P$id", "", precio, "x", "c", 10)
    )

    @Test fun emailVacio_esError() = assertNotNull(Validadores.validarEmail(""))
    @Test fun emailSinArroba_esError() = assertNotNull(Validadores.validarEmail("correo.com"))
    @Test fun emailValido_noTieneError() = assertNull(Validadores.validarEmail("cliente@mimarket.com"))
    @Test fun claveVacia_esError() = assertNotNull(Validadores.validarClave("  "))
    @Test fun claveConTexto_esValida() = assertNull(Validadores.validarClave("123456"))
    @Test fun formatoEmail() {
        assertTrue(Validadores.esEmailValido("a.b@dominio.pe"))
        assertFalse(Validadores.esEmailValido("a@b"))
    }

    @Test fun subtotalEnvioYTotal() {
        val items = listOf(fila(1, 2, 2.50), fila(2, 1, 4.00), fila(3, 1, 1.50))
        val subtotal = CalculadoraCarrito.subtotal(items)
        val envio = CalculadoraCarrito.costoEnvio(subtotal)
        assertEquals(10.50, subtotal, 0.001)
        assertEquals(2.00, envio, 0.001)
        assertEquals(12.50, CalculadoraCarrito.total(subtotal, envio), 0.001)
        assertEquals(4, CalculadoraCarrito.cantidadTotal(items))
    }

    @Test fun carritoVacio_noCobraEnvio() {
        val subtotal = CalculadoraCarrito.subtotal(emptyList())
        assertEquals(0.0, subtotal, 0.0)
        assertEquals(0.0, CalculadoraCarrito.costoEnvio(subtotal), 0.0)
    }

    @Test fun formatoMoneda() = assertEquals("S/ 12.50", Formato.moneda(12.5))
}
