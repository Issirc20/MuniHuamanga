package pe.gob.munihuamanga.licencias.expedientes.cuentas.service;

/**
 * Puerto de comunicaciones relativas a las cuentas de acceso.
 */
public interface PuertoNotificacionCuenta {

    void enviarVerificacionCorreo(String correo, String destinatario, String enlace);

    void enviarActivacionAcceso(String correo, String destinatario, String enlace);
}
