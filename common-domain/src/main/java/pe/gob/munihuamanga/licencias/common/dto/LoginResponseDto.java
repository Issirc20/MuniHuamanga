package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Respuesta de inicio de sesión. El token de renovación no forma parte del cuerpo:
 * se entrega en una cookie HttpOnly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String token;

    @Builder.Default
    private String tipo = "Bearer";

    private UUID id;
    private String username;
    private String nombreCompleto;
    private String email;

    /** Rol principal, para la presentación en interfaces. */
    private String rol;

    private List<String> roles;
    private List<RepresentacionDto> representaciones;
    private long expiracionMs;
}
