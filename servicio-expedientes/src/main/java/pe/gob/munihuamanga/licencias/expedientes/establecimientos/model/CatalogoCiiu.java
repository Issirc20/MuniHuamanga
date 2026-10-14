package pe.gob.munihuamanga.licencias.expedientes.establecimientos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

/**
 * Entidad JPA CatalogoCiiu (M02 - Catálogo Oficial de Actividades Económicas y Giros de Huamanga).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "catalogo_ciiu")
public class CatalogoCiiu {

    @Id
    @Column(name = "codigo", length = 20)
    private String codigo;

    @Column(name = "descripcion", nullable = false, length = 255)
    private String descripcion;

    @Column(name = "giro_principal", nullable = false, length = 255)
    private String giroPrincipal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "nivel_riesgo_sugerido", nullable = false, length = 20)
    private NivelRiesgo nivelRiesgoSugerido = NivelRiesgo.BAJO;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
