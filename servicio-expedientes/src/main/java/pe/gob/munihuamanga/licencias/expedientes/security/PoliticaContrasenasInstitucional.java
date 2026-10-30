package pe.gob.munihuamanga.licencias.expedientes.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Política de contraseñas: longitud mínima de 10 caracteres, combinación de mayúsculas, minúsculas
 * y dígitos, y prohibición de incluir el documento o el nombre de usuario.
 */
@Component
public class PoliticaContrasenasInstitucional implements PoliticaContrasenas {

    static final int LONGITUD_MINIMA = 10;

    @Override
    public List<String> evaluar(String contrasena, String... datosPersonales) {
        List<String> incumplimientos = new ArrayList<>();
        if (contrasena == null || contrasena.length() < LONGITUD_MINIMA) {
            incumplimientos.add("Debe tener al menos " + LONGITUD_MINIMA + " caracteres.");
            return incumplimientos;
        }
        if (contrasena.chars().noneMatch(Character::isUpperCase)) {
            incumplimientos.add("Debe incluir al menos una letra mayúscula.");
        }
        if (contrasena.chars().noneMatch(Character::isLowerCase)) {
            incumplimientos.add("Debe incluir al menos una letra minúscula.");
        }
        if (contrasena.chars().noneMatch(Character::isDigit)) {
            incumplimientos.add("Debe incluir al menos un dígito.");
        }
        String normalizada = contrasena.toLowerCase(Locale.ROOT);
        for (String dato : datosPersonales) {
            if (dato != null && dato.length() >= 4 && normalizada.contains(dato.toLowerCase(Locale.ROOT))) {
                incumplimientos.add("No debe contener su documento de identidad ni su nombre de usuario.");
                break;
            }
        }
        return incumplimientos;
    }
}
