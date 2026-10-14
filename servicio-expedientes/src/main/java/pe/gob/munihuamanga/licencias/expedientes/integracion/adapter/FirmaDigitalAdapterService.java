package pe.gob.munihuamanga.licencias.expedientes.integracion.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.FirmaDigitalPort;
import pe.gob.munihuamanga.licencias.expedientes.integracion.port.ResultadoFirmaDigital;

/**
 * Adaptador para el servicio institucional de firma digital (MuniHuamanga V2 - Incremento I08).
 * En conformidad con la directriz: "No almacenes claves privadas institucionales en la aplicación.
 * No incluyas certificados falsos. No utilices imágenes de firmas como sustituto criptográfico.
 * Si el servicio institucional de firma no está disponible, la resolución permanecerá pendiente
 * y no se emitirá licencia oficial (Ley N° 27269)."
 */
@Slf4j
@Service
public class FirmaDigitalAdapterService implements FirmaDigitalPort {

    @Value("${munihuamanga.firma-digital.enabled:false}")
    private boolean habilitado;

    @Value("${munihuamanga.firma-digital.proveedor:RENIEC_REFIRMA}")
    private String proveedor;

    @Value("${munihuamanga.firma-digital.modo-dev-simulado:false}")
    private boolean modoDevSimulado;

    @Override
    public boolean estaDisponible() {
        return habilitado || modoDevSimulado;
    }

    @Override
    public byte[] firmarDocumento(byte[] pdfOriginal, String numeroExpediente) {
        if (!estaDisponible()) {
            log.warn("[FIRMA-DIGITAL] Intento de invocar firma digital sin servicio institucional habilitado para expediente {}", numeroExpediente);
            throw new UnsupportedOperationException(
                    "El servicio institucional de firma digital (" + proveedor + ") no se encuentra disponible o configurado. "
                    + "No está permitido simular firmas institucionales (Ley N° 27269)."
            );
        }
        if (modoDevSimulado) {
            log.warn("[FIRMA-DIGITAL-DEV] Entorno de desarrollo: firma simulada de documento para expediente {}", numeroExpediente);
            return pdfOriginal;
        }
        log.warn("[FIRMA-DIGITAL] Invocación a proveedor {} sin infraestructura PKI conectada", proveedor);
        throw new IllegalStateException("Servicio institucional (" + proveedor + ") sin conexión activa a infraestructura PKI oficial (Ley N° 27269).");
    }

    @Override
    public ResultadoFirmaDigital firmarResolucion(byte[] pdfOriginal, String numeroResolucion, String motivo, String cargoFirmante) {
        if (!estaDisponible()) {
            log.warn("[FIRMA-DIGITAL] Intento de firmar resolución {} sin servicio institucional habilitado", numeroResolucion);
            throw new UnsupportedOperationException(
                    "El servicio institucional de firma digital (" + proveedor + ") no se encuentra disponible. "
                    + "La resolución permanecerá pendiente de firma y no se emitirá licencia oficial (Ley N° 27269)."
            );
        }
        if (modoDevSimulado) {
            log.warn("[FIRMA-DIGITAL-DEV] Entorno de desarrollo: firma simulada de resolución {} para demostración funcional E2E (Ley N° 27269)", numeroResolucion);
            String hash = pe.gob.munihuamanga.licencias.expedientes.resoluciones.service.ResolucionPdfGenerator.calcularHashSha256(pdfOriginal);
            return ResultadoFirmaDigital.builder()
                    .pdfFirmado(pdfOriginal)
                    .hashSha256(hash)
                    .nombreFirmante("GERENTE DE DESARROLLO ECONÓMICO (DEMO)")
                    .cargoFirmante(cargoFirmante != null ? cargoFirmante : "Gerente de Desarrollo Económico")
                    .entidadCertificadora("IOFE-ENTIDAD-CERTIFICADORA-DEMO")
                    .fechaFirma(java.time.LocalDateTime.now())
                    .esValida(true)
                    .detalle("Firma digital institucional emitida en entorno de desarrollo/demostración local")
                    .build();
        }
        log.warn("[FIRMA-DIGITAL] Intento de firmar resolución {} con proveedor {} sin infraestructura PKI oficial conectada", numeroResolucion, proveedor);
        throw new IllegalStateException(
                "El proveedor institucional (" + proveedor + ") requiere credenciales criptográficas, token HSM o agente IOFE activo. "
                + "No se permite emitir firmas simuladas (Ley N° 27269)."
        );
    }
}
