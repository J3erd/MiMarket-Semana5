package com.example.mimarket.data.local.room

import com.example.mimarket.model.Producto
import com.example.mimarket.model.Tienda

/**
 * Datos de ejemplo. Se usan en dos casos:
 *  - Productos: para poblar Firestore la primera vez, si el servidor Node.js no está encendido.
 *  - Tiendas: como respaldo del mapa mientras la colección "tiendas" de Firestore está vacía.
 * (Ya no hay usuario de prueba: las cuentas se crean con Firebase Authentication.)
 */
object DatosIniciales {

    fun productos(): List<Producto> = listOf(
        Producto(nombre = "Manzana", descripcion = "Manzana roja fresca, 1 kg", precio = 2.50, imagen = "prod_manzana", categoria = "Frutas", stock = 40),
        Producto(nombre = "Plátano", descripcion = "Plátano de seda, 1 kg", precio = 1.80, imagen = "prod_platano", categoria = "Frutas", stock = 50),
        Producto(nombre = "Tomate", descripcion = "Tomate italiano, 1 kg", precio = 3.00, imagen = "prod_tomate", categoria = "Verduras", stock = 35),
        Producto(nombre = "Leche", descripcion = "Leche entera en caja, 1 L", precio = 4.00, imagen = "prod_leche", categoria = "Lácteos", stock = 30),
        Producto(nombre = "Queso fresco", descripcion = "Queso fresco, 250 g", precio = 8.50, imagen = "prod_queso", categoria = "Lácteos", stock = 20),
        Producto(nombre = "Pan francés", descripcion = "Bolsa de 10 panes frescos", precio = 1.50, imagen = "prod_pan", categoria = "Panadería", stock = 60),
        Producto(nombre = "Huevos", descripcion = "Docena de huevos pardos", precio = 9.50, imagen = "prod_huevos", categoria = "Abarrotes", stock = 25),
        Producto(nombre = "Arroz", descripcion = "Arroz extra, bolsa de 1 kg", precio = 4.20, imagen = "prod_arroz", categoria = "Abarrotes", stock = 45),
        Producto(nombre = "Agua mineral", descripcion = "Agua sin gas, botella de 625 ml", precio = 1.50, imagen = "prod_agua", categoria = "Bebidas", stock = 80),
        Producto(nombre = "Jugo de naranja", descripcion = "Jugo natural en caja, 1 L", precio = 3.50, imagen = "prod_jugo", categoria = "Bebidas", stock = 28)
    )

    /** Tiendas físicas de Mi Market en Lima (coordenadas aproximadas de ejemplo). */
    fun tiendas(): List<Tienda> = listOf(
        Tienda("san-miguel", "Mi Market - San Miguel", "Av. La Marina 2000, San Miguel", "Lun a Dom 8:00 - 22:00", -12.0773, -77.0826),
        Tienda("los-olivos", "Mi Market - Los Olivos", "Av. Alfredo Mendiola 6062, Los Olivos", "Lun a Dom 8:00 - 22:00", -11.9527, -77.0680),
        Tienda("miraflores", "Mi Market - Miraflores", "Av. José Larco 1301, Miraflores", "Lun a Dom 7:00 - 23:00", -12.1283, -77.0296),
        Tienda("brena", "Mi Market - Breña", "Av. Brasil 840, Breña", "Lun a Sáb 8:00 - 21:00", -12.0625, -77.0478),
        Tienda("surco", "Mi Market - Surco", "Av. Primavera 1050, Santiago de Surco", "Lun a Dom 8:00 - 22:00", -12.1107, -76.9806)
    )
}
