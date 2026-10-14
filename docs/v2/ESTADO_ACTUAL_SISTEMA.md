# Estado Actual del Sistema — MuniHuamanga V2
## Informe de Auditoría y Estado Operativo Consolidado

**Proyecto:** Sistema de Gestión de Licencias de Funcionamiento
**Entidad:** Municipalidad Provincial de Huamanga (Ayacucho, Perú)
**Fecha de Corte:** 14 de octubre de 2026
**Rama:** `develop/munihuamanga-v2`
**Línea Base Automatizada:** 301 pruebas automatizadas exitosas (0 fallos, 0 errores, 0 skipped)
**Marco Normativo:** Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM / Ley N° 27269 / Ley N° 29733

---

## 1. Resumen Ejecutivo de Incrementos

El desarrollo de MuniHuamanga V2 ha avanzado a través de una serie de incrementos iterativos y verificados formalmente mediante pruebas automatizadas y cierres documentales:

| Incremento | Bounded Context / Módulo Principal | Estado de Implementación | Estado de Verificación | Pruebas Automatizadas |
| :---: | :--- | :---: | :---: | :---: |
| **I01** | Línea Base y Estructuración de Monolito Modular | Completado | Verificado | 80 tests |
| **I02 / I02.1** | Cuentas, Operadores y Establecimientos (M01) | Completado | Verificado | 115 tests |
| **I03** | Solicitudes Borrador y Anexo 1 Completo (M02) | Completado | Verificado | 148 tests |
| **I04 / I04.1** | Repositorio Documental Inmutable y Subsanación SHA-256 (M03) | Completado | Verificado | 182 tests |
| **I05 / I05.1** | Validación Manual de Pagos SAT y TUPA Dinámico (M04, M05) | Completado | Verificado | 214 tests |
| **I06 / I06.1** | Zonificación PDU, Edificaciones y Rectificación (M06) | Completado | Verificado | 245 tests |
| **I07 / I07.1** | Evaluación Administrativa e Informes Técnicos (M07) | Completado | Verificado | 281 tests |
| **I08 / I08.1** | Resoluciones con Correlativo Atómico y Firma Fail-Closed (M08, M09) | Completado | Verificado | 289 tests |
| **I09** | Verificación QR Pública y Móvil para Inspectores con Mapa (M10) | Completado | Verificado | 301 tests |
| **I10 (Fase A)** | Diagnóstico Integral de Integración Visual y Ajustes Modulares | Completado | Auditado | 301 tests |
| **I10 (Fase B)** | Demostración Funcional en Servidor Vivo y Pruebas E2E | *Pendiente (No autorizada en este turno)* | *Por ejecutar* | — |

> [!IMPORTANT]
> **Aclaración sobre el Incremento I10:**
> El Incremento I10 no se declara terminado al 100%. Se ha completado rigurosamente su **Fase A (Diagnóstico, Auditoría y Detección de Brechas Visuales)**. La **Fase B (Levantamiento del servidor local en vivo, pruebas end-to-end y validación en navegador)** queda formalmente pendiente para una etapa posterior por restricción explícita de alcance.

---

## 2. Estado Real de los Módulos M01 a M12

```mermaid
graph TD
    classDef complete fill:#10B981,stroke:#059669,color:#ffffff;
    classDef partial fill:#F59E0B,stroke:#D97706,color:#ffffff;

    M01["M01: Cuentas y Operadores"]:::complete
    M02["M02: Solicitudes Borrador"]:::complete
    M03["M03: Expedientes y Documentos"]:::complete
    M04["M04: Pagos SAT"]:::complete
    M05["M05: Tarifario TUPA"]:::complete
    M06["M06: Zonificación y Rectificación"]:::complete
    M07["M07: Evaluación e Informes"]:::complete
    M08["M08: Resoluciones y Firma"]:::complete
    M09["M09: Licencias Oficiales"]:::complete
    M10["M10: Verificación QR y Mapa"]:::complete
    M11["M11: Reclamaciones"]:::partial
    M12["M12: Auditoría y Seguridad"]:::partial
```

### 2.1. Módulos Completos (Backend + Lógica + Cobertura de Pruebas)
1. **M01 (Cuentas y Establecimientos):** Registro de titulares, operadores institucionales, perfiles RBAC, desacoplamiento de locales comerciales.
2. **M02 (Solicitudes Borrador):** Ciclo de vida de borradores (`BORRADOR`, `PRESENTADO`, `DESCARTADO`), autoguardado y checklist de requisitos.
3. **M03 (Expedientes y Documentos):** Máquina de 10 estados, generación en memoria de Anexos 1, 3 y 4 en OpenPDF, versionado de documentos con SHA-256.
4. **M04 (Pagos SAT):** Liquidación TUPA, voucher con código de barras, estados de validación manual (`EN_REVISION`, `VALIDADO_MANUALMENTE`, `OBSERVADO`, `RECHAZADO`).
5. **M05 (Tarifario TUPA Dinámico):** Mantenimiento en caliente por API REST (`PUT /api/tupa/tarifas/{id}`) con fallback resiliente a `application.yml`.
6. **M06 (Zonificación PDU y Rectificación):** Registro de dictamen de compatibilidad de uso PDU y flujo de solicitudes de rectificación de ubicación de locales.
7. **M07 (Evaluación e Informes Técnicos):** Evaluación de admisibilidad, elaboración de informes técnicos desglosados (actos de instrucción interna) y notificaciones asíncronas.
8. **M08 (Resoluciones Administrativas y Firma Digital):** Numeración atómica pesimista anual (`RES-YYYY-XXXXX`), integración con `FirmaDigitalPort` y política *fail-closed*.
9. **M09 (Licencias Oficiales):** Generación de licencia oficial (`LIC-YYYY-XXXXX`), marcas vectoriales de agua, doble marco ornamental y notas del Art. 3° Ley 28976.
10. **M10 (Verificación QR y Consulta Móvil):** Token QR aleatorio dinámico, vista pública minimizada (Ley 29733) y vista autenticada para inspectores con mapa interactivo Leaflet.

### 2.2. Módulos Parciales
1. **M11 (Reclamaciones Administrativas):**
   - *Backend:* Implementado al 100% (entidad `ReclamacionExpediente`, DTOs `ReclamacionExpedienteDto`, `ResolverReclamacionDto`, servicio `ReclamacionService`, repositorio y pruebas unitarias).
   - *Frontend:* Pendiente de incorporar formulario dedicado en `portal-ciudadano.html` para la presentación directa de reclamaciones por el administrado.
2. **M12 (Auditoría Inmutable y Seguridad Integral):**
   - *Backend:* Implementado al 100% (entidad `AuditoriaExpediente` inmutable *append-only*, `AuditoriaService`, `SecurityConfig` de producción, filtros JWT y control IDOR/BOLA mediante `@PreAuthorize`).
   - *Frontend:* Pendiente de crear una consola gráfica especializada en `portal-interno.html` para la consulta visual de trazas de auditoría por funcionarios de TI.

---

## 3. Brechas de Integración Detectadas en Fase A (I10)

Durante la auditoría visual y de código de la Fase A se identificaron las siguientes brechas operativas:

1. **Desconexión del Portal Ciudadano respecto a Borradores V2:**
   - El script `portal-ciudadano.js` y el formulario `formato-anexo1.js` todavía apuntan al endpoint legado directo `POST /api/expedientes`.
   - No consumen la API de Borradores V2 (`POST /api/borradores` y `POST /api/borradores/{id}/presentar`).
   - *Impacto:* Aunque el backend soporta plenamente el autoguardado en borrador y la presentación por checklist, el portal web público aún crea expedientes directamente en un solo paso.
2. **Visualización de Estados en Portal Interno:**
   - La tabla principal de expedientes en `portal-interno.js` fue actualizada para mostrar badges de los 10 estados de V2, pero requiere validación en vivo en navegador durante la Fase B.
3. **Mapeo de Coordenadas en Verificación de Inspectores:**
   - La interfaz `verificar-licencia.html` incluye la lógica de integración con Leaflet 1.9.4 para inspectores autenticados, pero depende de que el establecimiento cuente con latitud y longitud válidas en base de datos.

---

## 4. Bloqueos y Requisitos Pendientes para Producción

El sistema **NO se encuentra apto para despliegue en producción** debido a los siguientes bloqueos institucionales no resueltos:

1. **Falta de Proveedor Real de Firma Digital Acreditado (IOFE):**
   - Actualmente opera con el adaptador de prueba `FirmaDigitalAdapterService` (simulación controlada en entorno de tests).
   - Para producción, la Municipalidad Provincial de Huamanga debe suscribir convenio o contratar un servicio con RENIEC, ONPE o entidad acreditada ante INDECOPI, implementando las credenciales y certificados PKI correspondientes.
2. **Procedimientos Manuales Institucionales (ADR-06):**
   - La validación de pagos depende de operadores de caja en el SAT Huamanga. No existe un webhook ni API institucional del SAT activa.
   - Las inspecciones ITSE de Defensa Civil dependen de la presentación presencial física de los Anexos 3 y 4.
3. **Secretos y Credenciales de Producción:**
   - Los archivos de inicialización en `docker/postgres/init/` y `UsuarioDataInitializer.java` contienen usuarios y contraseñas de demostración (`admin123`, `eval123`, `caja123`, `insp123`, `ciud123`).
   - La clave `jwt.secret` en `application.yml` es un valor predeterminado de desarrollo que debe sustituirse por una variable de entorno segura (`JWT_SECRET`) antes de cualquier puesta en marcha real.

---

## 5. Resumen de Pruebas y Evidencias Disponibles

- **Total de Pruebas Automatizadas:** 301
- **Fallos:** 0
- **Errores:** 0
- **Omitidas (Skipped):** 0
- **Tiempo de Ejecución:** ~32 segundos
- **Herramienta de Verificación:** Maven Surefire Plugin 3.2.5 sobre Java 21 LTS y H2 in-memory.
- **Detalle de Evidencias:** Registradas formalmente en [`docs/v2/PRUEBAS_Y_VALIDACION.md`](PRUEBAS_Y_VALIDACION.md).
