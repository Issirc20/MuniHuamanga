# Entrega Técnica — Fase 04 / Sprint 4-B: Notificaciones por Correo Electrónico (JavaMail/SMTP + Thymeleaf)

## Metadatos

| Campo            | Detalle                                                              |
|------------------|----------------------------------------------------------------------|
| **Fase**         | 04 — PDF Licencia Oficial + Email + JWT + CRUD TUPA                  |
| **Sprint**       | Sprint 4-B                                                           |
| **Fecha**        | 2026-10-02                                                           |
| **Responsable**  | Equipo de Desarrollo — MuniHuamanga Licencias                        |
| **Build**        | `mvn test -pl servicio-expedientes` → **BUILD SUCCESS (47 tests)**    |
| **Commit**       | `6db4268 feat(fase-04/sprint4b): notificaciones por correo electronico (JavaMail/SMTP + Thymeleaf)` |

---

## 1. Objetivo del Sprint 4-B

Implementar el subsistema de **Notificaciones Electrónicas al Ciudadano** mediante **Spring Boot Starter Mail (JavaMail/SMTP)** y plantillas responsivas en **Thymeleaf**, manteniendo una arquitectura no bloqueante mediante ejecución asíncrona (`@Async`) y garantizando el cumplimiento de la **Ley N° 27444** (LPAG - Notificación Electrónica) y la **Ley N° 29733** (Protección de Datos Personales).

---

## 2. Arquitectura de Procesamiento Asíncrono

Para evitar la degradación de latencia en las peticiones HTTP del ciudadano y del evaluador municipal, el envío de correos se procesa en segundo plano a través de un pool de hilos dedicado configurado en `AsyncConfig.java`:

```
[ Petición HTTP ] ──────► [ ExpedienteService ]
                                 │
                     (Guarda / Aprueba / Rechaza)
                                 │
                                 ▼
                     [ NotificacionEmailService ]
                                 │
                     (@Async "emailExecutor")
                                 │
                                 ▼
                    ┌─────────────────────────┐
                    │ ThreadPoolTaskExecutor  │
                    │ Core: 2 | Max: 5        │
                    │ Queue: 50 hilos         │
                    └────────────┬────────────┘
                                 │
                                 ▼
                     [ Servidor SMTP / Gmail ] ──────► [ Bandeja Ciudadano ]
```

### Componente `AsyncConfig.java`
* `@EnableAsync` habilitado a nivel de contexto de Spring.
* Executor `emailExecutor` con prefijo de hilo `EmailThread-`.
* Captura de excepciones silenciosa para que una eventual caída del proveedor SMTP no interrumpa la transacción de negocio ni la respuesta HTTP al usuario.

---

## 3. Política de Envío Seguro (Filtro Triple)

El método `puedeEnviar(Expediente expediente)` evalúa obligatoriamente tres condiciones previas antes de intentar cualquier conexión SMTP:

1. **Flag global habilitado:** `${notificacion.email.enabled}` (`MAIL_ENABLED=true`). Por defecto es `false` en entornos locales/tests para evitar bloqueos por credenciales vacías.
2. **Dirección válida presente:** `expediente.getEmailNotificacion()` no nulo ni en blanco.
3. **Consentimiento explícito:** `expediente.isAutorizaNotificacion() == true`, respetando la autorización otorgada por el ciudadano en la Mesa de Partes Virtual.

---

## 4. Matriz de Eventos y Plantillas Thymeleaf

| Evento de Negocio | Disparador en `ExpedienteService` | Plantilla Thymeleaf | Asunto del Correo |
|---|---|---|---|
| **Registro de Solicitud** | `crearExpediente()` | `email/email-registro.html` | *Expediente N° {tramite} — Ingreso Recibido* |
| **Aprobación de Licencia** | `aprobar()` | `email/email-aprobacion.html` | *¡Su Licencia de Funcionamiento fue Aprobada! — Municipalidad de Huamanga* |
| **Rechazo / Observación** | `rechazar()` | `email/email-rechazo.html` | *Expediente N° {tramite} — Observado / Rechazado* |

### Contenido de las Plantillas
* **`email-registro.html`:** Informa número correlativo generado (`EXP-AAAA-XXXXX`), datos del establecimiento y giro, y plazo legal perentorio de 15 días hábiles conforme al D.S. N° 046-2017-PCM.
* **`email-aprobacion.html`:** Enhorabuena con número de Licencia, código QR embebido, enlace directo para descargar el certificado PDF oficial y enlace al Portal Público de Verificación.
* **`email-rechazo.html`:** Detalle del motivo técnico u objeción de la Gerencia, junto con la guía de 3 pasos para interponer subsanación en Mesa de Partes y teléfonos/canales de atención de la Subgerencia de Comercio y Licencias.

---

## 5. Configuración y Variables de Entorno

En `servicio-expedientes/src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000

notificacion:
  email:
    enabled: ${MAIL_ENABLED:false}
    remitente: ${MAIL_FROM:notificaciones@munihuamanga.gob.pe}
    nombre-remitente: "Municipalidad Provincial de Huamanga"
    portal-ciudadano: ${PORTAL_CIUDADANO_URL:http://localhost:8081}
    portal-verificacion: ${PORTAL_VERIFICACION_URL:http://localhost:8081/verificar-licencia.html?codigo=}
```

---

## 6. Cobertura de Pruebas Unitarias

La suite `NotificacionEmailServiceTest` valida exhaustivamente:
1. `testNotificarRegistro_Exitoso`: Envío correcto de MimeMessage con variables de contexto de registro.
2. `testNotificarAprobacion_Exitoso`: Envío correcto de correo con enlace de licencia y QR.
3. `testNotificarRechazo_Exitoso`: Envío con motivo de rechazo.
4. `testNoEnviar_CuandoEmailDeshabilitado`: Omisión limpia cuando `enabled=false`.
5. `testNoEnviar_CuandoSinCorreo`: Omisión limpia cuando el titular no consignó correo.
6. `testNoEnviar_CuandoNoAutoriza`: Omisión limpia cuando el titular desmarcó el consentimiento.
7. `testEnvioFalla_NoLanzaExcepcion`: Resiliencia ante excepciones `MailException` de transporte SMTP.

**Resultado:** 7/7 tests de email aprobados; **47/47 tests totales del microservicio en verde**.

---

## 7. Próximos Pasos — Fase 04 Restante

| Sprint | Entregable | Estado |
|--------|------------|--------|
| Sprint 4-A | PDF Licencia formato oficial municipal | ✅ Completado |
| Sprint 4-B | Notificaciones email (JavaMail/SMTP) al ciudadano | ✅ Completado |
| Sprint 4-C | Autenticación JWT + Spring Security | ⏳ Siguiente |
| Sprint 4-D | CRUD Tarifario TUPA (gestión de tasas desde portal interno) | ⏳ Pendiente |
