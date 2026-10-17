package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO que representa una plantilla documental oficial vigente y su información de auditoría.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaDocumentalDto {

    private UUID id;
    private String codigo;               // ITSE_ANEXO_1, ITSE_ANEXO_2, ITSE_ANEXO_3, ITSE_ANEXO_4, LICENCIA_ANEXO_1
    private String nombre;               // Nombre oficial del formato
    private String descripcion;          // Descripción y ámbito de aplicación
    private String baseLegal;            // Base normativa aplicable
    private String grupo;                // ITSE o LICENCIA
    private Integer versionActual;       // 1, 2, 3...
    private String versionTexto;         // ej: "v1.0 (2026)"
    private String nombreArchivo;        // ej: "itse-anexo1-plantilla.pdf"
    private String hashSha256;           // Hash de integridad SHA-256
    private Long tamanioBytes;           // Tamaño en bytes
    private LocalDateTime fechaActualizacion;
    private String actualizadoPor;       // Funcionario responsable
    private Boolean activo;
    private Integer totalVersionesHistoricas;
}
