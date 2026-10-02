package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDto {

    private UUID id;
    private String username;
    private String nombreCompleto;
    private String email;
    private RolUsuario rol;
    private boolean activo;
    private LocalDateTime fechaCreacion;
}
