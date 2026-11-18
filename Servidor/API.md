# Contrato de API – Mi Market

Base URL local: `http://localhost:3000` (emulador Android: `http://10.0.2.2:3000`).
Formato: JSON (`Content-Type: application/json`). Los errores siempre tienen la forma `{ "error": "mensaje" }`.

## Modelo Producto
| Campo | Tipo | Notas |
|---|---|---|
| id | número | asignado por el servidor |
| nombre | texto | obligatorio, no vacío |
| descripcion | texto | opcional |
| precio | número | obligatorio, mayor que 0 |
| stock | entero | obligatorio, mayor o igual a 0 |
| imagen | texto | URL (http/https) o nombre de recurso local (ej. `prod_manzana`) |
| categoria | texto | opcional (por defecto `General`) |

## Endpoints
| Método | Ruta | Descripción | Éxito | Errores |
|---|---|---|---|---|
| GET | /api/productos[?q=texto] | Lista productos (q filtra por nombre/descripción/categoría) | 200 `[Producto]` | 500 |
| GET | /api/productos/:id | Un producto | 200 `Producto` | 404 |
| POST | /api/productos | Registra un producto | 201 `Producto` | 400 |
| PUT | /api/productos/:id | Actualiza un producto | 200 `Producto` | 400, 404 |
| DELETE | /api/productos/:id | Elimina un producto **solo si su stock es 0** | 200 `{mensaje, producto}` | 404, 409 (tiene stock) |
| PATCH | /api/productos/:id/stock | Descuenta stock tras una compra. Cuerpo `{"cantidad": 2}` | 200 `Producto` | 400 (cantidad inválida o stock insuficiente), 404 |

## Ejemplo POST /api/productos
Solicitud:
```json
{ "nombre": "Leche evaporada", "descripcion": "Lata de leche evaporada de 400 g", "precio": 4.50, "stock": 25, "imagen": "https://ejemplo.com/leche.jpg" }
```
Respuesta 201:
```json
{ "id": 11, "nombre": "Leche evaporada", "descripcion": "Lata de leche evaporada de 400 g", "precio": 4.5, "stock": 25, "imagen": "https://ejemplo.com/leche.jpg", "categoria": "General" }
```

## Códigos de respuesta
| Código | Cuándo |
|---|---|
| 200 / 201 | Operación exitosa |
| 400 | JSON mal formado o datos inválidos (nombre vacío, precio ≤ 0, stock < 0) |
| 404 | El producto (o la ruta) no existe |
| 409 | Conflicto de regla de negocio: eliminar un producto que aún tiene stock |
| 500 | Error interno (mensaje genérico, sin detalles técnicos) |

Parámetro de prueba: `?demora=15000` retrasa la respuesta (ms) para probar el timeout.
