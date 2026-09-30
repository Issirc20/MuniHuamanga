# 🏛️ DOCUMENTACIÓN OFICIAL — ENTREGA FASE 02
## Motor de Generación e Impresión de Documentos Estándar (PDFs Oficiales)
### Municipalidad Provincial de Huamanga (MuniHuamanga)
> **Versión del Entregable:** Fase 02  
> **Fecha de Cierre:** 29 de Septiembre de 2026  
> **Estado:** Implementado, Verificado y Aprobado (`BUILD SUCCESS` en 7 módulos)  
> **Commits Realizados en la Fase:**
> - `d2295ca` — `feat(fase-02): digitalizar formato oficial de anexo 1 declaracion jurada de 2 paginas segun ley 28976`
> - `bbb242c` — `feat(fase-02): digitalizar formato oficial de anexo 3 matriz de riesgo itse para defensa civil`
> - `6d906d7` — `feat(fase-02): digitalizar formato oficial de anexo 4 declaracion jurada de condiciones de seguridad en 4 paginas`

---

## 1. Objetivo y Alcance de la Fase 02

El objetivo central de la **Fase 02** fue diseñar, construir y verificar el **Motor de Generación e Impresión Documental Oficial** de la Municipalidad Provincial de Huamanga, garantizando que los formatos generados en PDF reproduzcan fielmente, celda por celda, casilla por casilla y con la paginación oficial estricta, los formatos impresos normados por la legislación peruana vigente:

1. **Anexo 1 (Ley N° 28976, Versión 03 / D.S. N° 046-2017-PCM / D.S. N° 163-2020-PCM)**:
   - **Paginación oficial**: Exactamente **dos (2) páginas**.
   - **Página 1**: Membrete oficial institucional, recuadro de N° de Expediente/Fecha/Hora, Secciones I (Tipo de Trámite con checkboxes `[ X ]`), II (Datos del Solicitante o Titular), III (Representante Legal y Partida Registral SUNARP), IV (Datos del Establecimiento, Área en m², Zonificación y Croquis de Ubicación con manzanas colindantes).
   - **Página 2**: Sección V (Declaraciones Juradas de Zonificación, ITSE y Veracidad Ley 27444), Sección VI (Bloque de firmas del administrado y recepción municipal), y cuadro completo de **Instrucciones para el Llenado** desglosado por sección.

2. **Anexo 3 (D.S. N° 002-2018-PCM / R.J. N° 016-2018-CENEPRED/J)**:
   - **Paginación oficial**: Exactamente **dos (2) páginas**.
   - **Página 1**: Encabezado de la Subgerencia de Defensa Civil y Gestión del Riesgo de Desastres, Datos Generales (Área ocupada, aforo calculado), **Matriz de las 8 Funciones Oficiales de Edificación** (Salud, Encuentro, Hospedaje, Educación, Industrial, Oficinas, Comercio, Almacén), verificación de **Factores Agravantes Críticos** (Tanques de GLP $> 0.45\text{ m}^3$, calderas/recipientes a presión, sótano comercial $> 100\text{ m}^2$) y clasificación en cuadrícula con código de colores oficial CENEPRED (**Verde=Bajo**, **Amarillo=Medio**, **Naranja=Alto**, **Rojo=Muy Alto**).
   - **Página 2**: Tabla de criterios técnicos cuantitativos por función, **Dictamen Técnico de Defensa Civil** con N° de Reporte ITSE, suscripción técnica del Inspector Técnico acreditado (CIP/RITSE) y advertencia legal de fiscalización posterior.

3. **Anexo 4 (D.S. N° 002-2018-PCM / D.S. N° 046-2017-PCM)**:
   - **Paginación oficial**: Exactamente **cuatro (4) páginas**.
   - **Página 1**: Datos generales, dimensionamiento y aforo desglosado por niveles (Piso 1, Piso 2, Piso 3, Piso 4 / Azotea, Áreas ocupadas y techadas), antigüedad de la edificación y giro, y condiciones básicas de operatividad.
   - **Página 2 (Eje I)**: Seguridad Estructural, No Estructural y Rutas de Evacuación (8 ítems normativos con checkboxes `[ X ] SÍ` y `[   ] N/A`).
   - **Página 3 (Eje II)**: Seguridad de Protección Contra Incendios y Emergencias (Extintores PQS/CO2 con mantenimiento anual vigente, tarjeta de control, altura reglamentaria $\le 1.50\text{ m}$, radio libre de 1 m, luces de emergencia autónomas y señalética fotoluminiscente NTP 399.010-1).
   - **Página 4 (Eje III)**: Seguridad en Instalaciones Eléctricas (Tablero incombustible con mandil, rotulado, interruptores termomagnéticos y diferenciales, pozo a tierra vigente $< 25\ \Omega$, cableado protegido en tubos PVC autoextinguibles), Declaración Jurada de Veracidad (Art. 411 Código Penal), bloque de firma del titular, recuadro de **Huella Dactilar** y sello de recepción municipal.

---

## 2. Arquitectura del Motor Documental

El motor opera bajo una arquitectura desacoplada donde `servicio-formularios` (puerto `8082`) funciona como el microservicio documental especializado, mientras que `servicio-expedientes` (puerto `8081`) ofrece acceso directo a los documentos por expediente:

```text
                               ┌──────────────────────────────────────────────────────────┐
                               │                    PORTAL CIUDADANO                      │
                               │                (Frontend / Administrado)                 │
                               └──────────────┬────────────────────────────┬──────────────┘
                                              │ Descarga por Expediente    │ Generación bajo demanda
                                              ▼                            ▼
                       ┌──────────────────────────────┐            ┌──────────────────────────────┐
                       │    SERVICIO-EXPEDIENTES      │            │     SERVICIO-FORMULARIOS     │
                       │         (Puerto 8081)        │            │         (Puerto 8082)        │
                       └──────────────┬───────────────┘            └──────────────┬───────────────┘
                                      │                                           │
                                      ▼                                           ▼
                       ┌──────────────────────────────┐            ┌──────────────────────────────┐
                       │     DocumentoPdfService      │            │   GeneradorDocumentoService  │
                       └──────────────┬───────────────┘            └──────────────┬───────────────┘
                                      │                                           │
              ┌───────────────────────┼───────────────────────┐                   │
              ▼                       ▼                       ▼                   ▼
    ┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐  ┌───────────────────────┐
    │Anexo1PdfGenerator│    │Anexo3PdfGenerator│    │Anexo4PdfGenerator│  │ OpenPDF 1.3.39        │
    │(2 Págs Oficiales)│    │(2 Págs Matriz DC)│    │(4 Págs Seguridad)│  │ ZXing (QR & Code 128) │
    └──────────────────┘    └──────────────────┘    └──────────────────┘  └───────────────────────┘
```

---

## 3. Inventario de Endpoints del Motor Documental

### 3.1 Servicio de Formularios (`servicio-formularios` — Puerto `8082`)

| Método | Endpoint | Descripción Normativa | Formato y Paginación |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/formularios/anexo1-declaracion-jurada` | Anexo 1: Declaración Jurada para Licencia (Ley 28976) | PDF (2 páginas) |
| `POST` | `/api/formularios/anexo3-matriz-riesgo-itse` | Anexo 3: Matriz de Riesgo ITSE para Defensa Civil (PCM/CENEPRED) | PDF (2 páginas) |
| `POST` | `/api/formularios/anexo4-condiciones-seguridad`| Anexo 4: Condiciones de Seguridad en Edificación (Riesgo Bajo/Medio)| PDF (4 páginas) |
| `POST` | `/api/formularios/declaracion-jurada` | Retrocompatibilidad: Genera Anexo 1 oficial | PDF (2 páginas) |
| `POST` | `/api/formularios/defensa-civil` | Retrocompatibilidad: Genera Anexo 3 oficial ITSE | PDF (2 páginas) |
| `POST` | `/api/formularios/voucher-sat` | Orden de Pago SAT Huamanga con Código de Barras Code 128 | PDF (A5) |
| `POST` | `/api/formularios/licencia` | Certificado Oficial de Licencia con QR y Sello Digital SHA-256 | PDF (A4) |

### 3.2 Servicio de Expedientes (`servicio-expedientes` — Puerto `8081`)

| Método | Endpoint | Parámetro | Contenido Generado |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/expedientes/{id}/documentos/anexo1-declaracion-jurada` | `UUID id` | Descarga directa del Anexo 1 oficial (2 págs) |
| `GET` | `/api/expedientes/{id}/documentos/anexo3-matriz-riesgo-itse` | `UUID id` | Descarga directa del Anexo 3 oficial ITSE (2 págs) |
| `GET` | `/api/expedientes/{id}/documentos/anexo4-condiciones-seguridad`| `UUID id` | Descarga directa del Anexo 4 oficial de Seguridad (4 págs) |
| `GET` | `/api/expedientes/{id}/documentos/declaracion-jurada` | `UUID id` | Retrocompatibilidad: Anexo 1 |
| `GET` | `/api/expedientes/{id}/documentos/voucher-sat` | `UUID id` | Voucher de Pago SAT con código de barras |
| `GET` | `/api/expedientes/{id}/documentos/licencia` | `UUID id` | Certificado de Licencia con QR |

---

## 4. Cuadro Comparativo de Especificaciones Físicas y Digitales

| Formato | Norma de Referencia | Instancia Receptora | Páginas | Elementos Críticos Validados |
| :--- | :--- | :--- | :---: | :--- |
| **Anexo 1** | Ley N° 28976 / D.S. 046-2017-PCM | Mesa de Partes / Gerencia Licencias | **2** | • Secciones I a VI completas<br>• Casillas de verificación `[ X ]`<br>• Croquis vial de ubicación urbana<br>• Instrucciones oficiales de llenado |
| **Anexo 3** | D.S. N° 002-2018-PCM / R.J. 016-2018 | Subgerencia de Defensa Civil | **2** | • Matriz de 8 Funciones CENEPRED<br>• Factores agravantes (GLP, calderas, sótanos)<br>• Código de 4 colores de riesgo<br>• Dictamen e informe técnico ITSE |
| **Anexo 4** | D.S. N° 002-2018-PCM | Defensa Civil / Fiscalización | **4** | • Desglose de área por niveles (P1-P4)<br>• Aforo y antigüedad<br>• Eje I: Estructuras y evacuación<br>• Eje II: Extintores y señalética NTP<br>• Eje III: Tableros, pozo tierra y diferenciales<br>• Huella dactilar y recepción municipal |

---

## 5. Resultados de Pruebas Unitarias y de Integración

Se ejecutaron las pruebas automatizadas en los microservicios con 100% de éxito:

- **`servicio-formularios`**: 8 pruebas unitarias aprobadas (`BUILD SUCCESS`, 0 errores).
  - `testGenerarAnexo1DeclaracionJuradaOficial`: Valida cabecera `%PDF-`, tablas de 2 páginas y peso superior a 5 KB.
  - `testGenerarAnexo3MatrizRiesgoItseOficial`: Valida cabecera `%PDF-`, matriz de 8 funciones CENEPRED y factores agravantes.
  - `testGenerarAnexo4CondicionesSeguridadOficial`: Valida cabecera `%PDF-`, 4 páginas completas y peso superior a 8 KB.
  - `testGenerarDeclaracionJurada`, `testGenerarSolicitudItse`, `testGenerarVoucherSat`, `testGenerarLicenciaPdf`, `testGenerarImagenQr`.

- **`servicio-expedientes`**: 39 pruebas unitarias aprobadas (`BUILD SUCCESS`, 0 errores).
  - Incluye pruebas de concurrencia (150 usuarios simultáneos), cálculo de tasas, transiciones de estado, generación de PDFs oficiales de Anexos 1, 3 y 4, vouchers SAT y licencias QR.

---

## 6. Conclusión y Próximos Pasos (Fase 03)

Con la culminación de la **Fase 02**, la plataforma municipal cuenta con el motor completo y verificado para emitir e imprimir los documentos estándar obligatorios de la Municipalidad Provincial de Huamanga. 

Los pasos a seguir en la **Fase 03** comprenderán:
1. **Frontend del Portal Ciudadano y Mesa de Partes Virtual**: Integración del formulario dinámico multipaso que captura los Anexos 1, 3 y 4.
2. **Visualizador e Impresión en Línea**: Componente frontend para previsualizar y descargar en PDF los formatos con renderizado instantáneo.
3. **Bandejas de Derivación para Funcionarios**: Vistas especializadas para los inspectores de Defensa Civil (calificación de Anexo 3) y analistas urbanos.
