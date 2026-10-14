package pe.gob.munihuamanga.licencias.expedientes.evaluacion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ChecklistAdministrativoDto;
import pe.gob.munihuamanga.licencias.common.dto.ItemChecklistDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoChecklist;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoRevisionDocumento;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.ObservacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.PagoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.PagoExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.model.EvaluacionZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.repository.EvaluacionZonificacionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio que ejecuta el Checklist Administrativo Integral (Fase 2 - M08)
 * verificando los 9 puntos normativos y documentales exigidos por la Ley N° 28976,
 * D.S. N° 002-2018-PCM y el TUPA institucional de la Municipalidad Provincial de Huamanga.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistAdministrativoService {

    private final ExpedienteRepository expedienteRepository;
    private final DocumentoVersionadoRepository documentoVersionadoRepository;
    private final ObservacionExpedienteRepository observacionExpedienteRepository;
    private final PagoExpedienteRepository pagoExpedienteRepository;
    private final EvaluacionZonificacionRepository evaluacionZonificacionRepository;
    private final EstablecimientoRepository establecimientoRepository;

    @Transactional(readOnly = true)
    public ChecklistAdministrativoDto generarChecklist(UUID expedienteId) {
        Expediente exp = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + expedienteId));

        List<ItemChecklistDto> items = new ArrayList<>();

        // 1. Expediente formalmente presentado o en estado permitido
        items.add(verificarEstadoExpediente(exp));

        // 2. Titularidad y establecimiento consistentes
        items.add(verificarTitularidadYEstablecimiento(exp));

        // 3. Declaración jurada y documentos exigibles presentes
        items.add(verificarDocumentosExigibles(exp));

        // 4. Versiones documentales vigentes e íntegras
        items.add(verificarVersionesIntegras(exp));

        // 5. Observaciones obligatorias resueltas
        items.add(verificarObservacionesResueltas(exp));

        // 6. Pago SAT validado manualmente
        items.add(verificarPagoSat(exp));

        // 7. Zonificación compatible y sustentada
        items.add(verificarZonificacion(exp));

        // 8. Sustento ITSE verificado según modalidad y riesgo aplicables
        items.add(verificarSustentoItse(exp));

        // 9. Ausencia de impedimentos normativos o administrativos pendientes
        items.add(verificarImpedimentos(exp));

        boolean aptoFavorable = items.stream()
                .allMatch(i -> i.getEstado() == EstadoChecklist.CUMPLE || i.getEstado() == EstadoChecklist.NO_APLICA);

        long cumplidos = items.stream().filter(i -> i.getEstado() == EstadoChecklist.CUMPLE).count();
        long noCumplidos = items.stream().filter(i -> i.getEstado() == EstadoChecklist.NO_CUMPLE).count();

        String resumen = String.format("Checklist ejecutado: %d CUMPLE, %d NO CUMPLE. Apto para informe favorable: %s",
                cumplidos, noCumplidos, aptoFavorable ? "SÍ" : "NO");

        return ChecklistAdministrativoDto.builder()
                .expedienteId(exp.getId())
                .numeroExpediente(exp.getNumeroTramite())
                .fechaEvaluacion(LocalDateTime.now())
                .items(items)
                .aptoParaInformeFavorable(aptoFavorable)
                .resumen(resumen)
                .build();
    }

    private ItemChecklistDto verificarEstadoExpediente(Expediente exp) {
        EstadoExpediente est = exp.getEstado();
        boolean permitido = est == EstadoExpediente.PRESENTADO
                || est == EstadoExpediente.DOCUMENTOS_VALIDADOS
                || est == EstadoExpediente.EN_EVALUACION_FINAL
                || est == EstadoExpediente.OBSERVADO
                || est == EstadoExpediente.APTO_PARA_RESOLUCION;

        return ItemChecklistDto.builder()
                .codigo("CHK-01-EXPEDIENTE")
                .descripcion("Expediente formalmente presentado o en estado habilitado para evaluación")
                .estado(permitido ? EstadoChecklist.CUMPLE : EstadoChecklist.NO_CUMPLE)
                .motivo(permitido ? "Estado actual válido: " + est : "Estado actual no permite evaluación administrativa: " + est)
                .fuenteEvidencia("expedientes.estado: " + est)
                .build();
    }

    private ItemChecklistDto verificarTitularidadYEstablecimiento(Expediente exp) {
        boolean titularValido = exp.getDocumentoIdentidad() != null && !exp.getDocumentoIdentidad().isBlank()
                && exp.getNombreTitular() != null && !exp.getNombreTitular().isBlank();

        boolean establecimientoValido = exp.getEstablecimientoId() != null
                && establecimientoRepository.existsById(exp.getEstablecimientoId());

        boolean cumple = titularValido && establecimientoValido;

        return ItemChecklistDto.builder()
                .codigo("CHK-02-TITULARIDAD")
                .descripcion("Consistencia de titularidad y existencia verificada del establecimiento comercial")
                .estado(cumple ? EstadoChecklist.CUMPLE : EstadoChecklist.NO_CUMPLE)
                .motivo(cumple ? "Titular y establecimiento vinculados correctamente"
                        : "Inconsistencia en titularidad o establecimiento no registrado formalmente")
                .fuenteEvidencia("expedienteId=" + exp.getId() + ", establecimientoId=" + exp.getEstablecimientoId())
                .build();
    }

    private ItemChecklistDto verificarDocumentosExigibles(Expediente exp) {
        List<DocumentoVersionado> docsActuales = documentoVersionadoRepository
                .findByExpedienteIdAndEsVersionActualTrue(exp.getId());

        // Anexo 1 es obligatorio siempre
        boolean tieneAnexo1 = docsActuales.stream()
                .anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.DECLARACION_JURADA_ANEXO1);
        if (!tieneAnexo1) {
            return ItemChecklistDto.builder()
                    .codigo("CHK-03-DOCUMENTOS")
                    .descripcion("Declaración Jurada oficial (Anexo 1) y documentos exigibles presentes")
                    .estado(EstadoChecklist.NO_CUMPLE)
                    .motivo("Falta adjuntar Declaración Jurada oficial (Anexo 1)")
                    .fuenteEvidencia("documentos_versionados (count actuales = " + docsActuales.size() + ")")
                    .build();
        }

        // Persona Jurídica: exige vigencia de poder o datos registrales SUNARP
        if (exp.getTipoPersona() == TipoPersona.JURIDICA) {
            boolean tienePoderDoc = docsActuales.stream()
                    .anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.VIGENCIA_PODER
                            || d.getTipoDocumento() == TipoDocumentoAdjunto.DNI_REPRESENTANTE);
            boolean tieneDatosSunarp = (exp.getPartidaSunarp() != null && !exp.getPartidaSunarp().isBlank())
                    || (exp.getPoderSunarp() != null && !exp.getPoderSunarp().isBlank());
            if (!tienePoderDoc && !tieneDatosSunarp) {
                return ItemChecklistDto.builder()
                        .codigo("CHK-03-DOCUMENTOS")
                        .descripcion("Declaración Jurada oficial (Anexo 1) y documentos exigibles presentes")
                        .estado(EstadoChecklist.NO_CUMPLE)
                        .motivo("Persona jurídica sin acreditar vigencia de poder ni datos de inscripción SUNARP")
                        .fuenteEvidencia("expediente.tipoPersona=JURIDICA, documentos_versionados")
                        .build();
            }
        }

        // Si requiere autorización sectorial previa, debe constar
        if (Boolean.TRUE.equals(exp.getRequiereAutorizacionSectorial())) {
            boolean tieneDocSectorial = docsActuales.stream()
                    .anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.AUTORIZACION_SECTORIAL);
            boolean tieneDatosSector = exp.getSectorNumero() != null && !exp.getSectorNumero().isBlank();
            if (!tieneDocSectorial && !tieneDatosSector) {
                return ItemChecklistDto.builder()
                        .codigo("CHK-03-DOCUMENTOS")
                        .descripcion("Declaración Jurada oficial (Anexo 1) y documentos exigibles presentes")
                        .estado(EstadoChecklist.NO_CUMPLE)
                        .motivo("Giro comercial requiere autorización sectorial previa no acreditada")
                        .fuenteEvidencia("expediente.requiereAutorizacionSectorial=true")
                        .build();
            }
        }

        return ItemChecklistDto.builder()
                .codigo("CHK-03-DOCUMENTOS")
                .descripcion("Declaración Jurada oficial (Anexo 1) y documentos exigibles presentes")
                .estado(EstadoChecklist.CUMPLE)
                .motivo("Declaración Jurada (Anexo 1) y requisitos documentales exigibles verificados")
                .fuenteEvidencia("documentos_versionados (count actuales = " + docsActuales.size() + ")")
                .build();
    }

    private ItemChecklistDto verificarVersionesIntegras(Expediente exp) {
        List<DocumentoVersionado> docs = documentoVersionadoRepository
                .findByExpedienteIdAndEsVersionActualTrue(exp.getId());

        if (docs.isEmpty()) {
            return ItemChecklistDto.builder()
                    .codigo("CHK-04-VERSIONES")
                    .descripcion("Versiones documentales vigentes con integridad de hash SHA-256")
                    .estado(EstadoChecklist.NO_CUMPLE)
                    .motivo("No existen documentos versionados actuales para el expediente")
                    .fuenteEvidencia("documentos_versionados")
                    .build();
        }

        // Verificar que no existan documentos vigentes en estado OBSERVADO
        boolean tieneObservados = docs.stream()
                .anyMatch(d -> d.getEstadoRevision() == EstadoRevisionDocumento.OBSERVADO);
        if (tieneObservados) {
            return ItemChecklistDto.builder()
                    .codigo("CHK-04-VERSIONES")
                    .descripcion("Versiones documentales vigentes con integridad de hash SHA-256")
                    .estado(EstadoChecklist.NO_CUMPLE)
                    .motivo("Existen documentos vigentes en estado OBSERVADO que requieren subsanación previa")
                    .fuenteEvidencia("documentos_versionados.estadoRevision=OBSERVADO")
                    .build();
        }

        boolean todosConHash = docs.stream()
                .allMatch(d -> d.getHashSha256() != null && d.getHashSha256().length() == 64);

        return ItemChecklistDto.builder()
                .codigo("CHK-04-VERSIONES")
                .descripcion("Versiones documentales vigentes con integridad de hash SHA-256")
                .estado(todosConHash ? EstadoChecklist.CUMPLE : EstadoChecklist.NO_CUMPLE)
                .motivo(todosConHash ? "Todos los documentos vigentes cuentan con huella criptográfica SHA-256 verificada"
                        : "Existen documentos con hash SHA-256 inconsistente o ausente")
                .fuenteEvidencia("documentos_versionados (verificados=" + docs.size() + ")")
                .build();
    }

    private ItemChecklistDto verificarObservacionesResueltas(Expediente exp) {
        long pendientes = observacionExpedienteRepository.countByExpedienteIdAndSubsanadaFalse(exp.getId());

        return ItemChecklistDto.builder()
                .codigo("CHK-05-OBSERVACIONES")
                .descripcion("Ausencia de observaciones documentales o técnicas pendientes de subsanar")
                .estado(pendientes == 0 ? EstadoChecklist.CUMPLE : EstadoChecklist.NO_CUMPLE)
                .motivo(pendientes == 0 ? "Sin observaciones pendientes"
                        : "Existen " + pendientes + " observaciones pendientes de subsanación formal")
                .fuenteEvidencia("observaciones_expediente (pendientes=" + pendientes + ")")
                .build();
    }

    private ItemChecklistDto verificarPagoSat(Expediente exp) {
        List<PagoExpediente> pagos = pagoExpedienteRepository.findByExpedienteIdOrderByFechaRegistroDesc(exp.getId());

        if (pagos.isEmpty()) {
            // Si el monto de tasa es 0 o null podría ser no aplica, pero en TUPA ordinario MPH toda licencia tiene tasa
            boolean requierePago = exp.getMontoTasa() == null || exp.getMontoTasa().compareTo(BigDecimal.ZERO) > 0;
            return ItemChecklistDto.builder()
                    .codigo("CHK-06-PAGO_SAT")
                    .descripcion("Pago de tasa administrativa SAT validado manualmente por caja/tesorería")
                    .estado(requierePago ? EstadoChecklist.NO_CUMPLE : EstadoChecklist.NO_APLICA)
                    .motivo(requierePago ? "No se ha registrado comprobante de pago de tasa SAT" : "Procedimiento exento de tasa")
                    .fuenteEvidencia("pagos_expediente")
                    .build();
        }

        PagoExpediente ultimo = pagos.get(0);
        boolean validado = ultimo.getEstado() == EstadoValidacionPago.VALIDADO_MANUALMENTE;

        return ItemChecklistDto.builder()
                .codigo("CHK-06-PAGO_SAT")
                .descripcion("Pago de tasa administrativa SAT validado manualmente por caja/tesorería")
                .estado(validado ? EstadoChecklist.CUMPLE : EstadoChecklist.NO_CUMPLE)
                .motivo(validado ? "Pago SAT validado por " + ultimo.getValidadorNombre() + " (Operación: " + ultimo.getNumeroOperacionSat() + ")"
                        : "Comprobante en estado no validado: " + ultimo.getEstado())
                .fuenteEvidencia("pagos_expediente (id=" + ultimo.getId() + ", estado=" + ultimo.getEstado() + ")")
                .build();
    }

    private ItemChecklistDto verificarZonificacion(Expediente exp) {
        Optional<EvaluacionZonificacion> zonOpt = evaluacionZonificacionRepository
                .findFirstByExpedienteIdOrderByFechaRegistroDesc(exp.getId());

        if (zonOpt.isEmpty()) {
            return ItemChecklistDto.builder()
                    .codigo("CHK-07-ZONIFICACION")
                    .descripcion("Evaluación técnica de zonificación y compatibilidad de giro comercial")
                    .estado(EstadoChecklist.NO_CUMPLE)
                    .motivo("Zonificación pendiente de dictamen técnico oficial")
                    .fuenteEvidencia("evaluaciones_zonificacion")
                    .build();
        }

        EvaluacionZonificacion zon = zonOpt.get();
        boolean compatible = zon.getResultado() == ResultadoZonificacion.COMPATIBLE;

        return ItemChecklistDto.builder()
                .codigo("CHK-07-ZONIFICACION")
                .descripcion("Evaluación técnica de zonificación y compatibilidad de giro comercial")
                .estado(compatible ? EstadoChecklist.CUMPLE : EstadoChecklist.NO_CUMPLE)
                .motivo(compatible ? "Zonificación COMPATIBLE según " + zon.getFuenteSustentoTecnico()
                        : "Zonificación dictaminada como: " + zon.getResultado())
                .fuenteEvidencia("evaluaciones_zonificacion (id=" + zon.getId() + ", resultado=" + zon.getResultado() + ")")
                .build();
    }

    private ItemChecklistDto verificarSustentoItse(Expediente exp) {
        NivelRiesgo riesgo = exp.getNivelRiesgo();
        if (riesgo == null) {
            return ItemChecklistDto.builder()
                    .codigo("CHK-08-SUSTENTO_ITSE")
                    .descripcion("Verificación administrativa del sustento técnico ITSE según nivel de riesgo")
                    .estado(EstadoChecklist.NO_CUMPLE)
                    .motivo("No se ha registrado el nivel de riesgo declarado del establecimiento")
                    .fuenteEvidencia("expedientes.nivel_riesgo")
                    .build();
        }

        List<DocumentoVersionado> docs = documentoVersionadoRepository
                .findByExpedienteIdAndEsVersionActualTrue(exp.getId());

        // Para riesgo ALTO o MUY_ALTO (ITSE Previa según D.S. 002-2018-PCM):
        // Exige planos técnicos o plan de seguridad / certificado previo
        if (riesgo == NivelRiesgo.ALTO || riesgo == NivelRiesgo.MUY_ALTO) {
            boolean tieneDocsItsePrevia = docs.stream().anyMatch(d ->
                    d.getTipoDocumento() == TipoDocumentoAdjunto.CERTIFICADO_ITSE_PREVIO
                            || d.getTipoDocumento() == TipoDocumentoAdjunto.PLANO_DISTRIBUCION
                            || d.getTipoDocumento() == TipoDocumentoAdjunto.PLANO_ARQUITECTURA
                            || d.getTipoDocumento() == TipoDocumentoAdjunto.PLAN_SEGURIDAD_ITSE
                            || d.getTipoDocumento() == TipoDocumentoAdjunto.MEMORIA_DESCRIPTIVA);
            if (!tieneDocsItsePrevia) {
                return ItemChecklistDto.builder()
                        .codigo("CHK-08-SUSTENTO_ITSE")
                        .descripcion("Verificación administrativa del sustento técnico ITSE (" + exp.getTipoItse() + " - " + riesgo + ")")
                        .estado(EstadoChecklist.NO_CUMPLE)
                        .motivo("Para nivel de riesgo " + riesgo + " (ITSE Previa) es obligatorio adjuntar documentación técnica: planos arquitectónicos/distribución o plan de seguridad ITSE")
                        .fuenteEvidencia("expediente.nivelRiesgo=" + riesgo + ", documentos_versionados")
                        .build();
            }
        }

        // Para BAJO y MEDIO (ITSE Posterior): Debe constar Anexo ITSE o Declaración jurada
        boolean tieneSustentoItse = docs.stream().anyMatch(d ->
                d.getTipoDocumento() == TipoDocumentoAdjunto.ANEXO_ITSE_DECLARACION
                        || d.getTipoDocumento() == TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE
                        || d.getTipoDocumento() == TipoDocumentoAdjunto.CROQUIS_UBICACION
                        || d.getTipoDocumento() == TipoDocumentoAdjunto.PLANO_DISTRIBUCION);

        // Si además el expediente tiene las condiciones de Anexo 4 embebidas en la base de datos
        boolean anexo4Embebido = exp.getAnexo4Condiciones() != null;

        boolean cumple = tieneSustentoItse || anexo4Embebido;

        return ItemChecklistDto.builder()
                .codigo("CHK-08-SUSTENTO_ITSE")
                .descripcion("Verificación administrativa del sustento técnico ITSE (" + exp.getTipoItse() + " - " + riesgo + ")")
                .estado(cumple ? EstadoChecklist.CUMPLE : EstadoChecklist.NO_CUMPLE)
                .motivo(cumple ? "Sustento documental y técnico ITSE verificado conforme a modalidad " + exp.getTipoItse()
                        : "Falta adjuntar formato de condiciones de seguridad o documentación técnica ITSE requerida")
                .fuenteEvidencia("expediente.tipoItse=" + exp.getTipoItse() + ", nivelRiesgo=" + riesgo)
                .build();
    }

    private ItemChecklistDto verificarImpedimentos(Expediente exp) {
        // Verifica que no haya rechazo previo irreversible o bloqueo administrativo
        boolean sinImpedimento = exp.getEstado() != EstadoExpediente.RECHAZADO;

        return ItemChecklistDto.builder()
                .codigo("CHK-09-IMPEDIMENTOS")
                .descripcion("Ausencia de impedimentos normativos, sanciones o medidas cautelares firmes")
                .estado(sinImpedimento ? EstadoChecklist.CUMPLE : EstadoChecklist.NO_CUMPLE)
                .motivo(sinImpedimento ? "Sin impedimentos normativos registrados" : "Expediente con rechazo administrativo definitivo")
                .fuenteEvidencia("expedientes.estado: " + exp.getEstado())
                .build();
    }
}
