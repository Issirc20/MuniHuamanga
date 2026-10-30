package pe.gob.munihuamanga.licencias.expedientes.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Representación de la persona autenticada para Spring Security.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String username;
    private final String password;
    private final String nombreCompleto;
    private final String email;
    private final Set<RolUsuario> roles;
    private final boolean activo;
    private final boolean pendienteActivacion;
    private final LocalDateTime bloqueadoHasta;
    private final UUID sesionId;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {
        this(usuario, null);
    }

    public CustomUserDetails(Usuario usuario, UUID sesionId) {
        this.id = usuario.getId();
        this.username = usuario.getUsername();
        this.password = usuario.getPassword();
        this.nombreCompleto = usuario.getNombreCompleto();
        this.email = usuario.getEmail();
        this.roles = usuario.rolesInmutables();
        this.activo = usuario.isActivo();
        this.pendienteActivacion = usuario.isPendienteActivacion();
        this.bloqueadoHasta = usuario.getBloqueadoHasta();
        this.sesionId = sesionId;
        this.authorities = roles.stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(rol.name()))
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return bloqueadoHasta == null || bloqueadoHasta.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo && !pendienteActivacion;
    }
}
