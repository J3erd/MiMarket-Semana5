// Punto de entrada: configura Express, monta las rutas y arranca el servidor.
const express = require("express");
const cors = require("cors");
const productoRoutes = require("./routes/productoRoutes");

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Registro de cada petición en la consola
app.use((req, res, next) => {
  console.log(new Date().toISOString(), req.method, req.url);
  next();
});

// Retardo opcional para probar el timeout: GET /api/productos?demora=15000
app.use((req, res, next) => setTimeout(next, Math.min(parseInt(req.query.demora) || 0, 60000)));

// Rutas de la API (contrato en API.md)
app.use("/api", productoRoutes);

// JSON mal formado -> 400; ruta inexistente -> 404; error inesperado -> 500 sin detalles internos
app.use((err, req, res, next) => {
  if (err.type === "entity.parse.failed") return res.status(400).json({ error: "JSON inválido" });
  console.error(err);
  res.status(500).json({ error: "Error interno del servidor" });
});
app.use((req, res) => res.status(404).json({ error: "Ruta no encontrada" }));

if (require.main === module) {
  app.listen(PORT, "0.0.0.0", () => console.log(`Mi Market backend en http://localhost:${PORT}`));
}

module.exports = app;
