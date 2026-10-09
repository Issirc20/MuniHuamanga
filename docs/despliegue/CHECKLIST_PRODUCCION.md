# LISTA DE VERIFICACIÓN PRE-PASE A PRODUCCIÓN (CHECKLIST)
## Sistema de Gestión Digital de Licencias de Funcionamiento — MuniHuamanga
**Versión de Salida:** `v0.9.0-rc1`  
**Fecha de Validación:** 09 de Octubre de 2026  
**Responsable del Check:** Líder Técnico, Especialista Ciberseguridad & DevOps

---

## 1. CONTROL DE SEGURIDAD Y CONFIGURACIÓN (OWASP ASVS)

| # | Ítem de Control | Verificación / Evidencia | Estado |
|---|---|---|:---:|
| **SEC-01** | **Secreto JWT en Producción** | Se configuró `JWT_SECRET` en `.env` con cadena criptográfica aleatoria de más de 256 bits. No se utiliza el secreto predeterminado del repositorio. | [ ] |
| **SEC-02** | **Expiración de Tokens JWT** | El tiempo de vida está acotado a 8 horas (`JWT_EXPIRATION_MS=28800000`). | [ ] |
| **SEC-03** | **Credenciales de Base de Datos** | La contraseña de PostgreSQL en `.env` es robusta (mínimo 20 caracteres) y distinta a la de desarrollo (`muni_pass123`). | [ ] |
| **SEC-04** | **Perfil de Spring Boot Activo** | La variable `SPRING_PROFILES_ACTIVE` está fijada en `prod`. | [ ] |
| **SEC-05** | **Desactivación de Inicializadores Demo** | Se verificó que `UsuarioDataInitializer` y `DataInitializer` tienen `@Profile("!prod")` y NO inyectan usuarios por defecto (`admin123`, `eval123`, `caja123`) en producción. | [ ] |
| **SEC-06** | **Validación de Esquema Hibernate** | En `application-prod.yml`, `ddl-auto` está configurado en `validate` (prohibido `update` o `create-drop`). | [ ] |
| **SEC-07** | **Aislamiento de Puertos Internos** | El puerto `5432` de PostgreSQL y `5050` de pgAdmin NO están expuestos al tráfico público exterior de Internet (solo a red interna Docker o VPN municipal). | [ ] |
| **SEC-08** | **Protección de Datos Personales (BOLA/LPAG)** | El endpoint ciudadano `/api/expedientes/tramite/{num}` retorna datos ofuscados (`SeguimientoCiudadanoDto`) sin exponer DNI completo, correo ni teléfono. | [ ] |
| **SEC-09** | **Ejecución No-Root en Contenedores** | El `Dockerfile` ejecuta la JVM bajo el usuario `appuser` (UID 10001) sin privilegios de superusuario. | [ ] |
| **SEC-10** | **Clickjacking y Cabeceras HTTP** | Spring Security tiene activo `frameOptions.sameOrigin()` y cabeceras de seguridad HSTS y Content-Type Options. | [ ] |

---

## 2. INFRAESTRUCTURA, RENDIMIENTO Y RED

| # | Ítem de Control | Verificación / Evidencia | Estado |
|---|---|---|:---:|
| **INF-01** | **Capacidad Concurrente RNF-01** | Probada la capacidad de >= 150 usuarios concurrentes con 0% de error en `run-load-test-150.ps1` y `Concurrencia150UsuariosTest`. | [ ] |
| **INF-02** | **Latencia Promedio RNF-02** | El tiempo medio de respuesta en pruebas de estrés es < 110 ms (umbral máximo permitido: 3,000 ms). | [ ] |
| **INF-03** | **Healthcheck de Contenedor** | El servicio backend tiene configurado healthcheck automático contra `/actuator/health`. | [ ] |
| **INF-04** | **Memoria JVM y Garbage Collector** | La imagen Docker utiliza `-XX:+UseG1GC` y `-XX:MaxRAMPercentage=75.0` con salida automática ante error de memoria (`-XX:+ExitOnOutOfMemoryError`). | [ ] |
| **INF-05** | **Sincronización Horaria (NTP)** | El contenedor está sincronizado en la zona horaria `America/Lima` para el correcto sellado cronológico de expedientes. | [ ] |
| **INF-06** | **Backup Previo de Base de Datos** | Se ejecutó un respaldo de seguridad completo de PostgreSQL antes de iniciar el procedimiento de puesta en producción. | [ ] |

---

## 3. PROCESO ADMINISTRATIVO Y GENERACIÓN DOCUMENTAL

| # | Ítem de Control | Verificación / Evidencia | Estado |
|---|---|---|:---:|
| **DOC-01** | **Tasas TUPA Oficiales** | Verificadas las tasas de la Ordenanza N° 018-2024-MPH: S/ 154.50 (Bajo), S/ 218.00 (Medio), S/ 345.20 (Alto), S/ 480.00 (Muy Alto). | [ ] |
| **DOC-02** | **Flujo de Observación y Subsanación** | Implementado el estado `OBSERVADO` según Ley 27444 con posibilidad de subsanación ciudadana en línea. | [ ] |
| **DOC-03** | **Descarga de Licencia Oficial Blindada** | Se validó que un expediente que no esté en estado `APROBADO` no puede generar ni descargar la Licencia Oficial PDF (400 Bad Request). | [ ] |
| **DOC-04** | **Código QR y Verificación Pública** | Los certificados emitidos cuentan con código QR escaneable que redirige al portal público de autenticidad `verificar-licencia.html`. | [ ] |

---

## 4. FIRMA Y AUTORIZACIÓN DE SALIDA A PRODUCCIÓN

- **Fecha de Validación:** `_____ / _____ / 2026`
- **Responsable de Infraestructura / DevOps:** `___________________________`
- **Oficial de Seguridad de la Información (CISO):** `___________________________`
- **Líder de Proyecto de Transformación Digital:** `___________________________`
