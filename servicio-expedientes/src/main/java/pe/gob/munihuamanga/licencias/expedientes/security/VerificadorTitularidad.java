package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;

/**
 * Verificación de titularidad a nivel de objeto dentro de los servicios (defensa en profundidad
 * frente a accesos indebidos a objetos ajenos, OWASP API1:2023).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificadorTitularidad {

    private final ProveedorActor proveedorActor;
    private final ServicioRepresentacion servicioRepresentacion;

    /**
     * Exige que el actor sea un funcionario con alguno de los roles indicados o que represente
     * al titular del expediente con alguna de las facultades indicadas.
     */
    public Actor exigir(Expediente expediente, RolUsuario[] rolesFuncionario, PermisoOperador... facultades) {
        Actor actor = proveedorActor.actual();
        if (actor.tieneAlgunRol(rolesFuncionario)) {
            return actor;
        }
        if (servicioRepresentacion.tieneFacultad(actor.usuarioId(), expediente.getCuentaId(), facultades)) {
            return actor;
        }
        log.warn("[SEGURIDAD] Actuación denegada sobre el expediente {}: la persona no representa al titular.",
                expediente.getNumeroTramite());
        throw new AccessDeniedException("No cuenta con autorización para actuar sobre este expediente.");
    }

    public Actor actorActual() {
        return proveedorActor.actual();
    }
}
