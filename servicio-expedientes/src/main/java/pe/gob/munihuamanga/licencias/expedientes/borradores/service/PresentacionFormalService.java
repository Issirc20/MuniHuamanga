package pe.gob.munihuamanga.licencias.expedientes.borradores.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
import pe.gob.munihuamanga.licencias.expedientes.documentos.service.DocumentoVersionadoService;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.mapper.ExpedienteMapper;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.RolesInstitucionales;
import pe.gob.munihuamanga.licencias.expedientes.security.ServicioRepresentacion;
import pe.gob.munihuamanga.licencias.expedientes.service.AuditoriaService;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Presentación formal de una solicitud: convierte el borrador en expediente con número de trámite
 * correlativo. Es la única vía de creación de expedientes, tanto para la presentación virtual
 * del administrado como para la recepción presencial por mesa de partes.
 * La presentación no exige pago: la tasa se liquida después de validar el ITSE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresentacionFormalService {

    private final SolicitudBorradorRepository borradorRepository;
    private final SolicitudBorradorService borradorService;
    private final SecuenciaTramiteService secuenciaTramiteService;
    private final ExpedienteRepository expedienteRepository;
    private final ExpedienteMapper expedienteMapper;
    private final AuditoriaService auditoriaService;
    private final DocumentoVersionadoService documentoVersionadoService;
    private final ProveedorActor proveedorActor;
    private final ServicioRepresentacion servicioRepresentacion;
    private final NotificacionEmailService notificacionEmailService;

    @Transactional
    public ExpedienteResponseDto presentarBorrador(UUID borradorId, PresentarBorradorDto dto) {
        SolicitudBorrador borrador = borradorRepository.findByIdWithLock(borradorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Borrador no encontrado con ID: " + borradorId));

        if (borrador.getEstado() == EstadoBorrador.PRESENTADO && borrador.getExpediente() != null) {
            log.info("[PRESENTACION] El borrador {} ya fue presentado como {}.", borradorId, borrador.getExpediente().getNumeroTramite());
            return expedienteMapper.toDto(borrador.getExpediente());
        }
        if (borrador.getEstado() == EstadoBorrador.DESCARTADO) {
            throw new IllegalStateException("No se puede presentar un borrador que fue descartado.");
        }

        Cuenta cuenta = borrador.getCuenta();
        Establecimiento est = borrador.getEstablecimiento();
        Actor actor = proveedorActor.actual();
        boolean presencial = actor.tieneAlgunRol(RolesInstitucionales.ATENCION_PRESENCIAL);

        exigirFacultadDePresentacion(actor, cuenta, presencial);
        exigirCuentaHabilitada(cuenta, presencial);

        if (est == null || !est.getCuenta().getId().equals(cuenta.getId())) {
            throw new IllegalStateException("El establecimiento declarado no pertenece a la cuenta titular.");
        }
        if (cuenta.getTipoPersona() == TipoPersona.JURIDICA) {
            exigirAcreditacionDeRepresentacion(borradorId, borrador, cuenta);
        }

        ValidacionRequisitosDto validacion = borradorService.validarRequisitos(borradorId);
        if (!validacion.isAptoParaPresentacion()) {
            throw new IllegalStateException("Requisitos obligatorios incompletos. Campos faltantes: " + validacion.getCamposFaltantes()
                    + ". Documentos faltantes: " + validacion.getDocumentosFaltantes() + ".");
        }
        if (!Boolean.TRUE.equals(dto.getDeclaracionJuradaAceptada()) || !Boolean.TRUE.equals(dto.getConfirmacionPresentacion())) {
            throw new IllegalArgumentException("Debe aceptar expresamente la declaración jurada y confirmar la presentación.");
        }

        LocalDateTime ahora = LocalDateTime.now();
        Expediente expediente = Expediente.builder()
                .id(UUID.randomUUID())
                .numeroTramite(secuenciaTramiteService.generarSiguienteNumeroExpediente())
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
                .estado(EstadoExpediente.PRESENTADO)
                .fechaCreacion(ahora)
                .build();

        Expediente guardado = expedienteRepository.save(expediente);

        borrador.setEstado(EstadoBorrador.PRESENTADO);
        borrador.setExpediente(guardado);
        borrador.setFechaActualizacion(ahora);
        borradorRepository.save(borrador);

        documentoVersionadoService.migrarDocumentosBorradorAExpediente(borradorId, guardado.getId(), actor.usuarioId(), actor.nombreParaRegistro());

        auditoriaService.registrarTransicion(guardado.getId(), null, EstadoExpediente.PRESENTADO, actor.nombreParaRegistro(),
                (presencial ? "Presentación en ventanilla" : "Presentación virtual") + " de la solicitud formal"
                        + (dto.getComentarios() != null ? ". " + dto.getComentarios() : "."));
        notificacionEmailService.notificarRegistro(guardado);

        log.info("[PRESENTACION] Solicitud formalizada como {} ({}).", guardado.getNumeroTramite(), presencial ? "PRESENCIAL" : "VIRTUAL");
        return expedienteMapper.toDto(guardado);
    }

    private void exigirFacultadDePresentacion(Actor actor, Cuenta cuenta, boolean presencial) {
        if (presencial) {
            return;
        }
        if (!servicioRepresentacion.tieneFacultad(actor.usuarioId(), cuenta.getId(),
                PermisoOperador.PRESENTAR_EXPEDIENTE, PermisoOperador.CREAR_EXPEDIENTE)) {
            throw new AccessDeniedException("No cuenta con la facultad de presentar solicitudes en nombre de esta cuenta.");
        }
    }

    private static void exigirCuentaHabilitada(Cuenta cuenta, boolean presencial) {
        if (cuenta.getEstado() == EstadoCuenta.SUSPENDIDA || cuenta.getEstado() == EstadoCuenta.BLOQUEADA) {
            throw new IllegalStateException("La cuenta del administrado se encuentra " + cuenta.getEstado() + ".");
        }
        if (!presencial && (cuenta.getEstado() != EstadoCuenta.ACTIVA || !Boolean.TRUE.equals(cuenta.getEmailVerificado()))) {
            throw new IllegalStateException("Debe verificar su correo electrónico antes de presentar solicitudes formales.");
        }
    }

    private void exigirAcreditacionDeRepresentacion(UUID borradorId, SolicitudBorrador borrador, Cuenta cuenta) {
        boolean identidadOficial = Boolean.TRUE.equals(cuenta.getIdentidadValidadaOficialmente());
        boolean tienePoderRegistral = borrador.getDocumentos() != null && borrador.getDocumentos().stream()
                .anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.VIGENCIA_PODER);
        if (!identidadOficial && !tienePoderRegistral) {
            tienePoderRegistral = borradorService.listarDocumentos(borradorId).stream()
                    .anyMatch(d -> d.getTipoDocumento() == TipoDocumentoAdjunto.VIGENCIA_PODER);
        }
        if (!identidadOficial && !tienePoderRegistral) {
            throw new IllegalStateException("Las personas jurídicas deben acreditar la representación legal mediante la vigencia de poder registral.");
        }
    }
}
