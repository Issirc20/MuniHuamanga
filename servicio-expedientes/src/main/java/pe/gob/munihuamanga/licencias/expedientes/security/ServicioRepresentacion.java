package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.dto.RepresentacionDto;
import pe.gob.munihuamanga.licencias.common.enums.EstadoCuenta;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Única fuente de verdad sobre las facultades que una persona tiene respecto de una cuenta de administrado.
 * La representación se establece exclusivamente mediante el vínculo persistente persona-cuenta;
 * nunca por coincidencia de nombre de usuario, documento o identificador.
 */
@Service
@RequiredArgsConstructor
public class ServicioRepresentacion {

    private static final Set<EstadoCuenta> ESTADOS_INHABILITANTES = EnumSet.of(EstadoCuenta.SUSPENDIDA, EstadoCuenta.BLOQUEADA);

    private final OperadorCuentaRepository operadorCuentaRepository;

    /**
     * Indica si la persona representa a la cuenta con al menos una de las facultades indicadas.
     * Sin facultades indicadas, basta con que exista una representación vigente.
     */
    @Transactional(readOnly = true)
    public boolean tieneFacultad(UUID usuarioId, UUID cuentaId, PermisoOperador... algunaDe) {
        if (usuarioId == null || cuentaId == null) {
            return false;
        }
        return operadorCuentaRepository.findByCuentaIdAndUsuarioIdAndActivoTrue(cuentaId, usuarioId)
                .filter(this::esRepresentacionVigente)
                .map(operador -> algunaDe.length == 0 || contieneAlguna(operador.getPermisos(), algunaDe))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<RepresentacionDto> listar(UUID usuarioId) {
        if (usuarioId == null) {
            return List.of();
        }
        return operadorCuentaRepository.findByUsuarioIdAndActivoTrue(usuarioId).stream()
                .filter(operador -> operador.getCuenta() != null && Boolean.TRUE.equals(operador.getCuenta().getActivo()))
                .map(this::aDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Set<UUID> cuentasConFacultad(UUID usuarioId, PermisoOperador permiso) {
        if (usuarioId == null) {
            return Set.of();
        }
        return operadorCuentaRepository.findByUsuarioIdAndActivoTrue(usuarioId).stream()
                .filter(this::esRepresentacionVigente)
                .filter(operador -> operador.getPermisos().contains(permiso))
                .map(operador -> operador.getCuenta().getId())
                .collect(Collectors.toSet());
    }

    private boolean esRepresentacionVigente(OperadorCuenta operador) {
        Cuenta cuenta = operador.getCuenta();
        return Boolean.TRUE.equals(operador.getActivo())
                && cuenta != null
                && Boolean.TRUE.equals(cuenta.getActivo())
                && !ESTADOS_INHABILITANTES.contains(cuenta.getEstado());
    }

    private static boolean contieneAlguna(Set<PermisoOperador> permisos, PermisoOperador... candidatos) {
        for (PermisoOperador permiso : candidatos) {
            if (permisos.contains(permiso)) {
                return true;
            }
        }
        return false;
    }

    private RepresentacionDto aDto(OperadorCuenta operador) {
        Cuenta cuenta = operador.getCuenta();
        return RepresentacionDto.builder()
                .representacionId(operador.getId())
                .cuentaId(cuenta.getId())
                .tipoPersona(cuenta.getTipoPersona())
                .numeroDocumentoCuenta(cuenta.getNumeroDocumento())
                .razonSocialONombre(cuenta.getRazonSocialONombre())
                .rol(operador.getRolOperador())
                .permisos(operador.getPermisos().isEmpty()
                        ? EnumSet.noneOf(PermisoOperador.class)
                        : EnumSet.copyOf(operador.getPermisos()))
                .estadoCuenta(cuenta.getEstado())
                .build();
    }
}
