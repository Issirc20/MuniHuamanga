package pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.AdmitirSolicitudDto;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.service.ValidacionIdentidadService;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.EventoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.TransicionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.util.UUID;

/**
 * Verificación de admisibilidad de la solicitud presentada.
 * La admisión exige que la identidad del administrado esté validada; si no lo está,
 * mesa de partes la valida en el mismo acto indicando el medio empleado.
 * Las observaciones de admisibilidad se formulan mediante el servicio de observaciones.
 */
@Service
@RequiredArgsConstructor
public class AdmisionExpedienteService {

    private final ExpedienteRepository expedienteRepository;
    private final ValidacionIdentidadService validacionIdentidadService;
    private final ComandosExpediente comandosExpediente;

    @Transactional
    public TransicionExpediente admitir(UUID expedienteId, AdmitirSolicitudDto dto) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", expedienteId));

        if (expediente.getCuentaId() != null && !validacionIdentidadService.estaValidada(expediente.getCuentaId())) {
            if (dto.getMedioValidacionIdentidad() == null) {
                throw new IllegalArgumentException(
                        "La identidad del administrado aún no ha sido validada. Indique el medio y la referencia de la validación.");
            }
            validacionIdentidadService.validar(expediente.getCuentaId(),
                    dto.getMedioValidacionIdentidad(), dto.getReferenciaValidacionIdentidad());
        }

        return comandosExpediente.registrar(expedienteId, EventoExpediente.SOLICITUD_ADMITIDA, dto.getSustento());
    }
}
