# Entrega Técnica — Fase 04 / Sprint 4-A: Generador Oficial PDF Licencia de Funcionamiento

## Metadatos

| Campo            | Detalle                                                              |
|------------------|----------------------------------------------------------------------|
| **Fase**         | 04 — PDF Licencia Oficial + (próx.) Email + JWT + CRUD TUPA          |
| **Sprint**       | Sprint 4-A                                                           |
| **Fecha**        | 2026-10-02                                                           |
| **Responsable**  | Equipo de Desarrollo — MuniHuamanga Licencias                        |
| **Build**        | `mvn install -pl common-domain && mvn test -pl servicio-expedientes` → BUILD SUCCESS (40 tests) |
| **Commit**       | `feat(fase-04/sprint4a): generador PDF Licencia formato oficial municipal` |

---

## 1. Objetivo del Sprint 4-A

Implementar el **Generador Oficial del Certificado de Licencia de Funcionamiento** en PDF que replica **fielmente** el formato físico emitido por la Subgerencia de Comercio, Licencias y Control Sanitario de la **Municipalidad Provincial de Huamanga**, tal como aparece en los formatos de referencia (Licencia N° 005023 y N° 000286).

### Formatos de Referencia Analizados

| Característica | Formato N° 000286 | Formato N° 005023 |
|----------------|-------------------|-------------------|
| Borde perimetral | Línea única | Guarda decorativa azul |
| Encabezado | Escudo + 3 franjas | Escudo + 2 franjas + bandera |
| Título | "LICENCIA DE FUNCIONAMIENTO DEFINITIVA" | "LICENCIA DE FUNCIONAMIENTO" |
| Número esquina | Cuadro rojo "Nº 000286" | Cuadro rojo "Nº 005023" |
| Campos | RUC, Categoría, Área en fila compuesta | Ídem |
| Horario | "AUTORIZACIÓN PARA OPERAR DE: HH:mm HASTA: HH:mm" | Ídem |
| Cuadro VENCE | "**/**/****" | Ídem |
| SIGETI | Código de propietario 25 dígitos | Ídem |
| INDICACIONES | 4 bullets | 6 bullets normativa actualizada |
| QR | En pie del documento | En pie del documento |

---

## 2. Archivos Creados / Modificados

### 2.1 Nuevo Generador Dedicado

| Archivo | Tipo | Descripción |
|---------|------|-------------|
| `servicio-expedientes/src/main/java/.../service/LicenciaPdfGenerator.java` | **NUEVO** | Generador del PDF oficial con formato físico municipal (~500 líneas) |

### 2.2 Modelo y DTO Ampliados

| Archivo | Cambio |
|---------|--------|
| `common-domain/.../dto/ExpedienteResponseDto.java` | +5 campos: `numeroLicencia`, `categoriaEstablecimiento`, `horaInicio`, `horaFin`, `fechaAprobacion` |
| `servicio-expedientes/.../model/Expediente.java` | +5 campos JPA `@Column` correspondientes |

### 2.3 Servicio y Tests Actualizados

| Archivo | Cambio |
|---------|--------|
| `servicio-expedientes/.../service/DocumentoPdfService.java` | Inyección de `LicenciaPdfGenerator`; `generarLicenciaPdf()` simplificado a 5 líneas delegando al nuevo generador |
| `servicio-expedientes/.../service/LicenciaPdfServiceTest.java` | Fix constructor (4 args); +1 test `testGenerarLicenciaFormatoOficialCompletoFase04` con datos reales del formato físico |

---

## 3. Estructura del PDF Generado

```
┌─────────────────────────────────────────────────────────────────┐
│  [Borde azul + dorado]                          Nº XXXXXX (rojo)│
│                                                                 │
│  [Escudo]   MUNICIPALIDAD PROVINCIAL DE HUAMANGA               │
│  [███████]  GERENCIA DE DESARROLLO ECONÓMICO     (naranja)      │
│  [███████]  SUBGERENCIA DE COMERCIO Y LICENCIAS  (naranja claro)│
│                                                                 │
│            LICENCIA  DE  FUNCIONAMIENTO                         │
│                       DEFINITIVA                                │
│                                                                 │
│  NOMBRE O RAZÓN SOCIAL:  DONDE LOPEZ EIRL                      │
│  NOMBRE COMERCIAL:       "DONDE LOPEZ EIRL"                    │
│  DIRECCIÓN:              CENTRO POBLADO BARRIO DE LA MAGDALENA  │
│                          MZ E LOTE 09 — UNIDAD VECINAL          │
│  DATOS FISCALES:         R.U.C.: 20610371974                   │
│                          CATEGORÍA: 1-A (INTERMEDIO)            │
│                          ÁREA: 102.00 M²                        │
│  GIRO (S):               FERRETERIA (NO ALMACEN) (USO 30.27)   │
│                                                                 │
│  AYACUCHO, 30 DE SEPTIEMBRE DE 2026   LICENCIA N°: 202613788   │
│  ZONIFICACIÓN: CENTRO HISTORICO: SECTOR 04  EXPEDIENTE N°: ... │
│  AUTORIZACION PARA OPERAR DE: 06:00 HORAS  HASTA: 23:00 HORAS │
│                                               ┌──────────────┐ │
│  OBSERVACIÓN: CÓDIGO SIGETI: 0000000000000000 │ VENCE        │ │
│                                               │ Día/Mes/Año  │ │
│                                               │ **/**/****   │ │
│  ─────────── INDICACIONES: ───────────        └──────────────┘ │
│  • La licencia debe estar en lugar visible...                   │
│  • No puede ser usada por otra persona...                       │
│  • [6 bullet points normativos Ley 28976]                       │
│                                                                 │
│  Ing. / Lic. .................    [████]  ← Código QR          │
│  SUBGERENTE DE COMERCIO Y LICENCIAS   Escanee para verificar   │
│  PROVINCIA - HUAMANGA - AYACUCHO                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Lógica de Resolución de Campos

### 4.1 Número de Licencia
Prioridad: `numeroLicencia` > `licenciaQrCode` (extrae últimos dígitos) > `numeroTramite` (solo dígitos)

### 4.2 Categoría del Establecimiento
```java
resolverCategoria(exp):
  BAJO     → "1-A (BAJO)"
  MEDIO    → "1-A (INTERMEDIO)"
  ALTO     → "2-A (ALTO)"
  MUY_ALTO → "2-B (MUY ALTO)"
```

### 4.3 Dirección Completa
Construida concatenando: `direccionEstablecimiento` + ` MZ {manzana}` + ` LOTE {lote}` + ` — {urbanizacion}`

### 4.4 Fecha de Emisión
`fechaAprobacion` si disponible, sino `fechaCreacion`, sino `LocalDateTime.now()`

### 4.5 Horario de Operación
Campos `horaInicio` (default: "00:00") y `horaFin` (default: "24:00")

### 4.6 Código QR de Verificación
Generado por `DocumentoPdfService.generarImagenQr()` con URL del portal de verificación pública (RNF-20).

---

## 5. Nuevos Campos en el Modelo de Dominio

| Campo (Entidad) | Tipo | Columna BD | Descripción |
|----------------|------|-----------|-------------|
| `numeroLicencia` | `String` | `numero_licencia` | N° correlativo emitido por la Municipalidad (Ej: `202613788`) |
| `categoriaEstablecimiento` | `String` | `categoria_establecimiento` | Categoría ITSE (Ej: `1-A (INTERMEDIO)`) |
| `horaInicio` | `String` | `hora_inicio` | Hora inicio de operación autorizada (Ej: `06:00`) |
| `horaFin` | `String` | `hora_fin` | Hora fin de operación autorizada (Ej: `23:00`) |
| `fechaAprobacion` | `LocalDateTime` | `fecha_aprobacion` | Fecha y hora de emisión de la Licencia |

> **Nota:** Usar `spring.jpa.hibernate.ddl-auto=update` en desarrollo para que Hibernate agregue automáticamente las columnas en caliente.

---

## 6. Verificación y Pruebas

```bash
# Compilar con cambios en DTO
mvn install -pl common-domain

# Ejecutar suite de tests (40 tests — incluyendo test formato oficial Fase 04)
mvn test -pl servicio-expedientes

# Levantar servicio
mvn spring-boot:run -pl servicio-expedientes

# Descargar PDF de Licencia Oficial
curl -X GET http://localhost:8081/api/expedientes/{id}/documentos/licencia \
     -H "Accept: application/pdf" \
     --output licencia-oficial.pdf
```

### Tests Ejecutados

| Suite de Tests | Tests | Estado |
|----------------|-------|--------|
| `Concurrencia150UsuariosTest` | 1 | ✅ |
| `CalculadoraDeTasaDesgloseTest` | 3 | ✅ |
| `CalculadoraDeTasaTest` | 5 | ✅ |
| `ExpedienteServiceTest` | 9 | ✅ |
| `LicenciaPdfServiceTest` | 6 | ✅ (incluye test Fase 04) |
| `VerificacionPublicaTest` | 4 | ✅ |
| `EstadoExpedienteValidatorTest` | 6 | ✅ |
| `MesaPartesValidationTest` | 6 | ✅ |
| **TOTAL** | **40** | **✅ BUILD SUCCESS** |

---

## 7. Próximos Pasos — Fase 04 Restante

| Sprint | Entregable | Estado |
|--------|------------|--------|
| Sprint 4-A | PDF Licencia formato oficial municipal | ✅ Completado |
| Sprint 4-B | Notificaciones email (JavaMail/SMTP) al ciudadano | ✅ Completado |
| Sprint 4-C | Autenticación JWT + Spring Security | ⏳ Pendiente |
| Sprint 4-D | CRUD Tarifario TUPA (gestión de tasas desde portal interno) | ⏳ Pendiente |

---

## 8. Commit de Fase 04 Sprint 4-A

```
a847f5f  feat(fase-04/sprint4a): generador PDF Licencia de Funcionamiento formato oficial municipal

LicenciaPdfGenerator.java — Nuevo generador con formato físico exacto:
- Borde decorativo: azul institucional + dorado fino
- Nº Licencia en cuadro rojo esquina superior derecha
- Encabezado: Escudo + franjas naranjas Gerencia / Subgerencia
- Título dorado 22pt: LICENCIA DE FUNCIONAMIENTO DEFINITIVA
- Marca de agua (escudo 9% opacidad)
- Tabla campos normativos: Razón Social / Nombre Comercial / Dirección
  + Fila compuesta RUC | Categoría | Área m²  + Giro(s) CIIU
- Ciudad/Fecha larga | Licencia N°
- Zonificación | Expediente N°
- Autorización horaria (HH:mm → HH:mm) | Cuadro VENCE **/**/****
- Observación (Código SIGETI)
- INDICACIONES: 6 bullets legales Ley 28976
- Pie: Firma Subgerente + QR verificación (RNF-20)

Tests: 40/40 OK
Refs: Ley N° 28976 / D.S. N° 046-2017-PCM
Formatos ref: Licencia N° 005023 y N° 000286 — MuniHuamanga
```
