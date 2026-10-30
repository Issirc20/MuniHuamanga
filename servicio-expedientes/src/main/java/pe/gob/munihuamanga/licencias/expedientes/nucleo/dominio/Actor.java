package pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio;

import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persona o proceso que ejecuta una acción sobre el expediente.
 * Se obtiene siempre del contexto de seguridad; nunca de datos enviados por el cliente.
 */
public record Actor(UUID usuarioId, String username, String nombre, Set<RolUsuario> roles) {

    public static final String USERNAME_SISTEMA = "SISTEMA";

    public Actor {
        roles = (roles == null || roles.isEmpty()) ? EnumSet.noneOf(RolUsuario.class) : EnumSet.copyOf(roles);
    }

    /** Actor que representa a los procesos automáticos del sistema (vigilancia de plazos, cierres). */
    public static Actor sistema() {
        return new Actor(null, USERNAME_SISTEMA, "Proceso automático del sistema", EnumSet.noneOf(RolUsuario.class));
    }

    public boolean esSistema() {
        return USERNAME_SISTEMA.equals(username);
    }

    public boolean tieneRol(RolUsuario rol) {
        return roles.contains(rol);
    }

    public boolean tieneAlgunRol(RolUsuario... candidatos) {
        for (RolUsuario rol : candidatos) {
            if (roles.contains(rol)) {
                return true;
            }
        }
        return false;
    }

    public boolean esFuncionario() {
        return roles.stream().anyMatch(RolUsuario::esFuncionario);
    }

    /** Nombre que se registra en la trazabilidad. */
    public String nombreParaRegistro() {
        return (nombre != null && !nombre.isBlank()) ? nombre : username;
    }
}
