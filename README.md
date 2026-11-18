# Mi Market – Semana 5

Integración de Firebase (Authentication, Cloud Firestore, Storage), Google Maps con geolocalización
y medidas de privacidad y seguridad. Continuación del proyecto de la semana 4.

| Carpeta | Contenido | Se abre con |
|---|---|---|
| `MiMarket/` | App Android (Kotlin, Jetpack Compose, MVVM) | Android Studio |
| `Servidor/` | Backend Node.js + Express de la semana 4 | Visual Studio Code |
| `Documentos/` | Informe, reglas de seguridad y política de privacidad | — |

## Ejecutar la app
1. Copiar `google-services.json` en `MiMarket/app/`.
2. Abrir la carpeta `MiMarket` en Android Studio (se crea `local.properties`) y agregar al final:
   `MAPS_API_KEY=<clave de Google Maps>`
3. Sincronizar Gradle y ejecutar.

`google-services.json` y la clave de Maps no se incluyen en el repositorio por seguridad.

## Ejecutar el servidor (opcional)
```
cd Servidor
npm install
npm start
```

## Documentos
- `1_Informe_Semana5` (PDF y Word) – requisitos cumplidos, verificación y guion de la demostración
- `2_Reglas_de_Seguridad` (PDF y Word) + `firestore.rules` + `storage.rules`
- `3_Politica_de_Privacidad` (PDF y Word)
