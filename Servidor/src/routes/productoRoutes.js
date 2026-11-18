// Capa de rutas: asocia cada método HTTP + URL con su controlador.
const express = require("express");
const {
  obtenerProductos,
  obtenerProducto,
  crearProducto,
  actualizarProducto,
  eliminarProducto,
  descontarStock
} = require("../controllers/productoController");

const router = express.Router();

router.get("/productos", obtenerProductos);
router.get("/productos/:id", obtenerProducto);
router.post("/productos", crearProducto);
router.put("/productos/:id", actualizarProducto);
router.patch("/productos/:id/stock", descontarStock);
router.delete("/productos/:id", eliminarProducto);

module.exports = router;
