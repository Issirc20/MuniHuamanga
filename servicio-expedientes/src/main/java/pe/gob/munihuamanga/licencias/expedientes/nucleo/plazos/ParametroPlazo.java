package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

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

import java.time.LocalDateTime;

/**
 * Valor vigente de un plazo del procedimiento.
 */
@Entity
@Table(name = "parametros_plazo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParametroPlazo {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "codigo", nullable = false, length = 40)
    private CodigoPlazo codigo;

    @Column(name = "dias", nullable = false)
    private int dias;

    @Column(name = "actualizado_por", nullable = false, length = 100)
    private String actualizadoPor;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
