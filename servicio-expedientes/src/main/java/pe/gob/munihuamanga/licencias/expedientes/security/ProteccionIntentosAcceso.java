package pe.gob.munihuamanga.licencias.expedientes.security;

/**
 * Puerto de protección frente a intentos reiterados de inicio de sesión.
 */
public interface ProteccionIntentosAcceso {

    /** Indica si el nombre de usuario está temporalmente bloqueado. */
    boolean estaBloqueado(String username);

    void registrarFallo(String username);

    void registrarExito(String username);
}
