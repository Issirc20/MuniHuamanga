# Marco Normativo y Reglas Legales de Licencia de Funcionamiento
### Municipalidad Provincial de Huamanga

---

## 1. Base Legal Primaria

1. **Ley N° 28976** — *Ley Marco de Licencia de Funcionamiento*.
2. **Decreto Supremo N° 046-2017-PCM** — *Texto Único Ordenado (TUO) de la Ley N° 28976*.
3. **Decreto Supremo N° 002-2018-PCM** — *Reglamento de Inspecciones Técnicas de Seguridad en Edificaciones (ITSE)*.
4. **Ley N° 27444** — *Ley del Procedimiento Administrativo General (Principio de Presunción de Veracidad y Silencio Positivo)*.

---

## 2. Principios y Reglas Incorporadas en la Arquitectura

### A. Plazo Máximo Legal: 15 Días Hábiles
- El Art. 8 del TUO de la Ley N° 28976 estipula que el plazo máximo para resolver la solicitud de licencia de funcionamiento es de **quince (15) días hábiles**.
- **Impacto en el Software:**
  - El campo `fecha_limite` en la entidad `Expediente` se autocalcula sumando 15 días hábiles a partir de la fecha de registro.
  - El sistema activa una alerta visual preventiva cuando restan 3 días hábiles (`alertaVencimiento = true` segun el RNF-22) para mitigar el riesgo de silencio administrativo.

### B. Procedimiento de Evaluación Previa y Silencio Administrativo Positivo
- Todo el procedimiento está sujeto al **silencio administrativo positivo** si la entidad no se pronuncia dentro de los 15 días hábiles.
- La trazabilidad inmutable mediante `HistorialEstado` y `AuditoriaService` salvaguarda la responsabilidad de los funcionarios y la certeza jurídica del administrado.

### C. Inspección Técnica de Seguridad en Edificaciones (ITSE)
- La clasificación del nivel de riesgo del establecimiento determina la vía de inspección y la tasa administrativa:
  - **Riesgo Bajo / Riesgo Medio:** ITSE posterior (la licencia se emite y la inspección se realiza con posterioridad).
  - **Riesgo Alto / Riesgo Muy Alto:** ITSE previa (la inspección técnica de Defensa Civil debe ser favorable antes de emitir la resolución y licencia).
- La arquitectura desacopla el dictamen de Defensa Civil mediante el `Adaptador de Integración` en Fase 1 y conector en Fase 2.

### D. Firma Digital y Verificación Pública por Código QR
- Conforme a la Ley de Firmas y Certificados Digitales (Ley N° 27269), los actos administrativos emitidos por medios electrónicos deben contar con firma digital.
- El código QR único (`RNF-13` y `RNF-20`) permite a la ciudadanía y al cuerpo de fiscalizadores de la municipalidad comprobar in situ que la licencia no ha sido alterada o falsificada.
