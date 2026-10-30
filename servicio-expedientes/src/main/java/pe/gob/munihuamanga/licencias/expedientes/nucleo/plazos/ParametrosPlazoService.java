package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ParametroPlazoDto;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Consulta y mantenimiento de los plazos configurables del procedimiento.
 * Si un plazo no fue configurado, rige su valor predeterminado.
 */
@Service
@RequiredArgsConstructor
public class ParametrosPlazoService {

    private final ParametroPlazoRepository repositorio;
    private final ProveedorActor proveedorActor;

    @Transactional(readOnly = true)
    public int dias(CodigoPlazo codigo) {
        return repositorio.findById(codigo)
                .map(ParametroPlazo::getDias)
                .orElse(codigo.getDiasPredeterminados());
    }

    @Transactional(readOnly = true)
    public List<ParametroPlazoDto> listar() {
        return Arrays.stream(CodigoPlazo.values())
                .map(codigo -> aDto(codigo, dias(codigo)))
                .toList();
    }

    @Transactional
    public ParametroPlazoDto actualizar(CodigoPlazo codigo, int dias) {
        ParametroPlazo parametro = repositorio.findById(codigo)
                .orElseGet(() -> ParametroPlazo.builder().codigo(codigo).build());
        parametro.setDias(dias);
        parametro.setActualizadoPor(proveedorActor.actual().nombreParaRegistro());
        parametro.setFechaActualizacion(LocalDateTime.now());
        repositorio.save(parametro);
        return aDto(codigo, dias);
    }

    private static ParametroPlazoDto aDto(CodigoPlazo codigo, int dias) {
        return ParametroPlazoDto.builder()
                .codigo(codigo.name())
                .dias(dias)
                .descripcion(codigo.getDescripcion())
                .baseLegal(codigo.getBaseLegal())
                .build();
    }
}
