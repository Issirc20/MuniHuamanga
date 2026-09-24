# Sprint 3 Backlog — Evaluación Técnica, Licencia Oficial con QR y Verificación Pública

**Objetivo del Sprint:**  
Completar el flujo de resolución de licencias de funcionamiento por parte de la Gerencia de Licencias, emisión del Certificado Oficial de Licencia de Funcionamiento en formato PDF con firma digital simulada y código QR estampado (ZXing), e implementación del Portal Público de Verificación de Licencias en Tiempo Real (RNF-20 / RNF-02).

---

## 1. Historias de Usuario Abordadas

- **US-09:** Dictamen y Aprobación de Licencia de Funcionamiento (8 SP) — **Completada**
- **US-10:** Generación de Código QR Oficial y Sellado Digital Criptográfico (5 SP) — **Completada**
- **US-11:** Portal Público de Verificación de Licencia RNF-20 (5 SP) — **Completada**
- **US-12:** Seguimiento Ciudadano en Línea Enriquecido (5 SP) — **Completada**

**Total Story Points del Sprint 3:** 23 SP  
**Story Points Acumulados del Proyecto:** 60 SP

---

## 2. Tareas Ejecutadas en el Sprint 3

| Tarea | Estado | Entregable |
|---|:---:|---|
| **T-3.1:** Implementar generador del Certificado Oficial de Licencia de Funcionamiento en PDF con membrete institucional, vigencia indeterminada y sello digital | Completado | `DocumentoPdfService.java`, `GeneradorDocumentoService.java` |
| **T-3.2:** Integrar generación de código QR de alta resolución con ZXing codificando URL pública de verificación | Completado | `DocumentoPdfService.java`, `QrGeneratorService.java` |
| **T-3.3:** Implementar estampado de firma digital institucional y cálculo de hash SHA-256 de integridad | Completado | `DocumentoPdfService.java` |
| **T-3.4:** Implementar API pública de solo lectura `/api/public/licencias/{codigo}` con respuesta $< 3$ segundos | Completado | `PublicLicenciasController.java`, `ExpedienteService.java` |
| **T-3.5:** Desarrollar el portal web público responsivo para móviles `verificar-licencia.html` para inspectores y vecinos | Completado | `verificar-licencia.html`, `js/verificar-licencia.js` |
| **T-3.6:** Enriquecer el Portal Ciudadano para mostrar la tarjeta de licencia aprobada, QR y descargas | Completado | `portal-ciudadano.html`, `js/portal-ciudadano.js` |
| **T-3.7:** Habilitar en el Sistema Interno la descarga de licencia PDF y consulta de QR para funcionarios | Completado | `portal-interno.js` |
| **T-3.8:** Desarrollar suite de pruebas automatizadas para licencia PDF, QR y verificación pública | Completado | `LicenciaPdfServiceTest.java`, `VerificacionPublicaTest.java`, `QrGeneratorServiceTest.java` |

---

## 3. Criterios de Aceptación Cumplidos

1. La Licencia de Funcionamiento generada en PDF cuenta con escudo de Huamanga, datos completos del establecimiento, giro autorizado, vigencia indeterminada según Art. 11 de la Ley N° 28976, código QR incrustado y sellado digital criptográfico SHA-256.
2. Al escanear el código QR con cualquier lector o teléfono celular, redirige al portal público institucional `http://localhost:8081/verificar-licencia.html?codigo=LIC-AAAA-XXXXXXXX`.
3. El portal de verificación responde de forma inmediata confirmando el estado "VIGENTE / AUTORIZADA" o alertando en caso de códigos no válidos.
4. El administrado puede descargar directamente su Certificado Oficial de Licencia desde el Portal Ciudadano tras la aprobación.
