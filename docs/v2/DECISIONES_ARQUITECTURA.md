# Decisiones de Arquitectura de Software (ADR) — MuniHuamanga V2

Municipalidad Provincial de Huamanga — Gerencia de Licencias
**Código del Documento:** DAS-MH-V2-001 • ADR-Consolidado
**Fecha:** 13 de octubre de 2026
**Documentos Rectores:** [ERS-MH-V2-001 v1.0] y [DAS-MH-V2-001 v1.0]

---

## 1. Índice de Decisiones Arquitectónicas

| ID | Título de la Decisión | Estado | Impacto Principal |
| :--- | :--- | :--- | :--- |
| **ADR-01** | Preservación de Monolito Modular con Clean Architecture | Aceptado | Despliegue unificado, mantenimiento simplificado, cero sobrecarga de microservicios. |
| **ADR-02** | Autorización Centralizada en Backend y Protección IDOR/BOLA | Aceptado | Cero confianza en controles visuales de frontends; verificación de propiedad de recursos. |
| **ADR-03** | Normalización de Entidades y Repositorio Documental Inmutable | Aceptado | Desacoplamiento de Cuentas, Establecimientos y Expedientes; versionado con SHA-256. |
| **ADR-04** | Integración de Firma Digital Institucional Mediante Puerto y Bloqueo de Emisión Falsa | Aceptado | Desacoplamiento de proveedor de firma; no almacenamiento de claves privadas; prohibición de firmas simuladas. |
| **ADR-05** | Verificación Pública QR con Enlace Dinámico y Minimización de Datos | Aceptado | Cumplimiento Ley N° 29733; token no enumerable; vista básica pública y vista ampliada para inspectores. |
| **ADR-06** | Procesos Externos Manuales para SAT, Defensa Civil y Edificaciones (Sin Acoplamiento) | Aceptado | Delimitación del alcance en V2; registro manual de pagos y zonificación; exclusión de actas y sanciones. |
| **ADR-07** | Acto Administrativo Resolutivo, Correlativo Atómico y Bloqueo de Emisión sin Firma Digital | Aceptado | Separación de informe y resolución; secuencias atómicas pesimistas; bloqueo de licencias oficiales sin PKI institucional. |

---

## 2. Detalle de los Registros de Decisión (ADR)

### ADR-01: Preservación de Monolito Modular con Clean Architecture
- **Contexto:** El repositorio existente fue diseñado como un monolito modular en dos proyectos Maven (`common-domain` y `servicio-expedientes`). Separar la aplicación en microservicios independientes aumentaría exponencialmente la complejidad operacional, latencia de red y costos de infraestructura de la Municipalidad de Huamanga sin aportar valor directo en la etapa actual.
- **Decisión:** Mantener el Monolito Modular como topología de despliegue, reforzando la separación de capas (Controlador -> Puerto/Servicio de Aplicación -> Dominio -> Persistencia/Adaptador).
- **Consecuencias:**
  - *Positivas:* Despliegue atómico mediante un único contenedor Docker o JAR ejecutable; transacciones ACID locales consistentes; desarrollo ágil en un solo repositorio.
  - *Mitigaciones:* Mantener fronteras estrictas entre paquetes funcionales para permitir una eventual extracción modular si la demanda institucional lo exige a futuro.

---

### ADR-02: Autorización Centralizada en Backend y Protección IDOR/BOLA
- **Contexto:** Se detectaron endpoints en `SecurityConfig.java` donde se confiaba en la ofuscación de URL o en controles puramente visuales de JavaScript en los portales estáticos, exponiendo datos de expedientes y descargas de documentos sin validar si el usuario autenticado es el legítimo propietario.
- **Decisión:** Implementar autorización estricta en la capa backend a nivel de controlador y servicio de dominio utilizando Spring Security Method Security (`@PreAuthorize("@expedienteSecurity.isOwner(authentication, #id)")`). Las llamadas ciudadanas deben verificar que `solicitanteId` o `cuentaId` corresponda al `Principal` autenticado.
- **Consecuencias:**
  - *Positivas:* Mitiga vulnerabilidades OWASP Top 10 API1:2023 (Broken Object Level Authorization); garantiza confidencialidad según la Ley N° 29733.
  - *Compromiso:* Exige asociar las solicitudes ciudadanas a un usuario autenticado y agregar pruebas automatizadas de seguridad negativa (intentos de acceso cruzado).

---

### ADR-03: Normalización de Entidades y Repositorio Documental Inmutable
- **Contexto:** La tabla monolítica actual `expedientes` combina en una sola fila los datos personales del solicitante, persona jurídica, apoderado, establecimiento comercial, dirección desglosada, anexo 4, estado, pagos y licencias. Esto impide que un titular con RUC registre más de un local o reutilice sus datos en trámites sucesivos.
- **Decisión:** Evolucionar el modelo de datos hacia las entidades conceptuales del DAS V2: `Cuenta` (1:N) `Establecimiento` (1:N) `SolicitudBorrador` / `Expediente` (1:N) `Documento`. Cada archivo adjunto o generado se registrará en `Documento` con identificador único, tipo, versión correlativa, estado y hash criptográfico SHA-256. Al subsanar un documento, la versión previa se conserva como histórica.
- **Consecuencias:**
  - *Positivas:* Escalabilidad relacional; soporte nativo para empresas con múltiples sucursales; trazabilidad probatoria e inmutabilidad documental.
  - *Mitigaciones:* La migración del esquema en PostgreSQL se realizará de manera aditiva e incremental sin alterar ni eliminar expedientes existentes.

---

### ADR-04: Integración de Firma Digital Mediante Puerto y Bloqueo de Emisión Falsa
- **Contexto:** La emisión de certificados oficiales de licencia requiere firma digital conforme a la Infraestructura Oficial de Firma Electrónica (IOFE) y la Ley N° 27269. La versión previa insertaba un rótulo gráfico simulado de "Sello Digital MPH".
- **Decisión:** Definir una interfaz de puerto `FirmaDigitalPort` desacoplada del proveedor final (RENIEC, ONPE o proveedor acreditado). No se almacenarán claves privadas ni certificados dentro de la aplicación. En caso de que el servicio institucional de firma no esté disponible o falle la conexión, el sistema aprobará administrativamente el expediente pero **bloqueará la emisión oficial del documento y la publicación del QR** hasta que se obtenga una firma digital criptográficamente válida.
- **Consecuencias:**
  - *Positivas:* Cumplimiento legal estricto; seguridad criptográfica sin custodia indebida de certificados; cero emisión de documentos apócrifos.
  - *Compromiso:* La emisión queda sujeta a la disponibilidad del servicio de firma o a un mecanismo de contingencia institucional formalmente aprobado.

---

### ADR-05: Verificación Pública QR con Enlace Dinámico y Minimización de Datos
- **Contexto:** El código QR insertado en la licencia de funcionamiento física o digital es escaneado por ciudadanos, vecinos, clientes e inspectores. En la versión previa, el endpoint público exponía el número completo de DNI/RUC del titular comercial.
- **Decisión:** El código QR codificará una URL dinámica hacia el portal institucional (`https://munihuamanga.gob.pe/verificar-licencia.html?token=...`) utilizando un token aleatorio criptográfico y no enumerable. Se implementarán dos niveles de respuesta:
  1. *Vista Pública (Ciudadano/General):* Minimización estricta de datos (Nombre comercial, razón social, giro de negocio autorizado, dirección, estado de vigencia, sin DNI ni teléfonos personales).
  2. *Vista de Inspector Municipal (Autenticado):* Acceso ampliado a georreferenciación en mapa interactivo y coordenadas para cotejar el establecimiento físico contra el registro oficial.
- **Consecuencias:**
  - *Positivas:* Protección de datos personales según la Ley N° 29733; auditoría de accesos a coordenadas geográficas; apoyo operativo a inspecciones en campo.

---

### ADR-06: Procesos Externos Manuales para SAT, Defensa Civil y Edificaciones
- **Contexto:** La ERS y el DAS establecen que en esta etapa de MuniHuamanga V2, los sistemas del SAT, Defensa Civil, Edificaciones e Inspección operan fuera de la plataforma o mediante procesos manuales institucionales.
- **Decisión:**
  1. *SAT:* El pago se realiza presencialmente en ventanillas SAT. El ciudadano registra su código de comprobante en MuniHuamanga y un cajero/funcionario de Licencias valida manualmente el abono. No habrá conciliación automática de pagos.
  2. *Defensa Civil:* El administrado declara el nivel de riesgo con su sustento documental. MuniHuamanga registra la validación administrativa de dicho sustento sin emitir ni simular dictámenes técnicos de Defensa Civil.
  3. *Edificaciones:* La evaluación de zonificación y compatibilidad de uso PDU la realiza manualmente el evaluador de Licencias con base en la cartografía y ordenanzas municipales.
  4. *Inspección:* La Gerencia de Inspección utilizará el sistema únicamente como herramienta de consulta de autenticidad y ubicación en campo. Queda terminantemente prohibido incorporar módulos de fiscalización, actas de control, imposición de sanciones o trámites sancionadores.
- **Consecuencias:**
  - *Positivas:* Acotamiento estricto de responsabilidades; alineación con los acuerdos institucionales vigentes; reducción de riesgos por dependencias de sistemas externos no controlados.

---

### ADR-07: Acto Administrativo Resolutivo, Correlativo Atómico y Bloqueo de Emisión sin Firma Digital
- **Contexto:** En el procedimiento administrativo municipal (TUO Ley N° 27444 y Ley N° 28976), el informe administrativo técnico elaborado por el evaluador es un acto de instrucción interna que no produce efectos jurídicos directos frente al administrado. El acto administrativo habilitante o denegatorio es la Resolución de Subgerencia o Gerencia. Asimismo, emitir una licencia de funcionamiento física o digital sin contar con firma digital institucional acreditada ante la IOFE vulnera la Ley N° 27269 y genera documentos apócrifos.
- **Decisión:**
  1. *Separación de Entidades y Estados:* Implementar `ResolucionAdministrativa` con estados propios (`BORRADOR`, `APROBADA_PENDIENTE_FIRMA`, `EN_PROCESO_FIRMA`, `FIRMADA`, `ERROR_FIRMA`, `ANULADA`), separada de `InformeAdministrativo` e independiente del expediente.
  2. *Correlativos Atómicos Pesimistas:* Implementar secuencias anuales atómicas (`RES-YYYY-XXXXX` y `LIC-YYYY-XXXXX`) basadas en tablas dedicadas y bloqueo pesimista en base de datos (`PESSIMISTIC_WRITE`), evitando colisiones o duplicados concurrentes.
  3. *Bloqueo Técnico de Emisión:* No emitir licencia oficial ni activar códigos QR públicos mientras la resolución no cuente con firma digital institucional verificada (`FIRMADA`). Si la infraestructura de firma no está configurada, el expediente permanece en espera sin expedir certificados oficiales.
  4. *Inmutabilidad y Documentos Vectoriales:* Los borradores y vistas previas de resoluciones deben generarse en OpenPDF vectorial con la marca de agua explícita "VISTA PRELIMINAR - SIN VALIDEZ OFICIAL", sustituida por el bloque de verificación criptográfica únicamente al completarse la firma institucional válida.
- **Consecuencias:**
  - *Positivas:* Validez jurídica indubitable; segregación estricta de funciones entre evaluador y autoridad emisora; protección criptográfica total contra licencias fraudulentas; trazabilidad inmutable mediante tablas de historial *append-only*.
