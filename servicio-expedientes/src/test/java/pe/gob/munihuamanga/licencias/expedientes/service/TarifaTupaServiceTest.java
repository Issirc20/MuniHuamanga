package pe.gob.munihuamanga.licencias.expedientes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.dto.ActualizarTarifaDto;
import pe.gob.munihuamanga.licencias.common.dto.TarifaTupaDto;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.model.TarifaTupa;
import pe.gob.munihuamanga.licencias.expedientes.repository.TarifaTupaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TarifaTupaServiceTest {

    @Mock
    private TarifaTupaRepository tarifaRepository;

    @InjectMocks
    private TarifaTupaService tarifaService;

    private TarifaTupa tarifaBajo;
    private UUID tarifaId;

    @BeforeEach
    void setUp() {
        tarifaId = UUID.randomUUID();
        tarifaBajo = TarifaTupa.builder()
                .id(tarifaId)
                .codigoTupa("TUPA-ITSE-01")
                .nivelRiesgo(NivelRiesgo.BAJO)
                .concepto("ITSE Riesgo Bajo")
                .montoTotal(new BigDecimal("154.50"))
                .derechoTramite(new BigDecimal("45.00"))
                .costoItse(new BigDecimal("109.50"))
                .baseLegal("Ordenanza Municipal 018-2024")
                .activo(true)
                .fechaActualizacion(LocalDateTime.now())
                .usuarioModificacion("SYSTEM")
                .build();
    }

    @Test
    @DisplayName("Debe listar todas las tarifas del TUPA correctamente")
    void testListarTodas() {
        when(tarifaRepository.findAll()).thenReturn(Collections.singletonList(tarifaBajo));

        List<TarifaTupaDto> resultado = tarifaService.listarTodas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("TUPA-ITSE-01", resultado.get(0).getCodigoTupa());
        assertEquals(new BigDecimal("154.50"), resultado.get(0).getMontoTotal());
    }

    @Test
    @DisplayName("Debe obtener tarifa por nivel de riesgo ITSE")
    void testObtenerPorRiesgo() {
        when(tarifaRepository.findByNivelRiesgo(NivelRiesgo.BAJO)).thenReturn(Optional.of(tarifaBajo));

        TarifaTupaDto dto = tarifaService.obtenerPorRiesgo(NivelRiesgo.BAJO);

        assertNotNull(dto);
        assertEquals(NivelRiesgo.BAJO, dto.getNivelRiesgo());
        assertEquals(new BigDecimal("154.50"), dto.getMontoTotal());
    }

    @Test
    @DisplayName("Debe actualizar monto, desglose y registrar usuario que modificó")
    void testActualizarTarifa() {
        ActualizarTarifaDto updateDto = ActualizarTarifaDto.builder()
                .montoTotal(new BigDecimal("170.00"))
                .derechoTramite(new BigDecimal("50.00"))
                .costoItse(new BigDecimal("120.00"))
                .concepto("Nuevo Concepto TUPA 2026")
                .baseLegal("Nueva Ordenanza Municipal 2026")
                .activo(true)
                .build();

        when(tarifaRepository.findById(tarifaId)).thenReturn(Optional.of(tarifaBajo));
        when(tarifaRepository.save(any(TarifaTupa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TarifaTupaDto actualizada = tarifaService.actualizarTarifa(tarifaId, updateDto, "admin_juan");

        assertNotNull(actualizada);
        assertEquals(new BigDecimal("170.00"), actualizada.getMontoTotal());
        assertEquals(new BigDecimal("50.00"), actualizada.getDerechoTramite());
        assertEquals(new BigDecimal("120.00"), actualizada.getCostoItse());
        assertEquals("Nuevo Concepto TUPA 2026", actualizada.getConcepto());
        assertEquals("admin_juan", actualizada.getUsuarioModificacion());

        verify(tarifaRepository).save(any(TarifaTupa.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intenta actualizar una tarifa inexistente")
    void testActualizarTarifaInexistente() {
        UUID idInexistente = UUID.randomUUID();
        when(tarifaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () ->
                tarifaService.actualizarTarifa(idInexistente, ActualizarTarifaDto.builder().montoTotal(BigDecimal.TEN).build(), "admin")
        );
    }
}
