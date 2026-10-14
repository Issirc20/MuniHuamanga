package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * DTO para la representación de un operador nominado en una cuenta institucional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperadorCuentaDto {

    private UUID id;
    private UUID cuentaId;
    private UUID usuarioId;
    private String dniOperador;
    private String nombreCompleto;
    private String email;
    private RolOperador rolOperador;

    @Builder.Default
    private Set<PermisoOperador> permisos = new HashSet<>();

    private Boolean activo;
    private LocalDateTime fechaAsignacion;
    private String asignadoPor;
}
