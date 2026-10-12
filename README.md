# Tappazo

Tappazo es una aplicación social de partidas por rondas. El backend administra
partidas y participantes, aplica las modalidades de juego, resuelve empates,
calcula balances y deudas, y ofrece autenticación, almacenamiento de imágenes y
comunicación en tiempo real.

## Componentes

- [`tappazo-backend/`](./tappazo-backend/): API REST y WebSocket en Java 25,
  Spring Boot y MySQL.
- [`tappazo-mobile/`](./tappazo-mobile/): ubicación reservada para el cliente
  móvil; aún no contiene archivos versionados.
- [`tools/livekit-test/`](./tools/livekit-test/): cliente web mínimo para probar
  audio LiveKit durante el desarrollo.
- [`docker-compose.yml`](./docker-compose.yml): entorno local de MySQL, MinIO,
  LiveKit y backend.

## Empezar

### Entorno con Docker Compose

Con Docker Desktop iniciado, desde la raíz del repositorio:

```powershell
docker compose up -d
```

El backend queda disponible en `http://localhost:8080`. Para detener los
servicios sin borrar sus datos:

```powershell
docker compose stop
```

Los datos de MySQL y MinIO se mantienen en volúmenes de Docker. Evita
`docker compose down -v` si deseas conservarlos.

### Backend local con MySQL y cuentas de prueba

El perfil local permite crear usuarios de prueba sin Google OAuth y usar una
base MySQL local separada. Requiere Java 25 y MySQL 8:

```powershell
.\tappazo-backend\run-local.ps1
```

El script solicita la contraseña de MySQL de forma oculta. Para la guía
completa de autenticación local, Postman, simulación de rondas, consulta de
balances y voz, consulta
[la guía de pruebas locales](./tappazo-backend/docs/local-testing.md).

## Probar la API con Postman

En la raíz del repositorio están los exports de Postman:

- [Colección Tappazo](./tappazo.postman_collection.json): colección con 16
  peticiones organizadas en carpetas.
- [Environment `env_tappazo`](./env_tappazo.postman_environment.json):
  variables para configurar las peticiones.

En Postman, usa **Import** para importar ambos archivos y selecciona
`env_tappazo` en el selector de environments. Completa `baseUrl` con
`http://localhost:8080`; después autentícate y guarda el token de respuesta en
la variable `token` para ejecutar las peticiones protegidas. La guía de
[pruebas locales](./tappazo-backend/docs/local-testing.md) explica el flujo de
login, creación de partida, simulación de rondas y consulta de balances.

El environment exportado actualmente tiene sus valores vacíos. Antes de
compartir una exportación que ya hayas usado, revisa que no contenga JWT,
contraseñas ni otros datos privados.

## Pruebas del backend

Desde PowerShell:

```powershell
Set-Location .\tappazo-backend
.\mvnw.cmd test
```

El proyecto requiere JDK 25. Algunas pruebas de integración usan H2; el perfil
local de desarrollo usa MySQL.

## Documentación

- [Guía de pruebas locales y ejemplos de API](./tappazo-backend/docs/local-testing.md)
- [Especificación funcional, reglas de juego y arquitectura](./tappazo-backend/docs/tappazo-spec-final.md)

## Notas para contribuciones

Las credenciales de desarrollo en Docker Compose no deben reutilizarse en
producción. Antes de publicar una configuración compartida, sustituye
contraseñas y secretos por valores locales no versionados y confirma que no se
incluyeron credenciales reales ni archivos locales.
