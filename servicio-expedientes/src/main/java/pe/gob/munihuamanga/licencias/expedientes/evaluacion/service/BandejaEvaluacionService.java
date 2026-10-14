package pe.gob.munihuamanga.licencias.expedientes.evaluacion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.BandejaEvaluacionDto;
import pe.gob.munihuamanga.licencias.common.dto.FiltroBandejaEvaluacionDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoValidacionPago;
import pe.gob.munihuamanga.licencias.common.enums.ResultadoZonificacion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.DocumentoVersionadoRepository;
import pe.gob.munihuamanga.licencias.expedientes.documentos.repository.ObservacionExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.model.InformeAdministrativo;
import pe.gob.munihuamanga.licencias.expedientes.evaluacion.repository.InformeAdministrativoRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.model.PagoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.pagos.repository.PagoExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.model.EvaluacionZonificacion;
import pe.gob.munihuamanga.licencias.expedientes.zonificacion.repository.EvaluacionZonificacionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de Bandeja Unificada de Evaluación Administrativa (Fase 1 - M08).
 * Consulta y proyecta en tiempo real el estado integral del expediente desde sus módulos de origen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BandejaEvaluacionService {

    private final ExpedienteRepository expedienteRepository;
    private final PagoExpedienteRepository pagoExpedienteRepository;
    private final EvaluacionZonificacionRepository evaluacionZonificacionRepository;
    private final DocumentoVersionadoRepository documentoVersionadoRepository;
    private final ObservacionExpedienteRepository observacionExpedienteRepository;
    private final InformeAdministrativoRepository informeAdministrativoRepository;
    private final EstablecimientoRepository establecimientoRepository;

    @Transactional(readOnly = true)
    public Page<BandejaEvaluacionDto> listarBandeja(FiltroBandejaEvaluacionDto filtro, Pageable pageable) {
        String busqueda = (filtro != null && filtro.getBusqueda() != null && !filtro.getBusqueda().isBlank())
                ? filtro.getBusqueda().trim() : null;

        Page<Expediente> pagina = expedienteRepository.buscarConFiltros(
                filtro != null ? filtro.getEstadoExpediente() : null,
                filtro != null ? filtro.getNivelRiesgo() : null,
                busqueda,
                pageable
        );

        List<BandejaEvaluacionDto> dtos = new ArrayList<>();

        for (Expediente exp : pagina.getContent()) {
            // Pago SAT desde módulo original
            List<PagoExpediente> pagos = pagoExpedienteRepository.findByExpedienteIdOrderByFechaRegistroDesc(exp.getId());
            EstadoValidacionPago estadoPago = pagos.isEmpty() ? null : pagos.get(0).getEstado();

            // Filtrado adicional en memoria si el filtro de pago fue especificado
            if (filtro != null && filtro.getEstadoPago() != null && estadoPago != filtro.getEstadoPago()) {
                continue;
            }

            // Zonificación desde módulo original
            Optional<EvaluacionZonificacion> zonOpt = evaluacionZonificacionRepository
                    .findFirstByExpedienteIdOrderByFechaRegistroDesc(exp.getId());
            ResultadoZonificacion resZon = zonOpt.map(EvaluacionZonificacion::getResultado).orElse(null);

            // Filtrado adicional si se especificó resultado de zonificación
            if (filtro != null && filtro.getResultadoZonificacion() != null && resZon != filtro.getResultadoZonificacion()) {
                continue;
            }

            // Establecimiento
            String estabNombre = exp.getNombreComercial();
            String estabDireccion = exp.getDireccionEstablecimiento();
            if (exp.getEstablecimientoId() != null) {
                Optional<Establecimiento> estOpt = establecimientoRepository.findById(exp.getEstablecimientoId());
                if (estOpt.isPresent()) {
                    estabNombre = estOpt.get().getNombreComercial();
                    estabDireccion = estOpt.get().getDireccion();
                }
            }

            // Observaciones pendientes
            long obsPendientes = observacionExpedienteRepository.countByExpedienteIdAndSubsanadaFalse(exp.getId());

            // Sustento ITSE
            boolean tieneSustentoItse = exp.getAnexo4Condiciones() != null
                    || documentoVersionadoRepository.findByExpedienteIdAndEsVersionActualTrue(exp.getId()).stream()
                    .anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.ANEXO_ITSE_DECLARACION
                            || d.getTipoDocumento() == TipoDocumentoAdjunto.EVIDENCIA_RIESGO_ITSE
                            || d.getTipoDocumento() == TipoDocumentoAdjunto.PLANO_DISTRIBUCION);

            // Alerta de plazo legal
            LocalDate fechaLimite = exp.getFechaLimite() != null ? exp.getFechaLimite().toLocalDate() : null;
            String alertaPlazo = "DENTRO_DEL_PLAZO";
            if (fechaLimite != null) {
                LocalDate hoy = LocalDate.now();
                if (hoy.isAfter(fechaLimite)) {
                    alertaPlazo = "VENCIDO";
                } else if (!hoy.plusDays(2).isBefore(fechaLimite)) {
                    alertaPlazo = "PROXIMO_A_VENCER";
                }
            }

            // Informe administrativo actual
            Optional<InformeAdministrativo> infOpt = informeAdministrativoRepository
                    .findByExpedienteIdAndEsVersionActualTrue(exp.getId());

            BandejaEvaluacionDto dto = BandejaEvaluacionDto.builder()
                    .expedienteId(exp.getId())
                    .numeroExpediente(exp.getNumeroTramite())
                    .titularNombre(exp.getRazonSocial() != null && !exp.getRazonSocial().isBlank() ? exp.getRazonSocial() : exp.getNombreTitular())
                    .titularDocumento(exp.getDocumentoIdentidad())
                    .establecimientoId(exp.getEstablecimientoId())
                    .establecimientoNombre(estabNombre)
                    .establecimientoDireccion(estabDireccion)
                    .fechaPresentacion(exp.getFechaCreacion())
                    .estadoExpediente(exp.getEstado())
                    .estadoPagoSat(estadoPago)
                    .resultadoZonificacion(resZon)
                    .nivelRiesgoDeclarado(exp.getNivelRiesgo())
                    .sustentoItsePresente(tieneSustentoItse)
                    .observacionesPendientes(obsPendientes)
                    .responsableAsignado(infOpt.map(InformeAdministrativo::getEvaluadorNombre).orElse("Sin asignar"))
                    .fechaLimite(fechaLimite)
                    .alertaPlazo(alertaPlazo)
                    .informeId(infOpt.map(InformeAdministrativo::getId).orElse(null))
                    .informeEstado(infOpt.map(InformeAdministrativo::getEstado).orElse(null))
                    .informeConclusion(infOpt.map(InformeAdministrativo::getConclusion).orElse(null))
                    .build();

            dtos.add(dto);
        }

        return new PageImpl<>(dtos, pageable, pagina.getTotalElements());
    }
}
