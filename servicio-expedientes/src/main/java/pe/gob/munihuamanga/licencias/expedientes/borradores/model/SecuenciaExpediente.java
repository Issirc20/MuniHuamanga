package pe.gob.munihuamanga.licencias.expedientes.borradores.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "secuencias_expediente")
public class SecuenciaExpediente {

    @Id
    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "ultimo_correlativo", nullable = false)
    private Integer ultimoCorrelativo;
}
