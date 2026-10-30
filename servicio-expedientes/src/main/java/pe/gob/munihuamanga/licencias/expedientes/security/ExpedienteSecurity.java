package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Política de acceso a expedientes, utilizada desde las expresiones {@code @PreAuthorize}.
 * Los funcionarios acceden según su rol; el administrado accede solo a los expedientes
 * de las cuentas que representa y con la facultad que corresponde a la acción.
 */
@Slf4j
@Component("expedienteSecurity")
@RequiredArgsConstructor
public class ExpedienteSecurity {

    private final ExpedienteRepository expedienteRepository;
    private final ProveedorContextoSeguridad proveedorContexto;
    private final ServicioRepresentacion servicioRepresentacion;

    public boolean puedeVerExpediente(Authentication authentication, UUID expedienteId) {
        return evaluar(authentication, expedienteRepository.findById(expedienteId),
                PermisoOperador.CONSULTAR_EXPEDIENTE);
    }

    public boolean puedeVerExpedientePorTramite(Authentication authentication, String numeroTramite) {
        if (numeroTramite == null || numeroTramite.isBlank()) {
            return false;
        }
        return evaluar(authentication, expedienteRepository.findByNumeroTramite(numeroTramite.trim()),
                PermisoOperador.CONSULTAR_EXPEDIENTE);
    }

    public boolean puedeDescargarDocumento(Authentication authentication, UUID expedienteId) {
        return puedeVerExpediente(authentication, expedienteId);
    }

    /**
     * Subsanación de observaciones: el administrado con facultad de subsanar,
     * o mesa de partes cuando la subsanación se presenta en ventanilla.
     */
    public boolean puedeSubsanar(Authentication authentication, UUID expedienteId) {
        Optional<Expediente> expediente = expedienteRepository.findById(expedienteId);
        if (expediente.isEmpty() || expediente.get().getEstado() != EstadoExpediente.OBSERVADO) {
            return false;
        }
        return puedeActuarComoAdministrado(authentication, expediente.get(),
                RolesInstitucionales.ATENCION_PRESENCIAL, PermisoOperador.SUBSANAR_EXPEDIENTE);
    }

    /**
     * Actuaciones del administrado en etapas posteriores a la presentación (carga de anexos ITSE).
     * Mesa de partes puede registrarlas en nombre del administrado cuando se reciben en ventanilla.
     */
    public boolean puedeActuarComoAdministrado(Authentication authentication, UUID expedienteId, String permiso) {
        Optional<Expediente> expediente = expedienteRepository.findById(expedienteId);
        return expediente.isPresent() && puedeActuarComoAdministrado(authentication, expediente.get(),
                RolesInstitucionales.ATENCION_PRESENCIAL, PermisoOperador.valueOf(permiso));
    }

    /**
     * Declaración del comprobante de pago: el administrado, mesa de partes, o la caja del SAT
     * cuando el pago se recibe presencialmente en ventanilla.
     */
    public boolean puedeDeclararPago(Authentication authentication, UUID expedienteId) {
        Optional<Expediente> expediente = expedienteRepository.findById(expedienteId);
        return expediente.isPresent() && puedeActuarComoAdministrado(authentication, expediente.get(),
                RolesInstitucionales.RECEPCION_PAGOS, PermisoOperador.PRESENTAR_EXPEDIENTE);
    }

    /** Indica si la persona autenticada representa al titular del expediente con la facultad indicada. */
    public boolean esRepresentanteDelTitular(Authentication authentication, Expediente expediente, PermisoOperador permiso) {
        if (!ProveedorContextoSeguridad.estaAutenticado(authentication) || expediente == null) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        return servicioRepresentacion.tieneFacultad(actor.usuarioId(), expediente.getCuentaId(), permiso);
    }

    private boolean puedeActuarComoAdministrado(Authentication authentication, Expediente expediente,
                                                RolUsuario[] rolesPresenciales, PermisoOperador permiso) {
        if (!ProveedorContextoSeguridad.estaAutenticado(authentication)) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        if (actor.tieneAlgunRol(rolesPresenciales)) {
            return true;
        }
        return esRepresentanteDelTitular(authentication, expediente, permiso);
    }

    private boolean evaluar(Authentication authentication, Optional<Expediente> expediente, PermisoOperador permiso) {
        if (!ProveedorContextoSeguridad.estaAutenticado(authentication) || expediente.isEmpty()) {
            return false;
        }
        Actor actor = proveedorContexto.desde(authentication);
        if (actor.tieneAlgunRol(RolesInstitucionales.CONSULTA_EXPEDIENTES)) {
            return true;
        }
        boolean permitido = esRepresentanteDelTitular(authentication, expediente.get(), permiso);
        if (!permitido) {
            log.warn("[SEGURIDAD] Acceso denegado al expediente {}: la persona autenticada no representa al titular.",
                    expediente.get().getNumeroTramite());
        }
        return permitido;
    }
}
