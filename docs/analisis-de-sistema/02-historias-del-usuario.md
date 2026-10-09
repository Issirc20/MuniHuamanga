# 02. Historias de Usuario (User Stories)
## Sistema de Gestión Documentaria de Licencia de Funcionamiento
### Municipalidad Provincial de Huamanga (Ayacucho, Perú)

> **Marco Normativo:** Ley N° 28976 / TUO D.S. N° 046-2017-PCM / D.S. N° 002-2018-PCM / D.S. N° 163-2020-PCM  
> **Ubicación:** `docs/analisis-de-sistema/02-historias-del-usuario.md`

---

## 1. Introducción y Convención de Estructura

Las historias de usuario capturan las necesidades funcionales desde la perspectiva de los diferentes roles y actores del sistema. Cada historia sigue el formato estándar ágil:
- **Narrativa:** *Como [Actor], quiero [Acción / Funcionalidad], para [Beneficio / Valor de Negocio].*
- **Criterios de Aceptación:** Especificados con el formato *Dado que... Cuando... Entonces...* (BDD / Gherkin).
- **Prioridad MoSCoW:** Must Have (Debe tener), Should Have (Debería tener), Could Have (Podría tener), Won't Have (No tendrá en esta fase).
- **Contexto Delimitado (Bounded Context):** Módulo de la arquitectura responsable de su implementación.

---

## 2. Matriz Resumen de Historias de Usuario

| ID | Título de la Historia de Usuario | Actor Principal | Prioridad | Bounded Context | Puntos (SP) |
|---|---|---|:---:|---|:---:|
| **US-01** | Registro de Solicitud en Wizard 3 Pasos | Administrado | **MUST** | `expedientes` | 8 |
| **US-02** | Determinación Objetiva de Nivel de Riesgo ITSE | Sistema / Inspector | **MUST** | `expedientes` | 5 |
| **US-03** | Cálculo Automatizado de Tasas TUPA | Sistema / Administrado | **MUST** | `tupa` | 5 |
| **US-04** | Liquidación y Generación de Voucher SAT | Administrado / SAT | **MUST** | `expedientes` | 3 |
| **US-05** | Renderizado en Memoria de Anexos 1, 3 y 4 en PDF | Administrado | **MUST** | `documentos` | 8 |
| **US-06** | Conciliación y Registro de Pago de Tasa | Cajero SAT | **MUST** | `expedientes` | 5 |
| **US-07** | Evaluación Técnica y Zonificación Comercial | Evaluador Técnico | **MUST** | `expedientes` | 5 |
| **US-08** | Registro de Dictamen Técnico de Inspección ITSE | Inspector CENEPRED | **MUST** | `integracion` | 5 |
| **US-09** | Emisión y Generación del Certificado Oficial PDF | Gerente de Licencias | **MUST** | `documentos` | 8 |
| **US-10** | Generación de Código QR de Verificación | Sistema / ZXing | **MUST** | `verificacion` | 3 |
| **US-11** | Consulta Pública de Autenticidad QR (RNF-20) | Fiscalizador / Público | **MUST** | `verificacion` | 5 |
| **US-12** | Notificaciones Asíncronas por Correo Electrónico | Sistema / Administrado | **SHOULD** | `expedientes` | 5 |
| **US-13** | Mantenimiento en Caliente del Tarifario TUPA | Administrador TI | **SHOULD** | `tupa` | 5 |
| **US-14** | Autenticación Segura y Control de Acceso RBAC | Funcionario Municipal | **MUST** | `seguridad` | 5 |
| **US-15** | Asistente Guiado y Mesa de Ayuda para Anexos Físicos | Administrado | **COULD** | `documentos` | 3 |

---

## 3. Especificación Detallada de Historias de Usuario

### US-01: Registro de Solicitud en Mesa de Partes Virtual (Wizard 3 Pasos)
* **Narrativa:**
  > **Como** ciudadano o representante legal de un negocio en Huamanga,  
  > **quiero** registrar mi solicitud de licencia completando un formulario interactivo por pasos (Wizard),  
  > **para** formalizar mi petición administrativa de forma digital y sin trasladarme innecesariamente al palacio municipal.
* **Criterios de Aceptación:**
  - **Escenario 1 (Validación de identidad):**
    - *Dado* que el solicitante ingresa al Wizard Paso 1,
    - *Cuando* selecciona Persona Natural debe ingresar un DNI válido de 8 dígitos; si selecciona Persona Jurídica debe ingresar un RUC de 11 dígitos que comience con 20 y partida registral SUNARP.
    - *Entonces* el sistema valida la estructura sintáctica antes de permitir avanzar al Paso 2.
  - **Escenario 2 (Datos del establecimiento y giro):**
    - *Dado* que el solicitante está en el Paso 2,
    - *Cuando* ingresa la dirección, giro comercial, código CIIU, área ocupada ($m^2$) y aforo,
    - *Entonces* el sistema registra los 45 campos normativos requeridos por el Anexo 1.
  - **Escenario 3 (Confirmación y emisión de identificadores):**
    - *Dado* que el usuario confirma su solicitud en el Paso 3,
    - *Cuando* envía el formulario al endpoint `POST /api/expedientes`,
    - *Entonces* el sistema genera el código único `EXP-2026-XXXXX`, número de trámite, voucher `VCH-2026-XXXXXX` y asigna el estado `FORMATOS_GENERADOS`.

---

### US-02: Determinación Automática del Nivel de Riesgo ITSE
* **Narrativa:**
  > **Como** analista técnico municipal,  
  > **quiero** que el sistema clasifique automáticamente el nivel de riesgo del local comercial,  
  > **para** aplicar con objetividad matemática los criterios de la Matriz de Riesgo ITSE del D.S. N° 002-2018-PCM sin discrecionalidad humana.
* **Criterios de Aceptación:**
  - **Escenario 1 (Riesgo Bajo / Medio):**
    - *Dado* un establecimiento con área $< 100\,m^2$, aforo $< 20$ personas y sin almacenamiento de materiales inflamables,
    - *Cuando* se evalúa su giro en la Matriz ITSE,
    - *Entonces* el sistema asigna nivel `BAJO` o `MEDIO`, habilitando la inspección ITSE Posterior y la emisión del Anexo 4.
  - **Escenario 2 (Riesgo Alto / Muy Alto):**
    - *Dado* un local con aforo $> 100$ personas, almacenamiento de GLP $> 0.45\,m^3$ o uso de calderas,
    - *Cuando* se procesa la solicitud,
    - *Entonces* el sistema asigna nivel `ALTO` o `MUY_ALTO`, marcando obligatoriedad de Inspección Previa antes de emitir la licencia.

---

### US-03: Cálculo Automatizado de Tasas Administrativas TUPA
* **Narrativa:**
  > **Como** administrado,  
  > **quiero** conocer inmediatamente el monto exacto de la tasa municipal a pagar,  
  > **para** tener certidumbre económica y liquidar mis derechos en base al TUPA institucional de Huamanga.
* **Criterios de Aceptación:**
  - **Escenario 1 (Tarifa por Riesgo):**
    - *Dado* que el sistema determinó el nivel de riesgo del expediente,
    - *Cuando* se invoca la `CalculadoraDeTasa`,
    - *Entonces* retorna el valor configurado en el catálogo oficial (e.g., S/. 154.50 para Bajo, S/. 218.00 para Medio, S/. 345.20 para Alto, S/. 480.00 para Muy Alto).
  - **Escenario 2 (Resiliencia por Fallback):**
    - *Dado* que la tabla de base de datos de tarifas no responde o está vacía,
    - *Cuando* se ejecuta el cálculo,
    - *Entonces* el sistema utiliza los valores por defecto estipulados en `application.yml` sin interrumpir el registro.

---

### US-04: Liquidación y Generación de Orden de Pago SAT
* **Narrativa:**
  > **Como** administrado,  
  > **quiero** obtener un voucher oficial de liquidación con código de barras,  
  > **para** efectuar el pago de mi trámite en las ventanillas del SAT Huamanga o banca asociada.
* **Criterios de Aceptación:**
  - **Escenario 1:**
    - *Dado* que el expediente se encuentra registrado en estado `FORMATOS_GENERADOS`,
    - *Cuando* el usuario solicita su orden de pago,
    - *Entonces* el sistema emite el voucher en PDF con correlativo `VCH-2026-XXXXXX`, desglose de tasa, fecha de vencimiento y código de barras Code 128 legible por escáner óptico.

---

### US-05: Emisión de Formatos Oficiales en PDF (Anexos 1, 3 y 4)
* **Narrativa:**
  > **Como** administrado,  
  > **quiero** descargar de inmediato los PDFs oficiales de mi trámite con todos mis datos prellenados,  
  > **para** imprimirlos, suscribirlos y presentarlos ante Defensa Civil conforme a la normativa.
* **Criterios de Aceptación:**
  - **Escenario 1 (Anexo 1):**
    - *Dado* un expediente registrado,
    - *Cuando* se solicita el Anexo 1 (`/documentos/anexo1-declaracion-jurada`),
    - *Entonces* se retorna un PDF de exactamente 2 páginas cumpliendo el diseño del D.S. N° 163-2020-PCM con todas las 6 secciones completas.
  - **Escenario 2 (Anexo 3 y Anexo 4):**
    - *Dado* que el local requiere inspección y condiciones de seguridad,
    - *Cuando* se solicitan el Anexo 3 y Anexo 4,
    - *Entonces* se descargan los PDFs (Anexo 3 en 2 páginas y Anexo 4 en 4 páginas con la matriz de 23 ítems de seguridad técnica).

---

### US-06: Conciliación de Pago de Tasa en Caja SAT
* **Narrativa:**
  > **Como** cajero del SAT Huamanga,  
  > **quiero** registrar el pago de la orden tributaria ingresando el número de operación,  
  > **para** que el expediente avance automáticamente en el flujo sin demoras administrativas.
* **Criterios de Aceptación:**
  - **Escenario 1:**
    - *Dado* que un usuario con rol `ROLE_CAJERO` ingresa el código de voucher y monto abonado en `PUT /api/expedientes/{id}/pago`,
    - *Cuando* el monto coincide con la tasa liquidada,
    - *Entonces* el expediente cambia automáticamente a `DOCUMENTOS_VALIDADOS` y se registra en la auditoría inmutable.

---

### US-07: Evaluación Técnica y Legal de Zonificación
* **Narrativa:**
  > **Como** evaluador técnico de la Subgerencia de Licencias,  
  > **quiero** revisar la procedencia urbanística y de zonificación del expediente desde el Portal Interno,  
  > **para** determinar si el giro comercial es compatible con el Plano de Desarrollo Urbano (PDU).
* **Criterios de Aceptación:**
  - **Escenario 1 (Aprobación preliminar):**
    - *Dado* un expediente en estado `DOCUMENTOS_VALIDADOS`,
    - *Cuando* el evaluador valida la zonificación y requisitos sectoriales,
    - *Entonces* el expediente pasa al estado `EN_EVALUACION_FINAL`.
  - **Escenario 2 (Observación del trámite):**
    - *Dado* que faltan requisitos o existe inconsistencia en el giro,
    - *Cuando* el evaluador registra observaciones técnicas,
    - *Entonces* el sistema otorga un plazo legal de 5 días hábiles para subsanación.

---

### US-08: Inspección Técnica de Seguridad en Edificaciones (ITSE)
* **Narrativa:**
  > **Como** inspector acreditado CENEPRED de Defensa Civil,  
  > **quiero** registrar el informe técnico resultante de la inspección del local,  
  > **para** emitir el dictamen vinculante favorable o desfavorable para la licencia.
* **Criterios de Aceptación:**
  - **Escenario 1 (Dictamen Favorable):**
    - *Dado* que el establecimiento cumple las condiciones de seguridad en tableros eléctricos, extintores y rutas de evacuación,
    - *Cuando* el inspector ingresa el N° de Informe ITSE y dictamen favorable,
    - *Entonces* el sistema habilita el expediente para aprobación final del Gerente.

---

### US-09: Emisión y Generación del Certificado Oficial de Licencia
* **Narrativa:**
  > **Como** Gerente de Licencias,  
  > **quiero** aprobar y emitir la Licencia de Funcionamiento con su certificado oficial en PDF,  
  > **para** otorgar al administrado su título habilitante con valor legal y vigencia indeterminada.
* **Criterios de Aceptación:**
  - **Escenario 1:**
    - *Dado* un expediente en `EN_EVALUACION_FINAL` con dictamen favorable,
    - *Cuando* el funcionario emite la aprobación (`PUT /api/expedientes/{id}/aprobar`),
    - *Entonces* el expediente pasa a estado `APROBADO`, se genera el número oficial `LIC-2026-XXXXX` y se renderiza el Certificado PDF con doble marco perimetral, escudo provincial, código QR y 6 notas legales.

---

### US-10: Incorporación de Mecanismo de Seguridad y Código QR
* **Narrativa:**
  > **Como** autoridad municipal,  
  > **quiero** que cada certificado de licencia cuente con un código QR de alta definición generado con ZXing,  
  > **para** imposibilitar la falsificación material de licencias y permitir su validación instantánea.
* **Criterios de Aceptación:**
  - **Escenario 1:**
    - *Dado* que se genera la licencia,
    - *Cuando* el motor de PDF dibuja el documento,
    - *Entonces* incrusta una imagen QR de $250 \times 250$ píxeles que contiene la URL directa hacia la verificación institucional pública: `https://munihuamanga.gob.pe/verificar-licencia?codigo=LIC-2026-XXXXX`.

---

### US-11: Verificación Pública de Autenticidad en Línea (RNF-20)
* **Narrativa:**
  > **Como** fiscalizador municipal o ciudadano general,  
  > **quiero** consultar la validez de una licencia escaneando su QR desde un teléfono móvil,  
  > **para** constatar en tiempo real si el establecimiento cuenta con autorización legítima vigente.
* **Criterios de Aceptación:**
  - **Escenario 1:**
    - *Dado* que se realiza una petición a `GET /api/public/licencias/{codigoQr}`,
    - *Cuando* el endpoint recibe la consulta pública (sin requerir token ni login),
    - *Entonces* responde en un percentil $P_{95} \le 200\text{ ms}$ retornando el JSON con razón social, RUC, dirección, giro comercial, estado y fecha de expedición.

---

### US-12: Notificaciones Automatizadas por Correo Electrónico
* **Narrativa:**
  > **Como** administrado,  
  > **quiero** recibir correos electrónicos automáticos informándome sobre cada avance de mi trámite,  
  > **para** estar informado en tiempo real sin tener que consultar repetidamente el portal.
* **Criterios de Aceptación:**
  - **Escenario 1 (Registro):**
    - *Dado* el registro exitoso del trámite,
    - *Cuando* se crea el expediente,
    - *Entonces* el sistema despacha asíncronamente un correo HTML con su número de expediente y códigos de pago.
  - **Escenario 2 (Aprobación con PDF adjunto):**
    - *Dado* que el expediente es aprobado,
    - *Cuando* se emite la licencia,
    - *Entonces* el sistema envía un correo de felicitación con el **Certificado Oficial de Licencia adjunto en PDF**.

---

### US-13: Mantenimiento en Caliente del Tarifario TUPA Dinámico
* **Narrativa:**
  > **Como** Administrador del Sistema TI,  
  > **quiero** actualizar los montos de las tasas TUPA desde el portal interno,  
  > **para** reflejar cambios derivados de nuevas ordenanzas municipales sin reiniciar los servidores ni modificar código fuente.
* **Criterios de Aceptación:**
  - **Escenario 1:**
    - *Dado* un usuario autenticado con `ROLE_ADMIN`,
    - *Cuando* envía una petición `PUT /api/tupa/tarifas/{id}` con el nuevo monto y base legal,
    - *Entonces* la base de datos se actualiza inmediatamente y las nuevas solicitudes liquidan el monto modificado en tiempo real.

---

### US-14: Autenticación Segura y Control de Acceso RBAC
* **Narrativa:**
  > **Como** funcionario municipal,  
  > **quiero** iniciar sesión con mis credenciales institucionales y recibir un token JWT,  
  > **para** acceder únicamente a las funciones y bandejas autorizadas según mi cargo y rol.
* **Criterios de Aceptación:**
  - **Escenario 1 (Login exitoso):**
    - *Dado* que el usuario envía usuario y contraseña en `POST /api/auth/login`,
    - *Cuando* las credenciales son válidas,
    - *Entonces* se retorna un Bearer Token JWT firmado con HMAC-SHA256 con vigencia de 24 horas y los roles asignados.
  - **Escenario 2 (Bloqueo no autorizado):**
    - *Dado* un cajero con `ROLE_CAJERO`,
    - *Cuando* intenta invocar el endpoint de aprobación (`PUT /api/expedientes/{id}/aprobar`),
    - *Entonces* Spring Security intercepta la petición y retorna `403 Forbidden`.

---

### US-15: Mesa de Ayuda y Orientación para Anexos Físicos
* **Narrativa:**
  > **Como** administrado que tramita por primera vez,  
  > **quiero** contar con explicaciones claras sobre qué hacer con los Anexos 3 y 4 una vez impresos,  
  > **para** no cometer errores en la presentación presencial ante la Subgerencia de Defensa Civil.
* **Criterios de Aceptación:**
  - **Escenario 1:**
    - *Dado* que el ciudadano está en la pantalla de descarga de documentos,
    - *Cuando* hace clic en los botones contextuales `💡 Mesa de Ayuda / ¿Cómo se tramita?`,
    - *Entonces* se despliega un modal interactivo con la guía paso a paso: número de copias requeridas, lugar de ventanilla física, profesional que debe visar el plano y recomendaciones de seguridad técnica.

---

## 4. Conclusión

El conjunto de 15 Historias de Usuario cubre de punta a punta el ciclo de vida del trámite de Licencias de Funcionamiento en la Municipalidad Provincial de Huamanga, garantizando plena trazabilidad, cumplimiento de plazos legales y una experiencia de usuario transparente.
