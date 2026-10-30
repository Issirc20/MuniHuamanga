package pe.gob.munihuamanga.licencias.expedientes.security;

import java.util.List;

/**
 * Puerto de la política institucional de contraseñas.
 */
public interface PoliticaContrasenas {

    /**
     * @param datosPersonales datos que la contraseña no debe contener (documento, nombre de usuario).
     * @return incumplimientos detectados; vacío si la contraseña es aceptable.
     */
    List<String> evaluar(String contrasena, String... datosPersonales);

    default void exigir(String contrasena, String... datosPersonales) {
        List<String> incumplimientos = evaluar(contrasena, datosPersonales);
        if (!incumplimientos.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no cumple la política institucional: " + String.join(" ", incumplimientos));
        }
    }
}
