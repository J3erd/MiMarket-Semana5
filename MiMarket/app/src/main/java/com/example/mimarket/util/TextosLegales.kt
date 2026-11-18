package com.example.mimarket.util

/**
 * Textos del aviso de privacidad que se muestran dentro de la app.
 * La versión completa está en Documentos/Politica_de_Privacidad.md (entregable de la semana 5).
 */
object TextosLegales {

    /** Aviso general (registro y perfil). */
    val AVISO_PRIVACIDAD = """
        En Mi Market cuidamos tu información. Antes de continuar, te contamos de forma clara qué datos usamos y para qué.

        1. Qué datos recopilamos
        • Tu nombre y correo electrónico, para crear tu cuenta.
        • Tu contraseña la administra Firebase Authentication: Mi Market nunca la ve ni la guarda.
        • Tu historial de pedidos y, si tú lo decides, tu dirección de entrega.
        • Tu ubicación, solo si aceptas este aviso y concedes el permiso, y solo mientras usas el mapa.
        • Las imágenes que subes al registrar un producto.

        2. Para qué los usamos
        • Identificarte y mantener tu sesión.
        • Registrar tus compras y mostrarte tu historial.
        • Mostrarte las tiendas más cercanas y sugerir tu dirección de entrega.

        3. Cómo los protegemos
        • Solo tú puedes leer y modificar tus datos personales y tus pedidos (reglas de seguridad de Firestore).
        • La comunicación con nuestros servidores viaja cifrada (HTTPS).
        • No vendemos ni compartimos tus datos con terceros con fines comerciales.

        4. Tus derechos
        • Puedes revocar el uso de tu ubicación cuando quieras, desde Perfil o desde los ajustes del teléfono.
        • Puedes eliminar tu cuenta y tus datos personales desde Perfil > Eliminar cuenta.

        Tratamos tus datos conforme a la Ley N.° 29733, Ley de Protección de Datos Personales del Perú.
    """.trimIndent()

    /** Resumen que se muestra en la pantalla "Aviso de Privacidad" (después del registro). */
    val AVISO_RESUMEN = """
        En Mi Market recopilamos información personal y de ubicación para mejorar tu experiencia, procesar tus pedidos y mostrarte tiendas cercanas.

        Puedes revisar, actualizar o eliminar tus datos cuando lo desees desde tu perfil.

        Al continuar, aceptas nuestro Aviso de Privacidad.
    """.trimIndent()

    /** Aviso específico que se muestra ANTES de pedir el permiso de ubicación. */
    val AVISO_UBICACION = """
        Para mostrarte las tiendas de Mi Market más cercanas y sugerir tu dirección de entrega, necesitamos acceder a la ubicación de tu dispositivo.

        • Solo usamos tu ubicación mientras tienes abierto el mapa.
        • No la guardamos en nuestros servidores ni la compartimos con terceros.
        • Tu dirección solo se guarda si pulsas "Usar como dirección de entrega", y puedes borrarla eliminando tu cuenta.
        • Puedes revocar este permiso cuando quieras desde Perfil o desde los ajustes del teléfono.

        Si no aceptas, igual podrás ver el mapa y elegir una tienda manualmente.
    """.trimIndent()
}
