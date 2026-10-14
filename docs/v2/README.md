# MuniHuamanga V2 — Sistema de Gestión de Licencias de Funcionamiento

Municipalidad Provincial de Huamanga — Gerencia de Licencias  
**Rama de desarrollo:** `develop/munihuamanga-v2`  
**Fecha de inicialización:** 13 de octubre de 2026  
**Documentos rectores:** ERS-MH-V2-001 (v1.0) y DAS-MH-V2-001 (v1.0)

---

## 1. Propósito de esta Carpeta

La carpeta `docs/v2/` consolida los instrumentos técnicos, auditorías, matrices de brechas, planes de evolución e inventarios de decisiones arquitectónicas para la transformación de MuniHuamanga desde su versión inicial hacia **MuniHuamanga V2**.

Esta evolución se realiza **directamente sobre el repositorio existente**, sin sustituir el historial Git, preservando los módulos maduros y aplicando ingeniería incremental guiada por la Especificación de Requisitos de Software (ERS) y el Diseño de Arquitectura de Software (DAS).

---

## 2. Índice Documental V2

| Documento | Descripción | Estado |
| :--- | :--- | :--- |
| [AUDITORIA_INICIAL.md](file:///d:/ArqSoftware/MuniHuamanga/docs/v2/AUDITORIA_INICIAL.md) | Diagnóstico integral del código fuente, dependencias reales, suite de pruebas, seguridad y base de datos. | Aprobado / Línea Base |
| [MATRIZ_BRECHAS.md](file:///d:/ArqSoftware/MuniHuamanga/docs/v2/MATRIZ_BRECHAS.md) | Mapeo detallado de discrepancias frente a la ERS/DAS clasificado en: Conservar, Corregir, Refactorizar, Fusionar, Archivar, Eliminar e Implementar. | Aprobado / Línea Base |
| [PLAN_IMPLEMENTACION.md](file:///d:/ArqSoftware/MuniHuamanga/docs/v2/PLAN_IMPLEMENTACION.md) | Cronograma y hoja de ruta por incrementos pequeños (I01 a I10) para el despliegue seguro de V2. | Aprobado / Línea Base |
| [DECISIONES_ARQUITECTURA.md](file:///d:/ArqSoftware/MuniHuamanga/docs/v2/DECISIONES_ARQUITECTURA.md) | Registro de Decisiones de Arquitectura (ADR) que justifican los patrones técnicos y restricciones normativas. | Aprobado / Línea Base |
| [PRUEBAS_Y_VALIDACION.md](file:///d:/ArqSoftware/MuniHuamanga/docs/v2/PRUEBAS_Y_VALIDACION.md) | Estrategia de calidad, pruebas unitarias, de integración, seguridad (IDOR/BOLA), carga y criterios de aceptación. | Aprobado / Línea Base |

---

## 3. Estado del Repositorio al Inicio de V2

- **Compilación Maven:** `BUILD SUCCESS` (Java 21 / Spring Boot 3.3.4).
- **Pruebas Automatizadas Existentes:** 104 pruebas unitarias e integradas ejecutadas, **0 fallos, 0 errores**.
- **Topología:** Monolito Modular basado en Clean Architecture (`common-domain` y `servicio-expedientes`).
- **Frontend Existente:** Portal Ciudadano, Formato Oficial Anexo 1 interactivo, Portal Interno Municipal y Consulta Pública QR (HTML5, Vanilla CSS, JS).
