package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.DiaNoLaborableDto;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mantenimiento del calendario de días no laborables.
 */
@Service
@RequiredArgsConstructor
public class CalendarioInstitucionalService {

    private final DiaNoLaborableRepository repositorio;
    private final CalendarioLaboralBaseDatos calendario;
    private final ProveedorActor proveedorActor;

    @Transactional(readOnly = true)
    public List<DiaNoLaborableDto> listar(int anio) {
        return repositorio.findByFechaBetweenOrderByFechaAsc(LocalDate.of(anio, 1, 1), LocalDate.of(anio, 12, 31)).stream()
                .map(dia -> DiaNoLaborableDto.builder()
                        .fecha(dia.getFecha())
                        .descripcion(dia.getDescripcion())
                        .baseLegal(dia.getBaseLegal())
                        .build())
                .toList();
    }

    @Transactional
    public DiaNoLaborableDto registrar(DiaNoLaborableDto dto) {
        DiaNoLaborable dia = repositorio.findById(dto.getFecha())
                .orElseGet(() -> DiaNoLaborable.builder().fecha(dto.getFecha()).build());
        dia.setDescripcion(dto.getDescripcion().trim());
        dia.setBaseLegal(dto.getBaseLegal() != null ? dto.getBaseLegal().trim() : null);
        dia.setRegistradoPor(proveedorActor.actual().nombreParaRegistro());
        dia.setFechaRegistro(LocalDateTime.now());
        repositorio.save(dia);
        calendario.refrescar();
        return dto;
    }

    @Transactional
    public void eliminar(LocalDate fecha) {
        if (!repositorio.existsById(fecha)) {
            throw new RecursoNoEncontradoException("Día no laborable", fecha);
        }
        repositorio.deleteById(fecha);
        calendario.refrescar();
    }
}
