package pe.gob.munihuamanga.licencias.expedientes.cuentas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.expedientes.service.NotificacionEmailService;

/**
 * Envío por correo electrónico de los enlaces de verificación y activación.
 * Fuera del entorno de producción, si el correo está deshabilitado, el enlace se registra en la
 * bitácora de la aplicación para permitir las pruebas; en producción nunca se registra.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionCuentaCorreo implements PuertoNotificacionCuenta {

    private final NotificacionEmailService notificacionEmailService;
    private final Environment environment;

    @Override
    public void enviarVerificacionCorreo(String correo, String destinatario, String enlace) {
        enviar(correo, destinatario, "Verificación de su correo electrónico",
                "Verificación de correo electrónico",
                "Para habilitar la presentación de solicitudes en el portal de licencias, confirme su correo electrónico. "
                        + "El enlace es de un solo uso y vence en 24 horas.",
                enlace, "Verificar correo electrónico");
    }

    @Override
    public void enviarActivacionAcceso(String correo, String destinatario, String enlace) {
        enviar(correo, destinatario, "Activación de su acceso al sistema de licencias",
                "Activación de acceso",
                "Se le ha otorgado acceso al sistema de licencias de funcionamiento. Defina su contraseña mediante el enlace siguiente. "
                        + "El enlace es de un solo uso y vence en 72 horas.",
                enlace, "Definir contraseña");
    }

    private void enviar(String correo, String destinatario, String asunto, String titulo, String mensaje, String enlace, String textoEnlace) {
        if (notificacionEmailService.estaHabilitado()) {
            notificacionEmailService.enviarMensajeInstitucional(correo, destinatario, asunto, titulo, mensaje, null, enlace, textoEnlace);
        } else if (!environment.matchesProfiles("prod")) {
            log.info("[DESARROLLO] Correo deshabilitado. Enlace de '{}': {}", titulo, enlace);
        } else {
            log.warn("[CUENTAS] No se pudo enviar la comunicación '{}': el servicio de correo está deshabilitado.", titulo);
        }
    }
}
