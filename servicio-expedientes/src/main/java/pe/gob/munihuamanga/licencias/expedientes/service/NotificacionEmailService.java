package pe.gob.munihuamanga.licencias.expedientes.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Servicio de Notificaciones por Correo Electrónico — Fase 04 Sprint 4-B.
 *
 * Envía correos HTML al ciudadano en los eventos clave del trámite:
 *
 *  EVENTO                  | PLANTILLA              | ASUNTO
 * ─────────────────────────┼────────────────────────┼─────────────────────────────────────
 *  Registro / apertura     | email-registro.html    | Expediente N° XXX — Ingreso Recibido
 *  Aprobación de Licencia  | email-aprobacion.html  | ¡Su Licencia de Funcionamiento fue Aprobada!
 *  Rechazo del Trámite     | email-rechazo.html     | Expediente N° XXX — Observado / Rechazado
 *
 * Se ejecutan en modo @Async para no bloquear el hilo HTTP que atiende la petición del
 * funcionario o del ciudadano.
 *
 * La notificación solo se envía si:
 *  a) notificacion.email.enabled=true (variable de entorno MAIL_ENABLED=true)
 *  b) El expediente tiene correo electrónico registrado
 *  c) El titular autorizó notificaciones (autorizaNotificacion=true)
 */
@Slf4j
@Service
public class NotificacionEmailService {

    // ─── Inyecciones ──────────────────────────────────────────────────────────

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notificacion.email.enabled:false}")
    private boolean emailHabilitado;

    @Value("${notificacion.email.remitente:notificaciones@munihuamanga.gob.pe}")
    private String remitente;

    @Value("${notificacion.email.nombre-remitente:Municipalidad Provincial de Huamanga}")
    private String nombreRemitente;

    @Value("${notificacion.email.portal-ciudadano:http://localhost:8081}")
    private String portalCiudadano;

    @Value("${notificacion.email.portal-verificacion:http://localhost:8081/verificar-licencia.html?codigo=}")
    private String portalVerificacion;

    // ─── Formateadores ────────────────────────────────────────────────────────

    private static final DateTimeFormatter FMT_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.of("es", "PE"));

    // ─── Constructor ──────────────────────────────────────────────────────────

    public NotificacionEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender    = mailSender;
        this.templateEngine = templateEngine;
    }

    // ════════════════════════════════════════════════════════════════════════
    // EVENTO 1: Registro del Expediente (Mesa de Partes Virtual)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Notifica al ciudadano que su solicitud fue recibida y el expediente está abierto.
     * Se llama inmediatamente después de {@code ExpedienteService#crearExpediente()}.
     *
     * @param expediente Expediente recién creado.
     */
    @Async("emailExecutor")
    public void notificarRegistro(Expediente expediente) {
        if (!puedeEnviar(expediente)) return;

        try {
            Context ctx = new Context(Locale.of("es", "PE"));
            ctx.setVariable("titular",      expediente.getNombreTitular());
            ctx.setVariable("numeroTramite", expediente.getNumeroTramite());
            ctx.setVariable("nombreComercial", expediente.getNombreComercial());
            ctx.setVariable("giroNegocio",  expediente.getGiroNegocio());
            ctx.setVariable("direccion",    expediente.getDireccionEstablecimiento());
            ctx.setVariable("fechaCreacion",
                    expediente.getFechaCreacion() != null
                            ? expediente.getFechaCreacion().format(FMT_FECHA) : "—");
            ctx.setVariable("fechaLimite",
                    expediente.getFechaLimite() != null
                            ? expediente.getFechaLimite().format(FMT_FECHA) : "15 días hábiles");
            ctx.setVariable("portalCiudadano", portalCiudadano);

            String html = templateEngine.process("email/email-registro", ctx);
            String asunto = "Expediente N° " + expediente.getNumeroTramite()
                    + " — Su solicitud de Licencia ha sido registrada";

            enviarHtml(expediente.getCorreoElectronico(), asunto, html);

        } catch (Exception e) {
            log.error("[EMAIL] Error al enviar notificación de REGISTRO — expediente: {} — correo: {}",
                    expediente.getNumeroTramite(), expediente.getCorreoElectronico(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // EVENTO 2: Aprobación de la Licencia de Funcionamiento
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Notifica al ciudadano que su Licencia fue aprobada e incluye el código QR de verificación.
     * Se llama después de {@code ExpedienteService#aprobar()}.
     *
     * @param expediente Expediente en estado APROBADO con {@code licenciaQrCode} asignado.
     */
    @Async("emailExecutor")
    public void notificarAprobacion(Expediente expediente) {
        if (!puedeEnviar(expediente)) return;

        try {
            String urlVerificacion = portalVerificacion
                    + (expediente.getLicenciaQrCode() != null ? expediente.getLicenciaQrCode() : "");

            Context ctx = new Context(Locale.of("es", "PE"));
            ctx.setVariable("titular",         expediente.getNombreTitular());
            ctx.setVariable("numeroTramite",   expediente.getNumeroTramite());
            ctx.setVariable("nombreComercial", expediente.getNombreComercial());
            ctx.setVariable("razonSocial",     expediente.getRazonSocial());
            ctx.setVariable("giroNegocio",     expediente.getGiroNegocio());
            ctx.setVariable("direccion",       expediente.getDireccionEstablecimiento());
            ctx.setVariable("codigoLicencia",  expediente.getLicenciaQrCode());
            ctx.setVariable("numeroLicencia",  expediente.getNumeroLicencia());
            ctx.setVariable("nivelRiesgo",     expediente.getNivelRiesgo() != null
                    ? expediente.getNivelRiesgo().name() : "—");
            ctx.setVariable("urlVerificacion", urlVerificacion);
            ctx.setVariable("portalCiudadano", portalCiudadano);
            ctx.setVariable("urlDescargaPdf",  portalCiudadano + "/api/expedientes/"
                    + expediente.getId() + "/documentos/licencia");

            String html = templateEngine.process("email/email-aprobacion", ctx);
            String asunto = "¡Su Licencia de Funcionamiento N° "
                    + (expediente.getNumeroLicencia() != null
                            ? expediente.getNumeroLicencia() : expediente.getLicenciaQrCode())
                    + " fue Aprobada! — Municipalidad de Huamanga";

            enviarHtml(expediente.getCorreoElectronico(), asunto, html);

        } catch (Exception e) {
            log.error("[EMAIL] Error al enviar notificación de APROBACIÓN — expediente: {} — correo: {}",
                    expediente.getNumeroTramite(), expediente.getCorreoElectronico(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // EVENTO 3: Rechazo del Trámite
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Notifica al ciudadano que su expediente fue rechazado, indicando el motivo y la vía
     * para presentar un nuevo trámite o subsanar las observaciones.
     *
     * @param expediente Expediente en estado RECHAZADO.
     * @param motivo     Motivo de rechazo registrado por el funcionario.
     */
    @Async("emailExecutor")
    public void notificarRechazo(Expediente expediente, String motivo) {
        if (!puedeEnviar(expediente)) return;

        try {
            Context ctx = new Context(Locale.of("es", "PE"));
            ctx.setVariable("titular",         expediente.getNombreTitular());
            ctx.setVariable("numeroTramite",   expediente.getNumeroTramite());
            ctx.setVariable("nombreComercial", expediente.getNombreComercial());
            ctx.setVariable("motivo",          motivo != null && !motivo.isBlank()
                    ? motivo : "No se proporcionó un motivo específico.");
            ctx.setVariable("portalCiudadano", portalCiudadano);

            String html = templateEngine.process("email/email-rechazo", ctx);
            String asunto = "Expediente N° " + expediente.getNumeroTramite()
                    + " — Resultado de Evaluación: Observado";

            enviarHtml(expediente.getCorreoElectronico(), asunto, html);

        } catch (Exception e) {
            log.error("[EMAIL] Error al enviar notificación de RECHAZO — expediente: {} — correo: {}",
                    expediente.getNumeroTramite(), expediente.getCorreoElectronico(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVADOS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Verifica que el email esté habilitado, el expediente tenga correo válido
     * y el titular haya autorizado notificaciones.
     */
    private boolean puedeEnviar(Expediente exp) {
        if (!emailHabilitado) {
            log.debug("[EMAIL] Notificaciones deshabilitadas (MAIL_ENABLED=false) — omitiendo envío");
            return false;
        }
        if (exp.getCorreoElectronico() == null || exp.getCorreoElectronico().isBlank()) {
            log.debug("[EMAIL] Expediente {} sin correo electrónico — omitiendo envío",
                    exp.getNumeroTramite());
            return false;
        }
        if (Boolean.FALSE.equals(exp.getAutorizaNotificacion())) {
            log.debug("[EMAIL] Titular no autorizó notificaciones — expediente: {}",
                    exp.getNumeroTramite());
            return false;
        }
        return true;
    }

    /**
     * Envía el correo HTML de forma centralizada usando MimeMessage.
     */
    private void enviarHtml(String destinatario, String asunto, String htmlContent) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(htmlContent, true);   // true = HTML

            mailSender.send(msg);
            log.info("[EMAIL] ✅ Correo enviado a: {} — Asunto: {}", destinatario, asunto);

        } catch (Exception e) {
            log.warn("[EMAIL] ⚠ No se pudo enviar correo a {} — {}", destinatario, e.getMessage());
        }
    }
}
