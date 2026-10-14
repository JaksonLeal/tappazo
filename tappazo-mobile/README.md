# Tappazo Mobile Client

Cliente móvil de Tappazo desarrollado con Flutter bajo los principios de **Clean Architecture**, comunicación REST/Dio y Riverpod.

## Fuente de Verdad Visual (Design Tokens)

La apariencia visual de la app se rige estrictamente por los tokens definidos en `design/design_tokens.yaml`:
- **Colores:** `lib/presentation/theme/colors.dart` (Fondo Dark `#12131A`, Ámbar `#FFB300`, Lima `#BEF500`, Superficies `#1E1F26`).
- **Tipografía:** `lib/presentation/theme/typography.dart` (Familia `Outfit`, display gigante de tapas, precios y jerarquía).
- **Espaciado y Radios:** `lib/presentation/theme/spacing.dart` (Márgenes de 20px, radio de tarjetas de 12px, botones primarios de 56px).

## Arquitectura del Proyecto

```
lib/
├── core/
│   ├── constants/            # Constantes de red, URLs
│   ├── network/              # Cliente Dio con interceptor Bearer JWT
│   ├── storage/              # FlutterSecureStorage para tokens y datos de sesión
│   └── utils/                # Formateador de moneda colombiana COP ($#.##0 COP)
├── domain/
│   ├── entities/             # Entidades puras (User, Game, GameParticipant, Mesa, NetDebt, DebtMovement)
│   ├── repositories/         # Contratos e interfaces (AuthRepository, GameRepository, MesaRepository)
│   └── usecases/             # Casos de uso de negocio con validación
├── data/
│   ├── models/               # Modelos DTO con serialización JSON según endpoints Spring Boot
│   ├── datasources/          # Fuentes remotas (Dio REST) y locales (SecureStorage)
│   └── repositories/         # Implementaciones de repositorios
└── presentation/
    ├── theme/                # Tokens generados: colors, typography, spacing y app_theme
    ├── providers/            # Riverpod StateNotifiers (Auth, Game, Mesa)
    ├── screens/
    │   ├── splash/           # Verificación inicial de sesión
    │   ├── auth/             # Login con Google y bypass de prueba local
    │   ├── home/             # Dashboard con modal de Crear Partida y Unirse con Código
    │   ├── lobby/            # Sala de espera, código, participantes ordenados y ajustes host
    │   └── mesa/             # Resumen de mesa, saldos netos y movimientos brutos de deuda
    └── widgets/              # Componentes de UI (TappazoButton, GoogleSignInButton)
```

## Flujos y Módulos Implementados

### 1. Autenticación (Sección 3)
- Google Sign-In enviando el ID Token a `POST /api/v1/auth/google`.
- Almacenamiento seguro del JWT de Tappazo en `FlutterSecureStorage`.
- Inyección automática del header `Authorization: Bearer <tappazo-jwt>` en peticiones protegidas.
- Modo local para desarrollo rápido con `POST /api/v1/auth/local`.

### 2. Partidas y Lobby (Secciones 5, 6, 7, 8, 9, 10, 11 y 67)
- **Creación:** `POST /api/v1/games` con precio de bebida, modalidad y capacidad (2–10).
- **Unirse:** `POST /api/v1/games/join` mediante código alfanumérico.
- **Detalles y participantes:** `GET /api/v1/games/{id}`, ordenando participantes alfabéticamente por nickname.
- **Ajustes:** `PATCH /api/v1/games/{id}/settings` exclusivo para el anfitrión mientras la partida está en `LOBBY`.

### 3. Mesa y Resumen de Deudas (Secciones 50, 51 y 52)
- **Estado de Mesa:** `GET /api/v1/mesas/{gameId}`.
- **Saldos Netos:** `GET /api/v1/mesas/{gameId}/balances` con deuda total activa y consolidación quién le debe a quién.
- **Movimientos Brutos:** `GET /api/v1/mesas/{gameId}/debts` con el historial de pérdidas de ronda.

## Ejecución de Pruebas

```powershell
flutter test
flutter analyze
```
