// Capa de controladores: lógica de cada endpoint (validación, CRUD y respuestas HTTP).
const fs = require("fs");
const path = require("path");

const DATA_PATH = path.join(__dirname, "../data/productos.js");
const productos = require("../data/productos");

// Persiste el arreglo en data/productos.js (no escribe durante las pruebas automáticas).
const guardarProductos = (callback) => {
  if (process.env.NODE_ENV === "test") return callback(null);
  const contenido = `const productos = ${JSON.stringify(productos, null, 2)};\n\nmodule.exports = productos;\n`;
  fs.writeFile(DATA_PATH, contenido, "utf-8", (err) => {
    if (err) console.error("Error al guardar productos:", err);
    callback(err || null);
  });
};

const idValido = (s) => /^\d+$/.test(String(s));
const buscarPorId = (id) => (idValido(id) ? productos.find((p) => p.id === Number(id)) : undefined);
const siguienteId = () => productos.reduce((m, p) => Math.max(m, p.id), 0) + 1;

// Devuelve la lista de errores de validación (vacía si los datos son válidos).
const validar = (b) => {
  const errores = [];
  if (typeof b.nombre !== "string" || b.nombre.trim() === "") errores.push("El nombre es obligatorio");
  if (typeof b.precio !== "number" || !(b.precio > 0)) errores.push("El precio debe ser mayor que cero");
  if (!Number.isInteger(b.stock) || b.stock < 0) errores.push("El stock debe ser un entero no negativo");
  if (b.descripcion !== undefined && typeof b.descripcion !== "string") errores.push("La descripción debe ser texto");
  if (b.imagen !== undefined && typeof b.imagen !== "string") errores.push("La imagen debe ser texto");
  return errores;
};

// Normaliza los datos recibidos.
const limpiar = (b, id) => ({
  id,
  nombre: b.nombre.trim(),
  descripcion: (b.descripcion || "").trim(),
  precio: Math.round(b.precio * 100) / 100,
  stock: b.stock,
  imagen: (b.imagen || "").trim(),
  categoria: (b.categoria || "General").trim() || "General"
});

const noEncontrado = (res) => res.status(404).json({ error: "Producto no encontrado" });

// READ: GET /api/productos[?q=texto]
const obtenerProductos = (req, res) => {
  const q = typeof req.query.q === "string" ? req.query.q.trim().toLowerCase() : "";
  const lista = q
    ? productos.filter((p) => [p.nombre, p.descripcion, p.categoria].some((t) => String(t).toLowerCase().includes(q)))
    : productos;
  res.status(200).json(lista);
};

// READ: GET /api/productos/:id
const obtenerProducto = (req, res) => {
  const p = buscarPorId(req.params.id);
  if (!p) return noEncontrado(res);
  res.status(200).json(p);
};

// CREATE: POST /api/productos
const crearProducto = (req, res) => {
  const errores = validar(req.body || {});
  if (errores.length) return res.status(400).json({ error: errores.join(". ") });
  const nuevo = limpiar(req.body, siguienteId());
  productos.push(nuevo);
  guardarProductos((err) => {
    if (err) return res.status(500).json({ error: "Error interno del servidor" });
    res.status(201).json(nuevo);
  });
};

// UPDATE: PUT /api/productos/:id
const actualizarProducto = (req, res) => {
  const p = buscarPorId(req.params.id);
  if (!p) return noEncontrado(res);
  const errores = validar(req.body || {});
  if (errores.length) return res.status(400).json({ error: errores.join(". ") });
  Object.assign(p, limpiar({ categoria: p.categoria, ...req.body }, p.id));
  guardarProductos((err) => {
    if (err) return res.status(500).json({ error: "Error interno del servidor" });
    res.status(200).json(p);
  });
};

// DELETE: DELETE /api/productos/:id (solo si stock = 0; si no, responde 409)
const eliminarProducto = (req, res) => {
  const p = buscarPorId(req.params.id);
  if (!p) return noEncontrado(res);
  // Regla de negocio: solo se puede eliminar un producto sin stock (stock = 0)
  if (p.stock > 0) {
    return res.status(409).json({ error: `No se puede eliminar el producto "${p.nombre}" porque cuenta con ${p.stock} unidades en stock` });
  }
  productos.splice(productos.indexOf(p), 1);
  guardarProductos((err) => {
    if (err) return res.status(500).json({ error: "Error interno del servidor" });
    res.status(200).json({ mensaje: "Producto eliminado", producto: p });
  });
};

// PATCH: /api/productos/:id/stock {"cantidad": n} — descuenta stock tras una compra en la app
const descontarStock = (req, res) => {
  const p = buscarPorId(req.params.id);
  if (!p) return noEncontrado(res);
  const c = (req.body || {}).cantidad;
  if (!Number.isInteger(c) || c <= 0) return res.status(400).json({ error: "La cantidad debe ser un entero mayor que cero" });
  if (c > p.stock) return res.status(400).json({ error: "Stock insuficiente" });
  p.stock -= c;
  guardarProductos((err) => {
    if (err) return res.status(500).json({ error: "Error interno del servidor" });
    res.status(200).json(p);
  });
};

module.exports = {
  obtenerProductos,
  obtenerProducto,
  crearProducto,
  actualizarProducto,
  eliminarProducto,
  descontarStock
};
