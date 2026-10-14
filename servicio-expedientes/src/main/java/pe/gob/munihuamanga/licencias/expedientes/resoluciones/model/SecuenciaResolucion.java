package pe.gob.munihuamanga.licencias.expedientes.resoluciones.model;

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
@Table(name = "secuencias_resolucion")
public class SecuenciaResolucion {

    @Id
    @Column(name = "anio")
    private Integer anio;

    @Column(name = "ultimo_numero", nullable = false)
    private Integer ultimoNumero;
}
