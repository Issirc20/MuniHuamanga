package pe.gob.munihuamanga.licencias.expedientes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarTarifaDto;
import pe.gob.munihuamanga.licencias.common.dto.TarifaTupaDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.model.TarifaTupa;
import pe.gob.munihuamanga.licencias.expedientes.repository.TarifaTupaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la gestión del Tarifario TUPA (Fase 04 Sprint 4-D).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TarifaTupaService {

    private final TarifaTupaRepository tarifaRepository;

    @Transactional(readOnly = true)
    public List<TarifaTupaDto> listarTodas() {
        return tarifaRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TarifaTupaDto> listarActivas() {
        return tarifaRepository.findByActivoTrueOrderByMontoTotalAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TarifaTupaDto obtenerPorId(UUID id) {
        TarifaTupa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa TUPA no encontrada con ID: " + id));
        return toDto(tarifa);
    }

    @Transactional(readOnly = true)
    public TarifaTupaDto obtenerPorRiesgo(NivelRiesgo nivelRiesgo) {
        TarifaTupa tarifa = tarifaRepository.findByNivelRiesgo(nivelRiesgo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe tarifa configurada para nivel de riesgo: " + nivelRiesgo));
        return toDto(tarifa);
    }

    @Transactional
    public TarifaTupaDto actualizarTarifa(UUID id, ActualizarTarifaDto dto, String usuario) {
        TarifaTupa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa TUPA no encontrada con ID: " + id));

        log.info("[TUPA] Actualizando tarifa {} ({}) por usuario {}", tarifa.getCodigoTupa(), tarifa.getNivelRiesgo(), usuario);

        tarifa.setMontoTotal(dto.getMontoTotal());

        if (dto.getDerechoTramite() != null) {
            tarifa.setDerechoTramite(dto.getDerechoTramite());
        }

        if (dto.getCostoItse() != null) {
            tarifa.setCostoItse(dto.getCostoItse());
        } else if (dto.getDerechoTramite() != null) {
            tarifa.setCostoItse(dto.getMontoTotal().subtract(dto.getDerechoTramite()).max(BigDecimal.ZERO));
        }

        if (dto.getConcepto() != null && !dto.getConcepto().isBlank()) {
            tarifa.setConcepto(dto.getConcepto());
        }

        if (dto.getBaseLegal() != null && !dto.getBaseLegal().isBlank()) {
            tarifa.setBaseLegal(dto.getBaseLegal());
        }

        if (dto.getActivo() != null) {
            tarifa.setActivo(dto.getActivo());
        }

        tarifa.setFechaActualizacion(LocalDateTime.now());
        tarifa.setUsuarioModificacion(usuario != null ? usuario : "ADMIN_SISTEMA");

        TarifaTupa actualizada = tarifaRepository.save(tarifa);
        return toDto(actualizada);
    }

    @Transactional
    public TarifaTupaDto cambiarEstado(UUID id, boolean activo, String usuario) {
        TarifaTupa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Tarifa TUPA no encontrada con ID: " + id));

        tarifa.setActivo(activo);
        tarifa.setFechaActualizacion(LocalDateTime.now());
        tarifa.setUsuarioModificacion(usuario);

        return toDto(tarifaRepository.save(tarifa));
    }

    private TarifaTupaDto toDto(TarifaTupa t) {
        return TarifaTupaDto.builder()
                .id(t.getId())
                .codigoTupa(t.getCodigoTupa())
                .nivelRiesgo(t.getNivelRiesgo())
                .concepto(t.getConcepto())
                .montoTotal(t.getMontoTotal())
                .derechoTramite(t.getDerechoTramite())
                .costoItse(t.getCostoItse())
                .baseLegal(t.getBaseLegal())
                .activo(t.isActivo())
                .fechaActualizacion(t.getFechaActualizacion())
                .usuarioModificacion(t.getUsuarioModificacion())
                .build();
    }
}
