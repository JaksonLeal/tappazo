# Guía para probar Tappazo localmente

El backend ofrece APIs REST para autenticación, partidas, mesas/balances y
archivos, además de WebSocket/STOMP para eventos y acciones en tiempo real.
Con el perfil `local-auth` puedes crear jugadores de prueba sin Google: el
backend los guarda en MySQL y devuelve el mismo JWT de Tappazo que requieren
las rutas protegidas. Este perfil solo está habilitado al ejecutar el backend
localmente y el servidor se enlaza a `127.0.0.1`.

## Base de datos local

El backend local utiliza el esquema `tappazo_local` en el servicio MySQL 8
instalado en el equipo (`127.0.0.1:3306`). En MySQL Workbench, conectado a tu
servidor local, crea el esquema si aún no existe:

```sql
CREATE DATABASE IF NOT EXISTS tappazo_local
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

La tarea local usa el usuario `root`, ya existente en el equipo, y solicita su
contraseña sin mostrarla. No hace falta crear otro usuario si tu cuenta ya
puede acceder al esquema. Hibernate crea o actualiza las tablas dentro de
`tappazo_local`. El esquema y las tablas de la base Docker (`tappazo_db`) no se
modifican.

## Iniciar el backend fuera de Docker

El backend requiere Java 25. Para liberar el puerto 8080, si el backend de
Compose está en ejecución, detén solo ese servicio desde la raíz del proyecto:

```powershell
docker compose stop backend
```

Luego, en VS Code, ejecuta **Terminal → Run Task… → Run Tappazo Backend (Local
MySQL)**. La tarea solicita la contraseña de `root` de forma oculta y arranca
Spring Boot con los perfiles `dev,local-auth`.

Como alternativa, desde PowerShell en la raíz del repositorio puedes iniciar
el mismo proceso con:

```powershell
.\tappazo-backend\run-local.ps1
```

La API queda en `http://localhost:8080`. Al terminar, detén la tarea con
`Ctrl+C`. Para volver al backend de Docker, ejecuta `docker compose up -d`
desde la raíz. No uses `docker compose down -v` si quieres conservar sus
volúmenes.

## Preparar Postman

Crea un Environment con estas variables:

| Variable | Valor inicial |
| --- | --- |
| `baseUrl` | `http://localhost:8080` |
| `token` | Vacío; lo devuelve la autenticación |
| `token2` | Vacío; JWT del segundo jugador |
| `userId` | Vacío; ID del jugador autenticado |
| `gameId` | Vacío; ID de la partida |
| `gameCode` | Vacío; código de invitación |
| `fileKey` | Vacío; clave del archivo subido |

Para cada llamada protegida, selecciona **Authorization → Bearer Token** y
usa `{{token}}` (o `{{token2}}` para el segundo jugador). En cuerpos JSON,
selecciona **Body → raw → JSON**. Solo las rutas de autenticación son públicas;
las otras rutas REST requieren un JWT válido. Un `401` normalmente indica que
falta el token o que no es válido.

Opcionalmente, automatiza el guardado de tokens desde **Scripts →
Post-response** en una petición de login:

```javascript
const data = pm.response.json();
pm.environment.set("token", data.token);
pm.environment.set("userId", data.userId);
```

Al autenticar el segundo jugador, cambia `"token"` por `"token2"` en la
instrucción `pm.environment.set`.

## Flujo recomendado: dos jugadores y una partida

### 1. Crear o recuperar el jugador anfitrión

`POST {{baseUrl}}/api/v1/auth/local`

```json
{
  "email": "jugador1@example.test",
  "nickname": "Jugador 1"
}
```

El endpoint normaliza el correo y busca al jugador por email. Si no existe, lo
guarda en MySQL; si ya existe, reutiliza ese usuario y su nickname guardado.
Devuelve `token`, `userId`, `nickname` y `email`. La petición puede repetirse
para obtener otro JWT sin duplicar la cuenta.

### 2. Crear una partida como anfitrión

`POST {{baseUrl}}/api/v1/games`, con `{{token}}`:

```json
{
  "drinkPrice": 1000,
  "initialMode": "ULTIMO_PIERDE",
  "maxPlayers": 6
}
```

La respuesta es `201 Created`; guarda `id` como `gameId` y `code` como
`gameCode`. El anfitrión también queda registrado como participante. `drinkPrice`
debe ser mayor que cero; `maxPlayers`, si se envía, debe estar entre 2 y 10.
`maxPlayers` es el máximo total de jugadores de la partida, no el número de
perdedores. Debe ser suficiente para la modalidad: 2 para `ULTIMO_PIERDE`,
3 para `PEQUENOS_PAGAN` y 4 para `ULTIMOS_DOS_PIERDEN`.
`initialMode` puede ser `ULTIMO_PIERDE`, `PEQUENOS_PAGAN` o
`ULTIMOS_DOS_PIERDEN`. Si se omiten el modo y el máximo, el backend elige los
valores por defecto del dominio.

### 3. Crear un segundo jugador y unirse

Envía otra vez `POST {{baseUrl}}/api/v1/auth/local` con un correo diferente y
guarda el JWT como `token2`. Luego envía:

`POST {{baseUrl}}/api/v1/games/join`, con `{{token2}}`:

```json
{
  "gameCode": "{{gameCode}}"
}
```

En una partida que está en lobby, el nuevo jugador se incorpora como
participante. El endpoint es idempotente para un jugador ya inscrito. Si la
partida está en curso, el rol asignado puede ser espectador; una partida
finalizada rechaza nuevos participantes.

### 4. Verificar los datos persistidos y las vistas de la partida

Con cualquiera de los dos JWT, prueba las lecturas:

| Método | Ruta | Qué devuelve |
| --- | --- | --- |
| `GET` | `/api/v1/games/{{gameId}}` | Datos de la partida, participantes y ronda actual si existe |
| `GET` | `/api/v1/games/code/{{gameCode}}` | Partida localizada por su código |
| `GET` | `/api/v1/games/{{gameId}}/participants` | Lista de participantes |
| `GET` | `/api/v1/mesas/{{gameId}}` | Resumen de mesa: estado, anfitrión y participantes |
| `GET` | `/api/v1/mesas/{{gameId}}/balances` | Deudas netas compensadas entre jugadores |
| `GET` | `/api/v1/mesas/{{gameId}}/debts` | Movimientos de deuda originales |
| `GET` | `/api/v1/mesas/users/{{userId}}/statistics` | Estadísticas del jugador |

Una mesa es la vista agrupada de una partida para consultar a sus participantes,
balances y deudas; no es otra entidad o partida. Es normal que `balances` y
`debts` estén vacíos si todavía no hay movimientos de deuda producidos por una
ronda.

También puedes inspeccionar MySQL Workbench: selecciona el esquema
`tappazo_local`, actualiza **Schemas → Tables** y prueba:

```sql
SELECT id, google_id, nickname, email, created_at
FROM tappazo_local.users;

SELECT id, code, host_id, state, max_players, current_drink_price, current_mode
FROM tappazo_local.games;

SELECT id, game_id, user_id, role, participant_state
FROM tappazo_local.game_participants;
```

Para comprobar persistencia, crea los jugadores y partida, detén Spring Boot
con `Ctrl+C`, vuelve a iniciar la tarea local y repite las consultas. No borres
el esquema: Hibernate usa `ddl-auto=update`.

## Simular rondas sin fotos ni votos reales

Con el perfil `local-auth` también está disponible un simulador local. Solo el
anfitrión de la partida puede usarlo. El endpoint crea una ronda real, registra
los números como revelaciones aprobadas y ejecuta la misma resolución que usa
el juego; no usa cámara, voz ni servicios externos. No está disponible en
Docker con el perfil normal `dev` ni en producción.

Primero crea y llena la partida como se describe arriba. Envía un número de
`0` a `99` para cada jugador activo, usando sus `userId` de la respuesta de
login. La cantidad debe respetar el mínimo de la modalidad y el máximo total
configurado en la partida:

`POST {{baseUrl}}/api/v1/local/simulator/games/{{gameId}}/rounds`

```json
{
  "results": [
    { "userId": "{{userId}}", "number": 12 },
    { "userId": "{{player2Id}}", "number": 47 },
    { "userId": "{{player3Id}}", "number": 83 }
  ]
}
```

Usa el JWT del anfitrión. La respuesta informa el `roundId`, los perdedores
confirmados, ganadores, deudas calculadas y si se creó un desempate. Si no hay
empate, consulta inmediatamente `GET /api/v1/mesas/{{gameId}}/debts` y
`GET /api/v1/mesas/{{gameId}}/balances` para verificar los movimientos y el
neteo persistidos en MySQL.

Cuando la respuesta incluya `tieBreakId`, el voto de los jugadores empatados se
puede simular con:

`POST {{baseUrl}}/api/v1/local/simulator/tie-breaks/{{tieBreakId}}/resolve`

```json
{
  "votes": [
    { "playerId": "{{player1Id}}", "decision": "NUEVA_RONDA" },
    { "playerId": "{{player2Id}}", "decision": "DIVIDIR_CUENTA" }
  ]
}
```

Si todos los participantes de la ronda quedaron empatados, incluye el voto de
cada participante. Si no hay acuerdo, el anfitrión queda asignado como árbitro.
Si la decisión final para un empate total es `DIVIDIR_CUENTA`, la ronda termina
sin crear deudas: cada jugador paga su propia bebida y los balances no cambian.

Si los votos discrepan, la respuesta conserva `status: "PENDING"` e informa
`selectedDeciderId`; la ronda no se considera resuelta ni genera deudas por el
desempate hasta que el árbitro vote. Para simular su voto, repite la petición
con los mismos votos y añade, por ejemplo:

```json
{
  "votes": [
    { "playerId": "{{player1Id}}", "decision": "NUEVA_RONDA" },
    { "playerId": "{{player2Id}}", "decision": "DIVIDIR_CUENTA" }
  ],
  "deciderDecision": "DIVIDIR_CUENTA"
}
```

También puedes enviar los votos en una petición y, una vez asignado el árbitro,
hacer una segunda petición con `{"deciderDecision":"DIVIDIR_CUENTA"}`. El
simulador devuelve el estado del desempate y las deudas generadas. `NUEVA_RONDA`
crea una ronda de desempate. Cuando el empate original fue total, puedes volver
a llamar el endpoint `/games/{{gameId}}/rounds` con los resultados de todos los
jugadores para simular esa siguiente ronda. El simulador no procesa la ronda
posterior de un empate parcial; ese camino debe probarse mediante el flujo
normal de la aplicación.

## Catálogo REST

Todas las rutas de esta tabla están implementadas por el backend. Salvo los dos
endpoints de autenticación, requieren `Authorization: Bearer {{token}}`.

| Método | Ruta | Requisitos y efecto |
| --- | --- | --- |
| `POST` | `/api/v1/auth/local` | Solo con perfil local `local-auth`. Crea o reutiliza una cuenta de prueba y devuelve JWT. |
| `POST` | `/api/v1/auth/google` | Autenticación de Google; body `{"idToken":"<Google ID token>"}`. |
| `POST` | `/auth/google` | Alias legado del endpoint de autenticación Google. |
| `POST` | `/api/v1/games` | Body con `drinkPrice`, `initialMode`, opcional `maxPlayers`; crea partida y registra al host. |
| `POST` | `/api/v1/games/join` | Body `{"gameCode":"<código>"}`; el jugador del JWT se une. |
| `GET` | `/api/v1/games/{id}` | Detalles de la partida, participantes y ronda actual. |
| `GET` | `/api/v1/games/code/{code}` | Busca una partida por código. |
| `GET` | `/api/v1/games/{id}/participants` | Participantes de la partida. |
| `PATCH` | `/api/v1/games/{id}/settings` | Body opcional con `drinkPrice` y/o `mode`; solo el anfitrión puede cambiarlo. |
| `POST` | `/api/v1/games/{id}/voice-token` | Emite token de voz; requiere que el usuario pertenezca a la partida. |
| `GET` | `/api/v1/mesas/{gameId}` | Resumen de mesa/partida y participantes. |
| `GET` | `/api/v1/mesas/{gameId}/balances` | Deuda neta entre jugadores. |
| `GET` | `/api/v1/mesas/{gameId}/debts` | Movimientos de deuda de la partida. |
| `GET` | `/api/v1/mesas/users/{userId}/statistics` | Estadísticas agregadas de un usuario. |
| `POST` | `/api/v1/media/presigned-upload` | Body `{"fileName":"foto.jpg","contentType":"image/jpeg","folder":"avatars"}`; devuelve URLs PUT/GET y `fileKey`. |
| `GET` | `/api/v1/media/presigned-download?fileKey={fileKey}` | Devuelve una URL temporal para descargar el objeto. |

Para cambiar los ajustes, un body posible es:

```json
{
  "drinkPrice": 1500,
  "mode": "PEQUENOS_PAGAN"
}
```

En la subida de archivos, el `uploadUrl` devuelto es una URL prefirmada: crea
otra petición `PUT` a esa URL, establece el mismo `Content-Type` enviado al
endpoint y selecciona **Body → binary** para cargar el archivo. La descarga se
hace mediante un `GET` al `downloadUrl` o a la URL del endpoint de descarga,
sin añadir el JWT a la URL prefirmada.

## WebSocket para juego en tiempo real

El WebSocket nativo usa `ws://localhost:8080/ws` y el fallback SockJS usa
`http://localhost:8080/ws-sockjs`. El protocolo de mensajes es STOMP. Al
conectar, envía el JWT de Tappazo en el header STOMP `Authorization: Bearer
<token>` o `token: <token>`. Para estos mensajes usa un cliente STOMP; las
peticiones REST del catálogo anterior se prueban directamente en Postman.

Los clientes envían mensajes al prefijo `/app` y se suscriben a destinos
`/topic`:

| Acción | Destino de envío | Destino de suscripción |
| --- | --- | --- |
| Enviar intento de ronda | `/app/game/{gameId}/attempt` | `/topic/game/{gameId}` |
| Votar desempate | `/app/game/{gameId}/tiebreak-vote` | `/topic/game/{gameId}` |
| Enviar chat | `/app/game/{gameId}/chat` | `/topic/game/{gameId}/chat` |

Payload de intento:

```json
{
  "roundId": "ID_DE_RONDA",
  "proposedNumber": 42,
  "photoReference": "avatars/foto.jpg",
  "photoOmittedReason": null
}
```

Payload de voto:

```json
{
  "tieBreakId": "ID_DE_DESEMPATE",
  "decision": "NUEVA_RONDA"
}
```

`decision` acepta `NUEVA_RONDA` o `DIVIDIR_CUENTA`. Payload de chat:

```json
{
  "message": "¡Salud!",
  "type": "TEXT"
}
```

Si no se especifica `type`, el backend utiliza `TEXT`. Los eventos publicados en
`/topic/game/{gameId}` tienen `type`, `gameId`, `timestamp` y `payload`; entre
los tipos emitidos están `PLAYER_JOINED`, `ROUND_STARTED`,
`ATTEMPT_SUBMITTED`, `ROUND_RESOLVED`, `TIE_BREAK_STARTED`,
`TIE_BREAK_RESOLVED` y `GAME_FINISHED`. El chat se emite como `CHAT_MESSAGE`
en `/topic/game/{gameId}/chat`.

### Probar una llamada de voz localmente

El directorio `tools/livekit-test` contiene una página local mínima que usa el
SDK oficial `livekit-client`. Permite conectar el micrófono, publicar audio y
escuchar a los demás participantes sin implementar todavía la interfaz móvil.
Para que otro dispositivo de la red local use micrófono y audio, la página de
prueba requiere HTTPS, LiveKit debe anunciar la IP LAN del PC y el navegador
necesita WSS. El script de preparación configura esos tres detalles, crea un
certificado para la red local e inicia LiveKit.

La primera vez, con el PC conectado a la misma red que el móvil, desde la raíz
del repositorio ejecuta:

```powershell
.\tools\livekit-test\setup-lan.ps1
```

El script detecta la IP IPv4 de la ruta activa, genera un certificado para esa
IP, lo confía para tu usuario de Windows y recrea solo el servicio LiveKit. No
inicia el backend de Compose ni elimina volúmenes. Si Docker Desktop no está
abierto, inícialo antes. Luego instala el certificado
`%LOCALAPPDATA%\Tappazo\livekit-test\tappazo-local-dev-ca.cer` en Android como
certificado de CA:

**Ajustes → Seguridad y privacidad → Más ajustes de seguridad → Cifrado y
credenciales → Instalar un certificado → Certificado de CA**. Los nombres
varían según el fabricante de Android. Transfiere únicamente el archivo `.cer`
al teléfono; nunca copies `ca.p12`, `server.p12` ni los archivos de contraseña.
Android puede advertir que el tráfico de red podrá ser inspeccionado: es el
certificado local que habilita HTTPS confiable para esta prueba.
Al terminar la prueba, puedes quitar la CA desde la pantalla de certificados
de confianza del usuario en Android y Windows.

En una terminal normal, inicia la página HTTPS:

```powershell
Set-Location .\tools\livekit-test
npm run dev
```

En otra terminal inicia (o reinicia) el backend local para que el token
devuelva el URL WSS correcto:

```powershell
.\tappazo-backend\run-local.ps1
```

Si ya estaba ejecutándose, detén la tarea con `Ctrl+C` antes de reiniciarla.
Concede acceso de micrófono al sitio en el PC y en Android. Android y este PC
deben estar en la misma Wi-Fi/red local; el aislamiento de clientes Wi-Fi del
router debe estar desactivado.

Si el Firewall de Windows bloquea el acceso, abre PowerShell como
administrador y ejecuta las dos reglas limitadas a direcciones del mismo
subnet:

```powershell
New-NetFirewallRule -DisplayName 'Tappazo voice page (LAN)' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 5173 -RemoteAddress LocalSubnet -Profile Any
New-NetFirewallRule -DisplayName 'Tappazo LiveKit audio (LAN)' -Direction Inbound -Action Allow -Protocol UDP -LocalPort 7882 -RemoteAddress LocalSubnet -Profile Any
```

El router no necesita redirección de puertos para dispositivos conectados a la
misma red. No publiques estas reglas hacia Internet. Abre la dirección HTTPS
que imprimió el script (por ejemplo `https://192.168.2.4:5173`) en Android.
Para cada jugador:

1. Autentica o crea un jugador desde Postman y únete a la partida.
2. Con el Bearer JWT de ese jugador, llama a
   `POST /api/v1/games/{{gameId}}/voice-token`.
3. En la respuesta, copia el valor `token` al campo **Token de voz**. La página
   ya propone el URL WSS LAN; también puedes copiar `serverUrl` si la tarea local
   ya se reinició. No uses el JWT de Tappazo como token de LiveKit.
4. Pulsa **Conectar y activar micrófono** y permite el acceso al micrófono.
5. Abre otra pestaña en la página HTTPS, repite con el Bearer y el
   token de voz del segundo jugador.

Los dos deben usar la misma sala y un token vigente. La página muestra el
estado de conexión y eventos como participantes que entran o audio recibido.
Habla por un micrófono para probar; utiliza audífonos para evitar eco. Para
salir, pulsa **Desconectar** en ambas pestañas. Los tokens solo se mantienen
en memoria de la página, no se guardan.

Si LiveKit se inició después del backend local, no suele hacer falta reiniciar
Spring Boot para generar tokens: se firman localmente con la clave configurada;
el servidor LiveKit sí debe estar activo para establecer la llamada.

Para probar archivos, inicia también MinIO:

```powershell
docker compose up -d minio
Invoke-WebRequest http://127.0.0.1:9000/minio/health/live
```

Las URLs de subida y descarga prefirmadas solo funcionan mientras MinIO esté
disponible.

## Qué no expone actualmente la API

El catálogo anterior enumera las rutas REST y los mensajes STOMP actualmente
implementados. No hay rutas REST de creación/inicio manual de rondas,
resolución manual de rondas ni de escritura directa de balances. Las consultas
de balances reflejan movimientos que el juego haya generado; no añaden ni
modifican datos.
