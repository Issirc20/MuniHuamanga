package pe.gob.munihuamanga.licencias.expedientes.licencias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.EmitirLicenciaDto;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.dto.HistorialEmisionLicenciaDto;
import pe.gob.munihuamanga.licencias.common.dto.LicenciaEmitidaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.EstadoLicencia;
import pe.gob.munihuamanga.licencias.common.enums.EstadoResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoDecisionResolucion;
import pe.gob.munihuamanga.licencias.common.enums.TipoVigenciaLicencia;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.AuditoriaConsultaInspeccion;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.HistorialEmisionLicencia;
import pe.gob.munihuamanga.licencias.expedientes.licencias.model.LicenciaEmitida;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.HistorialEmisionLicenciaRepository;
import pe.gob.munihuamanga.licencias.expedientes.licencias.repository.LicenciaEmitidaRepository;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.model.ResolucionAdministrativa;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.repository.ResolucionAdministrativaRepository;
import pe.gob.munihuamanga.licencias.expedientes.resoluciones.service.ResolucionPdfGenerator;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.service.DocumentoPdfService;
import pe.gob.munihuamanga.licencias.expedientes.service.LicenciaPdfGenerator;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de Emisión Oficial de Licencias de Funcionamiento (M09 - Incremento I08).
 * Deriva licencias exclusivamente a partir de resoluciones gerenciales favorables debidamente firmadas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenciaService {

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    private final LicenciaEmitidaRepository licenciaRepository;
    private final HistorialEmisionLicenciaRepository historialEmisionRepository;
    private final ResolucionAdministrativaRepository resolucionRepository;
    private final ExpedienteRepository expedienteRepository;
    private final pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository establecimientoRepository;
    private final SecuenciaLicenciaService secuenciaLicenciaService;
    private final LicenciaPdfGenerator licenciaPdfGenerator;
    private final DocumentoPdfService documentoPdfService;
    private final ExpedienteMapper expedienteMapper;
    private final AuditoriaService auditoriaService;
    private final NotificacionEmailService notificacionEmailService;
    private final pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ComandosExpediente comandosExpediente;
    private final pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor proveedorActor;
    private final pe.gob.munihuamanga.licencias.expedientes.rectificacion.repository.SolicitudRectificacionUbicacionRepository solicitudRectificacionUbicacionRepository;
    private final pe.gob.munihuamanga.licencias.expedientes.licencias.repository.AuditoriaConsultaInspeccionRepository auditoriaConsultaInspeccionRepository;

    @Transactional
    public LicenciaEmitidaDto emitirLicencia(EmitirLicenciaDto dto, Authentication auth) {
        String actor = (auth != null) ? auth.getName() : "SISTEMA";
        log.info("[LICENCIA] Solicitud de emisión de licencia para resolución {} por {}", dto.getResolucionId(), actor);

        ResolucionAdministrativa res = resolucionRepository.findByIdWithLock(dto.getResolucionId())
                .orElseThrow(() -> new IllegalArgumentException("Resolución no encontrada: " + dto.getResolucionId()));

        // Regla 1: Sentido favorable obligatorio
        if (res.getTipoDecision() != TipoDecisionResolucion.FAVORABLE) {
            throw new IllegalStateException("No procede emitir licencia para una resolución con decisión " + res.getTipoDecision());
        }

        // Regla 2: Firma digital institucional verificada obligatoria
        if (res.getEstado() != EstadoResolucion.FIRMADA) {
            throw new IllegalStateException("No se puede emitir la Licencia de Funcionamiento oficial: "
                    + "La resolución administrativa N° " + res.getNumeroResolucion()
                    + " no cuenta con firma digital institucional válida (Estado: " + res.getEstado() + "). Ley N° 27269.");
        }

        Expediente exp = res.getExpediente();
        Establecimiento est = (exp.getEstablecimientoId() != null)
                ? establecimientoRepository.findById(exp.getEstablecimientoId()).orElse(null)
                : null;

        // Control de Idempotencia y Concurrencia: si ya existe licencia emitida para esta resolución o expediente, retornarla
        Optional<LicenciaEmitida> optExistente = licenciaRepository.findByResolucionId(res.getId());
        if (optExistente.isPresent()) {
            log.info("[LICENCIA] Emisión idempotente: ya existe licencia emitida {} para resolución {}",
                    optExistente.get().getNumeroLicencia(), res.getId());
            return mapearDto(optExistente.get());
        }

        // Concurrencia segura: bloquear el expediente para evitar emisiones concurrentes
        exp = expedienteRepository.findByIdWithLock(exp.getId()).orElse(exp);

        Optional<LicenciaEmitida> optExistenteExp = licenciaRepository.findByExpedienteId(exp.getId());
        if (optExistenteExp.isPresent()) {
            log.info("[LICENCIA] Emisión idempotente: ya existe licencia emitida {} para expediente {}",
                    optExistenteExp.get().getNumeroLicencia(), exp.getId());
            return mapearDto(optExistenteExp.get());
        }

        // Generar correlativo oficial y código de verificación no enumerable con SecureRandom (256 bits)
        String numeroLicencia = secuenciaLicenciaService.generarSiguienteNumeroLicencia();
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);
        String codigoVerificacionQr = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // Determinación de vigencia (Ley N° 28976 Art. 11)
        TipoVigenciaLicencia vigencia = (dto.getTipoVigencia() != null)
                ? dto.getTipoVigencia()
                : (exp.getPlazoTemporalMeses() != null && exp.getPlazoTemporalMeses() > 0 ? TipoVigenciaLicencia.TEMPORAL : TipoVigenciaLicencia.INDETERMINADA);

        LocalDate fechaEmision = LocalDate.now();
        LocalDate fechaCaducidad = null;
        if (vigencia == TipoVigenciaLicencia.TEMPORAL) {
            int meses = (dto.getPlazoMeses() != null && dto.getPlazoMeses() > 0)
                    ? dto.getPlazoMeses()
                    : (exp.getPlazoTemporalMeses() != null ? exp.getPlazoTemporalMeses() : 12);
            fechaCaducidad = fechaEmision.plusMonths(meses);
        }

        // Generar QR bytes y PDF oficial de la licencia
        byte[] qrBytes = documentoPdfService.generarImagenQr(codigoVerificacionQr, 180, 180);

        boolean esSimulada = Boolean.TRUE.equals(res.getEsSimulada());

        ExpedienteResponseDto expDto = expedienteMapper.toDto(exp);
        expDto.setNumeroLicencia(numeroLicencia);
        expDto.setLicenciaQrCode(codigoVerificacionQr);
        expDto.setFirmaDigitalValida(!esSimulada);
        expDto.setFechaAprobacion(fechaEmision.atStartOfDay());

        byte[] pdfLicencia = licenciaPdfGenerator.generarLicencia(expDto, qrBytes);
        String hashPdf = ResolucionPdfGenerator.calcularHashSha256(pdfLicencia);

        LicenciaEmitida licencia = LicenciaEmitida.builder()
                .id(UUID.randomUUID())
                .numeroLicencia(numeroLicencia)
                .resolucion(res)
                .expediente(exp)
                .establecimiento(est)
                .codigoVerificacionQr(codigoVerificacionQr)
                .titularTipoDocumento(exp.getTipoDocumento() != null ? exp.getTipoDocumento().name() : "DNI")
                .titularNumeroDocumento(exp.getDocumentoIdentidad())
                .titularNombreORazonSocial(exp.getNombreTitular())
                .nombreComercial(exp.getNombreComercial())
                .direccionEstablecimiento(exp.getDireccionEstablecimiento())
                .giroActividad(exp.getGiroNegocio())
                .ciiuCodigo(exp.getCiiuCodigo())
                .areaMetrosCuadrados(exp.getAreaMetrosCuadrados())
                .aforoPersonas(exp.getAforoPersonas())
                .zonificacion(exp.getZonificacion())
                .nivelRiesgoItse(exp.getNivelRiesgo() != null ? exp.getNivelRiesgo().name() : "MEDIO")
                .tipoVigencia(vigencia)
                .fechaEmision(fechaEmision)
                .fechaCaducidad(fechaCaducidad)
                .autoridadEmisora("Subgerencia de Comercio, Licencias y Control Sanitario")
                .hashPdf(hashPdf)
                .archivoPdf(pdfLicencia)
                .estado(EstadoLicencia.EMITIDA)
                .esSimulada(esSimulada)
                .observacionesEmision(esSimulada
                        ? "LICENCIA EMITIDA EN MODO DEMOSTRACIÓN — SIN VALIDEZ OFICIAL (LEY N° 27269)"
                        : dto.getObservacionesEmision())
                .fechaCreacion(LocalDateTime.now())
                .creadoPor(actor)
                .build();

        LicenciaEmitida guardada;
        try {
            guardada = licenciaRepository.save(licencia);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("[LICENCIA] Violación de unicidad en concurrencia al emitir licencia: {}", ex.getMessage());
            return licenciaRepository.findByExpedienteId(exp.getId())
                    .or(() -> licenciaRepository.findByResolucionId(res.getId()))
                    .map(this::mapearDto)
                    .orElseThrow(() -> ex);
        }

        // Registro de historial inmutable
        HistorialEmisionLicencia hist = HistorialEmisionLicencia.builder()
                .id(UUID.randomUUID())
                .licencia(guardada)
                .accion("EMISION_OFICIAL")
                .estadoAnterior(null)
                .estadoNuevo(EstadoLicencia.EMITIDA)
                .actor(actor)
                .detalle("Licencia de Funcionamiento emitida con resolución " + res.getNumeroResolucion())
                .fechaRegistro(LocalDateTime.now())
                .build();
        historialEmisionRepository.save(hist);

        // Referencias de la licencia en el expediente, para su consulta y para los formatos documentales
        exp.setNumeroLicencia(numeroLicencia);
        exp.setLicenciaQrCode(codigoVerificacionQr);
        exp.setFirmaDigitalValida(!esSimulada);
        exp.setFechaAprobacion(LocalDateTime.now());
        expedienteRepository.save(exp);

        comandosExpediente.registrar(exp.getId(), EventoExpediente.LICENCIA_EMITIDA,
                "Licencia de funcionamiento " + numeroLicencia + " emitida en mérito de la resolución " + res.getNumeroResolucion() + ".");

        // Notificación al administrado
        try {
            notificacionEmailService.notificarAprobacion(exp);
        } catch (Exception ex) {
            log.warn("[LICENCIA] No se pudo enviar notificación por correo al administrado: {}", ex.getMessage());
        }

        return mapearDto(guardada);
    }

    @Transactional(readOnly = true)
    public LicenciaEmitidaDto obtenerPorId(UUID id) {
        LicenciaEmitida lic = licenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Licencia no encontrada: " + id));
        return mapearDto(lic);
    }

    @Transactional(readOnly = true)
    public LicenciaEmitidaDto obtenerPorExpediente(UUID expedienteId) {
        LicenciaEmitida lic = licenciaRepository.findByExpedienteId(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("No existe licencia emitida para el expediente: " + expedienteId));
        return mapearDto(lic);
    }

    @Transactional(readOnly = true)
    public LicenciaEmitidaDto obtenerPorCodigoQr(String codigoQr) {
        LicenciaEmitida lic = licenciaRepository.findByCodigoVerificacionQr(codigoQr.trim())
                .orElseThrow(() -> new IllegalArgumentException("Código de verificación de licencia no encontrado: " + codigoQr));
        return mapearDto(lic);
    }

    @Transactional(readOnly = true)
    public LicenciaEmitida obtenerEntidadPorExpediente(UUID expedienteId) {
        return licenciaRepository.findByExpedienteId(expedienteId)
                .orElseThrow(() -> new pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException(
                        "El expediente no cuenta con licencia emitida."));
    }

    /**
     * Registra la notificación de la licencia. La primera notificación válida inicia el plazo de recursos.
     */
    @Transactional
    public LicenciaEmitidaDto registrarNotificacion(UUID licenciaId, pe.gob.munihuamanga.licencias.common.dto.RegistrarNotificacionLicenciaDto dto) {
        LicenciaEmitida licencia = licenciaRepository.findByIdWithLock(licenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Licencia no encontrada: " + licenciaId));
        if (licencia.estaNotificada()) {
            throw new IllegalStateException("La licencia ya fue notificada el " + licencia.getFechaNotificacion().toLocalDate() + ".");
        }
        marcarNotificada(licencia, dto.getMedio(), dto.getConstancia());
        return mapearDto(licencia);
    }

    /**
     * Entrega el documento de la licencia. Si quien descarga es el administrado o su representante,
     * la descarga constituye acuse de notificación.
     */
    @Transactional
    public byte[] descargarPdfLicencia(UUID licenciaId) {
        LicenciaEmitida lic = licenciaRepository.findById(licenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Licencia no encontrada: " + licenciaId));

        if (!lic.estaNotificada() && proveedorActor.actual().tieneRol(pe.gob.munihuamanga.licencias.common.enums.RolUsuario.ROLE_CIUDADANO)) {
            marcarNotificada(lic, pe.gob.munihuamanga.licencias.common.enums.MedioNotificacion.DESCARGA_PORTAL,
                    "Descarga desde el portal ciudadano por " + proveedorActor.actual().nombreParaRegistro());
        }

        if (lic.getArchivoPdf() != null && lic.getArchivoPdf().length > 0) {
            return lic.getArchivoPdf();
        }

        byte[] qrBytes = documentoPdfService.generarImagenQr(lic.getCodigoVerificacionQr(), 180, 180);

        ExpedienteResponseDto expDto = expedienteMapper.toDto(lic.getExpediente());
        expDto.setNumeroLicencia(lic.getNumeroLicencia());
        expDto.setLicenciaQrCode(lic.getCodigoVerificacionQr());
        expDto.setFirmaDigitalValida(true);
        expDto.setFechaAprobacion(lic.getFechaEmision().atStartOfDay());

        return licenciaPdfGenerator.generarLicencia(expDto, qrBytes);
    }

    private void marcarNotificada(LicenciaEmitida licencia, pe.gob.munihuamanga.licencias.common.enums.MedioNotificacion medio, String constancia) {
        licencia.setFechaNotificacion(LocalDateTime.now());
        licencia.setMedioNotificacion(medio);
        licencia.setConstanciaNotificacion(constancia);
        licenciaRepository.save(licencia);
        auditoriaService.registrarTransicion(licencia.getExpediente().getId(), licencia.getExpediente().getEstado(),
                licencia.getExpediente().getEstado(), proveedorActor.actual().nombreParaRegistro(),
                "Licencia " + licencia.getNumeroLicencia() + " notificada por " + medio + ".");
    }

    @Transactional(readOnly = true)
    public pe.gob.munihuamanga.licencias.common.dto.VerificacionPublicaQrDto verificarLicenciaPublica(String codigoQr) {
        if (codigoQr == null || codigoQr.trim().isEmpty()) {
            throw new IllegalArgumentException("Código de verificación QR requerido");
        }
        String cod = codigoQr.trim();
        LicenciaEmitida lic = licenciaRepository.findByCodigoVerificacionQr(cod)
                .or(() -> licenciaRepository.findByCodigoVerificacionQr(cod.toUpperCase()))
                .orElseThrow(() -> new IllegalArgumentException("El código QR proporcionado no corresponde a ninguna Licencia de Funcionamiento oficial emitida."));

        boolean esSimulada = Boolean.TRUE.equals(lic.getEsSimulada())
                || (lic.getResolucion() != null && Boolean.TRUE.equals(lic.getResolucion().getEsSimulada()));

        boolean firmaDigitalValida = !esSimulada
                && lic.getResolucion() != null
                && lic.getResolucion().getEstado() == EstadoResolucion.FIRMADA
                && lic.getArchivoPdf() != null && lic.getArchivoPdf().length > 0;

        boolean valida = false;
        String mensaje;

        if (esSimulada) {
            valida = false;
            mensaje = "LICENCIA DE DEMOSTRACIÓN — SIN VALIDEZ OFICIAL: Emitida en entorno de pruebas controlado (Ley N° 27269).";
        } else {
            switch (lic.getEstado()) {
                case SUSPENDIDA -> {
                    valida = false;
                    mensaje = "LICENCIA SUSPENDIDA TEMPORALMENTE - Acto administrativo vigente de suspensión";
                }
                case REVOCADA -> {
                    valida = false;
                    mensaje = "LICENCIA REVOCADA - El título habilitante ha sido revocado y carece de eficacia legal";
                }
                case VENCIDA -> {
                    valida = false;
                    mensaje = "LICENCIA VENCIDA - La autorización temporal ha caducado formalmente";
                }
                case EMITIDA -> {
                    if (!firmaDigitalValida) {
                        valida = false;
                        mensaje = "DOCUMENTO SIN VALIDEZ OFICIAL: Pendiente de firma digital institucional acreditada.";
                    } else if (lic.getTipoVigencia() == TipoVigenciaLicencia.TEMPORAL
                            && lic.getFechaCaducidad() != null
                            && lic.getFechaCaducidad().isBefore(LocalDate.now())) {
                        valida = false;
                        mensaje = "LICENCIA VENCIDA - La vigencia temporal de esta autorización caducó el " + lic.getFechaCaducidad();
                    } else {
                        valida = true;
                        mensaje = "LICENCIA OFICIAL VÁLIDA Y VIGENTE - Municipalidad Provincial de Huamanga";
                    }
                }
                default -> {
                    valida = false;
                    mensaje = "ESTADO DE LICENCIA: " + lic.getEstado();
                }
            }
        }

        return pe.gob.munihuamanga.licencias.common.dto.VerificacionPublicaQrDto.builder()
                .codigoVerificacionQr(lic.getCodigoVerificacionQr())
                .numeroLicencia(lic.getNumeroLicencia())
                .numeroResolucion(lic.getResolucion() != null ? lic.getResolucion().getNumeroResolucion() : null)
                .nombreComercial(lic.getNombreComercial())
                .titularNombreORazonSocial(lic.getTitularNombreORazonSocial())
                .titularDocumentoEnmascarado(null) // DNI eliminado de respuestas públicas según I09 FASE 0.3 y Ley 29733
                .direccionEstablecimiento(lic.getDireccionEstablecimiento())
                .giroActividad(lic.getGiroActividad())
                .ciiuCodigo(lic.getCiiuCodigo())
                .tipoVigencia(lic.getTipoVigencia())
                .fechaEmision(lic.getFechaEmision())
                .fechaCaducidad(lic.getFechaCaducidad())
                .autoridadEmisora(lic.getAutoridadEmisora())
                .estadoLicencia(lic.getEstado())
                .firmaDigitalValida(firmaDigitalValida)
                .valida(valida)
                .esSimulada(esSimulada)
                .mensajeVerificacion(mensaje)
                .fechaConsulta(LocalDateTime.now())
                .build();
    }

    @Transactional
    public pe.gob.munihuamanga.licencias.common.dto.VerificacionInspectorDto verificarLicenciaInspector(
            String token,
            String inspectorUsername,
            String ipOrigen,
            String userAgent) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token o código QR de verificación requerido");
        }
        String cod = token.trim();
        LicenciaEmitida lic = licenciaRepository.findByCodigoVerificacionQr(cod)
                .or(() -> licenciaRepository.findByCodigoVerificacionQr(cod.toUpperCase()))
                .orElseThrow(() -> new IllegalArgumentException("El código QR proporcionado no corresponde a ninguna Licencia oficial emitida."));

        pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento est = lic.getEstablecimiento();

        java.math.BigDecimal latitud = null;
        java.math.BigDecimal longitud = null;
        String tipoCoordenada = "NO_REGISTRADA";
        boolean coordenadasVerificadas = false;

        if (est != null) {
            latitud = est.getLatitud();
            longitud = est.getLongitud();
            if (latitud != null && longitud != null) {
                coordenadasVerificadas = true;
                boolean tieneRectificacionAprobada = solicitudRectificacionUbicacionRepository
                        .findByEstablecimientoIdOrderByFechaSolicitudDesc(est.getId())
                        .stream()
                        .anyMatch(r -> r.getEstado() == pe.gob.munihuamanga.licencias.common.enums.EstadoRectificacionUbicacion.APROBADA);

                tipoCoordenada = tieneRectificacionAprobada ? "RECTIFICADA_APROBADA" : "DECLARADA_OFICIAL";
            }
        }

        String enlaceNavegacion = coordenadasVerificadas
                ? "https://www.google.com/maps/search/?api=1&query=" + latitud + "," + longitud
                : null;

        boolean esSimulada = Boolean.TRUE.equals(lic.getEsSimulada())
                || (lic.getResolucion() != null && Boolean.TRUE.equals(lic.getResolucion().getEsSimulada()));

        boolean firmaDigitalValida = !esSimulada
                && lic.getResolucion() != null
                && lic.getResolucion().getEstado() == EstadoResolucion.FIRMADA
                && lic.getArchivoPdf() != null && lic.getArchivoPdf().length > 0;

        boolean valida = !esSimulada && firmaDigitalValida && lic.getEstado() == EstadoLicencia.EMITIDA;
        if (valida && lic.getTipoVigencia() == TipoVigenciaLicencia.TEMPORAL
                && lic.getFechaCaducidad() != null && lic.getFechaCaducidad().isBefore(LocalDate.now())) {
            valida = false;
        }

        String mensaje;
        if (esSimulada) {
            mensaje = "LICENCIA DE DEMOSTRACIÓN — SIN VALIDEZ OFICIAL (ENTORNO DE PRUEBAS - LEY N° 27269)";
        } else if (valida) {
            mensaje = "LICENCIA OFICIAL VÁLIDA Y VIGENTE - Verificada para fiscalización en campo";
        } else {
            mensaje = "LICENCIA NO VÁLIDA O INACTIVA (" + lic.getEstado() + ")";
        }

        // Registro de auditoría inmutable de la consulta del inspector
        AuditoriaConsultaInspeccion audit = AuditoriaConsultaInspeccion.builder()
                .id(UUID.randomUUID())
                .licencia(lic)
                .tokenQr(token)
                .inspectorUsername(inspectorUsername != null ? inspectorUsername : "INSPECTOR_ANONIMO")
                .fechaConsulta(LocalDateTime.now())
                .ipOrigen(ipOrigen)
                .userAgent(userAgent)
                .resultado(valida ? "CONSULTA_EXITOSA" : (esSimulada ? "CONSULTA_DEMOSTRACION" : "LICENCIA_INACTIVA_" + lic.getEstado()))
                .build();
        auditoriaConsultaInspeccionRepository.save(audit);

        return pe.gob.munihuamanga.licencias.common.dto.VerificacionInspectorDto.builder()
                .codigoVerificacionQr(lic.getCodigoVerificacionQr())
                .numeroLicencia(lic.getNumeroLicencia())
                .numeroExpediente(lic.getExpediente() != null ? lic.getExpediente().getNumeroTramite() : null)
                .numeroResolucion(lic.getResolucion() != null ? lic.getResolucion().getNumeroResolucion() : null)
                .nombreComercial(lic.getNombreComercial())
                .titularNombreORazonSocial(lic.getTitularNombreORazonSocial())
                .titularTipoDocumento(lic.getTitularTipoDocumento())
                .titularNumeroDocumento(lic.getTitularNumeroDocumento())
                .direccionAutorizada(lic.getDireccionEstablecimiento())
                .referenciaUbicacion(est != null ? est.getReferencia() : null)
                .latitud(latitud)
                .longitud(longitud)
                .tipoCoordenada(tipoCoordenada)
                .coordenadasVerificadas(coordenadasVerificadas)
                .enlaceNavegacion(enlaceNavegacion)
                .giroActividad(lic.getGiroActividad())
                .ciiuCodigo(lic.getCiiuCodigo())
                .areaMetrosCuadrados(lic.getAreaMetrosCuadrados())
                .aforoPersonas(lic.getAforoPersonas())
                .zonificacion(lic.getZonificacion())
                .nivelRiesgoItse(lic.getNivelRiesgoItse())
                .tipoVigencia(lic.getTipoVigencia())
                .fechaEmision(lic.getFechaEmision())
                .fechaCaducidad(lic.getFechaCaducidad())
                .autoridadEmisora(lic.getAutoridadEmisora())
                .estadoLicencia(lic.getEstado())
                .firmaDigitalValida(firmaDigitalValida)
                .valida(valida)
                .esSimulada(esSimulada)
                .mensajeVerificacion(mensaje)
                .inspectorConsultante(inspectorUsername)
                .fechaConsulta(LocalDateTime.now())
                .build();
    }

    public static String enmascararDocumento(String doc) {
        if (doc == null || doc.isBlank()) return null;
        String s = doc.trim();
        if (s.length() <= 4) {
            return "***" + s.charAt(s.length() - 1);
        }
        return s.substring(0, 2) + "***" + s.substring(s.length() - 2);
    }

    public LicenciaEmitidaDto mapearDto(LicenciaEmitida l) {
        List<HistorialEmisionLicenciaDto> historial = historialEmisionRepository.findByLicenciaIdOrderByFechaRegistroDesc(l.getId())
                .stream()
                .map(h -> HistorialEmisionLicenciaDto.builder()
                        .id(h.getId())
                        .licenciaId(l.getId())
                        .accion(h.getAccion())
                        .estadoAnterior(h.getEstadoAnterior())
                        .estadoNuevo(h.getEstadoNuevo())
                        .actor(h.getActor())
                        .detalle(h.getDetalle())
                        .fechaRegistro(h.getFechaRegistro())
                        .build())
                .toList();

        return LicenciaEmitidaDto.builder()
                .id(l.getId())
                .numeroLicencia(l.getNumeroLicencia())
                .resolucionId(l.getResolucion().getId())
                .numeroResolucion(l.getResolucion().getNumeroResolucion())
                .expedienteId(l.getExpediente().getId())
                .numeroExpediente(l.getExpediente().getNumeroTramite())
                .establecimientoId(l.getEstablecimiento() != null ? l.getEstablecimiento().getId() : null)
                .codigoVerificacionQr(l.getCodigoVerificacionQr())
                .urlVerificacion("/verificar/licencia/" + l.getCodigoVerificacionQr())
                .titularTipoDocumento(l.getTitularTipoDocumento())
                .titularNumeroDocumento(l.getTitularNumeroDocumento())
                .titularNombreORazonSocial(l.getTitularNombreORazonSocial())
                .nombreComercial(l.getNombreComercial())
                .direccionEstablecimiento(l.getDireccionEstablecimiento())
                .giroActividad(l.getGiroActividad())
                .ciiuCodigo(l.getCiiuCodigo())
                .areaMetrosCuadrados(l.getAreaMetrosCuadrados())
                .aforoPersonas(l.getAforoPersonas())
                .zonificacion(l.getZonificacion())
                .nivelRiesgoItse(l.getNivelRiesgoItse())
                .tipoVigencia(l.getTipoVigencia())
                .fechaEmision(l.getFechaEmision())
                .fechaCaducidad(l.getFechaCaducidad())
                .autoridadEmisora(l.getAutoridadEmisora())
                .hashPdf(l.getHashPdf())
                .estadoLicencia(l.getEstado())
                .observacionesEmision(l.getObservacionesEmision())
                .fechaCreacion(l.getFechaCreacion())
                .creadoPor(l.getCreadoPor())
                .historial(historial)
                .build();
    }
}
