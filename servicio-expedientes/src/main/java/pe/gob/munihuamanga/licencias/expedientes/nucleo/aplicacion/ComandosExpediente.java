package pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion;

import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.TransicionExpediente;

import java.util.UUID;

/**
 * Puerto por el cual los módulos del sistema comunican al expediente los hechos del procedimiento.
 * Ningún módulo modifica directamente el estado del expediente.
 */
public interface ComandosExpediente {

    /**
     * Registra un hecho del procedimiento, valida la transición, persiste el nuevo estado
     * y publica la transición para los componentes interesados (historial, plazos, etapas).
     */
    TransicionExpediente registrar(UUID expedienteId, EventoExpediente evento, String sustento);
}
