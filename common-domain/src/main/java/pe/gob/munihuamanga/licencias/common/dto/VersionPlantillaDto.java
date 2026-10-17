package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO que representa una versión histórica de una plantilla documental oficial.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionPlantillaDto {

    private UUID id;
    private UUID plantillaId;
    private Integer version;
    private String versionTexto;
    private String nombreArchivo;
    private String hashSha256;
    private Long tamanioBytes;
    private String motivoCambio;
    private LocalDateTime fechaCreacion;
    private String creadoPor;
    private Boolean esVersionVigente;
}
