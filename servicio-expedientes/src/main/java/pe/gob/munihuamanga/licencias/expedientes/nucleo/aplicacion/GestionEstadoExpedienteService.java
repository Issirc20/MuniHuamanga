package pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.TransicionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementación del puerto {@link ComandosExpediente}.
 * Obtiene el expediente con bloqueo, delega la regla de transición en el agregado,
 * persiste el resultado y publica la transición.
 */
@Service
@RequiredArgsConstructor
public class GestionEstadoExpedienteService implements ComandosExpediente {

    private final ExpedienteRepository expedienteRepository;
    private final ProveedorActor proveedorActor;
    private final ApplicationEventPublisher publicadorEventos;
    private final Clock reloj;

    @Override
    @Transactional
    public TransicionExpediente registrar(UUID expedienteId, EventoExpediente evento, String sustento) {
        Expediente expediente = expedienteRepository.findByIdWithLock(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", expedienteId));

        TransicionExpediente transicion = expediente.aplicar(
                evento, proveedorActor.actual(), LocalDateTime.now(reloj), sustento);

        expedienteRepository.save(expediente);
        publicadorEventos.publishEvent(new TransicionExpedienteRegistrada(transicion));
        return transicion;
    }
}
