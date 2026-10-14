# Product Backlog — Sistema de Gestión Documentaria de Licencias de Funcionamiento
### Municipalidad Provincial de Huamanga (MuniHuamanga V2)
**Marco Legal:** Ley N° 28976 (TUO D.S. N° 046-2017-PCM) / Ley N° 27269 / Ley N° 29733
**Metodología:** Ágil (Scrum) | **Ciclos:** Sprints de 2 semanas
**Estado:** Actualizado a MuniHuamanga V2 (Corte I10 Fase A)

---

## 1. Estructura de Épicas V2

| Épica | Nombre | Descripción |
|---|---|---|
| **EP-01** | **Mesa de Partes Virtual y Cuentas** | Registro de cuentas ciudadanas, establecimientos y captura de solicitudes de licencias. |
| **EP-02** | **Borradores y Máquina de Estados V2** | Autoguardado de borradores, máquina de 10 estados finita y auditoría cronológica inmutable. |
| **EP-03** | **Formatos Oficiales y Firma Digital** | Renderizado in-memory de Anexos 1, 3, 4 y resoluciones con firma digital IOFE (*fail-closed*). |
| **EP-04** | **Tasas TUPA y Recaudación SAT** | Cálculo automático, mantenimiento en caliente de tasas y validación manual de pagos en SAT. |
| **EP-05** | **Zonificación PDU y Rectificación** | Evaluación de compatibilidad de zonificación urbana y rectificación georreferenciada de locales. |
| **EP-06** | **Resolución, Licencia y QR Diferenciado** | Emisión atómica de resoluciones y licencias con QR dinámico minimizado y vista móvil para inspectores. |
| **EP-07** | **Seguridad, Reclamaciones e Integración** | Control RBAC con protección IDOR/BOLA, gestión de reclamaciones y notificaciones por correo. |

---

## 2. Historias de Usuario Priorizadas y Estado Real de Entrega

> [!NOTE]
> **Convención de Estado de Entrega:**
> - **DONE (Completo):** Backend implementado, frontend integrado y verificado con pruebas automatizadas.
> - **IN_PROGRESS (Parcial):** Backend implementado y verificado con pruebas; integración de interfaz gráfica pendiente o en borrador.
> - **PENDING:** Planificado para fases posteriores.

| ID | Historia de Usuario | Relación RF | Backend | Frontend | Aceptación Verificada | Estado Real |
|---|---|:---:|:---:|:---:|:---:|:---:|
| **US-01** | **Setup Arquitectura Monolito Modular y Docker** | RNF-09, RNF-17 | ✅ Implementado | ✅ Integrado | ✅ 301 tests Maven | **DONE** |
| **US-02** | **Registro Digital de Solicitud (Wizard 3 Pasos)** | RF-01 | ✅ Implementado | ✅ Integrado | ✅ Verificado MockMvc | **DONE** |
| **US-03** | **Máquina de 10 Estados y Validador** | RF-03 | ✅ Implementado | ✅ Integrado | ✅ Tests Unitarios | **DONE** |
| **US-04** | **Auditoría Inmutable de Transiciones** | RF-15, RNF-08 | ✅ Implementado | 🟡 Parcial (API lista) | ✅ Tests de Auditoría | **IN_PROGRESS** |
| **US-05** | **Renderizado de Anexos 1, 3 y 4 en PDF** | RF-07 | ✅ Implementado | ✅ Integrado | ✅ Tests OpenPDF | **DONE** |
| **US-06** | **Cálculo de Tasa según Nivel de Riesgo ITSE** | RF-05, RF-09 | ✅ Implementado | ✅ Integrado | ✅ Tests Calculadora | **DONE** |
| **US-07** | **Generación de Voucher de Pago SAT** | RF-04 | ✅ Implementado | ✅ Integrado | ✅ Tests ZXing Barcode | **DONE** |
| **US-08** | **Validación Manual de Pagos SAT** | RF-04 | ✅ Implementado | ✅ Integrado | ✅ Tests PagoController | **DONE** |
| **US-09** | **Acto Resolutivo y Licencia Oficial con Correlativo Anual** | RF-11, RF-12 | ✅ Implementado | ✅ Integrado | ✅ Tests Concurrencia | **DONE** |
| **US-10** | **Generación de Código QR Dinámico No Enumerable** | RF-13 | ✅ Implementado | ✅ Integrado | ✅ Tests ZXing QR | **DONE** |
| **US-11** | **Verificación Pública QR Minimizada (Ley 29733)** | RF-13, RNF-07 | ✅ Implementado | ✅ Integrado | ✅ Tests Verificación | **DONE** |
| **US-12** | **Seguimiento Ciudadano en Línea** | RF-03 | ✅ Implementado | ✅ Integrado | ✅ Tests Trámite | **DONE** |
| **US-13** | **Puertos y Adaptadores (Firma, SAT, DC, PDU)** | RNF-09 | ✅ Implementado | ✅ Integrado | ✅ Tests Adaptadores | **DONE** |
| **US-14** | **Métricas Actuator y Health Checks** | RNF-14 | ✅ Implementado | ✅ Integrado | ✅ Tests Actuator | **DONE** |
| **US-15** | **Pruebas de Carga (150 usuarios concurrentes)** | RNF-02 | ✅ Implementado | N/A | ✅ ConcurrenciaTest | **DONE** |
| **US-16** | **Gestión de Cuentas y Establecimientos Desacoplados** | RF-01 | ✅ Implementado | 🟡 Parcial | ✅ Tests Cuentas | **IN_PROGRESS** |
| **US-17** | **Solicitudes Borrador con Autoguardado** | RF-02 | ✅ Implementado | 🟡 Parcial (API lista) | ✅ Tests Borradores | **IN_PROGRESS** |
| **US-18** | **Subsanación con Documentos Versionados SHA-256** | RF-08 | ✅ Implementado | ✅ Integrado | ✅ Tests Subsanación | **DONE** |
| **US-19** | **TUPA Dinámico en Caliente (sin redeploy)** | RF-09, RNF-10 | ✅ Implementado | ✅ Integrado | ✅ Tests TarifaTupa | **DONE** |
| **US-20** | **Zonificación PDU y Rectificación de Ubicación** | RF-05, RF-06 | ✅ Implementado | ✅ Integrado | ✅ Tests Zonificación | **DONE** |
| **US-21** | **Firma Digital IOFE con Política Fail-Closed** | RF-11 | ✅ Implementado | ✅ Integrado | ✅ Tests FirmaDigital | **DONE** |
| **US-22** | **Verificación Móvil con Mapa Leaflet para Inspectores** | RF-13, RNF-16 | ✅ Implementado | ✅ Integrado | ✅ Tests Inspector | **DONE** |
| **US-23** | **Reclamaciones Administrativas Ciudadanas** | RF-14 | ✅ Implementado | ❌ Pendiente UI | ✅ Tests Reclamación | **IN_PROGRESS** |
| **US-24** | **Autorización Centralizada Backend (Anti IDOR/BOLA)** | RNF-05 | ✅ Implementado | ✅ Integrado | ✅ SecurityAuthTest | **DONE** |

---

## 3. Dependencias Críticas y Riesgos

1. **Dependencia Institucional de Firma Digital (IOFE / RENIEC):**
   - El sistema aplica política *fail-closed*. Si la municipalidad no suministra un servicio PKI activo, el flujo de emisión oficial queda detenido por diseño.
2. **Brecha de Integración Frontend en Borradores V2:**
   - La historia US-17 cuenta con backend probado al 100%, pero el formulario ciudadano (`portal-ciudadano.js`) aún no lo consume.
3. **Validación Operativa en Producción:**
   - Requiere sustitución obligatoria de credenciales y claves secretas de prueba antes del pase a producción.
