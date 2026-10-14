package pe.gob.munihuamanga.licencias.expedientes.establecimientos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.gob.munihuamanga.licencias.common.dto.CrearEstablecimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.EstablecimientoDto;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.CatalogoCiiu;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.CatalogoCiiuRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.repository.EstablecimientoRepository;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.service.EstablecimientoService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstablecimientoServiceTest {

    @Mock
    private EstablecimientoRepository establecimientoRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private CatalogoCiiuRepository catalogoCiiuRepository;

    private EstablecimientoService establecimientoService;

    @BeforeEach
    void setUp() {
        establecimientoService = new EstablecimientoService(
                establecimientoRepository,
                cuentaRepository,
                catalogoCiiuRepository
        );
    }

    @Test
    @DisplayName("I03: Registro de establecimiento genera código secuencial EST-001 sin número de expediente")
    void registrarEstablecimiento_GeneraCodigoSecuencialSinExpediente() {
        UUID cuentaId = UUID.randomUUID();
        Cuenta cuenta = Cuenta.builder().id(cuentaId).numeroDocumento("20601234567").build();

        CrearEstablecimientoDto dto = CrearEstablecimientoDto.builder()
                .nombreComercial("Sucursal Centro Histórico")
                .direccion("Portal Constitución N° 12")
                .distrito("Ayacucho")
                .provincia("Huamanga")
                .departamento("Ayacucho")
                .latitud(new BigDecimal("-13.1631400"))
                .longitud(new BigDecimal("-74.2235600"))
                .ciiuCodigo("4711")
                .giroPrincipal("Minimarket y Abarrotes")
                .areaMetrosCuadrados(new BigDecimal("85.50"))
                .build();

        CatalogoCiiu ciiu = CatalogoCiiu.builder().codigo("4711").descripcion("Abarrotes").build();

        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(catalogoCiiuRepository.findById("4711")).thenReturn(Optional.of(ciiu));
        when(establecimientoRepository.countByCuentaId(cuentaId)).thenReturn(0L);
        when(establecimientoRepository.save(any(Establecimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        EstablecimientoDto resultado = establecimientoService.registrarEstablecimiento(cuentaId, dto, "titular_juan");

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCodigoEstablecimiento()).isEqualTo("EST-001");
        assertThat(resultado.getNombreComercial()).isEqualTo("Sucursal Centro Histórico");
        assertThat(resultado.getAdvertenciaZonificacion()).contains("no constituyen zonificación municipal aprobada");
        // Verificar que no se generó ni asignó ningún número de expediente
        assertThat(resultado.getCodigoEstablecimiento()).doesNotContain("EXP-");
    }

    @Test
    @DisplayName("I03: Soporte para múltiples establecimientos bajo la misma cuenta (RUC)")
    void registrarMultiplesEstablecimientos_IncrementaCorrelativo() {
        UUID cuentaId = UUID.randomUUID();
        Cuenta cuenta = Cuenta.builder().id(cuentaId).numeroDocumento("20601234567").build();

        CrearEstablecimientoDto dto = CrearEstablecimientoDto.builder()
                .nombreComercial("Segunda Sucursal Santa Ana")
                .direccion("Av. Mariscal Cáceres N° 850")
                .latitud(new BigDecimal("-13.1610000"))
                .longitud(new BigDecimal("-74.2210000"))
                .ciiuCodigo("4773")
                .giroPrincipal("Artesanías")
                .areaMetrosCuadrados(new BigDecimal("40.00"))
                .build();

        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));
        when(catalogoCiiuRepository.findById("4773")).thenReturn(Optional.of(CatalogoCiiu.builder().codigo("4773").build()));
        // Ya existen 2 establecimientos registrados previamente
        when(establecimientoRepository.countByCuentaId(cuentaId)).thenReturn(2L);
        when(establecimientoRepository.save(any(Establecimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        EstablecimientoDto resultado = establecimientoService.registrarEstablecimiento(cuentaId, dto, "titular_juan");

        assertThat(resultado.getCodigoEstablecimiento()).isEqualTo("EST-003");
    }

    @Test
    @DisplayName("I03: Validación estricta de rangos de coordenadas geográficas")
    void registrarEstablecimiento_CoordenadasInvalidas_LanzaExcepcion() {
        UUID cuentaId = UUID.randomUUID();
        Cuenta cuenta = Cuenta.builder().id(cuentaId).build();

        CrearEstablecimientoDto dtoLatitudInvalida = CrearEstablecimientoDto.builder()
                .nombreComercial("Local Prueba")
                .direccion("Jr. Callao 100")
                .latitud(new BigDecimal("95.5000000")) // Excede 90
                .longitud(new BigDecimal("-74.2200000"))
                .ciiuCodigo("4711")
                .giroPrincipal("Comercio")
                .areaMetrosCuadrados(new BigDecimal("50.00"))
                .build();

        when(cuentaRepository.findById(cuentaId)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> establecimientoService.registrarEstablecimiento(cuentaId, dtoLatitudInvalida, "titular"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latitud");
    }
}
