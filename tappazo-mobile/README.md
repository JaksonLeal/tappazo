# Tappazo Mobile Client

Cliente móvil de Tappazo desarrollado con Flutter bajo los principios de **Clean Architecture**, comunicación REST/Dio y Riverpod.

## Arquitectura del Proyecto

```
lib/
├── core/
│   ├── constants/            # Constantes de red, URLs y colores
│   ├── network/              # Cliente Dio con interceptor Bearer JWT
│   ├── storage/              # FlutterSecureStorage para tokens y datos de sesión
│   └── theme/                # Tema oscuro moderno con paleta cervecera/social
├── domain/
│   ├── entities/             # Entidades de dominio puro (User, AuthSession)
│   ├── repositories/         # Contratos e interfaces de repositorio
│   └── usecases/             # Casos de uso (Login Google, Login Local, Check Auth, Logout)
├── data/
│   ├── models/               # Modelos DTO con serialización JSON (Backend spec)
│   ├── datasources/          # Fuente remota (API REST) y local (Secure Storage)
│   └── repositories/         # Implementación de AuthRepository con GoogleSignIn
└── presentation/
    ├── providers/            # Riverpod StateNotifier e Inyección de Dependencias
    ├── screens/
    │   ├── splash/           # Splash screen con verificación automática de token
    │   ├── auth/             # Login con Google y bypass de desarrollo local
    │   └── home/             # Home con datos de sesión activa y acciones
    └── widgets/              # Componentes visuales reutilizables
```

## Flujo de Autenticación

1. **Google Login:** El usuario selecciona su cuenta en el widget `GoogleSignInButton`. Se obtiene el `idToken`.
2. **Backend Handshake:** Se envía el `idToken` a `POST /api/v1/auth/google`.
3. **JWT Storage:** El backend responde con el JWT de Tappazo (`token`, `userId`, `nickname`, `email`), el cual se almacena de forma segura en `FlutterSecureStorage`.
4. **Bearer Interceptor:** Todas las peticiones posteriores protegidas inyectan automáticamente el header:
   ```http
   Authorization: Bearer <tappazo-jwt>
   ```
5. **Modo Desarrollo Local:** Para pruebas rápidas contra `run-local.ps1` del backend sin configurar credenciales de Google OAuth en consola de Google Cloud, la app incluye un botón "Probar sin Google (Backend Local)" que consume `POST /api/v1/auth/local`.

## Configuración de Red

Por defecto la app apunta a:
- Android Emulator: `http://10.0.2.2:8080/api/v1`
- iOS / Web / Desktop: `http://localhost:8080/api/v1`

Para apuntar a una IP de red local o servidor remoto:
```bash
flutter run --dart-define=API_BASE_URL=http://192.168.1.100:8080/api/v1
```

