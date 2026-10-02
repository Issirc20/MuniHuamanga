package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del NotificacionEmailService — Fase 04 Sprint 4-B.
 *
 * Estrategia:
 *  - Se mockea JavaMailSender y TemplateEngine para no necesitar SMTP real.
 *  - Se verifica que puedeEnviar() filtre correctamente:
 *      1) emailHabilitado = false → no envía
 *      2) Sin correo → no envía
 *      3) autorizaNotificacion = false → no envía
 *      4) Todos los campos OK → envía (templateEngine procesado + mailSender invocado)
 *  - No se prueban efectos SMTP reales (integration test separado).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pruebas Unitarias de NotificacionEmailService — Fase 04 Sprint 4-B")
class NotificacionEmailServiceTest {

    @Mock
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private NotificacionEmailService notificacionEmailService;

    private Expediente expedienteConCorreo;
    private Expediente expedienteSinCorreo;
    private Expediente expedienteNoAutorizado;

    @BeforeEach
    void setUp() throws Exception {
        // emailHabilitado=false por defecto (campo privado, lo seteamos via reflection)
        // — usamos directamente los casos donde el expediente controla el envío

        expedienteConCorreo = Expediente.builder()
                .numeroTramite("EXP-2026-99901")
                .nombreTitular("Ana Torres Quispe")
                .correoElectronico("ana.torres@gmail.com")
                .autorizaNotificacion(true)
                .nombreComercial("Boutique Ana")
                .giroNegocio("Comercio de ropa")
                .direccionEstablecimiento("Jr. Lima N° 101, Huamanga")
                .licenciaQrCode("LIC-2026-TESTCODE")
                .fechaCreacion(LocalDateTime.now())
                .build();

        expedienteConCorreo.setId(java.util.UUID.randomUUID());

        expedienteConCorreo.setFechaLimite(LocalDateTime.now().plusDays(15));

        expedienteSinCorreo = Expediente.builder()
                .numeroTramite("EXP-2026-99902")
                .nombreTitular("Pedro Palomino")
                .correoElectronico(null)           // SIN correo
                .autorizaNotificacion(true)
                .nombreComercial("Tienda Pedro")
                .giroNegocio("Abarrotes")
                .build();

        expedienteNoAutorizado = Expediente.builder()
                .numeroTramite("EXP-2026-99903")
                .nombreTitular("Luisa Mendoza")
                .correoElectronico("luisa@gmail.com")
                .autorizaNotificacion(false)        // NO autoriza notificaciones
                .nombreComercial("Restaurant Luisa")
                .giroNegocio("Restaurante")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tests de puedeEnviar() — condiciones de habilitación
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("No debe enviar si emailHabilitado=false (configuración por defecto en tests)")
    void noEnviaConEmailDeshabilitado() {
        // emailHabilitado=false por defecto (valor del campo @Value no inyectado en unit test puro)
        // El templateEngine NO debe ser invocado
        notificacionEmailService.notificarRegistro(expedienteConCorreo);

        verifyNoInteractions(templateEngine);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("No debe enviar si el expediente no tiene correo electrónico")
    void noEnviaSinCorreo() {
        // Aunque email esté habilitado, sin correo no debe intentar enviar
        setEmailHabilitado(true);

        notificacionEmailService.notificarRegistro(expedienteSinCorreo);

        verifyNoInteractions(templateEngine);
        verifyNoInteractions(mailSender);
    }

    @Test
    @DisplayName("No debe enviar si el titular no autorizó notificaciones")
    void noEnviaSinAutorizacion() {
        setEmailHabilitado(true);

        notificacionEmailService.notificarAprobacion(expedienteNoAutorizado);

        verifyNoInteractions(templateEngine);
        verifyNoInteractions(mailSender);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tests de envío efectivo (con mocks de TemplateEngine + JavaMailSender)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Debe procesar plantilla email-registro cuando hay correo y está habilitado")
    void enviaNotificacionRegistroCuandoHabilitado() {
        setEmailHabilitado(true);
        when(templateEngine.process(eq("email/email-registro"), any(IContext.class)))
                .thenReturn("<html>Correo de prueba registro</html>");

        // JavaMailSender.createMimeMessage() necesita mock de MimeMessage
        jakarta.mail.internet.MimeMessage mimeMsgMock =
                mock(jakarta.mail.internet.MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsgMock);

        notificacionEmailService.notificarRegistro(expedienteConCorreo);

        verify(templateEngine, times(1)).process(eq("email/email-registro"), any(IContext.class));
        verify(mailSender, times(1)).createMimeMessage();
    }

    @Test
    @DisplayName("Debe procesar plantilla email-aprobacion y llamar a mailSender")
    void enviaNotificacionAprobacionCuandoHabilitado() {
        setEmailHabilitado(true);
        when(templateEngine.process(eq("email/email-aprobacion"), any(IContext.class)))
                .thenReturn("<html>Correo de prueba aprobacion</html>");

        jakarta.mail.internet.MimeMessage mimeMsgMock =
                mock(jakarta.mail.internet.MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsgMock);

        notificacionEmailService.notificarAprobacion(expedienteConCorreo);

        verify(templateEngine, times(1)).process(eq("email/email-aprobacion"), any(IContext.class));
        verify(mailSender, times(1)).createMimeMessage();
    }

    @Test
    @DisplayName("Debe procesar plantilla email-rechazo con el motivo indicado")
    void enviaNotificacionRechazoCuandoHabilitado() {
        setEmailHabilitado(true);
        when(templateEngine.process(eq("email/email-rechazo"), any(IContext.class)))
                .thenReturn("<html>Correo de prueba rechazo</html>");

        jakarta.mail.internet.MimeMessage mimeMsgMock =
                mock(jakarta.mail.internet.MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMsgMock);

        notificacionEmailService.notificarRechazo(expedienteConCorreo,
                "Documentación incompleta — falta certificado INDECI.");

        verify(templateEngine, times(1)).process(eq("email/email-rechazo"), any(IContext.class));
        verify(mailSender, times(1)).createMimeMessage();
    }

    @Test
    @DisplayName("No debe lanzar excepción aunque SMTP falle (captura interna de errores)")
    void noPropagaExcepcionSiSmtpFalla() {
        setEmailHabilitado(true);
        when(templateEngine.process(anyString(), any(IContext.class)))
                .thenReturn("<html>OK</html>");

        jakarta.mail.internet.MimeMessage mimeMsgMock =
                mock(jakarta.mail.internet.MimeMessage.class);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMsgMock);
        // Simula falla en el envío SMTP
        lenient().doThrow(new org.springframework.mail.MailSendException("SMTP no disponible"))
                .when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        // No debe propagar la excepción — el error es logeado y silenciado
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                notificacionEmailService.notificarRegistro(expedienteConCorreo));
    }


    // ─── Utilidad: inyectar campo privado vía reflexión ─────────────────────

    private void setEmailHabilitado(boolean valor) {
        try {
            java.lang.reflect.Field field =
                    NotificacionEmailService.class.getDeclaredField("emailHabilitado");
            field.setAccessible(true);
            field.set(notificacionEmailService, valor);
        } catch (Exception e) {
            throw new RuntimeException("Error al inyectar emailHabilitado por reflexión", e);
        }
    }
}
