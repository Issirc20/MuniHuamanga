# Sprint 2 Backlog — Formatos PDF Oficiales, Tasas TUPA y Vouchers SAT

**Objetivo del Sprint:**  
Generar documentos oficiales en formato PDF con OpenPDF y ZXing según la Ley N° 28976 y el D.S. N° 046-2017-PCM (Declaración Jurada Anexo 1, Solicitud ITSE Defensa Civil y Orden de Pago SAT con código de barras Code 128), liquidación desglosada de tasas TUPA y habilitación de descarga directa desde la plataforma web.

---

## 1. Historias de Usuario Abordadas

- **US-05:** Generación de Formato de Declaración Jurada y Solicitud ITSE (5 SP) — **Completada**
- **US-06:** Cálculo y Desglose de Tasa según Nivel de Riesgo ITSE (5 SP) — **Completada**
- **US-07:** Generación de Voucher de Pago SAT con Código de Barras (5 SP) — **Completada**
- **US-08:** Registro y Conciliación de Pago (5 SP) — **Completada**

**Total Story Points Completados:** 20 SP

---

## 2. Tareas Ejecutadas en el Sprint 2

| Tarea | Estado | Entregable |
|---|:---:|---|
| **T-2.1:** Implementar generador del Anexo 1 oficial (Declaración Jurada) con condiciones de zonificación y seguridad | Completado | `GeneradorDocumentoService.java`, `DocumentoPdfService.java` |
| **T-2.2:** Implementar generador de Solicitud ITSE para Defensa Civil con modalidad Previa/Posterior | Completado | `GeneradorDocumentoService.java` |
| **T-2.3:** Integrar generación de código de barras Code 128 con ZXing en la orden de pago SAT | Completado | `GeneradorDocumentoService.java`, `DocumentoPdfService.java` |
| **T-2.4:** Implementar desglose de conceptos tributarios municipales (Trámite + ITSE) | Completado | `CalculadoraDeTasa.java`, `CalculadoraDeTasaDesgloseTest.java` |
| **T-2.5:** Exponer endpoints REST de descarga directa de PDF en `servicio-expedientes` y `servicio-formularios` | Completado | `ExpedienteController.java`, `FormulariosController.java` |
| **T-2.6:** Integrar botones de descarga en Portal Ciudadano y Bandeja Interna | Completado | `portal-ciudadano.html`, `portal-interno.js` |
| **T-2.7:** Desarrollar suite de pruebas automatizadas de validación de flujos de bytes PDF (%PDF-) | Completado | `GeneradorDocumentoServiceTest.java` |

---

## 3. Criterios de Aceptación Cumplidos

1. La Declaración Jurada generada contiene el membrete municipal de Huamanga, datos completos del establecimiento, condiciones de zonificación y pie de firma digital/manuscrita.
2. La orden de pago SAT incluye el identificador `VCH-AAAA-XXXXXX`, código de barras legible ópticamente y desglose del costo total.
3. Los administrados pueden descargar sus documentos directamente en PDF desde el Portal Ciudadano sin fricciones.
4. Los funcionarios pueden descargar los comprobantes tributarios desde su bandeja interna.
