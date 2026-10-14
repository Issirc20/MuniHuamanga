# Lista de Verificación Pre-Pase a Producción (Checklist) — MuniHuamanga V2
## Sistema de Gestión Digital de Licencias de Funcionamiento

**Entidad:** Municipalidad Provincial de Huamanga (Ayacucho, Perú)
**Versión:** `v2.0.0-rc1` (Corte I10 Fase A)
**Fecha de Validación:** 14 de octubre de 2026
**Responsables del Check:** Líder Técnico, Especialista en Seguridad & DevOps

---

## 1. Control de Seguridad, Criptografía y Configuración (OWASP ASVS / ADRs)

| # | Ítem de Control | Verificación / Evidencia | Estado |
|---|---|---|:---:|
| **SEC-01** | **Secreto JWT en Producción** | Se configuró `JWT_SECRET` en `.env` con cadena criptográfica aleatoria $\ge 256$ bits. No se utiliza el secreto predeterminado del repositorio. | [ ] |
| **SEC-02** | **Expiración de Tokens JWT** | El tiempo de vida está acotado a 8 horas (`JWT_EXPIRATION_MS=28800000`). | [ ] |
| **SEC-03** | **Credenciales de Base de Datos** | La contraseña de PostgreSQL 16 en `.env` es robusta ($\ge 20$ caracteres) y distinta a la de desarrollo (`muni_pass123`). | [ ] |
| **SEC-04** | **Perfil de Spring Boot Activo** | La variable `SPRING_PROFILES_ACTIVE` está fijada en `prod`. | [ ] |
| **SEC-05** | **Desactivación de Inicializadores Demo** | `UsuarioDataInitializer` y `DataInitializer` tienen `@Profile("!prod")` y NO inyectan usuarios ni contraseñas demo (`admin123`, `eval123`, `caja123`) en producción. | [ ] |
| **SEC-06** | **Validación de Esquema Hibernate** | En `application-prod.yml`, `ddl-auto` está configurado en `validate` (prohibido `update` o `create-drop`). | [ ] |
| **SEC-07** | **Aislamiento de Puertos Internos** | El puerto `5432` de PostgreSQL NO está expuesto al tráfico público exterior de Internet (solo a red interna Docker o VPN municipal). | [ ] |
| **SEC-08** | **Protección IDOR/BOLA en Backend (ADR-02)** | Todos los endpoints de expedientes, cuentas y establecimientos validan la propiedad del recurso mediante `@PreAuthorize`. | [ ] |
| **SEC-09** | **Ejecución No-Root en Contenedores** | El `Dockerfile` ejecuta la JVM bajo el usuario `appuser` (UID 10001) sin privilegios de superusuario. | [ ] |
| **SEC-10** | **Clickjacking y Cabeceras HTTP** | Spring Security tiene activo `frameOptions.sameOrigin()` y cabeceras de seguridad HSTS y Content-Type Options. | [ ] |
| **SEC-11** | **Firma Digital IOFE y Fail-Closed (ADR-04, ADR-07)** | Integración formal con proveedor acreditado IOFE configurada; comprobado que si el servicio falla, la emisión oficial se bloquea. | [ ] |
| **SEC-12** | **Minimización de Datos en QR Público (ADR-05)** | Verificado que la consulta pública QR no expone DNI, RUC ni coordenadas (Ley N° 29733). | [ ] |

---

## 2. Infraestructura, Base de Datos y Red

| # | Ítem de Control | Verificación / Evidencia | Estado |
|---|---|---|:---:|
| **INF-01** | **Capacidad Concurrente RNF-02** | Verificada la capacidad de $\ge 150$ transacciones concurrentes con HikariCP (`Concurrencia150UsuariosTest`). | [ ] |
| **INF-02** | **Latencia Promedio en Verificación QR** | El tiempo de respuesta del endpoint público es $\le 200\text{ ms}$ en percentil $P_{95}$. | [ ] |
| **INF-03** | **Healthcheck de Contenedor** | El servicio backend tiene configurado healthcheck automático contra `/actuator/health`. | [ ] |
| **INF-04** | **Memoria JVM y Garbage Collector** | La imagen Docker utiliza `-XX:+UseG1GC` y límite de Heap adecuado para 4-8 GB de RAM. | [ ] |
| **INF-05** | **Sincronización Horaria (NTP)** | Servidor y contenedor sincronizados en la zona horaria `America/Lima` para sellado temporal. | [ ] |
| **INF-06** | **Backup Previo Certificado** | Se ejecutó un respaldo completo de PostgreSQL 16 (`pg_dump`) antes del procedimiento de despliegue. | [ ] |
| **INF-07** | **Aislamiento de Scripts Init** | Se verificó que los scripts en `docker/postgres/init/` NO se apliquen sobre bases de datos productivas existentes con datos. | [ ] |

---

## 3. Procesos Administrativos y Generación Documental V2

| # | Ítem de Control | Verificación / Evidencia | Estado |
|---|---|---|:---:|
| **DOC-01** | **Tarifario TUPA Vigente** | Verificadas las tasas de la Ordenanza Municipal vigente en la tabla `tarifas_tupa`. | [ ] |
| **DOC-02** | **Flujo de Observación y Subsanación SHA-256** | Verificada la carga de documentos corregidos con versionado e inmutabilidad con hash SHA-256. | [ ] |
| **DOC-03** | **Bloqueo de Licencia sin Resolución Firmada** | Comprobado que no se puede emitir licencia oficial sin una resolución previa en estado `FIRMADA`. | [ ] |
| **DOC-04** | **Secuencias Atómicas Pesimistas** | Verificado que los correlativos `RES-YYYY-XXXXX` y `LIC-YYYY-XXXXX` no generen duplicados bajo concurrencia. | [ ] |
| **DOC-05** | **Validación Manual de Pagos SAT** | Operadores de caja SAT capacitados para usar el módulo de calificación manual de abonos. | [ ] |

---

## 4. Firma y Autorización de Salida a Producción

- **Fecha de Validación:** `_____ / _____ / 2026`
- **Responsable de Infraestructura / DevOps:** `___________________________`
- **Oficial de Seguridad de la Información (CISO):** `___________________________`
- **Líder de Proyecto de Transformación Digital:** `___________________________`
