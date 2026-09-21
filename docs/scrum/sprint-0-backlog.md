# Sprint 0 Backlog — Setup del Entorno y Arquitectura Base

**Objetivo del Sprint:**  
Establecer los cimientos arquitecturales, la configuración del repositorio, la infraestructura de contenedores y los módulos iniciales para habilitar el desarrollo ágil en sprints posteriores.

---

## Tareas del Sprint 0

| Tarea | Estado | Responsable | Entregable |
|---|:---:|---|---|
| **T-01:** Inicializar repositorio Git y configurar `.gitignore` | Completado | DevOps / Arquitecto | `.gitignore`, `git init` |
| **T-02:** Diseñar `pom.xml` padre multi-módulo (Java 21, Spring Boot 3.3.4) | Completado | Arquitecto | `pom.xml` raíz |
| **T-03:** Implementar módulo `common-domain` con Enums y DTOs | Completado | Dev Backend | `common-domain/` |
| **T-04:** Implementar microservicio `servicio-expedientes` según C4 Nivel 3 | Completado | Dev Backend | `servicio-expedientes/` |
| **T-05:** Implementar microservicio `servicio-formularios` (PDF) | Completado | Dev Backend | `servicio-formularios/` |
| **T-06:** Implementar microservicio `servicio-verificacion-licencias` (QR ZXing) | Completado | Dev Backend | `servicio-verificacion-licencias/` |
| **T-07:** Implementar microservicio `adaptador-integracion` (Stubs Fase 2) | Completado | Dev Backend | `adaptador-integracion/` |
| **T-08:** Configurar `api-gateway` con Spring Cloud Gateway | Completado | Dev Backend | `api-gateway/` |
| **T-09:** Configurar `docker-compose.yml` y script DDL de PostgreSQL 15 | Completado | DevOps | `docker-compose.yml`, `01-init-databases.sql` |
| **T-10:** Crear suite de pruebas unitarias para `servicio-expedientes` | Completado | QA / Dev | `EstadoExpedienteValidatorTest`, `CalculadoraDeTasaTest`, `ExpedienteServiceTest` |
| **T-11:** Documentar C4 y marco legal | Completado | Arquitecto | `docs/` |

---

## Definición de Terminado (Definition of Done - DoD)
1. El proyecto compila sin errores en Maven (`mvn clean test`).
2. Todos los tests unitarios pasan exitosamente.
3. El archivo `docker-compose.yml` inicia los servicios de base de datos de manera reproducible.
4. El repositorio local de Git se encuentra listo para vincularse al repositorio remoto de GitHub (`main`).
