package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
    private String rol;
    private long expiracionMs;
}
