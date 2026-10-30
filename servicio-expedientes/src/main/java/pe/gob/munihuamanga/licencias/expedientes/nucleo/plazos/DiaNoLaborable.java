package pe.gob.munihuamanga.licencias.expedientes.nucleo.plazos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Día no laborable del calendario institucional. Los sábados y domingos no se registran:
 * son inhábiles por regla general (TUO LPAG, art. 145).
 */
@Entity
@Table(name = "dias_no_laborables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiaNoLaborable {

    @Id
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "descripcion", nullable = false, length = 150)
    private String descripcion;

    @Column(name = "base_legal", length = 200)
    private String baseLegal;

    @Column(name = "registrado_por", nullable = false, length = 100)
    private String registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
