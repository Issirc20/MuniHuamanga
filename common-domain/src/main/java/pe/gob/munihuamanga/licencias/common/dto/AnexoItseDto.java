package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Situación de carga de un anexo ITSE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnexoItseDto {

    private TipoAnexoItse anexo;
    private String descripcion;
    private boolean cargado;
    private UUID documentoId;
    private Integer version;
    private String nombreArchivo;
    private LocalDateTime fechaCarga;
}
