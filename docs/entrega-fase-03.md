# Entrega Técnica — Fase 03: Frontend Wizard Multipaso y Descarga Integrada de Formatos

## Metadatos

| Campo            | Detalle                                                        |
|------------------|----------------------------------------------------------------|
| **Fase**         | 03 — Frontend del Portal Ciudadano y Sistema Interno           |
| **Sprint**       | Sprint 3                                                       |
| **Fecha**        | 2026-09-29                                                     |
| **Responsable**  | Equipo de Desarrollo — MuniHuamanga Licencias                  |
| **Build**        | `mvn test -pl servicio-expedientes` → BUILD SUCCESS (53 tests) |

---

## 1. Objetivo de la Fase

Elevar la experiencia de usuario del **Portal Ciudadano** y del **Sistema de Gestión Interna** mediante:

1. **Formulario Wizard Multipaso** (3 pasos) con validaciones por etapa, barra de progreso animada y resumen final antes del envío.
2. **Descarga inmediata de Formatos Oficiales** (Anexo 1, 3 y 4 en PDF) directamente desde la interfaz, sin necesidad de ir a Swagger o copiar IDs.
3. **Dashboard Interno mejorado** con KPI cards con iconos y columna dedicada de Formatos PDF en la bandeja de expedientes.
4. **Sistema de Diseño ampliado** con 15+ nuevas clases CSS (wizard, doc-cards, spinner, success-banner, kpi-card-v2).

---

## 2. Archivos Modificados

### 2.1 Sistema de Estilos

| Archivo | Cambio |
|---------|--------|
| `servicio-expedientes/src/main/resources/static/css/styles.css` | +350 líneas: clases wizard (stepper, progress bar, panels), doc-card (anexo1/3/4), success-banner, resumen-grid, alerta-legal, spinner, kpi-card-v2 |

### 2.2 Portal Ciudadano

| Archivo | Cambio |
|---------|--------|
| `static/portal-ciudadano.html` | Reescritura completa: 3 paneles de wizard, stepper horizontal, barra de progreso, resumen dinámico, banner de éxito, 3 doc-cards de descarga PDF |
| `static/js/portal-ciudadano.js` | Reescritura completa: `irPaso()`, `validarPaso()`, `construirResumen()`, `registrarSolicitud()` con spinner, `descargarAnexo()` con fetch+blob, `nuevaSolicitud()` |

### 2.3 Portal Interno

| Archivo | Cambio |
|---------|--------|
| `static/portal-interno.html` | KPI cards a `kpi-card-v2` con iconos circulares, nueva columna `<th>Formatos PDF</th>` en la tabla |
| `static/js/portal-interno.js` | Nueva función `renderFormatosPdf()` con botones Anexo 1/3/4/Voucher; `descargarFormatoInterno()` con fetch+blob hacia `servicio-formularios`; colspans 9 |

---

## 3. Flujo del Wizard Ciudadano

```
[Pestaña: Mesa de Partes Virtual]
         │
         ▼
┌────────────────────────┐
│  PASO 1: Solicitante   │  ← Validación en tiempo real: DNI/RUC regex, email, celular
│  Nombre, Documento,    │
│  Teléfono, Email       │
└──────────┬─────────────┘
           │ → irPaso(2)
           ▼
┌────────────────────────┐
│  PASO 2: Establecimiento│  ← Validación: area > 0, nombre comercial, giro, dirección
│  Nombre, Giro, Dir,    │
│  Área m², N° Pisos     │
└──────────┬─────────────┘
           │ → irPaso(3)
           ▼
┌────────────────────────┐
│  PASO 3: Confirmación  │  ← Resumen dinámico de 10 campos + Checkbox DDJJ
│  Resumen + DJJ +       │
│  Botón Enviar          │
└──────────┬─────────────┘
           │ → registrarSolicitud() → POST /api/expedientes
           ▼
┌────────────────────────┐
│  ✅ BANNER DE ÉXITO    │  ← Número de trámite + 3 doc-cards
│  EXP-2026-XXXXX        │
│  ┌─────────────────┐   │
│  │📋 Anexo 1 (DJ)  │ ──┼──► GET /api/expedientes/{id}/documentos/declaracion-jurada
│  │🛡️ Anexo 3 (ITSE)│ ──┼──► POST /api/formularios/anexo3-matriz-riesgo-itse
│  │🔒 Anexo 4 (Seg) │ ──┼──► POST /api/formularios/anexo4-condiciones-seguridad
│  └─────────────────┘   │
└────────────────────────┘
```

---

## 4. Integración de Endpoints

| Acción Frontend | Método | Endpoint | Servicio |
|-----------------|--------|----------|---------|
| Registrar solicitud | `POST` | `/api/expedientes` | servicio-expedientes (8081) |
| Descargar Anexo 1 (DJ) | `GET` | `/api/expedientes/{id}/documentos/declaracion-jurada` | servicio-expedientes (8081) |
| Descargar Anexo 3 (ITSE) | `POST` | `/api/formularios/anexo3-matriz-riesgo-itse` | servicio-formularios (8082) |
| Descargar Anexo 4 (Seg) | `POST` | `/api/formularios/anexo4-condiciones-seguridad` | servicio-formularios (8082) |
| Consultar estado | `GET` | `/api/expedientes/tramite/{numero}` | servicio-expedientes (8081) |
| Descargar formato interno | `POST` | `/api/formularios/{tipo}` | servicio-formularios (8082) |

> **CORS:** En producción, los llamados cross-origin al puerto 8082 se resuelven vía el `api-gateway` o configurando un proxy inverso en Nginx. En desarrollo local, ambos servicios deben estar corriendo.

---

## 5. Validaciones Implementadas (Paso 1)

| Campo | Regla |
|-------|-------|
| Nombre Titular | No vacío |
| Documento | Regex: `^([0-9]{8}|(10|20)[0-9]{9})$` — DNI 8 dígitos o RUC 11 dígitos (10/20) |
| Teléfono | Regex: `^9[0-9]{8}$` — Celular peruano válido |
| Email | Regex estándar de correo electrónico |

---

## 6. Diseño — Nuevas Clases CSS

| Clase | Propósito |
|-------|-----------|
| `.wizard-stepper` | Contenedor del stepper horizontal con línea conectora |
| `.wizard-step.active` | Nodo activo: azul institucional + escala 1.15 + glow |
| `.wizard-step.completed` | Nodo completado: verde + checkmark |
| `.wizard-progress-bar / fill` | Barra de progreso con transición suave |
| `.wizard-panel` | Panel de paso con animación `slideInRight` |
| `.doc-card.anexo1/3/4` | Tarjeta de documento con franja de color según tipo |
| `.success-banner` | Banner de éxito post-registro con animación `bounceIn` |
| `.resumen-grid` | Grid de resumen de datos en Paso 3 |
| `.alerta-legal` | Alerta normativa amarilla con icono |
| `.spinner` | Loader circular para estados de carga |
| `.kpi-card-v2` | KPI card con icono circular de color |

---

## 7. Verificación y Pruebas

```bash
# Ejecutar todas las pruebas unitarias (53 tests — sin cambios en backend)
mvn test

# Levantar servicio-expedientes (incluye el frontend estático)
cd servicio-expedientes && mvn spring-boot:run

# Levantar servicio-formularios (necesario para Anexo 3/4)
cd servicio-formularios && mvn spring-boot:run

# Acceder al Portal Ciudadano
http://localhost:8081/portal-ciudadano.html

# Acceder al Sistema Interno
http://localhost:8081/portal-interno.html
```

### Pasos de verificación manual:

1. ✅ **Wizard Paso 1→2→3**: Completar datos y avanzar con validaciones.
2. ✅ **Validación DNI**: Ingresar `ABC12345` → debe mostrar error.
3. ✅ **Resumen Paso 3**: Verificar que muestre los 10 campos correctamente.
4. ✅ **Registro**: Click en "Enviar Solicitud" → Número de trámite generado.
5. ✅ **Descarga Anexo 1**: PDF de 2 páginas descargado correctamente.
6. ✅ **Descarga Anexo 3**: PDF Matriz ITSE descargado desde servicio-formularios.
7. ✅ **Descarga Anexo 4**: PDF Condiciones de Seguridad descargado.
8. ✅ **Seguimiento**: Buscar por número de trámite → Timeline y datos correctos.
9. ✅ **Portal Interno**: KPI cards con iconos; columna Formatos PDF con 3 botones.

---

## 8. Commit de Fase 03

```
feat(fase-03/frontend): wizard multipaso ciudadano + descarga de anexos PDF

Portal Ciudadano:
- Formulario wizard de 3 pasos con stepper animado y barra de progreso
- Validaciones por paso (regex DNI/RUC, celular, email)
- Resumen dinámico de datos en Paso 3 + Declaración Jurada checkbox
- Banner de éxito con número de trámite y 3 doc-cards de descarga
- Descarga de Anexo 1 (GET), Anexo 3 y Anexo 4 (POST fetch+blob)

Portal Interno:
- KPI cards v2 con iconos circulares de color
- Nueva columna "Formatos PDF" con botones Anexo 1/3/4 + Voucher
- Función descargarFormatoInterno() con fetch+blob hacia servicio-formularios
- Colspans actualizados a 9 columnas

Sistema de Diseño:
- +350 líneas CSS: wizard, doc-cards, spinner, success-banner, kpi-card-v2

Tests: 53/53 OK (sin cambios en backend)
Refs: Ley N° 28976 / D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM
```
