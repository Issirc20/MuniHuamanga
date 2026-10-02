# Entrega Técnica Consolidada — Fase 04 (Completa)
### Sistema de Gestión Documentaria de Licencias de Funcionamiento — Municipalidad Provincial de Huamanga
**Marco Normativo:** Ley N° 28976 (TUO D.S. N° 046-2017-PCM)  
**Fecha:** 2026-10-02  
**Estado:** 100% COMPLETADO (80/80 tests en verde, 0 fallos)

---

## 1. Resumen Ejecutivo de la Fase 04

La **Fase 04** consolida el pase a producción con estándares de seguridad gubernamental, identidad visual municipal idéntica a los formatos físicos oficiales, comunicación transparente con el administrado y autonomía operativa para la administración de tasas:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        FASE 04: CUATRO PILARES                         │
├──────────────────┬──────────────────┬──────────────────┬───────────────┤
│   Sprint 4-A     │   Sprint 4-B     │   Sprint 4-C     │  Sprint 4-D   │
│   PDF Oficial    │   Notificaciones │  Autenticación   │  Tarifario    │
│   Municipal      │   Email Async    │  JWT + RBAC      │  TUPA Dinámico│
│                  │                  │                  │               │
│ • Réplica física │ • @Async Spring  │ • Spring Sec 6   │ • En caliente │
│ • Escudo y Marco │ • JavaMailSender │ • JJWT 0.12.6    │   (sin deploy)│
│ • QR ZXing       │ • Thymeleaf HTML │ • ADMIN, EVAL,   │ • Fallback    │
│ • 6 Indicaciones │ • Adjuntos PDF   │   CAJERO         │   a YAML      │
└──────────────────┴──────────────────┴──────────────────┴───────────────┘
```

---

## 2. Detalle por Sprint y Componentes Implementados

### 2.1 Sprint 4-A: Generador Oficial PDF de Licencia de Funcionamiento
* **Generador Dedicado:** [`LicenciaPdfGenerator.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/service/LicenciaPdfGenerator.java).
* **Fidelidad al Formato Físico (Licencias N° 005023 y N° 000286):**
  - Doble borde ornamental perimetral (azul institucional `#1A365D` y dorado `#C69214`).
  - Escudo oficial de Huamanga y franjas decorativas con bandera.
  - Marca de agua central tenue con el escudo provincial.
  - Recuadro rojo superior con número oficial de correlativo municipal: `Nº XXXXXX`.
  - Filas compuestas con RUC, Categoría de Establecimiento, Área, Horario de Atención y Vence (`**/**/****` para indeterminadas según Ley 28976).
  - Bloque normativo de 6 indicaciones legales actualizadas.
  - Código QR de alta resolución generado mediante ZXing (`250x250 px`) apuntando al endpoint público municipal.
* **Documentación:** [`docs/entrega-fase-04.md`](file:///d:/ArqSoftware/MuniHuamanga/docs/entrega-fase-04.md).

### 2.2 Sprint 4-B: Notificaciones Electrónicas al Ciudadano
* **Servicio:** [`NotificacionEmailService.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/service/NotificacionEmailService.java).
* **Configuración Asíncrona:** [`AsyncConfig.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/config/AsyncConfig.java) con thread pool `emailExecutor` dedicado.
* **Eventos de Disparo Automático:**
  1. **Registro:** Envío inmediato al administrado con su número de expediente (`EXP-2026-XXXXX`), número de trámite y código de seguimiento.
  2. **Aprobación:** Envío de felicitación con el **Certificado Oficial PDF adjunto** en memoria sin recargar el disco.
  3. **Rechazo:** Notificación formal motivada con las causales normativas del dictamen.
* **Plantillas Responsive:** `email-registro.html`, `email-aprobacion.html` y `email-rechazo.html` con paleta institucional.
* **Documentación:** [`docs/entrega-fase-04-sprint4b.md`](file:///d:/ArqSoftware/MuniHuamanga/docs/entrega-fase-04-sprint4b.md).

### 2.3 Sprint 4-C: Autenticación JWT y Spring Security 6 (RBAC)
* **Seguridad sin Estado:** [`SecurityConfig.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/config/SecurityConfig.java), [`JwtAuthenticationFilter.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/JwtAuthenticationFilter.java), [`JwtTokenProvider.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/security/JwtTokenProvider.java).
* **Matriz de Control de Acceso (RBAC):**
  - **Público (Sin Token):** Mesa de partes ciudadana (`POST /api/expedientes`), seguimiento (`/api/expedientes/tramite/**`), verificación QR (`/api/public/**`), descarga de formatos públicos (`/documentos/**`).
  - `ROLE_CAJERO`: Validación y conciliación de vouchers SAT (`PUT /api/expedientes/{id}/pago`).
  - `ROLE_EVALUADOR`: Evaluación ITSE, dictamen técnico, aprobación y rechazo de licencias.
  - `ROLE_ADMIN`: Control total, auditoría completa y mantenimiento del tarifario TUPA.
* **Frontend Seguro:** Login modal y cliente `fetchConAuth` en [`portal-interno.js`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/resources/static/js/portal-interno.js) con almacenamiento seguro de token y cierre de sesión.
* **Documentación:** [`docs/entrega-fase-04-sprint4c.md`](file:///d:/ArqSoftware/MuniHuamanga/docs/entrega-fase-04-sprint4c.md).

### 2.4 Sprint 4-D: Mantenimiento CRUD del Tarifario TUPA (RNF-17)
* **Entidad y Persistencia:** [`TarifaTupa.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/model/TarifaTupa.java) y [`TarifaTupaRepository.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/repository/TarifaTupaRepository.java).
* **Cálculo de Tasa Dinámica:** [`CalculadoraDeTasa.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/service/CalculadoraDeTasa.java) consulta BD en caliente y dispone de fallback automático a los valores de `application.yml` ante caídas de BD.
* **Semilla Oficial:**
  - *Riesgo Bajo:* S/. 154.50
  - *Riesgo Medio:* S/. 218.00
  - *Riesgo Alto:* S/. 345.20
  - *Riesgo Muy Alto:* S/. 480.00
* **API REST:** `GET /api/tupa/tarifas` (público/interno) y `PUT /api/tupa/tarifas/{id}` (`@PreAuthorize("hasRole('ADMIN')")`).
* **UI Administrativa:** Modal con tabla interactiva de edición y actualización de montos en tiempo real en el Portal Interno.
* **Documentación:** [`docs/entrega-fase-04-sprint4d.md`](file:///d:/ArqSoftware/MuniHuamanga/docs/entrega-fase-04-sprint4d.md).

---

## 3. Estado de la Batería de Pruebas Automatizadas

```bash
mvn clean test
```
Resultados de ejecución integral:
- **`servicio-expedientes`**: 67 tests passing (Unitarios, Integración MockMvc, Seguridad JWT, Concurrencia 150 usuarios, Generador PDF, TUPA Service/Controller).
- **`servicio-formularios`**: 8 tests passing.
- **`adaptador-integracion`**: 4 tests passing.
- **`servicio-verificacion-licencias`**: 1 test passing.
- **Total:** **80 tests ejecutados**, **0 fallos**, **0 errores**, **BUILD SUCCESS**.

---

## 4. Guía de Ejecución Rápida

1. **Compilar e instalar dependencias:**
   ```bash
   mvn clean install -DskipTests
   ```
2. **Ejecutar suite completa de tests:**
   ```bash
   mvn test
   ```
3. **Iniciar el servidor localmente:**
   ```bash
   mvn spring-boot:run -pl servicio-expedientes -Dspring-boot.run.profiles=local
   ```
4. **Acceder a los portales:**
   - **Mesa de Partes Ciudadana:** `http://localhost:8081/index.html`
   - **Portal Interno Municipal:** `http://localhost:8081/portal-interno.html`
   - **Verificación Pública de Licencias:** `http://localhost:8081/verificacion.html`
   - **Credenciales semilla:**
     - Admin: `admin` / `admin123`
     - Evaluador: `evaluador` / `eval123`
     - Cajero: `cajero` / `caja123`
