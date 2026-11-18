// Capa de datos: productos iniciales. El controlador reescribe este archivo cada vez que
// se crea, edita o elimina un producto (así los cambios persisten al reiniciar el servidor).
const productos = [
  { "id": 1, "nombre": "Manzana", "descripcion": "Manzana roja fresca, 1 kg", "precio": 2.5, "stock": 40, "imagen": "prod_manzana", "categoria": "Frutas" },
  { "id": 2, "nombre": "Plátano", "descripcion": "Plátano de seda, 1 kg", "precio": 1.8, "stock": 50, "imagen": "prod_platano", "categoria": "Frutas" },
  { "id": 3, "nombre": "Tomate", "descripcion": "Tomate italiano, 1 kg", "precio": 3, "stock": 35, "imagen": "prod_tomate", "categoria": "Verduras" },
  { "id": 4, "nombre": "Leche", "descripcion": "Leche entera en caja, 1 L", "precio": 4, "stock": 30, "imagen": "prod_leche", "categoria": "Lácteos" },
  { "id": 5, "nombre": "Queso fresco", "descripcion": "Queso fresco, 250 g", "precio": 8.5, "stock": 20, "imagen": "prod_queso", "categoria": "Lácteos" },
  { "id": 6, "nombre": "Pan francés", "descripcion": "Bolsa de 10 panes frescos", "precio": 1.5, "stock": 60, "imagen": "prod_pan", "categoria": "Panadería" },
  { "id": 7, "nombre": "Huevos", "descripcion": "Docena de huevos pardos", "precio": 9.5, "stock": 25, "imagen": "prod_huevos", "categoria": "Abarrotes" },
  { "id": 8, "nombre": "Arroz", "descripcion": "Arroz extra, bolsa de 1 kg", "precio": 4.2, "stock": 45, "imagen": "prod_arroz", "categoria": "Abarrotes" },
  { "id": 9, "nombre": "Agua mineral", "descripcion": "Agua sin gas, botella de 625 ml", "precio": 1.5, "stock": 80, "imagen": "prod_agua", "categoria": "Bebidas" },
  { "id": 10, "nombre": "Jugo de naranja", "descripcion": "Jugo natural en caja, 1 L", "precio": 3.5, "stock": 28, "imagen": "prod_jugo", "categoria": "Bebidas" }
];

module.exports = productos;
