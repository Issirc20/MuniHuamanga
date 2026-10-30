package pe.gob.munihuamanga.licencias.expedientes.nucleo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.munihuamanga.licencias.common.dto.AccionPropuestaDto;
import pe.gob.munihuamanga.licencias.common.dto.AdmitirSolicitudDto;
import pe.gob.munihuamanga.licencias.common.dto.DeclararArchivoDto;
import pe.gob.munihuamanga.licencias.common.dto.DiaNoLaborableDto;
import pe.gob.munihuamanga.licencias.common.dto.ParametroPlazoDto;
import pe.gob.munihuamanga.licencias.common.dto.PlazoProcedimientoDto;
import pe.gob.munihuamanga.licencias.common.dto.ResultadoTransicionDto;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.AdmisionExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ArchivoExpedienteService;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre.PoliticaCierreProcedimiento;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.cierre.ResultadoCondicion;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.TransicionExpediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CalendarioInstitucionalService;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CodigoPlazo;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ParametrosPlazoService;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ServicioPlazoLegal;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.vigilancia.VigilanteDePlazos;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Procedimiento", description = "Admisibilidad, archivo, plazos y calendario institucional")
public class ProcedimientoExpedienteController {

    private static final String ROLES_GESTION =
            "hasAnyRole('MESA_DE_PARTES', 'EVALUADOR', 'SUPERVISOR_LICENCIAS', 'AUTORIDAD_RESOLUTIVA', 'AUDITOR')";

    private final AdmisionExpedienteService admisionService;
    private final ArchivoExpedienteService archivoService;
    private final ServicioPlazoLegal servicioPlazoLegal;
    private final VigilanteDePlazos vigilanteDePlazos;
    private final PoliticaCierreProcedimiento politicaCierre;
    private final ParametrosPlazoService parametrosPlazo;
    private final CalendarioInstitucionalService calendarioService;
    private final ExpedienteRepository expedienteRepository;
    private final Clock reloj;

    @PostMapping("/api/expedientes/{id}/admision")
    @PreAuthorize("hasRole('MESA_DE_PARTES')")
    @Operation(summary = "Admitir la solicitud presentada y habilitar la etapa ITSE")
    public ResponseEntity<ResultadoTransicionDto> admitir(@PathVariable UUID id, @Valid @RequestBody AdmitirSolicitudDto dto) {
        return ResponseEntity.ok(aDto(admisionService.admitir(id, dto)));
    }

    @PostMapping("/api/expedientes/{id}/archivo")
    @PreAuthorize("hasAnyRole('SUPERVISOR_LICENCIAS', 'AUTORIDAD_RESOLUTIVA')")
    @Operation(summary = "Declarar el archivo del procedimiento por abandono, desistimiento o solicitud no presentada")
    public ResponseEntity<ResultadoTransicionDto> archivar(@PathVariable UUID id, @Valid @RequestBody DeclararArchivoDto dto) {
        return ResponseEntity.ok(aDto(archivoService.declarar(id, dto)));
    }

    @GetMapping("/api/expedientes/{id}/plazo")
    @PreAuthorize("@expedienteSecurity.puedeVerExpediente(authentication, #id)")
    @Operation(summary = "Consultar el plazo legal de evaluación del expediente")
    public ResponseEntity<PlazoProcedimientoDto> plazo(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioPlazoLegal.calcular(id));
    }

    @GetMapping("/api/expedientes/{id}/condiciones-cierre")
    @PreAuthorize(ROLES_GESTION)
    @Operation(summary = "Consultar el cumplimiento de las condiciones de cierre del procedimiento")
    public ResponseEntity<List<ResultadoCondicion>> condicionesCierre(@PathVariable UUID id) {
        return expedienteRepository.findById(id)
                .map(expediente -> ResponseEntity.ok(politicaCierre.evaluar(expediente, LocalDate.now(reloj))))
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", id));
    }

    @GetMapping("/api/plazos/vencimientos")
    @PreAuthorize(ROLES_GESTION)
    @Operation(summary = "Bandeja de expedientes con plazos próximos a vencer o vencidos y acciones propuestas")
    public ResponseEntity<List<AccionPropuestaDto>> vencimientos() {
        return ResponseEntity.ok(vigilanteDePlazos.evaluarExpedientesEnTramite());
    }

    @GetMapping("/api/plazos/parametros")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar los plazos configurables del procedimiento")
    public ResponseEntity<List<ParametroPlazoDto>> parametros() {
        return ResponseEntity.ok(parametrosPlazo.listar());
    }

    @PutMapping("/api/plazos/parametros/{codigo}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar un plazo configurable del procedimiento")
    public ResponseEntity<ParametroPlazoDto> actualizarParametro(@PathVariable CodigoPlazo codigo, @Valid @RequestBody ParametroPlazoDto dto) {
        return ResponseEntity.ok(parametrosPlazo.actualizar(codigo, dto.getDias()));
    }

    @GetMapping("/api/calendario/dias-no-laborables")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar los días no laborables del año indicado")
    public ResponseEntity<List<DiaNoLaborableDto>> diasNoLaborables(@RequestParam(required = false) Integer anio) {
        return ResponseEntity.ok(calendarioService.listar(anio != null ? anio : LocalDate.now(reloj).getYear()));
    }

    @PostMapping("/api/calendario/dias-no-laborables")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar un día no laborable (feriado regional o día no laborable decretado)")
    public ResponseEntity<DiaNoLaborableDto> registrarDiaNoLaborable(@Valid @RequestBody DiaNoLaborableDto dto) {
        return ResponseEntity.ok(calendarioService.registrar(dto));
    }

    @DeleteMapping("/api/calendario/dias-no-laborables/{fecha}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar un día no laborable registrado por error")
    public ResponseEntity<Void> eliminarDiaNoLaborable(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        calendarioService.eliminar(fecha);
        return ResponseEntity.noContent().build();
    }

    static ResultadoTransicionDto aDto(TransicionExpediente transicion) {
        return ResultadoTransicionDto.builder()
                .expedienteId(transicion.expedienteId())
                .numeroTramite(transicion.numeroTramite())
                .estadoAnterior(transicion.estadoAnterior())
                .estadoNuevo(transicion.estadoNuevo())
                .evento(transicion.evento().name())
                .fecha(transicion.momento())
                .build();
    }
}
