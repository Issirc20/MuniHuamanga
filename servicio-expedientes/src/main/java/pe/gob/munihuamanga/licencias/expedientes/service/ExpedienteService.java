package pe.gob.munihuamanga.licencias.expedientes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.LiquidacionDto;
import pe.gob.munihuamanga.licencias.common.dto.VoucherDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.itse.service.ItseService;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CalculadoraPlazos;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.CodigoPlazo;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos.ParametrosPlazoService;
import pe.gob.munihuamanga.licencias.expedientes.repository.ExpedienteRepository;
import pe.gob.munihuamanga.licencias.expedientes.security.ServicioRepresentacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Consultas sobre expedientes. Las transiciones de estado se registran exclusivamente
 * mediante el puerto {@code ComandosExpediente}.
 */
@Service
@RequiredArgsConstructor
public class ExpedienteService {

    private static final int DIAS_ALERTA_VENCIMIENTO = 3;

    private final ExpedienteRepository expedienteRepository;
    private final ServicioRepresentacion servicioRepresentacion;
    private final ItseService itseService;
    private final CalculadoraPlazos calculadoraPlazos;
    private final ParametrosPlazoService parametrosPlazo;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public Expediente obtenerPorId(UUID id) {
        return expedienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", id));
    }

    @Transactional(readOnly = true)
    public Expediente obtenerPorNumeroTramite(String numeroTramite) {
        return expedienteRepository.findByNumeroTramite(numeroTramite)
                .orElseThrow(() -> new RecursoNoEncontradoException("Expediente", numeroTramite));
    }

    /**
     * Seguimiento público: el número de trámite debe acompañarse de los cuatro últimos dígitos
     * del documento del titular, para impedir la consulta de expedientes ajenos por enumeración.
     */
    @Transactional(readOnly = true)
    public Expediente obtenerParaSeguimientoPublico(String numeroTramite, String digitosDocumento) {
        Expediente expediente = expedienteRepository.findByNumeroTramite(numeroTramite.trim()).orElse(null);
        boolean coincide = expediente != null
                && digitosDocumento != null
                && digitosDocumento.trim().length() == 4
                && expediente.getDocumentoIdentidad() != null
                && expediente.getDocumentoIdentidad().endsWith(digitosDocumento.trim());
        if (!coincide) {
            throw new RecursoNoEncontradoException("No se encontró un expediente con los datos proporcionados.");
        }
        return expediente;
    }

    @Transactional(readOnly = true)
    public List<Expediente> listarConFiltros(EstadoExpediente estado, Boolean conAlerta) {
        List<Expediente> lista = (estado != null)
                ? expedienteRepository.findByEstado(estado)
                : expedienteRepository.findAll();

        if (Boolean.TRUE.equals(conAlerta)) {
            LocalDate hoy = LocalDate.now(reloj);
            lista = lista.stream()
                    .filter(expediente -> expediente.getFechaLimite() != null)
                    .filter(expediente -> calculadoraPlazos.contarDiasHabiles(hoy, expediente.getFechaLimite().toLocalDate())
                            <= DIAS_ALERTA_VENCIMIENTO)
                    .toList();
        }
        return lista;
    }

    /** Expedientes de las cuentas que la persona representa con facultad de consulta. */
    @Transactional(readOnly = true)
    public List<Expediente> listarDeCuentasRepresentadas(UUID usuarioId) {
        Set<UUID> cuentas = servicioRepresentacion.cuentasConFacultad(usuarioId, PermisoOperador.CONSULTAR_EXPEDIENTE);
        return cuentas.isEmpty() ? List.of() : expedienteRepository.findByCuentaIdInOrderByFechaCreacionDesc(cuentas);
    }

    /**
     * Orden de pago de la tasa liquidada. Es una consulta: no modifica el expediente.
     */
    @Transactional(readOnly = true)
    public VoucherDto generarOrdenPago(UUID id) {
        Expediente expediente = obtenerPorId(id);
        LiquidacionDto liquidacion = itseService.liquidacionDefinitiva(id)
                .orElseThrow(() -> new IllegalStateException(
                        "La orden de pago se emite una vez validado el ITSE y liquidada la tasa del expediente "
                                + expediente.getNumeroTramite() + "."));

        LocalDate vencimiento = calculadoraPlazos.sumarDiasHabiles(
                liquidacion.getFechaLiquidacion().toLocalDate(), parametrosPlazo.dias(CodigoPlazo.ABANDONO_PAGO));
        String codigo = "OP-" + expediente.getNumeroTramite();

        return VoucherDto.builder()
                .voucherId(codigo)
                .expedienteId(expediente.getId())
                .numeroTramite(expediente.getNumeroTramite())
                .titular(expediente.getNombreTitular())
                .documentoIdentidad(expediente.getDocumentoIdentidad())
                .monto(liquidacion.getMonto())
                .concepto(liquidacion.getConcepto())
                .fechaEmision(liquidacion.getFechaLiquidacion())
                .fechaVencimiento(vencimiento.atTime(LocalTime.of(23, 59)))
                .codigoBarrasSat("0107" + codigo.replace("-", ""))
                .build();
    }
}
