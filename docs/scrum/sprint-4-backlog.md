# Sprint 4 Backlog — Integración Fase 2, Métricas Prometheus y Pruebas de Carga

**Objetivo del Sprint:**  
Completar el desacoplamiento de la Fase 2 mediante el patrón Puertos y Adaptadores (Hexagonal Architecture) para SAT, Defensa Civil, Edificaciones y Fiscalización; instrumentar observabilidad y métricas de rendimiento en tiempo real con Spring Boot Actuator y Micrometer Prometheus (RNF-02); y validar empíricamente la capacidad de atención concurrente de $\ge 150$ usuarios simultáneos con latencia $< 3$ segundos (RNF-01, RNF-02).

---

## 1. Historias de Usuario Abordadas

- **US-13:** Adaptador de Integración para Fase 2 (Ports & Adapters) (8 SP) — **Completada**
- **US-14:** Monitoreo y Métricas de Rendimiento con Micrometer Prometheus (5 SP) — **Completada**
- **US-15:** Validación de Capacidad de Concurrencia $\ge 150$ Usuarios Concurrentes (5 SP) — **Completada**

**Total Story Points del Sprint 4:** 18 SP  
**Story Points Totales del Proyecto:** 78 SP (100% Completado)

---

## 2. Tareas Ejecutadas en el Sprint 4

| Tarea | Estado | Entregable |
|---|:---:|---|
| **T-4.1:** Definir puertos de dominio (`SatPort`, `DefensaCivilPort`, `EdificacionesPort`, `FiscalizacionPort`) bajo Arquitectura Hexagonal | Completado | `pe.gob.munihuamanga.licencias.adaptador.port.*` |
| **T-4.2:** Implementar servicios adaptadores con stubs de integración para SAT (tasas/deudas), Defensa Civil (ITSE), Edificaciones (Zonificación) y Fiscalización (Actas) | Completado | `pe.gob.munihuamanga.licencias.adaptador.service.*` |
| **T-4.3:** Exponer API REST del Adaptador de Integración en el puerto `:8084` con DTOs de simulación | Completado | `AdaptadorController.java` |
| **T-4.4:** Añadir dependencia `micrometer-registry-prometheus` y configurar métricas personalizadas en `servicio-expedientes` | Completado | `pom.xml`, `MetricasExpedienteService.java` |
| **T-4.5:** Instrumentar contadores y temporizadores (`muni.expedientes.creados`, `muni.expedientes.aprobados`, `muni.licencias.consultas.publicas`, `muni.expedientes.tiempo.atencion`) | Completado | `ExpedienteService.java`, `MetricasExpedienteService.java` |
| **T-4.6:** Desarrollar prueba unitaria multihilo en JUnit (`Concurrencia150UsuariosTest`) simulando 150 hilos concurrentes | Completado | `Concurrencia150UsuariosTest.java` |
| **T-4.7:** Desarrollar script automatizado de prueba de carga concurrente HTTP para PowerShell (`run-load-test-150.ps1`) | Completado | `tests/load-test/run-load-test-150.ps1` |
| **T-4.8:** Crear script de carga para k6 (`k6-load-test.js`) con 150 usuarios virtuales (VUs) y umbrales RNF-01/02 | Completado | `tests/load-test/k6-load-test.js` |
| **T-4.9:** Corregir sintaxis de etiquetas en flechas Mermaid de `c4-model.md` para renderizado compatible en GitHub | Completado | `docs/arquitectura/c4-model.md` |

---

## 3. Criterios de Aceptación Cumplidos

1. **Aislamiento Arquitectónico Fase 2 (RNF-19):** El núcleo de expedientes (Fase 1) no depende de implementaciones concretas de sistemas externos; toda comunicación futura se realiza a través de puertos definidos e interfaces en `adaptador-integracion`.
2. **Observabilidad Actuator & Prometheus (RNF-02):** El endpoint `/actuator/prometheus` expone métricas estándar JVM y contadores de negocio municipales (`muni_expedientes_*`, `muni_licencias_*`) listos para ser scrapeados por Prometheus / Grafana.
3. **Validación de Concurrencia (RNF-01, RNF-02):**
   - **Peticiones simultáneas:** 150 solicitudes concurrentes ejecutadas con éxito (150/150 HTTP 200).
   - **Tasa de error:** 0.00% (umbral $< 1\%$).
   - **Latencia media observada:** 63.33 ms (umbral contractual $< 3,000$ ms).
   - **Throughput medido:** $> 2,300$ req/seg.
4. **Visualización en GitHub:** Todos los diagramas de arquitectura C4 (Contexto, Contenedores, Componentes Nivel 3 y Diagrama de Clases) renderizan sin errores en GitHub Flavored Markdown.
