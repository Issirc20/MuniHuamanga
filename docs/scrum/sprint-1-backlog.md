# Sprint 1 Backlog — Mesa de Partes Virtual y Flujo Integral del Expediente

**Objetivo del Sprint:**  
Digitalizar la captura de solicitudes en Mesa de Partes Virtual con validación legal rigurosa de identidad (DNI/RUC), implementar el flujo normativo de la Ley N° 28976 distinguiendo ITSE Previa e ITSE Posterior, auditoría inmutable de trazabilidad y proveer interfaces de usuario interactivas para administrados y funcionarios.

---

## 1. Historias de Usuario Abordadas

- **US-02:** Registro Digital de Solicitud en Mesa de Partes Virtual (8 SP) — **Completada**
- **US-03:** Máquina de Estados y Validador de Transiciones Normativas (8 SP) — **Completada**
- **US-04:** Auditoría Inmutable y Alertas de Vencimiento de Plazo Legal (5 SP) — **Completada**

**Total Story Points Completados:** 21 SP

---

## 2. Tareas Ejecutadas en el Sprint 1

| Tarea | Estado | Entregable |
|---|:---:|---|
| **T-1.1:** Incorporar validación regex legal de DNI (8 dígitos) y RUC (11 dígitos, inicia en 10 o 20) | Completado | `CrearExpedienteDto.java` |
| **T-1.2:** Crear suite de pruebas de validación de identidad peruana | Completado | `MesaPartesValidationTest.java` |
| **T-1.3:** Implementar cálculo de tipo de inspección ITSE (Previa vs. Posterior) según nivel de riesgo | Completado | `Expediente.java`, `ExpedienteMapper.java` |
| **T-1.4:** Agregar validación precondición de pago y riesgo antes de la aprobación final | Completado | `EstadoExpedienteValidator.java` |
| **T-1.5:** Incorporar filtrado de bandeja por estado y alerta de vencimiento (≤ 3 días) | Completado | `ExpedienteService.java`, `ExpedienteController.java` |
| **T-1.6:** Construir interfaz de usuario del Portal Ciudadano (Mesa de Partes & Seguimiento) | Completado | `portal-ciudadano.html`, `portal-ciudadano.js` |
| **T-1.7:** Construir interfaz de usuario del Sistema Interno (Bandeja, KPIs y modales de dictamen) | Completado | `portal-interno.html`, `portal-interno.js` |
| **T-1.8:** Diseñar página de inicio y sistema de diseño visual corporativo MuniHuamanga | Completado | `index.html`, `css/styles.css` |
| **T-1.9:** Pruebas unitarias de las reglas de negocio y aprobación | Completado | `ExpedienteServiceTest.java` |

---

## 3. Criterios de Aceptación Cumplidos

1. El sistema rechaza documentos de identidad que no cumplan el estándar de DNI (8 dígitos) o RUC (11 dígitos iniciado con 10 o 20).
2. El plazo legal se calcula sumando estrictamente 15 días hábiles (excluyendo sábados y domingos).
3. No es posible aprobar una solicitud si el voucher no ha sido cancelado ante el SAT o si falta dictamen de Defensa Civil.
4. El administrado puede consultar su trámite en el Portal Ciudadano y ver la línea temporal visual de su expediente.
5. Los funcionarios disponen de una bandeja con KPIs en tiempo real y alertas preventivas de vencimiento.
