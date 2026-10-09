# TAPPAZO

## Especificación funcional, reglas de negocio y arquitectura técnica

### Documento maestro final para implementación con Antigravity

---

# 1. OBJETIVO DEL PROYECTO

Tappazo es una aplicación móvil social para jugar un juego basado en bebidas con números ocultos en las tapas.

La aplicación permite crear partidas, invitar jugadores mediante un código, revelar los números de las tapas mediante fotografías, validar los resultados mediante votación de los demás jugadores y determinar automáticamente los perdedores según diferentes modalidades.

Tappazo también calcula las obligaciones económicas generadas durante las partidas.

**Importante:** Tappazo NO procesa dinero real. La aplicación únicamente registra quién debe dinero a quién, cuánto, por qué concepto, de qué ronda proviene la deuda, y balances acumulados entre jugadores **dentro de una misma partida**. Los pagos reales se hacen externamente (Nequi, transferencia, efectivo, etc.).

---

# 2. TECNOLOGÍAS

**Backend:** Java 25, Spring Boot, MySQL, JPA/Hibernate, Spring Security, JWT, Google Login, REST API, WebSocket, Docker, JUnit, Clean Architecture, SOLID.

**Almacenamiento de imágenes:** bucket compatible con S3. Desarrollo/beta: **MinIO** en el mismo `docker-compose.yml`. Producción: AWS S3 o Cloudflare R2 (API compatible, sin cambio de código).

**Comunicación de voz:** proveedor único para el MVP: **LiveKit** (`MVP_PROVIDER = LiveKit`), detrás de una interfaz `VoiceProvider` para poder agregar Agora/Twilio en el futuro sin tocar el dominio.

**Frontend:** Flutter, REST, WebSocket, SDK cliente de LiveKit.

**Repositorios:** `tappazo-backend` y `tappazo-mobile`, separados. El backend es la autoridad absoluta sobre las reglas del juego; el frontend nunca decide quién perdió, cuántos perdedores hay, quién vota, quién es host, cuándo termina un turno/ronda, ni cuánto debe cada jugador.

---

# 3. AUTENTICACIÓN

```text
Flutter → Google Login → Google ID Token → Backend
   → valida ID Token → busca/crea usuario → genera JWT Tappazo
   → Flutter recibe JWT
```

El JWT de Tappazo (no el token de Google) se usa para **todo**: REST y WebSocket (sección 72).

Endpoint: `POST /auth/google`

---

# 4. USUARIO Y NICKNAME

```text
User
- id, googleId, nickname, email, profileImage, createdAt, updatedAt
```

El nickname es global y solo se cambia desde el perfil, nunca dentro de una partida. Los jugadores se ordenan alfabéticamente por nickname para definir turnos y sucesión de host. Al congelar una ronda (sección 11), el nickname usado se guarda como `nicknameSnapshot` en `RoundParticipant` (sección 61) para que el historial no cambie si el usuario renombra su perfil después.

---

# 5. PARTIDA (GAME)

Una partida (`Game`) puede contener múltiples rondas (`Round`), cada una con su propia modalidad:

```text
Partida
 ├── Ronda 1 → Último pierde
 ├── Ronda 2 → Los pequeños pagan
 └── Ronda 3 → Últimos dos pierden
```

---

# 6. CÓDIGO DE PARTIDA

Al crear una partida, el backend genera un código único (ej. `X7K29P`), el creador se vuelve host, y la partida inicia en `LOBBY`. Los demás jugadores usan ese código para unirse.

---

# 7. LÍMITE DE JUGADORES

Mínimo global: **2**. Máximo configurable (no hardcodeado): `MAX_PLAYERS = 10`.

---

# 8. UNIRSE A UNA PARTIDA SEGÚN EL ESTADO DEL GAME

El rol que recibe un jugador que se une depende del estado del `Game` en ese momento (ver ciclo completo en sección 11):

```text
Game.state == LOBBY
   → el nuevo jugador entra directamente como PLAYER
   → puede participar desde la próxima ronda que se inicie

Game.state == IN_PROGRESS (hay una ronda corriendo)
   → el nuevo jugador entra como SPECTATOR temporal
   → puede observar jugadores, números revelados, fotos, votaciones,
     resultados, progreso, y escuchar el chat de voz permitido
   → NO puede votar, revelar, modificar reglas/precio, ni intervenir
   → cuando esa ronda termina y el Game vuelve a LOBBY,
     este jugador se promueve automáticamente a PLAYER
     y queda habilitado para la siguiente ronda sin acción adicional
```

Esto reemplaza la regla anterior de "el espectador nunca puede convertirse en jugador en esa partida" — si se unió mientras corría una ronda, espera a que termine y pasa a ser jugador normal desde la ronda siguiente.

---

# 9. HOST

El creador de la partida es el host inicial. El host puede: iniciar la ronda (sujeto al mínimo por modalidad, sección 12), modificar precio/modalidad mientras el `Game` esté en `LOBBY`, controlar acciones administrativas, arbitrar una revelación que agotó sus intentos (sección 25), y finalizar la partida (`POST /games/{id}/finish`, solo permitido estando en `LOBBY`, nunca a mitad de una ronda).

**Cambio de host:** si el host se desconecta, se aplica la política de espera general (secciones 14-17). Si definitivamente debe cambiar, el nuevo host es el primer jugador `ACTIVE` ordenado alfabéticamente por nickname. Un jugador `STAND_BY` o `SPECTATOR` no es elegible.

---

# 10. PRECIO DE LA BEBIDA

El host puede modificar el precio mientras `Game.state == LOBBY` (ej. $4.000 COP). Al iniciar una ronda, `drinkPrice` queda congelado para esa ronda y no puede cambiar hasta que termine. Al volver a `LOBBY`, el host puede ajustarlo de nuevo para la siguiente ronda.

---

# 11. CICLO DE VIDA DEL GAME Y DE LA ROUND

```text
Game.state:  LOBBY | IN_PROGRESS | FINISHED
Round.state: STARTING | REVEALING | VALIDATING | RESOLVING | TIE_BREAK | FINISHED
```

Transiciones del `Game`:

```text
Game creado           → LOBBY
Host inicia Round N   → Game pasa a IN_PROGRESS, Round N = STARTING
Round N termina       → Game vuelve a LOBBY, Round N = FINISHED
Host inicia Round N+1 → Game vuelve a IN_PROGRESS
...
Host finaliza partida (solo desde LOBBY) → Game = FINISHED
```

Mientras `Game == LOBBY`, el host puede ajustar precio y elegir la modalidad de la siguiente ronda. Mientras `Game == IN_PROGRESS`, el chat de voz está disponible solo para jugadores, no para espectadores (sección 27), y los nuevos que se unan entran como `SPECTATOR` (sección 8).

`FINISHED` identifica una partida ya cerrada: si alguien intenta unirse con el código después, el backend responde explícitamente que la partida terminó, en vez de dejarlo entrar como espectador de una partida muerta.

---

# 12. MÍNIMO DE JUGADORES POR MODALIDAD

```text
ULTIMO_PIERDE      → mínimo 2 jugadores
PEQUENOS_PAGAN     → mínimo 3 jugadores
ULTIMOS_DOS_PIERDEN → mínimo 4 jugadores
```

El mínimo de 4 en `ULTIMOS_DOS_PIERDEN` no es arbitrario: garantiza estructuralmente que `ganadores (W) ≥ perdedores (L=2)` siempre, evitando el caso roto `L > W`. Lo mismo se cumple siempre en `ULTIMO_PIERDE` (L=1) y en `PEQUENOS_PAGAN` (L=floor(N/2) ≤ W para cualquier N≥3) — con estos mínimos, el caso `L > W` queda eliminado de raíz en las tres modalidades, no solo evitado por casualidad.

Validación obligatoria antes de iniciar (en `StartRoundUseCase`, nunca en el frontend):

```text
if (jugadoresActivos.size() < modoSeleccionado.minimoJugadores()) {
    rechazar inicio de ronda
    mensaje: "Este modo requiere al menos {N} jugadores"
}
```

---

# 13. ESTADOS DEL PARTICIPANTE

Nivel partida (`GameParticipant.participantState`): `ACTIVE | STAND_BY | REMOVED`.
Nivel ronda (`RoundParticipant.state`): `ACTIVE | STAND_BY | REMOVED` — **independiente del estado a nivel partida** (ver sección 29: un `REMOVED` de ronda no saca al jugador de la partida completa).

El estado de conexión se mantiene separado del estado de participación: `ACTIVE + DISCONNECTED` no implica automáticamente `REMOVED`.

---

# 14. CONEXIÓN Y DESCONEXIÓN — REGLA GENERAL

La desconexión no elimina inmediatamente a un jugador. El backend espera `ACTION_TIMEOUT = 15 segundos` antes de actuar, y solo cuando esa desconexión realmente bloquea el avance (turno de revelar **o** turno de votar). Si el jugador alcanza a completar su acción a tiempo, no pasa nada.

---

# 15. STAND_BY — ENTRADA

```text
ACTIVE → (15s sin actuar) → STAND_BY
```

El sistema continúa con el siguiente jugador en la cola (de revelar o de votar) y el jugador en `STAND_BY` se mueve al final de la cola de turnos pendientes.

---

# 16. REINCORPORACIÓN DESDE STAND_BY

```text
STAND_BY → (reconecta) → ACTIVE
```

Su turno pendiente (revelar o votar) se coloca al final de la cola:

```text
Orden original: A → B → C → D
B queda STAND_BY: A → C → D
B vuelve: A → C → D → B
```

**Excepción — votación ya resuelta:** si B estaba `STAND_BY` durante la votación de un `RevealAttempt` y esa votación ya se resolvió (APPROVED/REJECTED) sin su voto, B **no vota retroactivamente** ese intento al reconectarse — la resolución ya es un hecho consumado (sección 24).

---

# 17. FLUJO COMPLETO CUANDO EL SISTEMA LLEGA AL FINAL DE LA COLA

Si se recorre toda la cola y el jugador en `STAND_BY` sigue sin reconectar:

```text
1. Se pregunta a todos los activos:
   "¿Lo esperamos un minuto o lo sacamos?"
   (ESPERAR_OTRO_MINUTO | REMOVER_JUGADOR)

2a. Decisión clara = ESPERAR_OTRO_MINUTO → espera 1 minuto.
    - Si regresa: se reincorpora (sección 16).
    - Si no regresa al agotarse el minuto: mensaje "el jugador se
      quedó sin conexión" → se remueve (ver regla de remoción abajo).

2b. Decisión clara = REMOVER_JUGADOR → se remueve de inmediato
    (ver regla de remoción abajo).

2c. Decisión EMPATADA → por defecto se espera 30 segundos.
    - Al agotarse, se vuelve a preguntar.
    - Si esta vez hay decisión clara → se aplica (2a o 2b).
    - Si vuelve a quedar empatada → se remueve directamente,
      mensaje "el jugador se quedó sin conexión"
      (ver regla de remoción abajo).
```

**Regla de remoción:** el jugador queda `REMOVED` **a nivel de esa ronda únicamente** (sección 29 — sigue perteneciendo a la partida y puede volver en la siguiente ronda). Se convierte en `automaticLoserCandidate`; si todavía queda algún loser slot disponible en esa ronda, ocupa uno; si los slots ya están todos ocupados, **no recibe loser slot ni genera deuda adicional** (sección 30 — así nunca se supera el número exacto de perdedores).

---

# 18. POLÍTICA DE ESPERA (ARQUITECTURA)

No duplicar lógica de espera en cada servicio. Separar dos capas:

```text
TimeoutPolicy  → solo calcula si un plazo ya venció (15s, 1min, 30s)

TurnWaitingService      → qué hacer si vence el turno de revelar
VotingWaitingService    → qué hacer si vence el turno de votar
HostAvailabilityService → qué hacer si el host no responde
StandByService          → maneja la cola y la reincorporación
```

`TimeoutPolicy` es puramente temporal y reutilizable; cada servicio de dominio decide la transición de estado correspondiente — no se debe fusionar todo en un único "WaitingService" que conozca todos los casos del juego.

Valores configurables:

```text
ACTION_TIMEOUT = 15 segundos
STANDBY_WAIT = 1 minuto
STANDBY_TIE_WAIT = 30 segundos
```

---

# 19. REVELACIÓN DE LA TAPA

```text
Jugador toma fotografía (obligatoria por defecto)
        ↓
Análisis local de imagen (OCR/IA)
        ↓
Sistema propone número
        ↓
Jugador confirma o corrige manualmente
        ↓
Backend registra el RevealAttempt
        ↓
Los demás jugadores elegibles votan (en paralelo, sección 22)
```

---

# 20. OCR / IA

El análisis de imagen (ML Kit, TensorFlow Lite, Tesseract, u otra solución local) es únicamente un asistente, nunca la autoridad. El jugador siempre puede corregir el número manualmente.

---

# 21. FOTO E INGRESO MANUAL

La foto es **obligatoria por defecto** en cada intento. El ingreso manual del número (0-99) está siempre disponible junto al resultado del OCR para corregirlo — pero sigue requiriendo la foto adjunta. Omitir la foto por completo (ingreso 100% manual sin foto) es una excepción únicamente para cuando falla la cámara del dispositivo, y debe quedar marcada como tal en el `RevealAttempt` (`photoOmittedReason`).

---

# 22. MODELO DE VOTACIÓN: REVELAR ES SECUENCIAL, VOTAR ES PARALELO

**Quién revela** sigue el `turnOrder` secuencial de la ronda (uno a la vez). **Quién vota** sobre un `RevealAttempt` ya enviado lo hace **en paralelo** — todos los votantes elegibles reciben el intento al mismo tiempo y pueden votar en cualquier orden, sin esperarse entre sí.

Ejemplo con desconexión durante la votación:

```text
A revela 17 (intento 1)
B → YES
D → YES
C se desconecta → timeout de 15s → C pasa a STAND_BY
   → C queda excluido de ESTA votación (no cuenta para unanimidad)
Resultado: B=YES, D=YES → APPROVED (sin esperar a C)
```

Si C regresa después, no vota retroactivamente ese intento ya resuelto (sección 16).

Votantes elegibles = jugadores `ACTIVE` de esa ronda, excluyendo `SPECTATOR`, `STAND_BY` y al propio revelador.

---

# 23. VOTACIÓN

Opciones: `SÍ` / `NO`. Se requiere unanimidad entre los votantes elegibles (sección 22) para `APPROVED`. Un solo `NO` produce `REJECTED` para ese intento.

---

# 24. RECHAZO Y LÍMITE DE INTENTOS — REVEAL Y REVEALATTEMPT

Se separan dos entidades para modelar correctamente los reintentos:

```text
Reveal
- id, roundId, playerId, status, officialNumber, approvedAt

RevealAttempt
- id, revealId, attemptNumber, proposedNumber, photoReference,
  photoOmittedReason (nullable), status, createdAt

Vote
- id, revealAttemptId, voterId, decision, createdAt
```

```text
Reveal
 ├── RevealAttempt 1 → Vote, Vote, Vote
 ├── RevealAttempt 2 → Vote, Vote
 └── RevealAttempt 3 → Vote, Vote
```

Esto evita que los votos de un intento se mezclen con los de otro — cada `Vote` referencia un `revealAttemptId` concreto, nunca el `Reveal` general.

`MAX_REVEAL_ATTEMPTS = 3`. Si el intento 3 también es `REJECTED` por votación, pasa a arbitraje del host (sección 25) en vez de permitir un cuarto intento.

---

# 25. ARBITRAJE DEL HOST EN EL 3ER RECHAZO

**Delegación del arbitraje:** si el host es precisamente el jugador que reveló (conflicto de interés) o el host no está `ACTIVE` en ese momento, el arbitraje pasa al jugador `ACTIVE` más antiguo de la partida (mismo criterio que el cambio de host normal, sección 9 — ordenado alfabéticamente por nickname).

El árbitro revisa la foto y el número propuesto y decide:

```text
APPROVED → el RevealAttempt queda oficial, Reveal.officialNumber = ese número
REJECTED definitivamente → el Reveal pasa a estado REVEAL_INVALID
```

**Qué pasa con `REVEAL_INVALID`:** el jugador no se queda sin resultado utilizable por el motor. Se marca como `automaticLoserCandidate` (igual que un removido por desconexión, sección 17) y pasa por la misma `LoserSlotResolver`: ocupa un loser slot si todavía queda alguno disponible en la ronda; si los slots ya están completos, no genera un perdedor adicional ni deuda extra (sección 30).

---

# 26. FOTOGRAFÍA — ALMACENAMIENTO Y RETENCIÓN

Bucket compatible con S3 (MinIO en beta, S3/R2 en producción):

```text
reveals/{gameId}/{roundId}/{playerId}-{attemptNumber}.jpg   (temporales)
saved-caps/{userId}/{1|2}.jpg                                (permanentes, máx. 2)
```

**Retención corregida:** las fotos de `reveals/*` **no se borran inmediatamente al quedar `APPROVED`** — se conservan hasta que la `Round` completa pasa a `FINISHED`, para que un jugador que se reconecta o un espectador que consulta `GET /games/{id}/state` siga viéndolas mientras la ronda está activa. Al terminar la ronda, se eliminan. Como respaldo, configurar expiración automática del bucket (ej. 48h).

`saved-caps/*` no expira; solo se borra o reemplaza cuando el usuario lo pide explícitamente (máximo 2 por perfil).

Subida recomendada: URL prefirmada (`presigned PUT`) — el cliente sube directo al bucket, el backend solo guarda la referencia (`photoReference`) en `RevealAttempt`.

---

# 27. COMUNICACIÓN DE VOZ

El MVP incluye llamada de voz entre jugadores de una partida (no espectadores), disponible solo mientras una ronda está en curso (chat de voz también disponible en el lobby entre rondas, según descripción del flujo en sección 11). Sin video por ahora, pero la arquitectura lo permite agregar después.

```java
public interface VoiceProvider {
    VoiceToken generateToken(String gameId, String userId);
}
```

Implementación única para el MVP: `LiveKitVoiceProvider`. `AgoraVoiceProvider` / `TwilioVoiceProvider` quedan como posibles implementaciones futuras de la misma interfaz, no como opciones abiertas durante el desarrollo actual.

El backend solo genera el token de corta duración (`POST /games/{id}/voice-token`); toda la transmisión ocurre en la infraestructura de LiveKit, no en el backend de Tappazo.

---

# 28. MODALIDADES DE JUEGO

```text
ULTIMO_PIERDE        (mínimo 2 jugadores)
PEQUENOS_PAGAN       (mínimo 3 jugadores)   [antes "Los altos ganan"]
ULTIMOS_DOS_PIERDEN  (mínimo 4 jugadores)
```

Patrón Strategy, nunca una cadena de `if(mode == ...)`:

```java
public interface GameModeRule {
    int minimoJugadores();
    GameResult resolve(List<PlayerResult> results);
}
```

Implementaciones: `LastLosesRule`, `SmallNumbersPayRule`, `LastTwoLoseRule`.

---

# 29. REGLA FUNDAMENTAL: NÚMERO EXACTO DE PERDEDORES

Cada modalidad define `TOTAL_LOSER_SLOTS` exacto. Ese número **nunca** aumenta ni disminuye por causa de un empate, una desconexión removida, o un `REVEAL_INVALID` (secciones 17 y 25 ya garantizan que esos casos respetan el tope).

---

# 30. FÓRMULA DE LOSER SLOTS CON REMOCIONES

```text
loserSlotsDisponibles =
    TOTAL_LOSER_SLOTS
    - automaticLosers        (por desconexión removida o REVEAL_INVALID)
    - confirmedLosers        (por resultado normal de la ronda)

loserSlotsDisponibles nunca puede ser menor que cero.

Si loserSlotsDisponibles == 0 y aparece un nuevo candidato
automático (otra desconexión u otro REVEAL_INVALID):
    → ese jugador queda REMOVED / REVEAL_INVALID igualmente
    → pero NO recibe loser slot
    → NO genera deuda adicional
```

Así, el número total de perdedores de la ronda jamás supera `TOTAL_LOSER_SLOTS`, sin importar cuántas desconexiones o revelaciones inválidas ocurran.

---

# 31. EJEMPLO CRÍTICO DE EMPATE

```text
TOTAL_LOSER_SLOTS = 3
Resultados: 10, 20, 20, 20
```

El `10` es perdedor confirmado (`CONFIRMED_LOSERS = 1`). `REMAINING_LOSER_SLOTS = 2`. Los tres `20` compiten únicamente por esos 2 puestos — no se convierten los tres en perdedores.

---

# 32. REGLA GENERAL DE EMPATES

1. Determinar perdedores ya confirmados.
2. Calcular puestos restantes.
3. Identificar solo a los empatados que compiten por esos puestos.
4. Ejecutar desempate (sección 33).
5. Asignar exactamente los puestos restantes.
6. Nunca aumentar el total de perdedores.

Un jugador con loser slot ya confirmado no vuelve a competir por ese puesto.

---

# 33. OPCIONES DE DESEMPATE

Los jugadores empatados eligen entre `NUEVA_RONDA` o `DIVIDIR_CUENTA`. Si todos coinciden, se aplica esa opción. Si hay desacuerdo y existe al menos un jugador **no empatado** disponible, el backend elige aleatoriamente a uno de ellos para decidir de forma definitiva (el azar se ejecuta en backend, nunca en Flutter).

**Caso especial — todos los jugadores de la ronda quedaron empatados** (no hay nadie no-empatado para decidir):

```text
1. Se ejecuta automáticamente NUEVA_RONDA.
2. Mensaje a todos: "Como todos quedaron empatados, jugaremos una
   ronda más. Si vuelven a empatar, la cuenta se dividirá entre todos."
3. Se juega la ronda de desempate.
4. Si vuelve a haber empate total entre los mismos jugadores:
   → se aplica DIVIDIR_CUENTA automáticamente, sin más intentos.
```

**Persistencia del desempate:**

```text
TieBreak
- id, sourceRoundId, tieType, affectedLoserSlots, status,
  resolutionMode, selectedDeciderId, allTiedCase,
  tieBreakRoundId (si se generó NUEVA_RONDA), createdAt, resolvedAt

TieBreakParticipant
- tieBreakId, playerId, decision, decisionAt
```

Esto permite reconstruir cadenas completas: `Ronda → TieBreak → NUEVA_RONDA → nuevo TieBreak → DIVIDIR_CUENTA`.

---

# 34. NUEVA RONDA COMO DESEMPATE: COMPETIDORES VS. PARTICIPANTES FÍSICOS

Cuando se elige `NUEVA_RONDA`, **todos** los jugadores de la ronda original vuelven a tomar una bebida físicamente (son "participantes físicos" / posibles beneficiarios de la deuda), pero **solo los jugadores empatados** compiten para determinar quién ocupa los puestos de perdedor restantes ("competidores del desempate").

```text
5 jugadores: A, B, C empatados; D, E ya confirmados ganadores.
Todos destapan de nuevo: A B C D E.
Compiten por el desempate: solo A, B, C.
D y E no pueden pasar a ser perdedores en este desempate,
pero sí pueden seguir siendo beneficiarios de la deuda final.
```

```text
TieBreakCompetitors ≠ RoundParticipants
```

El resultado final de la ronda (quién gana/pierde en total) se recalcula una vez resuelto el desempate, y la distribución económica se hace sobre ese resultado final usando el principio único de la sección 36 — no hace falta un modelo económico aparte para "nueva ronda".

---

# 35. CUENTA DE NUEVA RONDA

La nueva ronda de desempate se registra como una ronda/cuenta separada (`roundType = TIE_BREAK_ROUND`), distinguible de la ronda original (`NORMAL`), para poder auditar de dónde salió cada movimiento de deuda.

---

# 36. PRINCIPIO ECONÓMICO MAESTRO: QUÉ ES "LA CUENTA"

Para que la resolución normal, `NUEVA_RONDA` y `DIVIDIR_CUENTA` sean siempre equivalentes en el monto total movido, se define una única fórmula:

```text
CUENTA_DE_LA_RONDA = (N_jugadores - TOTAL_LOSER_SLOTS) × drinkPrice
```

Es decir, **la cuenta es siempre el valor de las bebidas de los ganadores finales**, nunca el total de las N bebidas de todos los jugadores. Esto es válido sin importar el camino de resolución:

* Resolución normal (sin empate): se distribuye con el algoritmo de la sección 51 (cada perdedor cubre primero una bebida completa a un ganador, y el resto se reparte entre los perdedores).
* `DIVIDIR_CUENTA` (sección 37): el mismo monto total se reparte usando porcentajes en vez del algoritmo de la sección 51.

Ejemplo de verificación (5 jugadores, 1 perdedor, $4.000 la bebida): `CUENTA = (5-1) × 4000 = 16.000` — exactamente lo que el perdedor paga repartido entre los 4 ganadores, nunca $20.000 (el total de las 5 bebidas).

---

# 37. DIVIDIR CUENTA — FÓRMULA DEFINITIVA DE PORCENTAJES

```text
CUENTA = (N - TOTAL_LOSER_SLOTS) × drinkPrice     (sección 36)

Para cada perdedor confirmado NO empatado:
    porcentaje = 1 / TOTAL_LOSER_SLOTS

Para cada candidato empatado que compite por los puestos restantes:
    porcentaje = REMAINING_LOSER_SLOTS / (TOTAL_LOSER_SLOTS × cantidadEmpatados)

monto_del_jugador = porcentaje × CUENTA
```

**Verificación con el ejemplo de referencia:**

```text
TOTAL_LOSER_SLOTS = 3
Resultados: 10 (confirmado), 20, 20, 20 (empatados)

confirmado (10):  porcentaje = 1/3 = 33,33%
cada empatado:    porcentaje = (3-1)/(3×3) = 2/9 = 22,22%

Suma: 33,33% + 3×22,22% = 33,33% + 66,66% = 100% ✓
```

Esta fórmula reemplaza cualquier noción anterior de "porciones discretas" — el porcentaje sale directamente del tamaño del grupo empatado y los slots restantes, sin ambigüedad.

---

# 38. MODELO 1 — ÚLTIMO PIERDE

Mínimo: 2 jugadores. Loser slots: `1`. El número más bajo pierde.

```text
25, 17, 30, 40 → 17 pierde
```

---

# 39. EMPATE EN ÚLTIMO PIERDE

```text
10, 10, 20, 30
```

Dos empatados compiten por el único loser slot (sección 32-33 resuelven cuál).

---

# 40. MODELO 2 — PEQUEÑOS PAGAN (antes "Los altos ganan")

Mínimo: 3 jugadores. Loser slots: `floor(N / 2)`.

| Jugadores | Perdedores | Ganadores |
| --------: | ---------: | --------: |
|         3 |          1 |         2 |
|         4 |          2 |         2 |
|         5 |          2 |         3 |
|         6 |          3 |         3 |
|         7 |          3 |         4 |
|         8 |          4 |         4 |
|         9 |          4 |         5 |
|        10 |          5 |         5 |
|        11 |          5 |         6 |

Los números más bajos ("los pequeños") ocupan los puestos de perdedor y pagan.

---

# 41. EJEMPLO — 11 JUGADORES

```text
11 jugadores → 5 perdedores, 6 ganadores. Nunca 4 ni 6 perdedores.
```

---

# 42. MODELO 3 — ÚLTIMOS DOS PIERDEN

Mínimo: **4 jugadores** (ajustado para garantizar `W ≥ L`). Loser slots: `2` fijos. Los dos números más bajos pierden.

```text
10, 10, 20, 30 → ambos 10 pierden, sin desempate adicional.
10, 20, 20, 30 → 10 confirmado, los dos 20 compiten por 1 puesto.
```

---

# 43. REGLA DE EMPATE CUANDO YA ESTÁN LLENOS LOS PUESTOS

```text
3 loser slots, resultados 10, 20, 20
```

Si esos tres ya ocupan exactamente los tres puestos, no hay desempate aunque los dos `20` sean iguales entre sí.

---

# 44. MOTOR DE RESOLUCIÓN

```text
LoserSlotResolver
```

Responsabilidades: recibir resultados, consultar loser slots totales, ordenar números, confirmar perdedores inequívocos, detectar empate en el límite, calcular puestos restantes, invocar el desempate, e **integrar los `automaticLoser` de desconexión/`REVEAL_INVALID`** (sección 30) para garantizar que el total final sea exactamente `TOTAL_LOSER_SLOTS`, nunca más.

---

# 45. MOTOR DE MODOS

```java
public interface GameModeRule {
    int minimoJugadores();
    GameResult resolve(List<PlayerResult> results);
}
```

Implementaciones: `LastLosesRule`, `SmallNumbersPayRule`, `LastTwoLoseRule`. El modo es lógica pura de dominio, sin conocer HTTP, WebSocket, JPA ni MySQL.

---

# 46. REGLAS ECONÓMICAS GENERALES

Dinero en enteros (`Long`/`Integer`), nunca `double`/`float`. Moneda: COP. Divisiones truncan decimales (`4000/3 = 1333`); el sobrante no se redistribuye.

---

# 47. DISTRIBUCIÓN ECONÓMICA — ÚLTIMO PIERDE

```text
5 jugadores, drinkPrice = 4.000, 1 perdedor, 4 ganadores
Perdedor → cada ganador = 4.000 → total 16.000
```

---

# 48. DISTRIBUCIÓN ECONÓMICA — PEQUEÑOS PAGAN

```text
L = floor(N/2), W = N - L
Cada perdedor cubre una bebida completa a un ganador.
Ganadores restantes (W - L) se reparten entre los L perdedores:
additionalPerLoser = ((W - L) × drinkPrice) / L
totalPerLoser = drinkPrice + additionalPerLoser
```

**11 jugadores:** `L=5, W=6`. `additionalPerLoser = (1×4000)/5 = 800`. `totalPerLoser = 4800`. Total pagado = `5×4800=24.000` = total recibido `6×4000=24.000`. Cuadra con el principio de la sección 36: `CUENTA = (11-5)×4000 = 24.000` ✓.

---

# 49. DISTRIBUCIÓN GENERAL DE MÚLTIPLES PERDEDORES

1. Asignar una bebida completa a cada perdedor para cubrir un ganador.
2. Identificar ganadores restantes.
3. Dividir las bebidas restantes entre los perdedores (división entera).
4. Ignorar el sobrante.

```text
3 perdedores, 5 ganadores, drinkPrice=4.000:
3 ganadores cubiertos directamente.
Quedan 2 ganadores → 8.000 a repartir entre 3 perdedores → 2.666 c/u.
Cada perdedor paga 4.000 + 2.666 = 6.666. Sobrante ignorado.
```

---

# 50. MOVIMIENTOS DE DEUDA Y ALCANCE DEL BALANCE

```text
DebtMovement
- id, gameId, roundId, fromPlayerId, toPlayerId, amount, reason, createdAt
```

Razones: `ROUND_LOSS`, `TIE_BREAK`, `TIE_BREAK_ROUND`, `STAND_BY_REMOVAL`, `REVEAL_INVALID`.

**Alcance:** cada `Game` tiene su propio balance independiente; las rondas de esa partida suman solo a ese balance. Al terminar la partida, el balance queda cerrado como historial; la siguiente partida empieza en cero. `DebtNettingService` opera con alcance `gameId`.

**Estado de pago:** por ahora no existe campo `PENDING`/`SETTLED` — se confía en que los jugadores se paguen fuera de la app (mejora futura documentada, no bloqueante).

---

# 51. ESTADÍSTICAS: BRUTAS VS. NETAS

```text
Statistics (brutas, para moneySpent/moneyReceived):
Carlos → Nicolas = 4000 ⇒ Carlos.moneySpent += 4000
Nicolas → Carlos = 4000 ⇒ Nicolas.moneySpent += 4000
(cada uno también acumula moneyReceived += 4000)

CurrentBalance (neto, para saber quién le debe a quién):
Carlos ↔ Nicolas = 0 (se cancelan)
```

`Statistics` y `CurrentBalance` son dos conceptos distintos y ambos deben existir.

---

# 52. NETEO DE DEUDAS

```text
saldo(A → B) = deuda(A → B) - deuda(B → A), dentro de un mismo gameId.
saldo > 0 → A debe pagar a B. saldo < 0 → B debe pagar a A. saldo = 0 → nada.
```

---

# 53. NO EXISTE "GANADOR DE PARTIDA"

Una partida tiene varias rondas, y un jugador puede perder en una y ganar en la siguiente. No hay condición de victoria para el `Game` completo — solo balance acumulado de esa partida. Por tanto, `gamesWon`/`gamesLost`/`ties` **no entran en el alcance del MVP**. Las estadísticas válidas desde ya son: `roundsPlayed`, `roundsWon`, `roundsLost`, y el balance acumulado por partida (gana = le pagan, pierde = debe pagar).

---

# 54. HISTORIAL

Detalle profundo (fotos, votos, reintentos) se guarda localmente en el dispositivo (SQLite/Hive/Isar), no en el backend. El backend expone solo un resumen agregado vía `GET /users/me/history`:

```text
gameId, fecha, jugadores participantes, modo(s) por ronda,
resultado por ronda, dinero ganado/gastado en esa partida,
fecha de ingreso del usuario a la partida
```

---

# 55. ENTIDADES PRINCIPALES

```text
User, Game, GameParticipant, Round, RoundParticipant,
Reveal, RevealAttempt, Vote, TieBreak, TieBreakParticipant,
DebtMovement, PlayerStatistics
```

---

# 56. GAME

```text
Game
- id, code, hostId, state (LOBBY|IN_PROGRESS|FINISHED),
  maxPlayers, currentDrinkPrice, currentMode, version, createdAt, updatedAt
```

`version` para optimistic locking (sección 70).

---

# 57. GAME PARTICIPANT

```text
GameParticipant
- id, gameId, userId, role (PLAYER|SPECTATOR),
  participantState (ACTIVE|STAND_BY|REMOVED), joinedAt
```

---

# 58. ROUND

```text
Round
- id, gameId, mode, state, drinkPrice, startedAt, finishedAt,
  roundType (NORMAL|TIE_BREAK_ROUND), version
```

---

# 59. ROUND PARTICIPANT

```text
RoundParticipant
- id, roundId, userId, nicknameSnapshot, turnOrder, state,
  isConfirmedLoser, isWinner,
  automaticLoserCandidate (boolean),
  removedByDisconnection (boolean),
  revealInvalid (boolean)
```

`turnOrder` y `nicknameSnapshot` quedan congelados al iniciar la ronda. `REMOVED` aquí es de ronda, no de partida (sección 13) — el jugador sigue en `GameParticipant` y puede volver en la siguiente ronda.

---

# 60. REVEAL, REVEALATTEMPT Y VOTE

```text
Reveal
- id, roundId, playerId, status (PENDING|APPROVED|REVEAL_INVALID),
  officialNumber, approvedAt

RevealAttempt
- id, revealId, attemptNumber, proposedNumber, photoReference,
  photoOmittedReason, status (PENDING|APPROVED|REJECTED), createdAt

Vote
- id, revealAttemptId, voterId, decision (YES|NO), createdAt
```

Constraint: `voterId != reveal.playerId`. `UNIQUE(revealAttemptId, voterId)` (sección 70).

---

# 61. TIEBREAK Y TIEBREAKPARTICIPANT

```text
TieBreak
- id, sourceRoundId, tieType, affectedLoserSlots, status,
  resolutionMode, selectedDeciderId, allTiedCase,
  tieBreakRoundId, createdAt, resolvedAt, version

TieBreakParticipant
- tieBreakId, playerId, decision, decisionAt
```

---

# 62. ARQUITECTURA CLEAN

```text
domain        → entidades, value objects, enums, reglas, servicios de dominio
              (sin depender de Spring/JPA/HTTP/WebSocket/MySQL)
application   → casos de uso (CreateGameUseCase, StartRoundUseCase,
                SubmitRevealUseCase, VoteRevealUseCase, HostArbitrateRevealUseCase,
                ResolveTieBreakUseCase, FinishRoundUseCase, FinishGameUseCase,
                CalculateBalancesUseCase, LeaveGameUseCase, StandByDecisionUseCase)
infrastructure → JPA, repositories, MySQL, JWT, Google auth,
                 WebSocket (con HandshakeInterceptor), cliente S3/MinIO,
                 LiveKitVoiceProvider
presentation  → REST controllers, WebSocket handlers, DTOs, mappers, exception handlers
```

---

# 63. SOLID

Single Responsibility, Open/Closed, Dependency Inversion. Evitar un `GameService` gigante que mezcle partidas, votaciones, desempates, dinero, conexiones y estadísticas — separar en servicios de dominio dedicados.

---

# 64. MOTOR ECONÓMICO

```text
DebtDistributionService → genera los DebtMovement (sección 49, usando CUENTA de sección 36)
DebtNettingService      → calcula saldos netos, alcance gameId
LoserPortionCalculator  → aplica la fórmula de porcentajes de la sección 37
```

---

# 65. MOTOR DE DESEMPATE

```text
TieBreakResolver
```

Detecta empate relevante, determina loser slots restantes, recibe la decisión, resuelve `NUEVA_RONDA` o `DIVIDIR_CUENTA`, maneja el caso "todos empatados" (sección 33) y garantiza que los puestos restantes queden exactamente ocupados.

---

# 66. CONCURRENCIA E IDEMPOTENCIA

```text
Constraints únicos:
  UNIQUE(revealAttemptId, voterId)   -- un voto por jugador por intento
  UNIQUE(gameId, userId)             -- no duplicar participante

Optimistic locking (@Version) en: Game, Round, Reveal, TieBreak

Transaccional (atómico): StartRound, SubmitReveal, Vote,
  ResolveTieBreak, FinishRound, JoinGame, HostArbitration

Idempotencia (Idempotency-Key o commandId) requerida en:
  POST /rounds/{roundId}/reveal
  POST /rounds/{roundId}/reveals/{attemptId}/vote
  POST /games/{id}/rounds/{roundId}/start
```

Esto evita dobles inicios de ronda, votos duplicados por reintentos de red, y condiciones de carrera al unirse con el último cupo disponible.

---

# 67. API REST

```http
POST   /auth/google
POST   /games
POST   /games/{code}/join
GET    /games/{id}
GET    /games/{id}/state
PATCH  /games/{id}/beer-price
POST   /games/{id}/leave
POST   /games/{id}/finish
POST   /games/{id}/voice-token
POST   /games/{id}/rounds
POST   /games/{id}/rounds/{roundId}/start
POST   /rounds/{roundId}/reveal
POST   /rounds/{roundId}/reveals/{revealId}/attempts
POST   /rounds/{roundId}/reveals/{revealId}/vote
POST   /rounds/{roundId}/reveals/{revealId}/host-arbitration
POST   /games/{id}/standby-decision
POST   /tie-breaks/{id}/decision
POST   /tie-breaks/{id}/decider-decision
GET    /users/me/profile
PATCH  /users/me/profile
GET    /users/me/stats
GET    /users/me/balances
GET    /users/me/history
POST   /users/me/saved-caps
DELETE /users/me/saved-caps/{id}
```

Conceptuales — ajustables en implementación sin cambiar las reglas de negocio.

---

# 68. WEBSOCKET

**Autenticación:** mismo JWT de Tappazo, validado en el `HandshakeInterceptor` antes de aceptar la conexión y asociarla a `gameId`/`userId`.

Eventos:

```text
PLAYER_CONNECTED, PLAYER_DISCONNECTED, PLAYER_JOINED, PLAYER_LEFT
GAME_LOBBY, GAME_IN_PROGRESS, GAME_FINISHED
ROUND_STARTED, TURN_CHANGED
REVEAL_PROPOSED, VOTE_REGISTERED, REVEAL_APPROVED, REVEAL_REJECTED,
REVEAL_HOST_ARBITRATION_REQUIRED, REVEAL_INVALID
PLAYER_STAND_BY, PLAYER_RECONNECTED, STANDBY_DECISION_REQUIRED,
STANDBY_DECISION_RESULT, PLAYER_REMOVED_DISCONNECTED
HOST_CHANGED
TIE_BREAK_STARTED, TIE_BREAK_ALL_TIED_WARNING,
TIE_BREAK_DECISION_REQUIRED, TIE_BREAK_RESOLVED
ROUND_FINISHED
```

---

# 69. BACKEND COMO AUTORIDAD

El frontend nunca decide perdedores, ganadores, votos válidos, turnos, timeouts, host, desempates ni dinero — todo viene del backend ya resuelto.

---

# 70. FLUJO COMPLETO DE UNA RONDA

```text
LOBBY (Game)
   ↓ host elige modo (valida mínimo de jugadores, sección 12)
   ↓ host inicia ronda → Game = IN_PROGRESS, congela jugadores/turnOrder/drinkPrice
REVEALING (secuencial por turnOrder)
   ↓ jugador revela (foto + número)
VALIDATING (votación en paralelo, hasta 3 intentos, luego arbitraje de host)
   ↓ todos revelaron
RESOLVING
   ↓ aplica GameModeRule → ¿empate relevante?
   ├─ NO → resultado final
   └─ SÍ → TIE_BREAK (maneja caso "todos empatados")
            → NUEVA_RONDA o DIVIDIR_CUENTA
            → resuelve loser slots exactos (incluye automaticLosers)
   ↓ calcula obligaciones (CUENTA = (N-L)×drinkPrice, sección 36)
FINISHED (Round) → Game vuelve a LOBBY
   ↓ guarda estadísticas, actualiza balance de esta partida
   ↓ espectadores que se unieron durante la ronda pasan a PLAYER
```

---

# 71. ORDEN DE IMPLEMENTACIÓN

```text
Fase 1 — Dominio: GameMode, GameResult, PlayerResult, LoserSlotResolver,
         TieBreakResolver, DebtDistributionService, DebtNettingService,
         LoserPortionCalculator, TimeoutPolicy + servicios de espera
Fase 2 — Motor de resultados: LastLosesRule, SmallNumbersPayRule, LastTwoLoseRule
Fase 3 — Motor económico completo
Fase 4 — Tests de dominio (sin Spring/MySQL)
Fase 5 — Persistencia: JPA, Hibernate, MySQL, Repositories, @Version
Fase 6 — Casos de uso (application layer)
Fase 7 — REST
Fase 8 — WebSocket (con auth JWT en handshake)
Fase 9 — Seguridad: Google ID Token, JWT, Spring Security
Fase 10 — Infraestructura: Docker (backend+MySQL+MinIO), LiveKitVoiceProvider
Fase 11 — Flutter: REST, WebSocket, SDK de voz
```

---

# 72. PRUEBAS UNITARIAS OBLIGATORIAS

```text
Mínimos: 2 jug. en ULTIMO_PIERDE → puede iniciar.
         3 jug. en ULTIMOS_DOS_PIERDEN → NO puede iniciar (mínimo 4).
         1 jugador → nunca puede iniciar.

Último pierde: [25,17,25,25] → 17 pierde.
Últimos 2 (4+ jugadores): [10,10,20,30] → ambos 10 pierden, sin desempate.

Empate: [10,20,20,30] en ULTIMOS_DOS_PIERDEN →
        10 confirmado, un 20 compite por el puesto restante.

Loser slots críticos: TOTAL=3, [10,20,20,20] →
        1 fijo + 2 de 3 compiten. Nunca 4 perdedores.

Remociones vs. loser slots: TOTAL=1 (ULTIMO_PIERDE), A y B se
desconectan y se remueven → solo el primero ocupa el loser slot;
el segundo queda REMOVED sin generar deuda adicional.

Economía 11 jugadores (PEQUENOS_PAGAN): L=5, W=6, drinkPrice=4000 →
        cada perdedor paga 4800, total 24.000 = CUENTA=(11-5)×4000.

División entera: 4000/3 = 1333, nunca 1333.33.

Neteo: Carlos→Nicolas=4000, Nicolas→Carlos=4000 → saldo 0, dentro del mismo gameId.

Dividir cuenta — porcentajes: TOTAL=3, [10 confirmado, 20,20,20 empatados] →
        10 = 33,33%, cada 20 = 22,22%, suma = 100%.

Votación: revelador no vota su propia tapa; todos Sí → APPROVED;
          un solo No → REJECTED; 3er rechazo → arbitraje de host,
          no exige 4to intento; host afectado/no-ACTIVE → arbitraje
          delega al jugador ACTIVE más antiguo.

Votación paralela + desconexión: C se desconecta durante la
votación → se excluye de esa votación, B+D deciden sin esperarlo;
si C regresa después, no vota retroactivamente ese intento.

Conexión: desconexión temporal que sí completa su acción = NO
eliminar. Turno bloqueado 15s sin regresar = STAND_BY.

Reincorporación: STAND_BY → reconexión → ACTIVE, turno al final de la cola.

STAND_BY con decisión empatada dos veces: primera pregunta empatada
→ espera 30s por defecto → se pregunta de nuevo → si vuelve a
empatar, se remueve con mensaje "el jugador se quedó sin conexión"
y queda como loser slot automático (si hay disponible).

Espectador: no puede votar, revelar, ni modificar reglas; se
promueve a PLAYER automáticamente al terminar la ronda en la que
se unió.

Precio: modificable solo con Game en LOBBY; congelado durante la ronda.

Desempate por desacuerdo: selección aleatoria de un jugador no
empatado, en backend. Caso todos empatados: NUEVA_RONDA automática
con aviso, segundo empate total → DIVIDIR_CUENTA forzado.

Host: desconexión y regreso a tiempo = sigue como host; cambio
definitivo = primer jugador ACTIVE por orden alfabético.

Idempotencia: dos POST /start simultáneos → una sola transición
válida. Doble voto del mismo jugador al mismo intento → rechazado
por constraint único.
```

---

# 73. REGLAS QUE NO DEBEN VIOLARSE

1. El número de perdedores siempre es exacto (incluye remociones y `REVEAL_INVALID`, nunca lo supera — sección 30).
2. Un empate nunca crea perdedores adicionales.
3. Los loser slots son la unidad fundamental de resolución.
4. Un jugador con loser slot confirmado no vuelve a competir por ese puesto.
5. Un desempate solo resuelve los loser slots restantes.
6. El revelador no vota su propia tapa.
7. Un solo voto NO rechaza la revelación, salvo que sea el 3er intento (pasa a arbitraje de host).
8. El frontend no es autoridad; el backend determina todos los resultados.
9. El precio queda congelado durante la ronda y solo es editable con `Game == LOBBY`.
10. El dinero se maneja con enteros; las divisiones truncan sin redistribuir sobrante.
11. Tappazo no mueve dinero real.
12. Las deudas se registran como movimientos, con alcance `gameId` (nunca global entre partidas).
13. Una desconexión temporal no implica eliminación.
14. Los timeouts están centralizados en `TimeoutPolicy`, pero la transición de dominio resultante es responsabilidad de cada servicio (turno, votación, host, stand-by), no de un único servicio genérico.
15. Los espectadores no pueden intervenir en una ronda en curso, pero se promueven automáticamente a jugador en la siguiente ronda.
16. El nickname no se modifica desde una partida; el historial usa `nicknameSnapshot`.
17. Cada modalidad exige su propio mínimo de jugadores (`ULTIMO_PIERDE`=2, `PEQUENOS_PAGAN`=3, `ULTIMOS_DOS_PIERDEN`=4); el host no puede iniciar sin cumplirlo.
18. "La cuenta" de una ronda es siempre `(N - TOTAL_LOSER_SLOTS) × drinkPrice`, sin importar el método de resolución.
19. El WebSocket se autentica con el mismo JWT de Tappazo.
20. No existe condición de "ganador de partida"; las estadísticas de partida completa se basan en balance acumulado, no en victorias/derrotas de `Game`.
21. Operaciones críticas (`StartRound`, `Vote`, `Reveal`, `HostArbitration`, `TieBreakDecision`, `JoinGame`) son transaccionales, con constraints únicos y, donde aplica, idempotencia.

---

# 74. PRINCIPIO ARQUITECTÓNICO FINAL

```text
DOMAIN FIRST, no UI FIRST.

Reglas de negocio → Dominio → Casos de uso → Persistencia →
REST/WebSocket → Seguridad → Flutter
```

Toda la lógica de Tappazo debe poder probarse con pruebas unitarias sin levantar Spring Boot, MySQL, WebSocket ni Flutter — garantizando que las reglas del juego sean correctas antes de añadir complejidad tecnológica.
