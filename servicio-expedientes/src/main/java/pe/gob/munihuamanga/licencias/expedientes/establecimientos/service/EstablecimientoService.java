package pe.gob.munihuamanga.licencias.expedientes.establecimientos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.CatalogoCiiuDto;
import pe.gob.munihuamanga.licencias.common.dto.CrearEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.EstablecimientoDto;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.CatalogoCiiu;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.CatalogoCiiuRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de Negocio para el Módulo M02: Establecimientos y Catálogo CIIU.
 * Permite múltiples locales por cuenta, valida rangos geográficos y
 * explicita que la ubicación física no constituye zonificación municipal aprobada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EstablecimientoService {

    private final EstablecimientoRepository establecimientoRepository;
    private final CuentaRepository cuentaRepository;
    private final CatalogoCiiuRepository catalogoCiiuRepository;

    public static final String ADVERTENCIA_ZONIFICACION_REFERENCIAL =
            "La ubicación física y georreferenciación declaradas son de carácter estrictamente referencial y no constituyen "
            + "zonificación municipal aprobada ni compatibilidad de uso comercial según el Plan de Desarrollo Urbano (PDU) vigente.";

    @Transactional
    public EstablecimientoDto registrarEstablecimiento(UUID cuentaId, CrearEstablecimientoDto dto, String usuarioResponsable) {
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new NoSuchElementException("Cuenta titular no encontrada con ID: " + cuentaId));

        validarCoordenadas(dto.getLatitud(), dto.getLongitud());

        // Validar catálogo CIIU
        CatalogoCiiu ciiu = null;
        if (dto.getCiiuCodigo() != null && !dto.getCiiuCodigo().isBlank()) {
            ciiu = catalogoCiiuRepository.findById(dto.getCiiuCodigo().trim())
                    .orElseGet(() -> {
                        log.warn("[ESTABLECIMIENTOS] Código CIIU {} no encontrado en catálogo; registrando genérico referencial", dto.getCiiuCodigo());
                        return catalogoCiiuRepository.save(CatalogoCiiu.builder()
                                .codigo(dto.getCiiuCodigo().trim())
                                .descripcion(dto.getGiroPrincipal().trim())
                                .giroPrincipal(dto.getGiroPrincipal().trim())
                                .activo(true)
                                .build());
                    });
        }

        // Generar código de establecimiento secuencial por cuenta (ej. EST-001) sin asignar número de expediente
        long correlativo = establecimientoRepository.countByCuentaId(cuentaId) + 1;
        String codigoEstablecimiento = String.format("EST-%03d", correlativo);

        UUID establecimientoId = UUID.randomUUID();

        Establecimiento est = Establecimiento.builder()
                .id(establecimientoId)
                .cuenta(cuenta)
                .codigoEstablecimiento(codigoEstablecimiento)
                .nombreComercial(dto.getNombreComercial().trim())
                .direccion(dto.getDireccion().trim())
                .distrito(dto.getDistrito() != null ? dto.getDistrito().trim() : "Ayacucho")
                .provincia(dto.getProvincia() != null ? dto.getProvincia().trim() : "Huamanga")
                .departamento(dto.getDepartamento() != null ? dto.getDepartamento().trim() : "Ayacucho")
                .referencia(dto.getReferencia() != null ? dto.getReferencia().trim() : null)
                .latitud(dto.getLatitud())
                .longitud(dto.getLongitud())
                .ciiu(ciiu)
                .giroPrincipal(dto.getGiroPrincipal().trim())
                .areaMetrosCuadrados(dto.getAreaMetrosCuadrados())
                .activo(true)
                .fechaRegistro(LocalDateTime.now())
                .build();

        Establecimiento guardado = establecimientoRepository.save(est);
        log.info("[ESTABLECIMIENTOS] Establecimiento registrado: ID={}, Código={}, Cuenta={}, RegistradoPor={}",
                guardado.getId(), codigoEstablecimiento, cuentaId, usuarioResponsable);

        return toDto(guardado);
    }

    @Transactional
    public EstablecimientoDto actualizarEstablecimiento(UUID establecimientoId, ActualizarEstablecimientoDto dto, String usuarioResponsable) {
        Establecimiento est = establecimientoRepository.findById(establecimientoId)
                .orElseThrow(() -> new NoSuchElementException("Establecimiento no encontrado con ID: " + establecimientoId));

        if (dto.getLatitud() != null || dto.getLongitud() != null) {
            BigDecimal lat = dto.getLatitud() != null ? dto.getLatitud() : est.getLatitud();
            BigDecimal lon = dto.getLongitud() != null ? dto.getLongitud() : est.getLongitud();
            validarCoordenadas(lat, lon);
            est.setLatitud(lat);
            est.setLongitud(lon);
        }

        if (dto.getNombreComercial() != null) est.setNombreComercial(dto.getNombreComercial().trim());
        if (dto.getDireccion() != null) est.setDireccion(dto.getDireccion().trim());
        if (dto.getDistrito() != null) est.setDistrito(dto.getDistrito().trim());
        if (dto.getReferencia() != null) est.setReferencia(dto.getReferencia().trim());
        if (dto.getGiroPrincipal() != null) est.setGiroPrincipal(dto.getGiroPrincipal().trim());
        if (dto.getAreaMetrosCuadrados() != null) est.setAreaMetrosCuadrados(dto.getAreaMetrosCuadrados());
        if (dto.getActivo() != null) est.setActivo(dto.getActivo());

        if (dto.getCiiuCodigo() != null && !dto.getCiiuCodigo().isBlank()) {
            CatalogoCiiu ciiu = catalogoCiiuRepository.findById(dto.getCiiuCodigo().trim())
                    .orElse(null);
            est.setCiiu(ciiu);
        }

        est.setFechaModificacion(LocalDateTime.now());
        Establecimiento actualizado = establecimientoRepository.save(est);

        log.info("[ESTABLECIMIENTOS] Establecimiento actualizado: ID={}, ModificadoPor={}", establecimientoId, usuarioResponsable);
        return toDto(actualizado);
    }

    @Transactional(readOnly = true)
    public EstablecimientoDto obtenerPorId(UUID establecimientoId) {
        Establecimiento est = establecimientoRepository.findById(establecimientoId)
                .orElseThrow(() -> new NoSuchElementException("Establecimiento no encontrado con ID: " + establecimientoId));
        return toDto(est);
    }

    @Transactional(readOnly = true)
    public List<EstablecimientoDto> listarPorCuenta(UUID cuentaId) {
        return establecimientoRepository.findByCuentaId(cuentaId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CatalogoCiiuDto> listarCatalogoCiiu() {
        return catalogoCiiuRepository.findByActivoTrueOrderByCodigoAsc().stream()
                .map(c -> CatalogoCiiuDto.builder()
                        .codigo(c.getCodigo())
                        .descripcion(c.getDescripcion())
                        .giroPrincipal(c.getGiroPrincipal())
                        .nivelRiesgoSugerido(c.getNivelRiesgoSugerido())
                        .activo(c.getActivo())
                        .build())
                .collect(Collectors.toList());
    }

    private void validarCoordenadas(BigDecimal latitud, BigDecimal longitud) {
        if (latitud == null || longitud == null) {
            throw new IllegalArgumentException("Las coordenadas geográficas (latitud y longitud) son obligatorias.");
        }

        // Rango global estricto
        if (latitud.compareTo(new BigDecimal("-90.0")) < 0 || latitud.compareTo(new BigDecimal("90.0")) > 0) {
            throw new IllegalArgumentException("La latitud debe estar comprendida entre -90.0 y 90.0 grados.");
        }
        if (longitud.compareTo(new BigDecimal("-180.0")) < 0 || longitud.compareTo(new BigDecimal("180.0")) > 0) {
            throw new IllegalArgumentException("La longitud debe estar comprendida entre -180.0 y 180.0 grados.");
        }

        // Validación de coherencia territorial en territorio nacional peruano (latitud entre -19 y 0, longitud entre -82 y -68)
        if (latitud.compareTo(new BigDecimal("-19.0")) < 0 || latitud.compareTo(new BigDecimal("1.0")) > 0 ||
            longitud.compareTo(new BigDecimal("-82.0")) < 0 || longitud.compareTo(new BigDecimal("-68.0")) > 0) {
            log.warn("[ESTABLECIMIENTOS] Advertencia de geolocalización: coordenadas ({}, {}) fuera del territorio peruano estándar", latitud, longitud);
        }
    }

    public EstablecimientoDto toDto(Establecimiento est) {
        return EstablecimientoDto.builder()
                .id(est.getId())
                .cuentaId(est.getCuenta().getId())
                .codigoEstablecimiento(est.getCodigoEstablecimiento())
                .nombreComercial(est.getNombreComercial())
                .direccion(est.getDireccion())
                .distrito(est.getDistrito())
                .provincia(est.getProvincia())
                .departamento(est.getDepartamento())
                .referencia(est.getReferencia())
                .latitud(est.getLatitud())
                .longitud(est.getLongitud())
                .ciiuCodigo(est.getCiiu() != null ? est.getCiiu().getCodigo() : null)
                .giroPrincipal(est.getGiroPrincipal())
                .areaMetrosCuadrados(est.getAreaMetrosCuadrados())
                .advertenciaZonificacion(ADVERTENCIA_ZONIFICACION_REFERENCIAL)
                .activo(est.getActivo())
                .fechaRegistro(est.getFechaRegistro())
                .fechaModificacion(est.getFechaModificacion())
                .build();
    }
}
