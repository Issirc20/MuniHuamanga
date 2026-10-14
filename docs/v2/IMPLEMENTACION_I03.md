# MUNIHUAMANGA V2 — INFORME TÉCNICO DE IMPLEMENTACIÓN I03
## Módulos M01 (Cuentas y Usuarios) y M02 (Establecimientos)

**Municipalidad Provincial de Huamanga — Gerencia de Licencias y Autorizaciones Comerciales**
**Fecha de Cierre:** 14 de octubre de 2026
**Rama:** `develop/munihuamanga-v2`
**Arquitectura:** Java 21, Spring Boot 3.3.4, Spring Security 6, PostgreSQL, Flyway/SQL nativo, Maven
**Resultado de Verificación:** `mvn clean verify` — **151 pruebas exitosas, 0 fallos, 0 errores**

---

## 1. Resumen Ejecutivo del Incremento I03

El **Incremento I03** sienta los cimientos estructurales para desacoplar el modelo de datos monolítico y desnormalizado heredado de V1, introduciendo:
1. **Fase previa de blindaje:** Eliminación definitiva de cualquier posibilidad de creación anónima de expedientes (anulación de la cabecera `X-Legacy-Client: v1-portal`), garantía de bloqueo de H2 Console en entornos de producción y retención estricta de licencias oficiales sin firma digital institucional.
2. **Módulo M01 (Cuentas y Usuarios):** Gestión de cuentas independientes tanto para personas naturales (identificadas por DNI normalizado) como personas jurídicas (identificadas por RUC normalizado). Admite múltiples operadores nominativos con asignación granular de permisos, autenticación con contraseñas cifradas en BCrypt, verificación de correo y registro de auditoría ante cambios de permisos.
3. **Módulo M02 (Establecimientos):** Gestión de sedes o locales comerciales asociados a una cuenta, con validación de rangos de coordenadas geográficas (-90° a +90° latitud, -180° a +180° longitud), asignación de giros normalizados mediante el `CatalogoCiiu` y desacoplamiento formal del trámite administrativo (no se asigna número de expediente municipal al crear locales).

---

## 2. Fase Previa Obligatoria — Cierre de Vulnerabilidades

Antes de proceder a la creación de los nuevos módulos, se ejecutó y validó la fase de blindaje requerida:

### 2.1 Eliminación de Creación Anónima de Expedientes
- **Diagnóstico previo:** En la transición I02.1 se mantenía un mecanismo transitorio que admitía la cabecera `X-Legacy-Client: v1-portal` permitiendo la inserción de expedientes sin autenticación previa.
- **Acción implementada:** Se removió completamente la lógica del bypass en [ExpedienteController.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/ExpedienteController.java#L75-L95). Todo intento de registro en `POST /api/expedientes` exige forzosamente `@PreAuthorize("isAuthenticated()")`. Cualquier intento anónimo (con o sin cabecera heredada) es rechazado inmediatamente con `401 Unauthorized` por Spring Security.
- **Prueba automatizada:** [ExpedienteSecurityAuthorizationTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/security/ExpedienteSecurityAuthorizationTest.java#L355-L385) (`creacionExpedienteAnonimoConCabeceraLegacyEsRechazada`, `creacionExpedienteV2AnonimoSinCabeceraRetorna403`, `creacionExpedienteAutenticadoExitoso`).

### 2.2 Bloqueo de H2 Console en Producción
- **Verificación:** [SecurityConfigProdTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/security/SecurityConfigProdTest.java) confirmó que bajo el perfil `prod` cualquier petición a `/h2-console/**` devuelve `404 Not Found` o `401 Unauthorized`, dado que H2 Console solo se registra en perfiles de desarrollo o pruebas (`!prod`).

### 2.3 Bloqueo de Emisión de Licencias y Códigos QR sin Firma Digital
- **Verificación:** [ExpedienteServiceTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/service/ExpedienteServiceTest.java) (`aprobarExpedienteSinFirmaDigital_NoEmiteLicenciaNiQr`) y pruebas de [LicenciaPdfServiceTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/service/LicenciaPdfServiceTest.java) ratifican que si el puerto `FirmaDigitalPort` reporta firma no válida o ausente, la licencia no recibe código QR de verificación ni correlativo oficial emitido.

---

## 3. Implementación M01 — Cuentas y Operadores

### 3.1 Modelo de Entidades y Unicidad en BD
- [Cuenta.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/cuentas/model/Cuenta.java):
  - `id` (UUID clave primaria).
  - `tipoPersona`: Enum `TipoPersona` (`NATURAL`, `JURIDICA`).
  - `tipoDocumento`: Enum `TipoDocumento` (`DNI`, `RUC`, `CE`).
  - `numeroDocumento`: String normalizado (8 dígitos para DNI, 11 dígitos para RUC), con restricción `UNIQUE` en base de datos.
  - `estado`: Enum `EstadoCuenta` (`PENDIENTE_VERIFICACION`, `ACTIVA`, `BLOQUEADA`, `SUSPENDIDA`).
  - `email`: Correo de contacto y notificaciones oficiales.
  - `tokenVerificacionEmail` y `emailVerificado`: Gestión de activación por enlace.
- [OperadorCuenta.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/cuentas/model/OperadorCuenta.java):
  - Permite a personas jurídicas (RUC) delegar gestiones en operadores nominativos con DNI propio, impidiendo el uso indebido de credenciales compartidas.
  - Roles soportados: `REPRESENTANTE_LEGAL`, `TRAMITADOR`, `GESTOR_TECNICO`.
  - Permisos granulares explícitos (`PermisoOperador`):
    - `CREAR_BORRADOR`: Iniciar trámites.
    - `PRESENTAR_EXPEDIENTE`: Firma/envío formal del trámite.
    - `REGISTRAR_PAGO`: Subir constancias SAT.
    - `CONSULTAR_EXPEDIENTE`: Visualizar estado y formatos.
    - `SUBSANAR_OBSERVACIONES`: Atender observaciones de evaluación.
    - `ADMINISTRAR_OPERADORES`: Agregar o remover operadores de la cuenta.
- [AuditoriaCuentaPermiso.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/cuentas/model/AuditoriaCuentaPermiso.java):
  - Registra cada alta, baja o modificación de permisos otorgados a los operadores, indicando el usuario responsable, timestamp y motivo.

### 3.2 Principio de Honestidad Institucional (RENIEC / SUNAT)
- De acuerdo con la directiva arquitectónica, el sistema **no simula validaciones oficiales de identidad ni consultas automatizadas a RENIEC o SUNAT**, dado que actualmente no existe un convenio de interoperabilidad activo.
- Los DTOs y respuestas de la API incluyen de forma transparente el campo:
  ```json
  "avisoValidacion": "Información registrada bajo declaración jurada del administrado. Validación oficial con RENIEC/SUNAT pendiente de integración por interoperabilidad institucional."
  ```

---

## 4. Implementación M02 — Establecimientos y Catálogo CIIU

### 4.1 Modelo de Establecimientos
- [Establecimiento.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/cuentas/model/Establecimiento.java):
  - Vinculado a `Cuenta` (`FK cuenta_id`).
  - `codigoEstablecimiento`: Identificador secuencial amigable para el administrado (`EST-001`, `EST-002`), **sin asignar número de expediente municipal**.
  - `nombreComercial`, `direccion`, `distrito`, `provincia`, `departamento`, `referencia`.
  - `latitud` y `longitud`: Coordenadas geográficas con validación numérica estricta en el servicio (-90 a +90 latitud, -180 a +180 longitud).
  - `ciiu`: Relación Many-to-One con [CatalogoCiiu.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/cuentas/model/CatalogoCiiu.java).

### 4.2 Soporte Multi-Establecimiento
- Una misma cuenta jurídica (mismo RUC) puede registrar múltiples locales comerciales independientes (sucursales, almacenes, sedes de atención).
- Cada establecimiento mantiene su propia ficha técnica y geográfica.

### 4.3 Advertencia de Zonificación
- Se añade de forma explícita el descargo legal en el DTO de respuesta:
  ```json
  "avisoZonificacion": "Las coordenadas y giro declarados corresponden a información proporcionada por el administrado y NO constituyen zonificación municipal aprobada ni compatibilidad de uso vigente."
  ```

---

## 5. Esquema de Base de Datos y Persistencia Aditiva

Se implementaron scripts aditivos en `docker/postgres/init/02-v2-cuentas-establecimientos.sql` y se actualizaron en `01-init-databases.sql`:

1. **Tabla `catalogo_ciiu`:**
   - Columnas: `codigo VARCHAR(10) PRIMARY KEY`, `descripcion TEXT NOT NULL`, `nivel_riesgo_sugerido VARCHAR(20)`, `activo BOOLEAN`.
   - Incluye semillas para comercio minorista, restaurantes, farmacias, bodegas, consultorios y manufactura.
2. **Tabla `cuentas`:**
   - Columnas: `id UUID PRIMARY KEY`, `tipo_persona`, `tipo_documento`, `numero_documento UNIQUE`, `razon_social_o_nombre`, `email`, `telefono`, `estado`, `email_verificado`, `token_verificacion_email`, `fecha_creacion`.
   - Índices: `idx_cuentas_documento`, `idx_cuentas_email`.
3. **Tabla `operadores_cuenta` y `operadores_cuenta_permisos`:**
   - Integridad referencial hacia `cuentas` y colección de permisos en tabla de unión.
4. **Tabla `auditoria_cuenta_permisos`:**
   - Trazabilidad forense de asignación y revocación de permisos.
5. **Tabla `establecimientos`:**
   - Columnas: `id UUID PRIMARY KEY`, `cuenta_id FK`, `codigo_establecimiento`, `nombre_comercial`, `direccion`, `distrito`, `latitud`, `longitud`, `ciiu_codigo FK`, `activo`.
   - Índices: `idx_establecimientos_cuenta`, `idx_establecimientos_ciiu`.
6. **Evolución aditiva de `expedientes`:**
   - Columnas añadidas: `cuenta_id UUID`, `establecimiento_id UUID` (opcionales para conservar compatibilidad absoluta con expedientes legados).
   - Sin eliminación ni alteración destructiva de tablas existentes.

---

## 6. Endpoints de la API Implementados

### 6.1 Módulo Cuentas (`/api/cuentas`)
| Método | Endpoint | Seguridad / Autorización | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/cuentas/registro` | Público (`permitAll`) | Registro de cuenta natural o jurídica con credenciales BCrypt. |
| `GET` | `/api/cuentas/verificar-correo` | Público (`permitAll`) | Confirmación y activación de cuenta mediante token. |
| `GET` | `/api/cuentas/{id}` | `@PreAuthorize("@cuentaSecurity.puedeVerCuenta(...)")` | Consulta de datos de cuenta por titular, operador o funcionario. |
| `POST` | `/api/cuentas/{id}/operadores` | `@PreAuthorize("@cuentaSecurity.puedeGestionarOperadores(...)")` | Alta de operador nominado para cuenta jurídica. |
| `PUT` | `/api/cuentas/{id}/operadores/{opId}/permisos` | `@PreAuthorize("@cuentaSecurity.puedeGestionarOperadores(...)")` | Modificación y auditoría de permisos de un operador. |

### 6.2 Módulo Establecimientos (`/api/establecimientos`)
| Método | Endpoint | Seguridad / Autorización | Descripción |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/establecimientos` | `@PreAuthorize("@establecimientoSecurity.puedeGestionarEstablecimientosDeCuenta(...)")` | Registro de nuevo establecimiento asociado a cuenta. |
| `GET` | `/api/establecimientos/cuenta/{cuentaId}` | `@PreAuthorize("@establecimientoSecurity.puedeVerEstablecimientosDeCuenta(...)")` | Listado de establecimientos de la cuenta. |
| `GET` | `/api/establecimientos/{id}` | `@PreAuthorize("@establecimientoSecurity.puedeVerEstablecimiento(...)")` | Detalle técnico del establecimiento. |
| `PUT` | `/api/establecimientos/{id}` | `@PreAuthorize("@establecimientoSecurity.puedeModificarEstablecimiento(...)")` | Modificación de datos y coordenadas de establecimiento. |

### 6.3 Módulo Catálogo CIIU (`/api/catalogo-ciiu`)
| Método | Endpoint | Seguridad / Autorización | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/catalogo-ciiu` | Público (`permitAll`) | Listado de giros y actividades económicas CIIU activas. |
| `GET` | `/api/catalogo-ciiu/{codigo}` | Público (`permitAll`) | Detalle de un código CIIU. |

---

## 7. Resultados de Pruebas Automatizadas

La suite integral de pruebas se ejecutó mediante Maven:
```powershell
mvn clean verify
```

### Métricas de Validación:
- **Total de pruebas ejecutadas:** 151
- **Fallos (Failures):** 0
- **Errores (Errors):** 0
- **Omitidas (Skipped):** 0
- **Estado final:** `BUILD SUCCESS` (34.150 s)

### Resumen de Suites Clave:
1. [CuentaServiceTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/cuentas/CuentaServiceTest.java):
   - Registro de persona natural por DNI con contraseña cifrada en BCrypt.
   - Registro de persona jurídica por RUC asignando automáticamente al representante legal con permisos integrales.
   - Rechazo de documentos de identidad duplicados.
   - Verificación de correo electrónico por token.
   - Auditoría de modificación de permisos a operadores.
2. [EstablecimientoServiceTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/cuentas/EstablecimientoServiceTest.java):
   - Registro secuencial con código `EST-001` sin asignar número de trámite.
   - Registro de múltiples establecimientos para una misma cuenta RUC.
   - Validación estricta y rechazo de coordenadas fuera del rango (-90 a 90 / -180 a 180).
3. [CuentaEstablecimientoSecurityIntegrationTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/cuentas/CuentaEstablecimientoSecurityIntegrationTest.java):
   - Flujo HTTP end-to-end con MockMvc.
   - Protección contra vulnerabilidades IDOR/BOLA: Administrado A no puede ver establecimientos de Administrado B.
   - Protección contra escalamiento de privilegios: operador sin permiso no puede modificar permisos ni alterar establecimientos.
4. [ExpedienteSecurityAuthorizationTest.java](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/security/ExpedienteSecurityAuthorizationTest.java):
   - 23 pruebas de autorización integral.
   - Rechazo estricto de accesos y creaciones anónimas (401 Unauthorized).
   - Preservación de compatibilidad con expedientes históricos de V1.

---

## 8. Riesgos Residuales y Transición hacia I04

1. **Transición de titularidad en expedientes históricos:** En V1 los expedientes guardaban `documento_identidad` en texto plano sin `cuenta_id`. En el Incremento I04 se implementará un proceso de enlace automático para vincular los expedientes históricos con la cuenta que se registre con dicho DNI/RUC.
2. **Desacoplamiento de creación formal en I04:** Actualmente `/api/expedientes` aún genera número de trámite inmediato; en el Incremento I04 se implementará la entidad `SolicitudBorrador` (M03) y la máquina de estados formal (M06) para diferir la asignación del correlativo `EXP-2026-xxxxx` al momento de la presentación formal.
3. **Persistencia de coordenadas en evaluación:** Al presentarse un borrador, las coordenadas del establecimiento se capturarán como instantánea para la evaluación de zonificación en M07.

**Conclusión:** El Incremento I03 ha concluido de manera exitosa, con 100% de pruebas aprobadas y cumplimiento total de las directivas arquitectónicas y de seguridad. Queda expedito el camino para abordar el Incremento I04.
