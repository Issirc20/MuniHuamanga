package pe.gob.munihuamanga.licencias.expedientes.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Persona con credenciales de acceso al sistema: funcionario municipal o persona natural
 * que actúa en nombre propio o en representación de una cuenta de administrado.
 * Una persona jurídica nunca tiene credenciales propias.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuarios_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 40)
    @Builder.Default
    private Set<RolUsuario> roles = new HashSet<>();

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /** Verdadero mientras la persona no haya definido su contraseña mediante el enlace de activación. */
    @Column(name = "pendiente_activacion", nullable = false)
    @Builder.Default
    private boolean pendienteActivacion = false;

    @Column(name = "token_activacion_hash", length = 64)
    private String tokenActivacionHash;

    @Column(name = "token_activacion_expira")
    private LocalDateTime tokenActivacionExpira;

    @Column(name = "intentos_fallidos", nullable = false)
    @Builder.Default
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "fecha_ultimo_acceso")
    private LocalDateTime fechaUltimoAcceso;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public boolean tieneRol(RolUsuario rol) {
        return roles != null && roles.contains(rol);
    }

    public boolean esFuncionario() {
        return roles != null && roles.stream().anyMatch(RolUsuario::esFuncionario);
    }

    public boolean estaBloqueado(LocalDateTime momento) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(momento);
    }

    public boolean puedeIniciarSesion(LocalDateTime momento) {
        return activo && !pendienteActivacion && !estaBloqueado(momento);
    }

    /** Rol de mayor jerarquía funcional, para la presentación en interfaces. */
    public RolUsuario rolPrincipal() {
        if (roles == null || roles.isEmpty()) {
            return RolUsuario.ROLE_CIUDADANO;
        }
        return roles.stream().min(Comparator.comparingInt(Enum::ordinal)).orElse(RolUsuario.ROLE_CIUDADANO);
    }

    public Set<RolUsuario> rolesInmutables() {
        return (roles == null || roles.isEmpty()) ? EnumSet.noneOf(RolUsuario.class) : EnumSet.copyOf(roles);
    }
}
