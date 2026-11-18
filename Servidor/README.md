# Mi Market – Backend (Node.js + Express)
API REST de productos organizada por capas (misma estructura del servidor de ejemplo):

```
Servidor/
├─ package.json
├─ test.js                         pruebas automáticas (npm test)
└─ src/
   ├─ app.js                       entrada: Express, middlewares, montaje de rutas
   ├─ routes/productoRoutes.js     método HTTP + URL → controlador
   ├─ controllers/productoController.js   validación, lógica CRUD y respuestas HTTP
   └─ data/productos.js            datos (se reescribe al crear/editar/eliminar: persisten)
```

## Ejecutar
```
cd Servidor
npm install
npm start      # http://localhost:3000
npm test
```
Contrato completo en `API.md`. Despliegue opcional en Render con `render.yaml`.

## Semana 5
El CRUD principal y la autenticación se migraron a Firebase. Este servidor se conserva solo para **importar el catálogo inicial a Firestore** (botón *Cargar productos iniciales* de la app, que llama a `GET /api/productos`). Si está apagado, la app usa los productos de ejemplo.
