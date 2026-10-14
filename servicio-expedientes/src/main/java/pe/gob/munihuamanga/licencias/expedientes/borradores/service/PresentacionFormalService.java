package pe.gob.munihuamanga.licencias.expedientes.borradores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.PresentarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidacionRequisitosDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SolicitudBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PresentacionFormalService {

    private final SolicitudBorradorRepository borradorRepository;
    private final SolicitudBorradorService borradorService;
    private final SecuenciaTramiteService secuenciaTramiteService;
    private final ExpedienteRepository expedienteRepository;
    private final OperadorCuentaRepository operadorCuentaRepository;
    private final ExpedienteMapper expedienteMapper;
    private final AuditoriaService auditoriaService;
    private final pe.gob.munihuamanga.licencias.expedientes.documentos.service.DocumentoVersionadoService documentoVersionadoService;

    public PresentacionFormalService(
            SolicitudBorradorRepository borradorRepository,
            SolicitudBorradorService borradorService,
            SecuenciaTramiteService secuenciaTramiteService,
            ExpedienteRepository expedienteRepository,
            OperadorCuentaRepository operadorCuentaRepository,
            ExpedienteMapper expedienteMapper,
            AuditoriaService auditoriaService
    ) {
        this(borradorRepository, borradorService, secuenciaTramiteService, expedienteRepository,
                operadorCuentaRepository, expedienteMapper, auditoriaService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public PresentacionFormalService(
            SolicitudBorradorRepository borradorRepository,
            SolicitudBorradorService borradorService,
            SecuenciaTramiteService secuenciaTramiteService,
            ExpedienteRepository expedienteRepository,
            OperadorCuentaRepository operadorCuentaRepository,
            ExpedienteMapper expedienteMapper,
            AuditoriaService auditoriaService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            pe.gob.munihuamanga.licencias.expedientes.documentos.service.DocumentoVersionadoService documentoVersionadoService
    ) {
        this.borradorRepository = borradorRepository;
        this.borradorService = borradorService;
        this.secuenciaTramiteService = secuenciaTramiteService;
        this.expedienteRepository = expedienteRepository;
        this.operadorCuentaRepository = operadorCuentaRepository;
        this.expedienteMapper = expedienteMapper;
        this.auditoriaService = auditoriaService;
        this.documentoVersionadoService = documentoVersionadoService;
    }

    @Transactional
    public ExpedienteResponseDto presentarBorrador(
            UUID borradorId,
            PresentarBorradorDto dto,
            UUID usuarioId,
            String username
    ) {
        SolicitudBorrador borrador = borradorRepository.findByIdWithLock(borradorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Borrador no encontrado con ID: " + borradorId));

        // 1. Garantía de Idempotencia: Si ya fue presentado, retornar el expediente existente sin duplicar
        if (borrador.getEstado() == EstadoBorrador.PRESENTADO && borrador.getExpediente() != null) {
            log.info("[IDEMPOTENCIA-PRESENTACION] El borrador {} ya fue presentado previamente. Retornando expediente {}",
                    borradorId, borrador.getExpediente().getNumeroTramite());
            return expedienteMapper.toDto(borrador.getExpediente());
        }

        if (borrador.getEstado() == EstadoBorrador.DESCARTADO) {
            throw new IllegalStateException("No se puede presentar un borrador que fue descartado.");
        }

        Cuenta cuenta = borrador.getCuenta();
        Establecimiento est = borrador.getEstablecimiento();

        // 2. Validar que la cuenta esté activa y correo verificado
        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new IllegalStateException("La cuenta del administrado se encuentra en estado " + cuenta.getEstado() + ". Debe estar ACTIVA para formalizar trámites.");
        }
        if (!Boolean.TRUE.equals(cuenta.getEmailVerificado())) {
            throw new IllegalStateException("El correo electrónico de la cuenta no ha sido verificado. Active su cuenta antes de presentar solicitudes formales.");
        }

        // 3. Validar establecimiento vinculado
        if (!est.getCuenta().getId().equals(cuenta.getId())) {
            throw new IllegalStateException("El establecimiento declarado no pertenece a la cuenta titular.");
        }

        // 4. Validar permisos del operador si es persona jurídica y acreditación de representación legal
        if (cuenta.getTipoPersona() == TipoPersona.JURIDICA) {
            if (usuarioId != null && !usuarioId.equals(cuenta.getId())) {
                OperadorCuenta operador = operadorCuentaRepository.findByCuentaIdAndUsuarioId(cuenta.getId(), usuarioId)
                        .orElse(null);

                if (operador == null || !Boolean.TRUE.equals(operador.getActivo()) ||
                        !operador.getPermisos().contains(PermisoOperador.PRESENTAR_EXPEDIENTE)) {
                    log.warn("[SEGURIDAD-OPERADOR] Usuario {} no tiene permiso PRESENTAR_EXPEDIENTE en cuenta {}", usuarioId, cuenta.getId());
                    throw new org.springframework.security.access.AccessDeniedException(
                            "No cuenta con el permiso PRESENTAR_EXPEDIENTE asignado para esta persona jurídica."
                    );
                }
            }

            // Exigencia de acreditación formal de personería jurídica (no basta conocer un RUC o tener correo verificado)
            boolean identidadOficial = Boolean.TRUE.equals(cuenta.getIdentidadValidadaOficialmente());
            boolean tienePoderRegistral = borrador.getDocumentos() != null && borrador.getDocumentos().stream()
                    .anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.VIGENCIA_PODER);
            if (!identidadOficial && !tienePoderRegistral) {
                tienePoderRegistral = borradorService.listarDocumentos(borradorId).stream()
                        .anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.VIGENCIA_PODER);
            }
            if (!identidadOficial && !tienePoderRegistral) {
                log.warn("[SEGURIDAD-REPRESENTACION] Intento de presentación formal de persona jurídica RUC: {} sin acreditación de personería",
                        cuenta.getNumeroDocumento());
                throw new IllegalStateException(
                        "Para personas jurídicas, se exige acreditar formalmente la representación legal mediante vigencia de poder registral (SUNARP) adjunta o validación oficial de identidad. Conocer el RUC o verificar un correo no es suficiente."
                );
            }
        }

        // 5. Validar requisitos completos del borrador
        ValidacionRequisitosDto validacion = borradorService.validarRequisitos(borradorId);
        if (!validacion.isAptoParaPresentacion()) {
            throw new IllegalStateException(
                    "Requisitos obligatorios incompletos: Campos faltantes: " + validacion.getCamposFaltantes()
                            + " | Documentos faltantes: " + validacion.getDocumentosFaltantes()
            );
        }

        // 6. Confirmación expresa
        if (!Boolean.TRUE.equals(dto.getDeclaracionJuradaAceptada()) || !Boolean.TRUE.equals(dto.getConfirmacionPresentacion())) {
            throw new IllegalArgumentException("Debe aceptar expresamente la Declaración Jurada y confirmar la presentación.");
        }

        // 7. Generación atómica y segura de correlativo EXP-YYYY-XXXXX
        String numeroTramite = secuenciaTramiteService.generarSiguienteNumeroExpediente();
        LocalDateTime ahora = LocalDateTime.now();
        UUID expedienteId = UUID.randomUUID();

        // 8. Crear instantánea del expediente formal
        Expediente expediente = Expediente.builder()
                .id(expedienteId)
                .numeroTramite(numeroTramite)
                .solicitanteId(cuenta.getId())
                .cuentaId(cuenta.getId())
                .establecimientoId(est.getId())
                .modalidadTramite(borrador.getModalidadTramite())
                .tipoPersona(cuenta.getTipoPersona())
                .tipoDocumento(cuenta.getTipoDocumento())
                .nombreTitular(cuenta.getRazonSocialONombre())
                .documentoIdentidad(cuenta.getNumeroDocumento())
                .razonSocial(cuenta.getTipoPersona() == TipoPersona.JURIDICA ? cuenta.getRazonSocialONombre() : null)
                .nombreComercial(borrador.getNombreComercialDeclarado() != null ? borrador.getNombreComercialDeclarado() : est.getNombreComercial())
                .ciiuCodigo(est.getCiiu() != null ? est.getCiiu().getCodigo() : null)
                .giroNegocio(borrador.getGiroNegocioDeclarado() != null ? borrador.getGiroNegocioDeclarado() : est.getGiroPrincipal())
                .actividadDetallada(borrador.getActividadDetallada())
                .direccionEstablecimiento(est.getDireccion())
                .distrito(est.getDistrito())
                .provincia(est.getProvincia())
                .departamento(est.getDepartamento())
                .referenciaUbicacion(est.getReferencia())
                .areaMetrosCuadrados(borrador.getAreaMetrosCuadrados())
                .aforoPersonas(borrador.getAforoPersonas())
                .numeroPisos(borrador.getNumeroPisos())
                .horaInicio(borrador.getHorarioInicio())
                .horaFin(borrador.getHorarioFin())
                .telefono(borrador.getTelefonoContacto())
                .correoElectronico(borrador.getCorreoNotificacion())
                .autorizaNotificacion(borrador.getAutorizaNotificacion())
                .nivelRiesgo(borrador.getNivelRiesgoDeclarado())
                .montoTasa(borrador.getMontoTasa() != null ? borrador.getMontoTasa() : new BigDecimal("154.50"))
                .numeroOperacionSat(borrador.getNumeroOperacionSat())
                .fechaPagoSat(borrador.getFechaPagoSat() != null ? borrador.getFechaPagoSat().atStartOfDay() : ahora)
                .voucherId(borrador.getVoucherId() != null ? borrador.getVoucherId() : "VCH-" + borrador.getNumeroOperacionSat())
                .estado(EstadoExpediente.PRESENTADO)
                .fechaCreacion(ahora)
                .fechaLimite(ahora.plusDays(21))
                .build();

        Expediente guardado = expedienteRepository.save(expediente);

        // 9. Actualizar estado del borrador
        borrador.setEstado(EstadoBorrador.PRESENTADO);
        borrador.setExpediente(guardado);
        borrador.setFechaActualizacion(ahora);
        borradorRepository.save(borrador);

        // 10. Migrar documentos adjuntos del borrador al expediente formal en versión 1
        if (documentoVersionadoService != null) {
            try {
                documentoVersionadoService.migrarDocumentosBorradorAExpediente(borradorId, guardado.getId(), usuarioId, username);
            } catch (Exception ex) {
                log.warn("[PRESENTACION-FORMAL] Advertencia al migrar documentos versionados: {}", ex.getMessage());
            }
        }

        // 11. Registrar auditoría del evento de presentación formal
        auditoriaService.registrarTransicion(
                guardado.getId(),
                null,
                EstadoExpediente.PRESENTADO,
                username != null ? username : cuenta.getNumeroDocumento(),
                "Presentación formal desde borrador " + borrador.getId() + (dto.getComentarios() != null ? ". " + dto.getComentarios() : "")
        );

        log.info("[PRESENTACION-FORMAL] Solicitud formalizada con éxito. Borrador ID: {} -> Expediente ID: {} ({})",
                borradorId, guardado.getId(), guardado.getNumeroTramite());

        return expedienteMapper.toDto(guardado);
    }
}
