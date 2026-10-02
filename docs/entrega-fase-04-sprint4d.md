# Entrega Técnica — Fase 04 / Sprint 4-D: CRUD Tarifario TUPA (Gestión Dinámica de Tasas Municipales)

## Metadatos

| Campo            | Detalle                                                              |
|------------------|----------------------------------------------------------------------|
| **Fase**         | 04 — PDF Licencia Oficial + Email + JWT + CRUD TUPA                  |
| **Sprint**       | Sprint 4-D                                                           |
| **Fecha**        | 2026-10-02                                                           |
| **Responsable**  | Equipo de Desarrollo — MuniHuamanga Licencias                        |
| **Build**        | `mvn test` → **BUILD SUCCESS (80 tests totales / 67 en servicio-expedientes)** |
| **Arquitectura** | Persistencia JPA + Fallback configurable + Control RBAC (ROLE_ADMIN) |

---

## 1. Objetivo del Sprint 4-D

Implementar el módulo de **Mantenimiento y Administración Dinámica del Tarifario TUPA** (Texto Único de Procedimientos Administrativos) de la Municipalidad Provincial de Huamanga, permitiendo a los administradores:
1. Consultar de forma centralizada las 4 tasas oficiales según nivel de riesgo ITSE (Bajo, Medio, Alto y Muy Alto).
2. Modificar dinámicamente los montos totales, desgloses tributarios (Derecho de Trámite Administrativo vs. Costo de Inspección Técnica ITSE), base legal y conceptos sin requerir un nuevo despliegue ni reinicio de la aplicación (cumplimiento estricto de **RNF-17**).
3. Integrar de forma reactiva la [`CalculadoraDeTasa.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/service/CalculadoraDeTasa.java) con la base de datos, garantizando fallback resiliente a `application.yml` en caso de contingencia.
4. Restringir la edición y mantenimiento exclusivamente a usuarios con rol `ROLE_ADMIN`, manteniendo la consulta abierta para evaluadores, cajeros y administrados.

---

## 2. Tasas Oficiales TUPA Huamanga (Valores Semilla Preconfigurados)

Cargadas automáticamente por [`TarifaTupaDataInitializer.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/config/TarifaTupaDataInitializer.java):

| Código TUPA | Nivel Riesgo ITSE | Monto Total | Trámite Base | Costo ITSE | Base Legal de Referencia |
|---|:---:|:---:|:---:|:---:|---|
| **TUPA-ITSE-01** | **BAJO** | **S/. 154.50** | S/. 45.00 | S/. 109.50 | Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM |
| **TUPA-ITSE-02** | **MEDIO** | **S/. 218.00** | S/. 45.00 | S/. 173.00 | Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM |
| **TUPA-ITSE-03** | **ALTO** | **S/. 345.20** | S/. 45.00 | S/. 300.20 | Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM |
| **TUPA-ITSE-04** | **MUY_ALTO** | **S/. 480.00** | S/. 45.00 | S/. 435.00 | Ordenanza Municipal N° 018-2024-MPH / D.S. N° 046-2017-PCM |

---

## 3. Arquitectura del Tarifario Dinámico

```
[ Petición HTTP ] ──► [ SecurityConfig (RBAC) ]
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
       GET (Público/Cajero/Eval)       PUT / PATCH (Solo ADMIN)
               │                               │
               ▼                               ▼
     [ TarifaTupaController ] ──────► [ TarifaTupaService ]
                                               │
                                               ▼
                                    [ TarifaTupaRepository ]
                                               │
                                               ▼
                                   [( Tabla tarifas_tupa )]
                                               ▲
                                               │ (Consulta dinámica en caliente)
                                    [ CalculadoraDeTasa ]
                                               │
                                               ▼
                                  [ Liquidación de Tasas / Vouchers ]
```

---

## 4. Componentes Desarrollados

### 4.1 Dominio y DTOs
* [`TarifaTupaDto.java`](file:///d:/ArqSoftware/MuniHuamanga/common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/dto/TarifaTupaDto.java): DTO con identificación, código, nivel de riesgo, montos, desglose y trazabilidad de modificación.
* [`ActualizarTarifaDto.java`](file:///d:/ArqSoftware/MuniHuamanga/common-domain/src/main/java/pe/gob/munihuamanga/licencias/common/dto/ActualizarTarifaDto.java): DTO validado con `@DecimalMin` para actualización de montos por el administrador.

### 4.2 Persistencia y Lógica de Negocio
* [`TarifaTupa.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/model/TarifaTupa.java): Entidad JPA con constraints de unicidad y auditoría.
* [`TarifaTupaRepository.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/repository/TarifaTupaRepository.java): Métodos de consulta por código TUPA y nivel de riesgo.
* [`TarifaTupaService.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/service/TarifaTupaService.java): Métodos transaccionales para listar, obtener, actualizar montos y alternar estado activo.
* [`CalculadoraDeTasa.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/service/CalculadoraDeTasa.java): Actualizada para consultar prioritariamente `TarifaTupaRepository` en caliente, manteniendo fallback transparente a `application.yml`.

### 4.3 Endpoints REST y Seguridad
* [`TarifaTupaController.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/java/pe/gob/munihuamanga/licencias/expedientes/controller/TarifaTupaController.java):
  * `GET /api/tupa/tarifas`: Consulta pública de tasas.
  * `GET /api/tupa/tarifas/{id}`: Detalle de tarifa.
  * `GET /api/tupa/tarifas/riesgo/{nivelRiesgo}`: Tarifa por nivel ITSE.
  * `PUT /api/tupa/tarifas/{id}`: Actualización (`@PreAuthorize("hasRole('ADMIN')")`).
  * `PATCH /api/tupa/tarifas/{id}/estado`: Activación/Desactivación (`@PreAuthorize("hasRole('ADMIN')")`).

### 4.4 Interfaz de Usuario
* En [`portal-interno.html`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/resources/static/portal-interno.html):
  * Botón interactivo **📋 Tarifario TUPA** en la barra superior de acciones.
  * Modal de Tarifario con tabla comparativa de los 4 niveles de riesgo.
  * Modal de edición de tasas con recálculo automático en tiempo real del costo ITSE (`Monto Total - Trámite Base`).
* En [`portal-interno.js`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/main/resources/static/js/portal-interno.js):
  * Detección automática del rol `ROLE_ADMIN` para activar el botón de edición o mostrar el modo lectura protegida.
  * Petición asíncrona `PUT` al backend con actualización inmediata de la interfaz.

---

## 5. Pruebas Automatizadas del Sprint 4-D

* [`TarifaTupaServiceTest.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/service/TarifaTupaServiceTest.java) (4 tests unitarios):
  * Listado completo de tarifas TUPA.
  * Búsqueda por nivel de riesgo ITSE.
  * Actualización de monto, desglose y trazabilidad de usuario modificador.
  * Manejo de excepciones ante IDs inexistentes.
* [`TarifaTupaControllerIntegrationTest.java`](file:///d:/ArqSoftware/MuniHuamanga/servicio-expedientes/src/test/java/pe/gob/munihuamanga/licencias/expedientes/controller/TarifaTupaControllerIntegrationTest.java) (4 tests de integración MockMvc):
  * `GET /api/tupa/tarifas` responde HTTP 200 sin autenticación previa.
  * `PUT /api/tupa/tarifas/{id}` con credenciales de **ADMIN** modifica la tasa con HTTP 200.
  * `PUT /api/tupa/tarifas/{id}` con credenciales de **EVALUADOR** es rechazado con **HTTP 403 Forbidden**.
  * `PUT /api/tupa/tarifas/{id}` sin token es rechazado con **HTTP 401 Unauthorized**.

---

## 6. Resumen de la Fase 04 Completa (100% Finalizada)

| Sprint | Entregable | Estado |
|--------|------------|:---:|
| **Sprint 4-A** | PDF Licencia formato oficial municipal (N° 005023 y 000286) | ✅ **Completado** |
| **Sprint 4-B** | Notificaciones email (JavaMail/SMTP + Thymeleaf) al ciudadano | ✅ **Completado** |
| **Sprint 4-C** | Autenticación JWT + Spring Security 6 con RBAC | ✅ **Completado** |
| **Sprint 4-D** | CRUD Tarifario TUPA (gestión dinámica de tasas desde portal interno) | ✅ **Completado** |
