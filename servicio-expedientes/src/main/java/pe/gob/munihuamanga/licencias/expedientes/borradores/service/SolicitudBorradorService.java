package pe.gob.munihuamanga.licencias.expedientes.borradores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.AdjuntarDocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.DocumentoBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.SolicitudBorradorDto;
import pe.gob.munihuamanga.licencias.common.dto.ValidacionRequisitosDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumentoAdjunto;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.DocumentoBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.model.SolicitudBorrador;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.DocumentoBorradorRepository;
import pe.gob.munihuamanga.licencias.expedientes.borradores.repository.SolicitudBorradorRepository;
import pe.gob.munihuamanga.licencias.common.dto.ExpedienteResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;

import pe.gob.munihuamanga.licencias.expedientes.borradores.security.DocumentoSecurityValidator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SolicitudBorradorService {

    public static final String AVISO_LEGAL = "Solicitud preliminar en estado de borrador. No constituye inicio de procedimiento administrativo formal ni genera derecho adquirido.";
    public static final String AVISO_PAGO_SAT = "El registro o carga de comprobante SAT es una constancia referencial ciudadana y NO constituye validación oficial de pago. La validación se efectúa manualmente por la Gerencia de Licencias.";
    public static final String AVISO_RIESGO_ITSE = "La clasificación de nivel de riesgo declarada es una estimación técnica preliminar y NO equivale a una certificación ni dictamen institucional de Defensa Civil.";
    public static final String AVISO_ZONIFICACION = "La ubicación y giro declarados NO constituyen zonificación municipal aprobada ni compatibilidad de uso vigente.";

    private final SolicitudBorradorRepository borradorRepository;
    private final DocumentoBorradorRepository documentoRepository;
    private final CuentaRepository cuentaRepository;
    private final EstablecimientoRepository establecimientoRepository;
    private final DocumentoSecurityValidator documentoSecurityValidator;

    public SolicitudBorradorService(
            SolicitudBorradorRepository borradorRepository,
            DocumentoBorradorRepository documentoRepository,
            CuentaRepository cuentaRepository,
            EstablecimientoRepository establecimientoRepository,
            DocumentoSecurityValidator documentoSecurityValidator
    ) {
        this.borradorRepository = borradorRepository;
        this.documentoRepository = documentoRepository;
        this.cuentaRepository = cuentaRepository;
        this.establecimientoRepository = establecimientoRepository;
        this.documentoSecurityValidator = documentoSecurityValidator;
    }

    @Transactional
    public SolicitudBorradorDto crearBorrador(CrearBorradorDto dto, UUID creadorId) {
        Cuenta cuenta = cuentaRepository.findById(dto.getCuentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada con ID: " + dto.getCuentaId()));

        Establecimiento establecimiento = establecimientoRepository.findById(dto.getEstablecimientoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Establecimiento no encontrado con ID: " + dto.getEstablecimientoId()));

        if (!establecimiento.getCuenta().getId().equals(cuenta.getId())) {
            throw new IllegalArgumentException("El establecimiento especificado no pertenece a la cuenta titular.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        SolicitudBorrador borrador = SolicitudBorrador.builder()
                .id(UUID.randomUUID())
                .cuenta(cuenta)
                .establecimiento(establecimiento)
                .creadorId(creadorId)
                .fechaCreacion(ahora)
                .fechaActualizacion(ahora)
                .estado(EstadoBorrador.EN_EDICION)
                .version(0L)
                .modalidadTramite(dto.getModalidadTramite())
                .nombreComercialDeclarado(dto.getNombreComercialDeclarado() != null ? dto.getNombreComercialDeclarado().trim() : establecimiento.getNombreComercial())
                .giroNegocioDeclarado(dto.getGiroNegocioDeclarado() != null ? dto.getGiroNegocioDeclarado().trim() : establecimiento.getGiroPrincipal())
                .actividadDetallada(dto.getActividadDetallada())
                .areaMetrosCuadrados(dto.getAreaMetrosCuadrados() != null ? dto.getAreaMetrosCuadrados() : establecimiento.getAreaMetrosCuadrados())
                .aforoPersonas(dto.getAforoPersonas())
                .numeroPisos(dto.getNumeroPisos())
                .horarioInicio(dto.getHorarioInicio())
                .horarioFin(dto.getHorarioFin())
                .telefonoContacto(dto.getTelefonoContacto())
                .correoNotificacion(dto.getCorreoNotificacion())
                .autorizaNotificacion(dto.getAutorizaNotificacion())
                .nivelRiesgoDeclarado(dto.getNivelRiesgoDeclarado())
                .documentos(new ArrayList<>())
                .build();

        SolicitudBorrador guardado = borradorRepository.save(borrador);
        log.info("[BORRADOR] Creado nuevo borrador ID: {} para cuenta: {}, establecimiento: {}",
                guardado.getId(), cuenta.getId(), establecimiento.getId());

        return mapearADto(guardado);
    }

    @Transactional
    public SolicitudBorradorDto guardarAvance(UUID borradorId, ActualizarBorradorDto dto, UUID usuarioId) {
        SolicitudBorrador borrador = obtenerEntidadBorrador(borradorId);

        if (borrador.getEstado() == EstadoBorrador.PRESENTADO) {
            throw new IllegalStateException("El borrador ya fue presentado formalmente y no admite modificaciones.");
        }
        if (borrador.getEstado() == EstadoBorrador.DESCARTADO) {
            throw new IllegalStateException("El borrador fue descartado y se encuentra cerrado.");
        }

        if (dto.getModalidadTramite() != null) borrador.setModalidadTramite(dto.getModalidadTramite());
        if (dto.getNombreComercialDeclarado() != null) borrador.setNombreComercialDeclarado(dto.getNombreComercialDeclarado().trim());
        if (dto.getGiroNegocioDeclarado() != null) borrador.setGiroNegocioDeclarado(dto.getGiroNegocioDeclarado().trim());
        if (dto.getActividadDetallada() != null) borrador.setActividadDetallada(dto.getActividadDetallada().trim());
        if (dto.getAreaMetrosCuadrados() != null) borrador.setAreaMetrosCuadrados(dto.getAreaMetrosCuadrados());
        if (dto.getAforoPersonas() != null) borrador.setAforoPersonas(dto.getAforoPersonas());
        if (dto.getNumeroPisos() != null) borrador.setNumeroPisos(dto.getNumeroPisos());
        if (dto.getHorarioInicio() != null) borrador.setHorarioInicio(dto.getHorarioInicio());
        if (dto.getHorarioFin() != null) borrador.setHorarioFin(dto.getHorarioFin());
        if (dto.getTelefonoContacto() != null) borrador.setTelefonoContacto(dto.getTelefonoContacto());
        if (dto.getCorreoNotificacion() != null) borrador.setCorreoNotificacion(dto.getCorreoNotificacion());
        if (dto.getAutorizaNotificacion() != null) borrador.setAutorizaNotificacion(dto.getAutorizaNotificacion());
        if (dto.getNivelRiesgoDeclarado() != null) borrador.setNivelRiesgoDeclarado(dto.getNivelRiesgoDeclarado());

        // Comprobante SAT
        if (dto.getNumeroOperacionSat() != null) borrador.setNumeroOperacionSat(dto.getNumeroOperacionSat().trim());
        if (dto.getFechaPagoSat() != null) borrador.setFechaPagoSat(dto.getFechaPagoSat());
        if (dto.getMontoTasa() != null) borrador.setMontoTasa(dto.getMontoTasa());
        if (dto.getVoucherId() != null) borrador.setVoucherId(dto.getVoucherId().trim());

        borrador.setFechaActualizacion(LocalDateTime.now());
        SolicitudBorrador actualizado = borradorRepository.save(borrador);
        log.info("[BORRADOR] Avance guardado en borrador ID: {} por usuario: {}", borradorId, usuarioId);

        return mapearADto(actualizado);
    }

    @Transactional(readOnly = true)
    public SolicitudBorradorDto obtenerBorrador(UUID borradorId) {
        SolicitudBorrador borrador = borradorRepository.findByIdWithDocumentos(borradorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Borrador no encontrado con ID: " + borradorId));
        return mapearADto(borrador);
    }

    @Transactional(readOnly = true)
    public List<SolicitudBorradorDto> listarPorCuenta(UUID cuentaId) {
        return borradorRepository.findByCuentaId(cuentaId).stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Transactional
    public DocumentoBorradorDto adjuntarDocumento(UUID borradorId, AdjuntarDocumentoBorradorDto dto, UUID usuarioId) {
        SolicitudBorrador borrador = obtenerEntidadBorrador(borradorId);

        if (borrador.getEstado() == EstadoBorrador.PRESENTADO || borrador.getEstado() == EstadoBorrador.DESCARTADO) {
            throw new IllegalStateException("No se pueden adjuntar documentos a un borrador cerrado o presentado.");
        }

        byte[] contenido = Base64.getDecoder().decode(dto.getContenidoBase64());
        // Validación de seguridad OWASP A08: extensión, magic number, tamaño máximo y sanitización path traversal
        String nombreSanitizado = documentoSecurityValidator.validarYSanitizar(dto.getNombreArchivo(), dto.getTipoContenido(), contenido);
        String hashSha256 = calcularSha256(contenido);

        DocumentoBorrador documento = DocumentoBorrador.builder()
                .id(UUID.randomUUID())
                .borrador(borrador)
                .tipoDocumento(dto.getTipoDocumento())
                .nombreArchivo(nombreSanitizado)
                .tipoContenido(dto.getTipoContenido() != null ? dto.getTipoContenido() : "application/pdf")
                .tamanioBytes((long) contenido.length)
                .hashSha256(hashSha256)
                .rutaAlmacenamiento("storage/borradores/" + borradorId + "/" + nombreSanitizado)
                .fechaCarga(LocalDateTime.now())
                .cargadoPor(usuarioId)
                .descripcion(dto.getDescripcion())
                .build();

        DocumentoBorrador guardado = documentoRepository.save(documento);
        if (borrador.getDocumentos() != null) {
            borrador.getDocumentos().add(guardado);
        }
        borrador.setFechaActualizacion(LocalDateTime.now());
        borradorRepository.save(borrador);

        log.info("[BORRADOR-DOC] Documento seguro adjuntado ID: {}, tipo: {}, hash: {} al borrador: {}",
                guardado.getId(), guardado.getTipoDocumento(), hashSha256, borradorId);

        return mapearDocDto(guardado);
    }

    @Transactional(readOnly = true)
    public List<DocumentoBorradorDto> listarDocumentos(UUID borradorId) {
        return documentoRepository.findByBorradorId(borradorId).stream()
                .map(this::mapearDocDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentoBorradorDto descargarDocumento(UUID borradorId, UUID documentoId, UUID usuarioId, String rolUsuario) {
        SolicitudBorrador borrador = obtenerEntidadBorrador(borradorId);

        // Control de autorización: creador, operador de la cuenta titular o funcionario municipal
        boolean esFuncionario = rolUsuario != null && (rolUsuario.contains("MESA_DE_PARTES") || rolUsuario.contains("EVALUADOR") || rolUsuario.contains("SUPERVISOR_LICENCIAS") || rolUsuario.contains("AUDITOR"));
        if (!esFuncionario && usuarioId != null && !usuarioId.equals(borrador.getCreadorId())) {
            boolean esOperador = borrador.getCuenta() != null && borrador.getCuenta().getOperadores() != null &&
                    borrador.getCuenta().getOperadores().stream()
                            .anyMatch(op -> op.getUsuario() != null && usuarioId.equals(op.getUsuario().getId()) && Boolean.TRUE.equals(op.getActivo()));
            if (!esOperador) {
                log.warn("[SEGURIDAD-DOC] Intento no autorizado de acceso al documento {} del borrador {} por usuario {}",
                        documentoId, borradorId, usuarioId);
                throw new org.springframework.security.access.AccessDeniedException(
                        "No cuenta con autorización para acceder a los documentos de este borrador."
                );
            }
        }

        DocumentoBorrador doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado con ID: " + documentoId));

        if (!doc.getBorrador().getId().equals(borradorId)) {
            throw new IllegalArgumentException("El documento no corresponde al borrador especificado.");
        }

        log.info("[SEGURIDAD-DOC] Descarga y consulta autorizada de documento ID: {} (SHA-256: {}) en borrador: {} por usuario: {}",
                documentoId, doc.getHashSha256(), borradorId, usuarioId);

        return mapearDocDto(doc);
    }

    @Transactional
    public void eliminarDocumento(UUID borradorId, UUID documentoId, UUID usuarioId) {
        SolicitudBorrador borrador = obtenerEntidadBorrador(borradorId);
        if (borrador.getEstado() == EstadoBorrador.PRESENTADO || borrador.getEstado() == EstadoBorrador.DESCARTADO) {
            throw new IllegalStateException("No se pueden eliminar documentos de un borrador cerrado o presentado.");
        }

        DocumentoBorrador doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Documento no encontrado con ID: " + documentoId));

        if (!doc.getBorrador().getId().equals(borradorId)) {
            throw new IllegalArgumentException("El documento no corresponde al borrador especificado.");
        }

        documentoRepository.delete(doc);
        borrador.setFechaActualizacion(LocalDateTime.now());
        borradorRepository.save(borrador);
        log.info("[BORRADOR-DOC] Documento eliminado ID: {} del borrador: {} por usuario: {}", documentoId, borradorId, usuarioId);
    }

    @Transactional(readOnly = true)
    public ValidacionRequisitosDto validarRequisitos(UUID borradorId) {
        SolicitudBorrador borrador = borradorRepository.findByIdWithDocumentos(borradorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Borrador no encontrado con ID: " + borradorId));

        List<String> camposFaltantes = new ArrayList<>();
        List<String> documentosFaltantes = new ArrayList<>();
        List<String> observaciones = new ArrayList<>();

        if (borrador.getModalidadTramite() == null) camposFaltantes.add("Modalidad de trámite");
        if (borrador.getGiroNegocioDeclarado() == null || borrador.getGiroNegocioDeclarado().isBlank()) camposFaltantes.add("Giro del negocio");
        if (borrador.getAreaMetrosCuadrados() == null || borrador.getAreaMetrosCuadrados().compareTo(java.math.BigDecimal.ZERO) <= 0) camposFaltantes.add("Área ocupada (m²)");
        // El nivel de riesgo y el pago no se exigen al presentar: el riesgo lo determina Defensa Civil
        // en los anexos ITSE firmados y la tasa se liquida y paga después de su validación.
        List<DocumentoBorrador> docs = documentoRepository.findByBorradorId(borradorId);

        if (borrador.getCuenta().getTipoPersona() == TipoPersona.JURIDICA) {
            boolean tienePoder = docs.stream().anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.VIGENCIA_PODER || d.getTipoDocumento() == TipoDocumentoAdjunto.DNI_REPRESENTANTE);
            if (!tienePoder && !Boolean.TRUE.equals(borrador.getCuenta().getIdentidadValidadaOficialmente())) {
                documentosFaltantes.add("Vigencia de poder registral (SUNARP) o acreditación formal de representación legal");
            }
        }

        boolean apto = camposFaltantes.isEmpty() && documentosFaltantes.isEmpty();
        String resumen = apto
                ? "El borrador cumple con todos los requisitos documentales y de formulario para ser presentado formalmente."
                : "El borrador presenta requisitos pendientes antes de poder formalizar la presentación.";

        return ValidacionRequisitosDto.builder()
                .borradorId(borradorId)
                .aptoParaPresentacion(apto)
                .camposFaltantes(camposFaltantes)
                .documentosFaltantes(documentosFaltantes)
                .observaciones(observaciones)
                .mensajeResumen(resumen)
                .build();
    }

    @Transactional
    public void descartarBorrador(UUID borradorId, String motivo, UUID usuarioId) {
        SolicitudBorrador borrador = obtenerEntidadBorrador(borradorId);

        if (borrador.getEstado() == EstadoBorrador.PRESENTADO) {
            throw new IllegalStateException("No se puede descartar un borrador que ya fue formalizado en expediente.");
        }

        borrador.setEstado(EstadoBorrador.DESCARTADO);
        borrador.setMotivoDescarte(motivo != null ? motivo.trim() : "Descartado a solicitud del administrado.");
        borrador.setFechaDescarte(LocalDateTime.now());
        borrador.setUsuarioDescarte(usuarioId);
        borradorRepository.save(borrador);

        log.info("[BORRADOR-DESCARTADO] Borrador ID: {} marcado como DESCARTADO por usuario: {}", borradorId, usuarioId);
    }

    private SolicitudBorrador obtenerEntidadBorrador(UUID borradorId) {
        return borradorRepository.findById(borradorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Borrador no encontrado con ID: " + borradorId));
    }

    private String calcularSha256(byte[] contenido) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contenido);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo criptográfico SHA-256 no disponible en la plataforma.", e);
        }
    }

    private SolicitudBorradorDto mapearADto(SolicitudBorrador b) {
        List<DocumentoBorradorDto> docsDto = b.getDocumentos() != null
                ? b.getDocumentos().stream().map(this::mapearDocDto).collect(Collectors.toList())
                : List.of();

        return SolicitudBorradorDto.builder()
                .id(b.getId())
                .cuentaId(b.getCuenta().getId())
                .establecimientoId(b.getEstablecimiento().getId())
                .nombreEstablecimiento(b.getEstablecimiento().getNombreComercial())
                .direccionEstablecimiento(b.getEstablecimiento().getDireccion())
                .creadorId(b.getCreadorId())
                .fechaCreacion(b.getFechaCreacion())
                .fechaActualizacion(b.getFechaActualizacion())
                .estado(b.getEstado())
                .version(b.getVersion())
                .modalidadTramite(b.getModalidadTramite())
                .nombreComercialDeclarado(b.getNombreComercialDeclarado())
                .giroNegocioDeclarado(b.getGiroNegocioDeclarado())
                .actividadDetallada(b.getActividadDetallada())
                .areaMetrosCuadrados(b.getAreaMetrosCuadrados())
                .aforoPersonas(b.getAforoPersonas())
                .numeroPisos(b.getNumeroPisos())
                .horarioInicio(b.getHorarioInicio())
                .horarioFin(b.getHorarioFin())
                .telefonoContacto(b.getTelefonoContacto())
                .correoNotificacion(b.getCorreoNotificacion())
                .autorizaNotificacion(b.getAutorizaNotificacion())
                .nivelRiesgoDeclarado(b.getNivelRiesgoDeclarado())
                .numeroOperacionSat(b.getNumeroOperacionSat())
                .fechaPagoSat(b.getFechaPagoSat())
                .montoTasa(b.getMontoTasa())
                .voucherId(b.getVoucherId())
                .documentos(docsDto)
                .expedienteId(b.getExpediente() != null ? b.getExpediente().getId() : null)
                .numeroTramiteFormal(b.getExpediente() != null ? b.getExpediente().getNumeroTramite() : null)
                .avisoLegal(AVISO_LEGAL)
                .avisoPagoSat(AVISO_PAGO_SAT)
                .avisoRiesgoItse(AVISO_RIESGO_ITSE)
                .avisoZonificacion(AVISO_ZONIFICACION)
                .build();
    }

    private DocumentoBorradorDto mapearDocDto(DocumentoBorrador d) {
        String aviso = switch (d.getTipoDocumento()) {
            case COMPROBANTE_PAGO_SAT -> AVISO_PAGO_SAT;
            case EVIDENCIA_RIESGO_ITSE -> AVISO_RIESGO_ITSE;
            default -> null;
        };

        return DocumentoBorradorDto.builder()
                .id(d.getId())
                .borradorId(d.getBorrador().getId())
                .tipoDocumento(d.getTipoDocumento())
                .nombreArchivo(d.getNombreArchivo())
                .tipoContenido(d.getTipoContenido())
                .tamanioBytes(d.getTamanioBytes())
                .hashSha256(d.getHashSha256())
                .fechaCarga(d.getFechaCarga())
                .cargadoPor(d.getCargadoPor())
                .descripcion(d.getDescripcion())
                .avisoLegal(aviso)
                .build();
    }

    /**
     * Reutiliza íntegramente la información de la cuenta titular, establecimiento y borrador
     * para proyectar el DTO requerido por los generadores oficiales de Anexo 1, Anexo 3 y Anexo 4 (OpenPDF).
     */
    @Transactional(readOnly = true)
    public ExpedienteResponseDto convertirBorradorAExpedienteResponseDto(UUID borradorId) {
        SolicitudBorrador borrador = obtenerEntidadBorrador(borradorId);
        Cuenta cuenta = borrador.getCuenta();
        Establecimiento est = borrador.getEstablecimiento();

        return ExpedienteResponseDto.builder()
                .id(borrador.getId())
                .numeroTramite("BORRADOR-" + borrador.getId().toString().substring(0, 8).toUpperCase())
                .solicitanteId(cuenta != null ? cuenta.getId() : borrador.getCreadorId())
                .cuentaId(cuenta != null ? cuenta.getId() : null)
                .establecimientoId(est != null ? est.getId() : null)
                .modalidadTramite(borrador.getModalidadTramite() != null ? borrador.getModalidadTramite() : ModalidadTramite.LICENCIA_INDETERMINADA)
                .tipoPersona(cuenta != null && cuenta.getTipoPersona() != null ? cuenta.getTipoPersona() : TipoPersona.NATURAL)
                .tipoDocumento(cuenta != null && cuenta.getTipoDocumento() != null ? cuenta.getTipoDocumento() : TipoDocumento.DNI)
                .nombreTitular(cuenta != null ? cuenta.getRazonSocialONombre() : "SOLICITANTE DECLARADO")
                .documentoIdentidad(cuenta != null ? cuenta.getNumeroDocumento() : "00000000")
                .razonSocial(cuenta != null && cuenta.getTipoPersona() == TipoPersona.JURIDICA ? cuenta.getRazonSocialONombre() : null)
                .nombreComercial(borrador.getNombreComercialDeclarado() != null ? borrador.getNombreComercialDeclarado() : (est != null ? est.getNombreComercial() : null))
                .ciiuCodigo(est != null && est.getCiiu() != null ? est.getCiiu().getCodigo() : null)
                .giroNegocio(borrador.getGiroNegocioDeclarado() != null ? borrador.getGiroNegocioDeclarado() : (est != null ? est.getGiroPrincipal() : null))
                .actividadDetallada(borrador.getActividadDetallada())
                .direccionEstablecimiento(est != null ? est.getDireccion() : null)
                .distrito(est != null ? est.getDistrito() : "Ayacucho")
                .provincia(est != null ? est.getProvincia() : "Huamanga")
                .departamento(est != null ? est.getDepartamento() : "Ayacucho")
                .referenciaUbicacion(est != null ? est.getReferencia() : null)
                .areaMetrosCuadrados(borrador.getAreaMetrosCuadrados())
                .aforoPersonas(borrador.getAforoPersonas())
                .numeroPisos(borrador.getNumeroPisos())
                .horaInicio(borrador.getHorarioInicio())
                .horaFin(borrador.getHorarioFin())
                .telefono(borrador.getTelefonoContacto())
                .correoElectronico(borrador.getCorreoNotificacion())
                .autorizaNotificacion(borrador.getAutorizaNotificacion())
                .nivelRiesgo(borrador.getNivelRiesgoDeclarado())
                .montoTasa(borrador.getMontoTasa())
                .numeroOperacionSat(borrador.getNumeroOperacionSat())
                .estado(EstadoExpediente.PRESENTADO)
                .build();
    }
}
