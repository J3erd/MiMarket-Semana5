// Pruebas automáticas de los endpoints: npm test (no modifica los datos reales).
process.env.NODE_ENV = "test";
const http = require("http");
const assert = require("assert");
const app = require("./src/app");
const productos = require("./src/data/productos");

const srv = app.listen(0, async () => {
  const port = srv.address().port;
  const call = (method, url, body) => new Promise((ok) => {
    const r = http.request({ port, method, path: url, headers: { "Content-Type": "application/json" } }, (res) => {
      let d = ""; res.on("data", (c) => (d += c)); res.on("end", () => ok({ s: res.statusCode, b: d ? JSON.parse(d) : null }));
    });
    if (body !== undefined) r.write(typeof body === "string" ? body : JSON.stringify(body));
    r.end();
  });
  productos.length = 0; // lista vacía solo en memoria
  let r = await call("POST", "/api/productos", { nombre: "Leche evaporada", descripcion: "Lata 400 g", precio: 4.5, stock: 25, imagen: "x" });
  assert.equal(r.s, 201); const id = r.b.id;
  assert.equal((await call("GET", "/api/productos")).b.length, 1);
  assert.equal((await call("GET", "/api/productos/" + id)).s, 200);
  assert.equal((await call("GET", "/api/productos?q=evapor")).b.length, 1);
  assert.equal((await call("GET", "/api/productos?q=zzz")).b.length, 0);
  assert.equal((await call("POST", "/api/productos", { nombre: "", precio: 1, stock: 1 })).s, 400);
  assert.equal((await call("POST", "/api/productos", { nombre: "a", precio: 0, stock: 1 })).s, 400);
  assert.equal((await call("POST", "/api/productos", { nombre: "a", precio: 1, stock: -1 })).s, 400);
  assert.equal((await call("POST", "/api/productos", "{mal")).s, 400);
  r = await call("PUT", "/api/productos/" + id, { nombre: "Leche", precio: 5, stock: 10 });
  assert.equal(r.s, 200); assert.equal(r.b.precio, 5);
  r = await call("PATCH", "/api/productos/" + id + "/stock", { cantidad: 4 });
  assert.equal(r.s, 200); assert.equal(r.b.stock, 6);
  assert.equal((await call("PATCH", "/api/productos/" + id + "/stock", { cantidad: 99 })).s, 400);
  assert.equal((await call("PUT", "/api/productos/999", { nombre: "a", precio: 1, stock: 1 })).s, 404);
  assert.equal((await call("DELETE", "/api/productos/999")).s, 404);
  assert.equal((await call("DELETE", "/api/productos/" + id)).s, 409); // con stock no se elimina
  await call("PUT", "/api/productos/" + id, { nombre: "Leche", precio: 5, stock: 0 });
  assert.equal((await call("DELETE", "/api/productos/" + id)).s, 200);
  assert.equal((await call("GET", "/api/productos/" + id)).s, 404);
  assert.equal((await call("GET", "/otra")).s, 404);
  console.log("OK: todas las pruebas pasaron");
  srv.close();
});
