package pe.gob.munihuamanga.licencias.expedientes.establecimientos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA Establecimiento (M02).
 * Representa un local físico comercial vinculado a una Cuenta titular.
 * Nota: La ubicación declarada no constituye zonificación municipal aprobada.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "establecimientos")
public class Establecimiento {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @Column(name = "codigo_establecimiento", nullable = false, length = 30)
    private String codigoEstablecimiento;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Builder.Default
    @Column(name = "distrito", nullable = false, length = 100)
    private String distrito = "Ayacucho";

    @Builder.Default
    @Column(name = "provincia", nullable = false, length = 100)
    private String provincia = "Huamanga";

    @Builder.Default
    @Column(name = "departamento", nullable = false, length = 100)
    private String departamento = "Ayacucho";

    @Column(name = "referencia", length = 255)
    private String referencia;

    @Column(name = "latitud", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(name = "longitud", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ciiu_codigo")
    private CatalogoCiiu ciiu;

    @Column(name = "giro_principal", nullable = false, length = 255)
    private String giroPrincipal;

    @Column(name = "area_metros_cuadrados", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaMetrosCuadrados;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;
}
