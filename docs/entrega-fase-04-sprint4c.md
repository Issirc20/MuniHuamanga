# Entrega Técnica — Fase 04 / Sprint 4-C: Autenticación JWT y Spring Security 6

## Metadatos

| Campo            | Detalle                                                              |
|------------------|----------------------------------------------------------------------|
| **Fase**         | 04 — PDF Licencia Oficial + Email + JWT + CRUD TUPA                  |
| **Sprint**       | Sprint 4-C                                                           |
| **Fecha**        | 2026-10-02                                                           |
| **Responsable**  | Equipo de Desarrollo — MuniHuamanga Licencias                        |
| **Build**        | `mvn test` → **BUILD SUCCESS (72 tests totales / 59 en servicio-expedientes)** |
| **Arquitectura** | Spring Security 6.3 + JJWT 0.12.x + Control de Acceso RBAC           |

---

## 1. Objetivo del Sprint 4-C

Implementar el subsistema de **Seguridad y Control de Acceso Basado en Roles (RBAC)** mediante **Spring Security 6** y tokens **JWT (JSON Web Tokens)** stateless para proteger los endpoints administrativos y de evaluación de la Municipalidad Provincial de Huamanga, garantizando que:
1. Las operaciones sensibles de gestión interna (evaluación técnica ITSE, registro de pagos SAT, dictamen, aprobación y rechazo) requieran autenticación y rol autorizado.
2. Los servicios dirigidos a los ciudadanos (registro en Mesa de Partes Virtual, seguimiento de trámite, descarga de formatos PDF y verificación pública mediante código QR) permanezcan accesibles de forma pública y sin fricción.
3. El frontend de Gestión Interna (`portal-interno.html`) integre inicio de sesión con JWT, guardado seguro en cliente, inyección automática de cabeceras `Authorization: Bearer <token>`, detección de expiración (HTTP 401) y denegación de permisos (HTTP 403).

---

## 2. Matriz de Roles y Permisos (RBAC)

Se definieron tres roles funcionales institucionales en el enum [`RolUsuario.java`](file:///d:/ArqSoftware/MuniHuamanga/common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/enums/RolUsuario.java):

| Rol | Denominación Municipal | Permisos y Capacidades |
|---|---|---|
| **`ROLE_ADMIN`** | Administrador / Jefe de Licencias | Acceso total a todas las operaciones, configuración y bandeja. |
| **`ROLE_EVALUADOR`** | Inspector / Evaluador Técnico | Dictamen ITSE, evaluación de condiciones, aprobación y rechazo de licencias. |
| **`ROLE_CAJERO`** | Personal de Ventanilla SAT | Consulta de expedientes, generación de vouchers y validación de pagos de tasas TUPA. |

### Matriz de Endpoints y Seguridad

| Método | Endpoint | Acceso / Rol Requerido | Descripción |
|---|---|:---:|---|
| `POST` | `/api/auth/login` | **Público** | Autenticación con credenciales y entrega de token JWT |
| `GET` | `/api/auth/me` | Autenticado | Perfil del usuario conectado actualmente |
| `POST` | `/api/expedientes` | **Público** | Mesa de Partes Virtual (registro del ciudadano) |
| `GET` | `/api/expedientes/tramite/**` | **Público** | Seguimiento ciudadano por correlativo `EXP-AAAA-XXXXX` |
| `GET` | `/api/expedientes/*/documentos/**` | **Público** | Descarga de Anexo 1, Voucher SAT, Licencia PDF y QR |
| `GET` | `/api/public/**` | **Público** | Portal y API de verificación pública QR (RNF-20) |
| `GET` | `/api/expedientes` | `ROLE_ADMIN`, `ROLE_EVALUADOR`, `ROLE_CAJERO` | Bandeja general de expedientes |
| `PATCH` | `/api/expedientes/*/clasificar-riesgo` | `ROLE_ADMIN`, `ROLE_EVALUADOR` | Dictamen de riesgo de Defensa Civil |
| `POST` | `/api/expedientes/*/pago` | `ROLE_ADMIN`, `ROLE_CAJERO` | Registro y validación de pago SAT |
| `POST` | `/api/expedientes/*/aprobar` | `ROLE_ADMIN`, `ROLE_EVALUADOR` | Emisión definitiva de Licencia con QR |
| `POST` | `/api/expedientes/*/rechazar` | `ROLE_ADMIN`, `ROLE_EVALUADOR` | Notificación de rechazo con sustento |

---

## 3. Arquitectura de Seguridad Implementada

```
[ Petición HTTP ]
       │
       ▼
[ JwtAuthenticationFilter ] ──(Header: Authorization: Bearer <token>)──┐
       │                                                                ▼
       ├─► [ JwtTokenProvider ] ──(Valida firma HMAC-SHA256 y vigencia) ─┤
       │                                                                │
       ▼                                                                ▼
[ SecurityFilterChain ] ──(Session: STATELESS)                  [ SecurityContextHolder ]
       │                                                                │
       ├──► ¿Ruta pública? ───────────────► Permitir petición           │
       │                                                                │
       ├──► ¿No autenticado? (401) ────────► [ JwtAuthenticationEntryPoint ]
       │                                                                │
       ├──► ¿Rol insuficiente? (403) ──────► [ JwtAccessDeniedHandler ]
       │                                                                │
       └──► ¿Rol autorizado? ─────────────► [ Controller / Service ] ◄──┘
```

---

## 4. Componentes Creados y Modificados

### 4.1 Dominio y DTOs
* [`RolUsuario.java`](file:///d:/ArqSoftware/MuniHuamanga/common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/enums/RolUsuario.java): Enum con `ROLE_ADMIN`, `ROLE_EVALUADOR`, `ROLE_CAJERO`.
* [`LoginRequestDto.java`](file:///d:/ArqSoftware/MuniHuamanga/common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/dto/LoginRequestDto.java): DTO de credenciales (username y password validados).
* [`LoginResponseDto.java`](file:///d:/ArqSoftware/MuniHuamanga/common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/dto/LoginResponseDto.java): DTO de respuesta con token, tipo Bearer, usuario, rol y vigencia.
* [`UsuarioDto.java`](file:///d:/ArqSoftware/MuniHuamanga/common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/dto/UsuarioDto.java): Perfil público del funcionario municipal.

### 4.2 Persistencia y Modelo
* [`Usuario.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/model/Usuario.java): Entidad JPA con contraseña cifrada en BCrypt.
* [`UsuarioRepository.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/repository/UsuarioRepository.java): Búsqueda por username y validación de existencia.
* [`UsuarioDataInitializer.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/config/UsuarioDataInitializer.java): Seeder automático de 3 usuarios institucionales con contraseñas encriptadas.

### 4.3 Núcleo de Seguridad
* [`JwtTokenProvider.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/JwtTokenProvider.java): Generación, parsing y validación con JJWT 0.12.6.
* [`CustomUserDetails.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/CustomUserDetails.java) y [`CustomUserDetailsService.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/CustomUserDetailsService.java): Integración con Spring Security.
* [`JwtAuthenticationFilter.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/JwtAuthenticationFilter.java): Filtro HTTP OncePerRequest.
* [`JwtAuthenticationEntryPoint.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/JwtAuthenticationEntryPoint.java): Manejador de error 401 en JSON.
* [`JwtAccessDeniedHandler.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/JwtAccessDeniedHandler.java): Manejador de error 403 en JSON.
* [`SecurityConfig.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/config/SecurityConfig.java): Reglas de autorización HTTP, encoder BCrypt y deshabilitación de CSRF/Frames.
* [`AuthService.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/service/AuthService.java) y [`AuthController.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/AuthController.java): Endpoints `/api/auth/login` y `/api/auth/me`.

### 4.4 Frontend Actualizado
* [`portal-interno.html`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/resources/static/portal-interno.html):
  - Badge de sesión en encabezado: *👤 [Nombre Completo] (ROL)* + Botón *Cerrar Sesión*.
  - Modal interactivo de Login con botones de acceso rápido para pruebas (`Evaluador`, `Cajero SAT`, `Admin`).
* [`portal-interno.js`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/resources/static/js/portal-interno.js):
  - Interceptor `fetchConAuth()` que adjunta el token JWT y maneja errores 401 y 403.
  - Almacenamiento seguro del token en `localStorage`.

---

## 5. Usuarios Semilla Preconfigurados

| Usuario | Contraseña | Rol Asignado | Funcionario Representado |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Lic. Fernando Quispe (Administrador del Sistema) |
| `evaluador` | `eval123` | `ROLE_EVALUADOR` | Ing. Carlos Mendoza (Subgerencia de Comercio y Licencias) |
| `cajero` | `caja123` | `ROLE_CAJERO` | Rosa Flores (Ventanilla de Recaudación SAT Huamanga) |

---

## 6. Pruebas Automatizadas Ejecutadas

Se incorporaron 12 nuevas pruebas automáticas de seguridad:
* [`JwtTokenProviderTest.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/security/JwtTokenProviderTest.java) (3 tests): Generación, validación, extracción de claims, rechazo de firmas adulteradas y tokens expirados.
* [`AuthServiceTest.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/service/AuthServiceTest.java) (4 tests): Flujo de autenticación con credenciales correctas, rechazo de credenciales inválidas y consulta de perfil.
* [`AuthControllerIntegrationTest.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/controller/AuthControllerIntegrationTest.java) (5 tests): Pruebas de integración HTTP MockMvc:
  1. `POST /api/auth/login` con credenciales válidas retorna HTTP 200 y JSON con token Bearer.
  2. `POST /api/auth/login` con credenciales erróneas retorna HTTP 401 Unauthorized.
  3. `GET /api/expedientes` sin token retorna HTTP 401 Unauthorized.
  4. `GET /api/expedientes` con token Bearer válido retorna HTTP 200 OK.
  5. `GET /api/expedientes/tramite/{num}` responde sin requerir token (público para ciudadanos).

**Resultado total del proyecto multi-módulo:** 72/72 tests en verde (**BUILD SUCCESS**).

---

## 7. Próximos Pasos — Fase 04 Restante

| Sprint | Entregable | Estado |
|---|---|:---:|
| Sprint 4-A | PDF Licencia formato oficial municipal | ✅ Completado |
| Sprint 4-B | Notificaciones email (JavaMail/SMTP) al ciudadano | ✅ Completado |
| Sprint 4-C | Autenticación JWT + Spring Security 6 (RBAC) | ✅ Completado |
| Sprint 4-D | CRUD Tarifario TUPA (gestión dinámica de tasas desde portal interno) | ⏳ Siguiente |
