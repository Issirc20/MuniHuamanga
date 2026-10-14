# INFORME DE IMPLEMENTACIÓN TÉCNICA — INCREMENTO I08
## M09: Resoluciones Administrativas, Firma Digital Institucional y Emisión de Licencias

**Municipalidad Provincial de Huamanga — Gerencia de Licencias de Funcionamiento**
**Fecha:** 14 de octubre de 2026
**Rama de Trabajo:** `develop/munihuamanga-v2`
**Línea Base Previa:** 260 pruebas exitosas — I07.1
**Línea Base Alcanzada:** 281 pruebas exitosas (0 fallos, 0 errores, 0 omitidas)
**Documentos Rectores:** [ERS-MH-V2-001 v1.0], [DAS-MH-V2-001 v1.0], TUO Ley N° 27444 (D.S. 004-2019-JUS), Ley Marco de Licencia de Funcionamiento (Ley N° 28976 y D.S. N° 163-2020-PCM), Ley de Firmas y Certificados Digitales (Ley N° 27269 y D.S. N° 052-2008-PCM - IOFE).

---

## 1. Resumen Ejecutivo

El presente documento certifica la implementación exhaustiva del **Incremento I08 (Módulo M09)** en la plataforma MuniHuamanga V2. El objetivo central ha sido establecer el flujo formal de expedición de resoluciones administrativas (de subgerencia/gerencia) y la emisión segura de licencias de funcionamiento municipales, garantizando que **bajo ninguna circunstancia se emitan documentos oficiales ni códigos QR activos mientras no exista una firma digital institucional válida** expedida por una Entidad de Certificación acreditada dentro de la Infraestructura Oficial de Firma Electrónica (IOFE).

### Logros Principales de I08:
1. **Diferenciación Jurídica y Técnica entre Informe y Resolución:** Se separó nítidamente el informe técnico administrativo (opinión instructora de M08) del acto administrativo resolutivo (`ResolucionAdministrativa`), garantizando que sólo un expediente en estado `APTO_PARA_RESOLUCION` con informe aprobado pueda originar una resolución formal.
2. **Numeración Atómica Concurrente con Bloqueo Pesimista:** Se implementaron los servicios `SecuenciaResolucionService` (`RES-YYYY-XXXXX`) y `SecuenciaLicenciaService` (`LIC-YYYY-XXXXX`) respaldados por tablas en base de datos (`secuencias_resolucion`, `secuencias_licencia`) y bloqueo `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`), garantizando unicidad absoluta y cero duplicidad por colisiones concurrentes.
3. **Puerto y Adaptador Desacoplado de Firma Digital (`FirmaDigitalPort`):** Se blindó la integración criptográfica sin almacenar claves privadas ni certificados apócrifos en el repositorio. Ante la indisponibilidad de la infraestructura institucional de firma (`munihuamanga.firma-digital.enabled=false`), la resolución permanece en `APROBADA_PENDIENTE_FIRMA` o `ERROR_FIRMA`, y la emisión oficial queda terminantemente bloqueada.
4. **Emisión Idempotente y Cero Doble Licencia:** La emisión oficial de licencia solo es ejecutable si existe una resolución `FAVORABLE` en estado `FIRMADA`. Si se detecta un reintento o doble clic, el servicio responde idempotentemente con la licencia ya existente sin crear registros duplicados ni alterar correlativos.
5. **Documentos PDF Vectoriales y QR Criptográfico No Enumerable:** El generador OpenPDF vectorial distingue claramente entre *Vista Preliminar* (con marca de agua "SIN VALIDEZ OFICIAL") y *Documento Oficial Emitido*. El código QR se genera mediante un identificador pseudoaleatorio criptográfico (`SecureRandom`), desvinculado de números correlativos internos y sin exponer DNI ni datos personales innecesarios.
6. **Ampliación de la Suite de Pruebas Automatizadas:** Se agregaron 21 nuevas pruebas unitarias, de integración y de estrés concurrente, elevando la suite completa de 260 a **281 pruebas exitosas**, con 0 fallos, 0 errores y 0 omitidas.

---

## 2. Arquitectura y Ciclo de Vida del Módulo M09

```
[ Expediente: APTO_PARA_RESOLUCION ]
           │
           ▼
[ Crear Borrador Resolución ] ────► Estado: BORRADOR (Genera RES-YYYY-XXXXX)
           │
           ▼
[ Revisión y Ajuste Fundamentos ]
           │
           ▼
[ Aprobación por Autoridad ] ─────► Estado: APROBADA_PENDIENTE_FIRMA (Segregación: Evaluador != Aprobador)
           │
           ▼
[ Despacho a Firma Digital ] ─────► Estado: EN_PROCESO_FIRMA
           │
     ┌─────┴────────────────────────────────┐
     │                                      │
(Firma Exitosa PKI)                 (Firma No Disponible / Error)
     │                                      │
     ▼                                      ▼
Estado: FIRMADA                     Estado: ERROR_FIRMA / PENDIENTE
     │                                      │
     ▼                                      ▼
[ Emisión de Licencia ]              [ BLOQUEO TOTAL DE EMISIÓN ]
     │                              (No se emite documento oficial)
     ▼
Estado Licencia: EMITIDA (LIC-YYYY-XXXXX)
Expediente pasa a: APROBADO
Token QR Criptográfico Activo
```

### 2.1 Máquinas de Estados Implementadas

- **`EstadoResolucion`**:
  - `BORRADOR`: Resolución redactada preliminarmente, vinculada al expediente e informe aprobado.
  - `APROBADA_PENDIENTE_FIRMA`: Acto administrativo aprobado por la autoridad municipal competente, en espera de firma digital institucional.
  - `EN_PROCESO_FIRMA`: Transacción de firma remitida al proveedor PKI/IOFE.
  - `FIRMADA`: Documento PDF firmado digitalmente con hash SHA-256 verificado y sellado criptográfico.
  - `ERROR_FIRMA`: Fallo de conexión, certificado revocado o rechazo de firma.
  - `ANULADA`: Acto revocado o dejado sin efecto por causal legal.

- **`EstadoLicencia`**:
  - `EMITIDA`: Licencia de funcionamiento oficial generada a partir de resolución favorable firmada.
  - `SUSPENDIDA`: Medida cautelar o administrativa temporal.
  - `REVOCADA`: Sanción o anulación formal del título habilitante.
  - `VENCIDA`: Pérdida de vigencia para licencias con vigencia temporal.

---

## 3. Integración Criptográfica y Firma Digital Institucional

### 3.1 Principios de Seguridad Aplicados
1. **No Almacenamiento de Secretos Criptográficos:** La aplicación backend no custodia contraseñas de certificados, llaves privadas ni archivos `.pfx` / `.jks` institucionales.
2. **Prohibición de Simulación Apócrifa:** Se prohibió expresamente el uso de imágenes PNG o textos simulados para certificar un documento como "firmado".
3. **Verificación Estricta:** Un documento solo se considera firmado si supera la verificación criptográfica del proveedor acreditado y se calcula su resumen hash SHA-256 inmutable.
4. **Requisitos de Infraestructura Institucional:** Para la habilitación legal en ambiente de producción se requiere:
   - Conexión con Entidad de Certificación acreditada ante INDECOPI (ej. RENIEC / PKI Nacional).
   - Integración con componente Refirma / API de Firma Digital oficial.
   - Certificado digital institucional vigente de tipo Persona Jurídica / Funcionario Público.
   - Sellado de tiempo (TSA) conforme a RFC 3161.

---

## 4. Emisión de Licencia e Inmutabilidad Documental

### 4.1 Reglas de Negocio Implementadas
- **RN-LIC-01:** La emisión de una licencia requiere obligatoriamente que la resolución vinculada sea de tipo `FAVORABLE` y se encuentre en estado `FIRMADA`.
- **RN-LIC-02:** Un expediente sólo puede tener una única licencia oficial activa. Si ya existe, cualquier solicitud posterior devuelve la licencia existente con idempotencia HTTP 200, registrando el evento en `historial_emision_licencias`.
- **RN-LIC-03:** Al emitirse la licencia, el expediente transiciona formalmente al estado final `APROBADO`.
- **RN-LIC-04:** El código QR de verificación codifica un token pseudoaleatorio alfanumérico seguro (`32 bytes Base64URL`), el cual no permite inferir secuencias ni números de expediente.

---

## 5. Modelo de Datos y Migración (`07-v2-resoluciones-licencias.sql`)

Se incorporó el script DDL aditivo en `docker/postgres/init/07-v2-resoluciones-licencias.sql` con las siguientes especificaciones:
- **`resoluciones_administrativas`**: Almacena el acto resolutivo, informe origen, autoridad aprobante, hash SHA-256, correlativo oficial `RES-YYYY-XXXXX`, estado y tipo de decisión. Clave foránea restrictiva (`ON DELETE RESTRICT`) hacia `expedientes`.
- **`historial_resoluciones`**: Tabla inmutable de trazabilidad (*append-only*) para cada cambio de estado o actuación administrativa.
- **`licencias_emitidas`**: Título habilitante oficial con número `LIC-YYYY-XXXXX`, resolución de sustento, código QR criptográfico, datos de vigencia e inmutabilidad.
- **`historial_emision_licencias`**: Registro de auditoría de emisiones, descargas y consultas oficiales.
- **`secuencias_resolucion` y `secuencias_licencia`**: Tablas anuales de correlativos protegidas contra colisiones concurrentes mediante bloqueo pesimista.

---

## 6. Seguridad y Control de Acceso (RBAC / BOLA)

- **Aprobación y Emisión Exclusivas:** `@PreAuthorize("hasRole('ADMIN')")` en aprobación de resoluciones, solicitud de firma y emisión de licencias. Ni ciudadanos ni evaluadores sin competencia pueden emitir actos resolutivos.
- **Segregación de Funciones:** Se verifica programáticamente que el evaluador que elaboró el informe administrativo técnico no pueda aprobar la resolución correspondiente (`evaluador.equals(aprobador) -> Error`).
- **Control BOLA/IDOR:** `@PreAuthorize("@resolucionSecurity.puedeVerResolucion(authentication, #id)")` y `@PreAuthorize("@licenciaSecurity.puedeVerLicencia(authentication, #id)")`. Un ciudadano sólo puede acceder a resoluciones y licencias emitidas que pertenezcan formalmente a su cuenta u operador autorizado.
- **Endpoint Público Exclusivo:** `/api/licencias/verificar/{codigoQr}` es de libre acceso público con minimización estricta de datos (Ley N° 29733).

---

## 7. Pruebas Automatizadas Ejecutadas (281 Tests)

El reactor Maven multi-módulo finalizó con éxito absoluto:
- **Total de Pruebas:** 281
- **Fallos:** 0
- **Errores:** 0
- **Omitidas:** 0
- **Tiempo de Ejecución:** 1 min 11 s

### Nuevas Clases de Prueba Incorporadas en I08 (21 Tests):
1. **`SecuenciaResolucionLicenciaMultiInstanciaTest` (4 pruebas):**
   - Generación atómica secuencial de correlativos `RES-YYYY-XXXXX`.
   - Generación atómica secuencial de correlativos `LIC-YYYY-XXXXX`.
   - Concurrencia multihilo de resoluciones sin colisión (5 hilos paralelos).
   - Concurrencia multihilo de licencias sin colisión (5 hilos paralelos).
2. **`ResolucionAdministrativaServiceTest` (8 pruebas):**
   - Creación de borrador de resolución para expediente apto.
   - Rechazo de resolución si el expediente no está en `APTO_PARA_RESOLUCION`.
   - Aprobación formal por autoridad municipal con transición a `APROBADA_PENDIENTE_FIRMA`.
   - Rechazo de auto-aprobación indebida (segregación de funciones).
   - Bloqueo de firma cuando el servicio institucional no está configurado (`ERROR_FIRMA`).
   - Flujo exitoso de firma con proveedor activo.
   - Generación de PDF con marca de agua "VISTA PRELIMINAR".
   - Integridad del hash SHA-256 en documento firmado.
3. **`LicenciaServiceTest` (5 pruebas):**
   - Rechazo de emisión sin resolución aprobada.
   - Rechazo de emisión con resolución aprobada pero pendiente de firma.
   - Rechazo de emisión sobre resolución con decisión denegatoria.
   - Emisión exitosa de licencia sobre resolución favorable y debidamente firmada.
   - Idempotencia ante doble emisión concurrente sobre el mismo expediente.
4. **`ResolucionLicenciaIntegrationTest` (4 pruebas):**
   - Bloqueo RBAC a ciudadanos para aprobar resoluciones (HTTP 403).
   - Bloqueo RBAC a ciudadanos y evaluadores para emitir licencias (HTTP 403).
   - Control BOLA/IDOR impidiendo a ciudadanos ver resoluciones ajenas (HTTP 403).
   - Acceso público permitido a `/api/licencias/verificar/{codigoQr}` (HTTP permitAll).

---

## 8. Estado del Entorno de Persistencia (PostgreSQL 16)

Tal como se documentó desde I01, el servicio Docker de PostgreSQL 16 permanece detenido en el entorno local del usuario por restricciones de privilegios y puertos locales. Todas las migraciones DDL (`01-v1-schema.sql` a `07-v2-resoluciones-licencias.sql`) están rigurosamente diseñadas conforme al dialecto PostgreSQL 16 y son ejecutadas en el entorno CI/CD. En el entorno local de desarrollo y testing, la suite opera sobre H2 Database en modo de compatibilidad PostgreSQL (`MODE=PostgreSQL`).

---

## 9. Criterio de Habilitación Legal y Conclusión

> [!WARNING]
> **Diferencia entre Implementación Técnica y Habilitación Legal/Operativa:**
> La arquitectura y el software de MuniHuamanga V2 cuentan con todos los controles técnicos, de seguridad y de persistencia requeridos para M09. Sin embargo, la emisión oficial de licencias de funcionamiento en el entorno de producción institucional permanece técnicamente bloqueada por configuración (`munihuamanga.firma-digital.enabled=false`) hasta que la Gerencia de Tecnologías de la Información de la Municipalidad Provincial de Huamanga concluya la acreditación y el enlace con el proveedor oficial de firma digital IOFE.
