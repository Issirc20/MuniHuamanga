package pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.AccionPropuestaDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Aplica las reglas de vencimiento sobre los expedientes en trámite y consolida las acciones propuestas.
 */
@Service
@RequiredArgsConstructor
public class VigilanteDePlazos {

    private final ExpedienteRepository expedienteRepository;
    private final List<ReglaVencimiento> reglas;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public List<AccionPropuestaDto> evaluarExpedientesEnTramite() {
        LocalDate hoy = LocalDate.now(reloj);
        return expedienteRepository.findByEstadoIn(EstadoExpediente.noTerminales()).stream()
                .flatMap(expediente -> evaluar(expediente, hoy).stream())
                .sorted(Comparator.comparing(AccionPropuestaDto::getAccion).thenComparing(AccionPropuestaDto::getNumeroTramite))
                .toList();
    }

    private List<AccionPropuestaDto> evaluar(Expediente expediente, LocalDate hoy) {
        return reglas.stream()
                .filter(regla -> regla.aplicaA(expediente.getEstado()))
                .map(regla -> regla.evaluar(expediente, hoy))
                .flatMap(Optional::stream)
                .toList();
    }
}
