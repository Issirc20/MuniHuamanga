package pe.gob.munihuamanga.licencias.expedientes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.Anexo4CondicionesDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearExpedienteDto;
import pe.gob.munihuamanga.licencias.common.dto.VerificacionLicenciaDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.FuncionEdificacion;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.enums.TipoDocumento;
import pe.gob.munihuamanga.licencias.common.enums.TipoPersona;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.model.Anexo4Condiciones;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.validator.EstadoExpedienteValidator;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Servicio central de gestión de expedientes según el Diagrama C4 y Diagrama de Clases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpedienteService {

    private final ExpedienteRepository expedienteRepository;
    private final EstadoExpedienteValidator estadoExpedienteValidator;
    private final CalculadoraDeTasa calculadoraDeTasa;
    private final AuditoriaService auditoriaService;
    private final MetricasExpedienteService metricasExpedienteService;

    /**
     * Registra un nuevo expediente en el sistema e inicia en estado FORMATOS_GENERADOS.
     */
    @Transactional
    public Expediente crearExpediente(CrearExpedienteDto dto) {
        UUID expedienteId = UUID.randomUUID();
        String numeroTramite = generarNumeroTramite();
        LocalDateTime fechaCreacion = LocalDateTime.now();
        LocalDateTime fechaLimite = calcularFechaLimite15DiasHabiles(fechaCreacion.toLocalDate());

        Anexo4Condiciones anexo4 = null;
        if (dto.getAnexo4Condiciones() != null) {
            Anexo4CondicionesDto a4 = dto.getAnexo4Condiciones();
            anexo4 = Anexo4Condiciones.builder()
                    .areaTerreno(a4.getAreaTerreno())
                    .areaPiso1(a4.getAreaPiso1())
                    .areaPiso2(a4.getAreaPiso2())
                    .areaPiso3(a4.getAreaPiso3())
                    .areaPiso4(a4.getAreaPiso4())
                    .areaOtrosPisos(a4.getAreaOtrosPisos())
                    .areaTechadaTotal(a4.getAreaTechadaTotal())
                    .areaOcupadaTotal(a4.getAreaOcupadaTotal())
                    .aforoPersonas(a4.getAforoPersonas())
                    .antiguedadEdificacionAnios(a4.getAntiguedadEdificacionAnios())
                    .antiguedadGiroAnios(a4.getAntiguedadGiroAnios())
                    .noEnProcesoConstruccion(a4.getNoEnProcesoConstruccion())
                    .cuentaServiciosBasicos(a4.getCuentaServiciosBasicos())
                    .cuentaMobiliarioBasico(a4.getCuentaMobiliarioBasico())
                    .tieneEquiposInstalados(a4.getTieneEquiposInstalados())
                    .mediosEvacuacionLibres(a4.getMediosEvacuacionLibres())
                    .senalizacionSeguridad(a4.getSenalizacionSeguridad())
                    .lucesEmergenciaOperativas(a4.getLucesEmergenciaOperativas())
                    .tableroElectricoProtegido(a4.getTableroElectricoProtegido())
                    .interruptoresDiferenciales(a4.getInterruptoresDiferenciales())
                    .pozoTierraVigente(a4.getPozoTierraVigente())
                    .extintoresOperativos(a4.getExtintoresOperativos())
                    .estructurasSinRiesgoColapso(a4.getEstructurasSinRiesgoColapso())
                    .cablesProtegidosTubosPvc(a4.getCablesProtegidosTubosPvc())
                    .build();
        }

        Expediente expediente = Expediente.builder()
                .id(expedienteId)
                .numeroTramite(numeroTramite)
                .solicitanteId(dto.getSolicitanteId())
                // Sección I: Modalidad
                .modalidadTramite(dto.getModalidadTramite() != null ? dto.getModalidadTramite() : ModalidadTramite.LICENCIA_INDETERMINADA)
                .plazoTemporalMeses(dto.getPlazoTemporalMeses())
                .tipoAnuncio(dto.getTipoAnuncio())
                .numeroLicenciaPrincipal(dto.getNumeroLicenciaPrincipal())
                // Sección II: Solicitante
                .tipoPersona(dto.getTipoPersona() != null ? dto.getTipoPersona() : TipoPersona.NATURAL)
                .tipoDocumento(dto.getTipoDocumento() != null ? dto.getTipoDocumento() : TipoDocumento.DNI)
                .nombreTitular(dto.getNombreTitular().trim())
                .documentoIdentidad(dto.getDocumentoIdentidad().trim())
                .razonSocial(dto.getRazonSocial() != null ? dto.getRazonSocial().trim() : null)
                .correoElectronico(dto.getCorreoElectronico() != null ? dto.getCorreoElectronico().trim() : null)
                .telefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null)
                .autorizaNotificacion(dto.getAutorizaNotificacion() != null ? dto.getAutorizaNotificacion() : true)
                // Sección III: Representante Legal / SUNARP
                .partidaSunarp(dto.getPartidaSunarp())
                .asientoSunarp(dto.getAsientoSunarp())
                .dniRepresentante(dto.getDniRepresentante())
                .nombreRepresentante(dto.getNombreRepresentante())
                .poderSunarp(dto.getPoderSunarp())
                // Sección IV: Establecimiento
                .nombreComercial(dto.getNombreComercial().trim())
                .ciiuCodigo(dto.getCiiuCodigo())
                .giroNegocio(dto.getGiroNegocio().trim())
                .actividadDetallada(dto.getActividadDetallada())
                .zonificacion(dto.getZonificacion())
                .funcionEdificacion(dto.getFuncionEdificacion() != null ? dto.getFuncionEdificacion() : FuncionEdificacion.COMERCIO)
                .direccionEstablecimiento(dto.getDireccionEstablecimiento().trim())
                // Dirección desglosada
                .tipoVia(dto.getTipoVia())
                .nombreVia(dto.getNombreVia())
                .numeroVivienda(dto.getNumeroVivienda())
                .interior(dto.getInterior())
                .manzana(dto.getManzana())
                .lote(dto.getLote())
                .urbanizacion(dto.getUrbanizacion())
                .distrito(dto.getDistrito() != null ? dto.getDistrito() : "Ayacucho")
                .provincia(dto.getProvincia() != null ? dto.getProvincia() : "Huamanga")
                .departamento(dto.getDepartamento() != null ? dto.getDepartamento() : "Ayacucho")
                .referenciaUbicacion(dto.getReferenciaUbicacion())
                // Dimensionamiento y capacidad
                .areaMetrosCuadrados(dto.getAreaMetrosCuadrados())
                .areaTerreno(dto.getAreaTerreno())
                .areaTechadaTotal(dto.getAreaTechadaTotal())
                .areaOcupadaTotal(dto.getAreaOcupadaTotal())
                .aforoPersonas(dto.getAforoPersonas())
                .numeroPisos(dto.getNumeroPisos())
                .antiguedadEdificacion(dto.getAntiguedadEdificacion())
                .antiguedadGiro(dto.getAntiguedadGiro())
                // Sectorial
                .requiereAutorizacionSectorial(dto.getRequiereAutorizacionSectorial())
                .sectorEntidad(dto.getSectorEntidad())
                .sectorDenominacion(dto.getSectorDenominacion())
                .sectorFecha(dto.getSectorFecha())
                .sectorNumero(dto.getSectorNumero())
                // Anexo 4 embebido
                .anexo4Condiciones(anexo4)
                // Flujo y fechas
                .estado(EstadoExpediente.FORMATOS_GENERADOS)
                .fechaCreacion(fechaCreacion)
                .fechaLimite(fechaLimite)
                .build();

        Expediente guardado = expedienteRepository.save(expediente);

        auditoriaService.registrarTransicion(
                guardado.getId(),
                null,
                EstadoExpediente.FORMATOS_GENERADOS,
                dto.getNombreTitular(),
                "Ingreso digital de solicitud y formatos autogenerados (Mesa de Partes Virtual)"
        );

        log.info("Expediente creado exitosamente: ID={}, Número={}", guardado.getId(), guardado.getNumeroTramite());
        metricasExpedienteService.registrarCreacion();
        return guardado;
    }

    /**
     * Registra la clasificación de riesgo recibida de Defensa Civil (ITSE)
     * y transiciona el expediente a DOCUMENTOS_VALIDADOS.
     */
    @Transactional
    public void registrarClasificacionRiesgo(UUID id, NivelRiesgo nivel) {
        registrarClasificacionRiesgo(id, nivel, null, null);
    }

    /**
     * Sobrecarga completa para registrar clasificación ITSE con número de informe y observaciones.
     */
    @Transactional
    public void registrarClasificacionRiesgo(UUID id, NivelRiesgo nivel, String informeItseNumero, String observaciones) {
        Expediente expediente = obtenerPorId(id);

        EstadoExpediente actual = expediente.getEstado();
        EstadoExpediente nuevo = EstadoExpediente.DOCUMENTOS_VALIDADOS;

        estadoExpedienteValidator.validarTransicion(actual, nuevo);

        BigDecimal montoTasa = calculadoraDeTasa.calcularTasa(nivel);
        expediente.setNivelRiesgo(nivel);
        expediente.setMontoTasa(montoTasa);
        if (informeItseNumero != null && !informeItseNumero.trim().isEmpty()) {
            expediente.setNumeroInformeItse(informeItseNumero.trim());
            expediente.setFechaInformeItse(LocalDateTime.now());
        }
        expediente.cambiarEstado(nuevo);

        expedienteRepository.save(expediente);

        String tipoItse = (nivel == NivelRiesgo.BAJO || nivel == NivelRiesgo.MEDIO) ? "ITSE POSTERIOR" : "ITSE PREVIA";
        String detalleObs = (observaciones != null && !observaciones.isBlank()) ? " Obs: " + observaciones : "";

        auditoriaService.registrarTransicion(
                id,
                actual,
                nuevo,
                "DEFENSA_CIVIL",
                String.format("Clasificación de riesgo: %s (%s). Tasa TUPA calculada: S/. %s.%s", nivel, tipoItse, montoTasa, detalleObs)
        );

        log.info("Clasificación de riesgo registrada para expediente {}: Nivel={}, Tasa={}", id, nivel, montoTasa);
    }

    /**
     * Genera la orden de pago (Voucher SAT) para el pago de la tasa administrativa.
     */
    @Transactional
    public VoucherDto generarVoucher(UUID id) {
        Expediente expediente = obtenerPorId(id);

        if (expediente.getMontoTasa() == null) {
            BigDecimal monto = calculadoraDeTasa.calcularTasa(
                    expediente.getNivelRiesgo() != null ? expediente.getNivelRiesgo() : NivelRiesgo.BAJO
            );
            expediente.setMontoTasa(monto);
        }

        String voucherId = "VCH-" + LocalDate.now().getYear() + "-" + String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
        expediente.setVoucherId(voucherId);
        expedienteRepository.save(expediente);

        return VoucherDto.builder()
                .voucherId(voucherId)
                .expedienteId(expediente.getId())
                .numeroTramite(expediente.getNumeroTramite())
                .titular(expediente.getNombreTitular())
                .documentoIdentidad(expediente.getDocumentoIdentidad())
                .monto(expediente.getMontoTasa())
                .concepto("TASA LICENCIA FUNCIONAMIENTO - RIESGO " + (expediente.getNivelRiesgo() != null ? expediente.getNivelRiesgo() : "ESTÁNDAR"))
                .fechaEmision(LocalDateTime.now())
                .fechaVencimiento(LocalDateTime.now().plusDays(5))
                .codigoBarrasSat("0107" + voucherId.replace("-", ""))
                .build();
    }

    /**
     * Registra el pago del voucher de tasa administrativa y transiciona a EN_EVALUACION_FINAL.
     */
    @Transactional
    public void registrarPago(UUID id, String voucherId) {
        registrarPago(id, voucherId, null);
    }

    /**
     * Sobrecarga completa para registrar pago con número de operación SAT / bancaria.
     */
    @Transactional
    public void registrarPago(UUID id, String voucherId, String numeroOperacionSat) {
        Expediente expediente = obtenerPorId(id);

        EstadoExpediente actual = expediente.getEstado();
        EstadoExpediente nuevo = EstadoExpediente.EN_EVALUACION_FINAL;

        estadoExpedienteValidator.validarTransicion(actual, nuevo);

        expediente.setVoucherId(voucherId);
        if (numeroOperacionSat != null && !numeroOperacionSat.trim().isEmpty()) {
            expediente.setNumeroOperacionSat(numeroOperacionSat.trim());
        }
        expediente.setFechaPagoSat(LocalDateTime.now());
        expediente.cambiarEstado(nuevo);
        expedienteRepository.save(expediente);

        String motivoAuditoria = "Pago de tasa validado mediante voucher: " + voucherId;
        if (numeroOperacionSat != null && !numeroOperacionSat.isBlank()) {
            motivoAuditoria += " (Op. SAT: " + numeroOperacionSat + ")";
        }

        auditoriaService.registrarTransicion(
                id,
                actual,
                nuevo,
                "SAT_CAJA",
                motivoAuditoria
        );

        log.info("Pago registrado para expediente {}: Voucher={}, Estado={}", id, voucherId, nuevo);
    }

    /**
     * Dictamen final favorable por parte de la Gerencia de Licencias: transiciona a APROBADO
     * y genera el código QR único para la licencia digital.
     * Valida precondiciones legales antes de emitir la licencia.
     */
    @Transactional
    public void aprobar(UUID id) {
        Expediente expediente = obtenerPorId(id);

        // Validación estricta de precondiciones legales (pago efectuado y riesgo clasificado)
        estadoExpedienteValidator.validarAprobacion(expediente);

        EstadoExpediente actual = expediente.getEstado();
        EstadoExpediente nuevo = EstadoExpediente.APROBADO;

        String qrCodeUnico = "LIC-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        expediente.setLicenciaQrCode(qrCodeUnico);
        expediente.cambiarEstado(nuevo);

        expedienteRepository.save(expediente);

        auditoriaService.registrarTransicion(
                id,
                actual,
                nuevo,
                "GERENCIA_LICENCIAS",
                "Expediente evaluado conforme y APROBADO. Licencia emitida con QR: " + qrCodeUnico
        );

        log.info("Expediente {} APROBADO exitosamente con código QR: {}", id, qrCodeUnico);
        metricasExpedienteService.registrarAprobacion();
    }

    /**
     * Dictamen final de rechazo u observación insubsanable: transiciona a RECHAZADO.
     */
    @Transactional
    public void rechazar(UUID id, String motivo) {
        Expediente expediente = obtenerPorId(id);

        EstadoExpediente actual = expediente.getEstado();
        EstadoExpediente nuevo = EstadoExpediente.RECHAZADO;

        estadoExpedienteValidator.validarTransicion(actual, nuevo);

        expediente.cambiarEstado(nuevo);
        expedienteRepository.save(expediente);

        auditoriaService.registrarTransicion(
                id,
                actual,
                nuevo,
                "GERENCIA_LICENCIAS",
                "Expediente RECHAZADO. Motivo formal: " + (motivo != null ? motivo : "No especificado")
        );

        log.warn("Expediente {} RECHAZADO. Motivo: {}", id, motivo);
        metricasExpedienteService.registrarRechazo();
    }

    @Transactional(readOnly = true)
    public Expediente obtenerPorId(UUID id) {
        return expedienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", id));
    }

    @Transactional(readOnly = true)
    public Expediente obtenerPorNumeroTramite(String numeroTramite) {
        return expedienteRepository.findByNumeroTramite(numeroTramite)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", numeroTramite));
    }

    @Transactional(readOnly = true)
    public List<Expediente> listarTodos() {
        return expedienteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Expediente> listarConFiltros(EstadoExpediente estado, Boolean conAlerta) {
        List<Expediente> list = (estado != null)
                ? expedienteRepository.findByEstado(estado)
                : expedienteRepository.findAll();

        if (Boolean.TRUE.equals(conAlerta)) {
            LocalDate hoy = LocalDate.now();
            list = list.stream().filter(e -> {
                long dias = calcularDiasHabiles(hoy, e.getFechaLimite().toLocalDate());
                return dias <= 3 && e.getEstado() != EstadoExpediente.APROBADO && e.getEstado() != EstadoExpediente.RECHAZADO;
            }).collect(Collectors.toList());
        }

        return list;
    }

    @Transactional(readOnly = true)
    public List<Expediente> listarPorSolicitante(UUID solicitanteId) {
        return expedienteRepository.findBySolicitanteId(solicitanteId);
    }

    private String generarNumeroTramite() {
        int anio = LocalDate.now().getYear();
        int correlativo = ThreadLocalRandom.current().nextInt(10000, 99999);
        return String.format("EXP-%d-%05d", anio, correlativo);
    }

    private LocalDateTime calcularFechaLimite15DiasHabiles(LocalDate fechaInicio) {
        LocalDate fecha = fechaInicio;
        int diasHabilesContados = 0;
        while (diasHabilesContados < 15) {
            fecha = fecha.plusDays(1);
            DayOfWeek dow = fecha.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                diasHabilesContados++;
            }
        }
        return LocalDateTime.of(fecha, LocalTime.of(18, 0));
    }

    private long calcularDiasHabiles(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) return 0;
        long dias = 0;
        LocalDate curr = inicio;
        while (!curr.isAfter(fin)) {
            DayOfWeek d = curr.getDayOfWeek();
            if (d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY) {
                dias++;
            }
            curr = curr.plusDays(1);
        }
        return dias;
    }

    /**
     * US-11 / RNF-20: Consulta pública de autenticidad y estado de licencia para ciudadanos y fiscalizadores.
     */
    @Transactional(readOnly = true)
    public VerificacionLicenciaDto verificarLicencia(String codigoLicencia) {
        metricasExpedienteService.registrarConsultaPublica();
        if (codigoLicencia == null || codigoLicencia.trim().isEmpty()) {
            return VerificacionLicenciaDto.builder()
                    .valida(false)
                    .estado("CÓDIGO INVÁLIDO")
                    .mensajeVerificacion("Debe proporcionar un código de licencia válido.")
                    .build();
        }

        return expedienteRepository.findByLicenciaQrCode(codigoLicencia.trim())
                .map(exp -> {
                    boolean esAprobado = exp.getEstado() == EstadoExpediente.APROBADO;
                    return VerificacionLicenciaDto.builder()
                            .numeroLicencia(exp.getLicenciaQrCode())
                            .numeroTramite(exp.getNumeroTramite())
                            .titular(exp.getNombreTitular())
                            .documentoIdentidad(exp.getDocumentoIdentidad())
                            .razonSocial(exp.getRazonSocial())
                            .nombreComercial(exp.getNombreComercial())
                            .giro(exp.getGiroNegocio())
                            .direccion(exp.getDireccionEstablecimiento())
                            .nivelRiesgo(exp.getNivelRiesgo())
                            .estado(esAprobado ? "VIGENTE / AUTORIZADA" : "NO VIGENTE (" + exp.getEstado() + ")")
                            .fechaEmision(exp.getFechaCreacion())
                            .valida(esAprobado)
                            .mensajeVerificacion(esAprobado
                                    ? "Licencia de Funcionamiento oficial, auténtica y vigente expedida por la Municipalidad Provincial de Huamanga (Vigencia Indeterminada - Art. 11 Ley N° 28976)."
                                    : "El trámite asociado a este código no se encuentra en estado APROBADO.")
                            .build();
                })
                .orElseGet(() -> VerificacionLicenciaDto.builder()
                        .numeroLicencia(codigoLicencia)
                        .valida(false)
                        .estado("NO ENCONTRADA")
                        .mensajeVerificacion("El código de licencia consultado no se encuentra registrado en el padrón municipal de Huamanga.")
                        .build());
    }
}
