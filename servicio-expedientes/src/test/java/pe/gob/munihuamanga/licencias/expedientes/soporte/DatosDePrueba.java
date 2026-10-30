package pe.gob.munihuamanga.licencias.expedientes.soporte;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pe.gob.munihuamanga.licencias.common.dto.LoginRequestDto;
import pe.gob.munihuamanga.licencias.common.dto.LoginResponseDto;
import pe.gob.munihuamanga.licencias.common.enums.PermisoOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolOperador;
import pe.gob.munihuamanga.licencias.common.enums.RolUsuario;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.OperadorCuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.OperadorCuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.model.Usuario;
import pe.gob.munihuamanga.licencias.expedientes.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Credenciales de los accesos semilla de desarrollo y utilidades comunes para las pruebas de integración.
 */
public final class DatosDePrueba {

    public static final String ADMIN = "admin";
    public static final String CLAVE_ADMIN = "Sistemas.Huamanga26";
    public static final String MESA_DE_PARTES = "mesapartes";
    public static final String CLAVE_MESA_DE_PARTES = "Ventanilla.Huamanga26";
    public static final String EVALUADOR = "evaluador";
    public static final String CLAVE_EVALUADOR = "Licencias.Huamanga26";
    public static final String SUPERVISOR = "supervisor";
    public static final String CLAVE_SUPERVISOR = "Revision.Huamanga26";
    public static final String AUTORIDAD = "autoridad";
    public static final String CLAVE_AUTORIDAD = "Gerencia.Huamanga26";
    public static final String CAJERO = "cajero";
    public static final String CLAVE_CAJERO = "Caja.Huamanga2026";
    public static final String INSPECTOR = "inspector";
    public static final String CLAVE_INSPECTOR = "Campo.Huamanga26";
    public static final String AUDITOR = "auditor";
    public static final String CLAVE_AUDITOR = "Control.Huamanga26";
    public static final String CIUDADANA_TITULAR = "42567891";
    public static final String CIUDADANO_TERCERO = "78999999";
    public static final String CLAVE_CIUDADANO = "Ciudadano.Huamanga26";

    private DatosDePrueba() {
    }

    public static String obtenerToken(MockMvc mockMvc, ObjectMapper objectMapper, String usuario, String clave) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LoginRequestDto.builder().username(usuario).password(clave).build())))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(resultado.getResponse().getContentAsString(), LoginResponseDto.class).getToken();
    }

    /**
     * Garantiza que la persona con el DNI indicado exista y represente a la cuenta como titular con todas las facultades.
     */
    public static Usuario vincularTitular(UsuarioRepository usuarioRepository, OperadorCuentaRepository operadorRepository,
                                          Cuenta cuenta, String dni, String nombre) {
        Usuario persona = usuarioRepository.findByUsername(dni).orElseGet(() -> usuarioRepository.save(Usuario.builder()
                .id(UUID.randomUUID())
                .username(dni)
                .password("$2a$10$sinUsoEnPruebasDeIntegracionXXXXXXXXXXXXXXXXXXXXXXXXXX")
                .nombreCompleto(nombre)
                .email(dni + "@correo.pe")
                .roles(new HashSet<>(Set.of(RolUsuario.ROLE_CIUDADANO)))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build()));
        if (operadorRepository.findByCuentaIdAndUsuarioId(cuenta.getId(), persona.getId()).isEmpty()) {
            operadorRepository.save(OperadorCuenta.builder()
                    .id(UUID.randomUUID())
                    .cuenta(cuenta)
                    .usuario(persona)
                    .dniOperador(dni)
                    .nombreCompleto(nombre)
                    .email(dni + "@correo.pe")
                    .rolOperador(RolOperador.TITULAR)
                    .permisos(EnumSet.allOf(PermisoOperador.class))
                    .activo(true)
                    .fechaAsignacion(LocalDateTime.now())
                    .asignadoPor("PRUEBA")
                    .build());
        }
        return persona;
    }
}
