package pe.gob.munihuamanga.licencias.expedientes.cuentas.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad JPA OperadorCuenta (M01).
 * Representa la delegación de facultades nominativas sobre una Cuenta institucional
 * para prevenir credenciales compartidas y elevación de privilegios.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operadores_cuenta")
public class OperadorCuenta {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "dni_operador", nullable = false, length = 20)
    private String dniOperador;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "email", nullable = false, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_operador", nullable = false, length = 40)
    private RolOperador rolOperador;

    @ElementCollection(targetClass = PermisoOperador.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "operadores_cuenta_permisos", joinColumns = @JoinColumn(name = "operador_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permiso", nullable = false, length = 50)
    @Builder.Default
    private Set<PermisoOperador> permisos = new HashSet<>();

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    @Column(name = "asignado_por", nullable = false, length = 100)
    private String asignadoPor;
}
