package pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.DeclararArchivoDto;
import pe.gob.munihuamanga.licencias.common.enums.MotivoArchivo;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.TransicionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Declaración de archivo del procedimiento por la autoridad competente.
 */
@Service
@RequiredArgsConstructor
public class ArchivoExpedienteService {

    private static final Map<MotivoArchivo, EventoExpediente> EVENTO_POR_MOTIVO = new EnumMap<>(MotivoArchivo.class);

    static {
        EVENTO_POR_MOTIVO.put(MotivoArchivo.ABANDONO, EventoExpediente.ABANDONO_DECLARADO);
        EVENTO_POR_MOTIVO.put(MotivoArchivo.DESISTIMIENTO, EventoExpediente.DESISTIMIENTO_ACEPTADO);
        EVENTO_POR_MOTIVO.put(MotivoArchivo.SOLICITUD_NO_PRESENTADA, EventoExpediente.SOLICITUD_TENIDA_POR_NO_PRESENTADA);
    }

    private final ExpedienteRepository expedienteRepository;
    private final ComandosExpediente comandosExpediente;

    @Transactional
    public TransicionExpediente declarar(UUID expedienteId, DeclararArchivoDto dto) {
        TransicionExpediente transicion = comandosExpediente.registrar(
                expedienteId, EVENTO_POR_MOTIVO.get(dto.getMotivo()), dto.getSustento());

        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", expedienteId));
        expediente.setMotivoArchivo(dto.getMotivo());
        expedienteRepository.save(expediente);
        return transicion;
    }
}
