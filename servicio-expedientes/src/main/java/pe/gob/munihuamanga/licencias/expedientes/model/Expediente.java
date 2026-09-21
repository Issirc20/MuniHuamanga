package pe.gob.munihuamanga.licencias.expedientes.model;

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
import pe.gob.munihuamanga.licencias.common.enums.EstadoExpediente;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad principal de Expediente según el Diagrama de Clases de la arquitectura.
 */
@Entity
@Table(name = "expedientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expediente {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "numero_tramite", nullable = false, unique = true, length = 30)
    private String numeroTramite;

    @Column(name = "solicitante_id", nullable = false)
    private UUID solicitanteId;

    @Column(name = "nombre_titular", nullable = false, length = 150)
    private String nombreTitular;

    @Column(name = "documento_identidad", nullable = false, length = 20)
    private String documentoIdentidad;

    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "giro_negocio", nullable = false, length = 150)
    private String giroNegocio;

    @Column(name = "direccion_establecimiento", nullable = false, length = 255)
    private String direccionEstablecimiento;

    @Column(name = "area_metros_cuadrados", precision = 10, scale = 2)
    private BigDecimal areaMetrosCuadrados;

    @Column(name = "correo_electronico", length = 120)
    private String correoElectronico;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoExpediente estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo", length = 20)
    private NivelRiesgo nivelRiesgo;

    @Column(name = "monto_tasa", precision = 10, scale = 2)
    private BigDecimal montoTasa;

    @Column(name = "voucher_id", length = 50)
    private String voucherId;

    @Column(name = "licencia_qr_code", length = 255)
    private String licenciaQrCode;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    /**
     * Determina si según la Ley N° 28976 y D.S. 002-2018-PCM corresponde ITSE previa o posterior.
     */
    public String getTipoItse() {
        if (nivelRiesgo == null) {
            return "POR_CLASIFICAR";
        }
        return (nivelRiesgo == NivelRiesgo.BAJO || nivelRiesgo == NivelRiesgo.MEDIO)
                ? "ITSE_POSTERIOR"
                : "ITSE_PREVIA";
    }

    /**
     * Actualiza el estado del expediente.
     * @param nuevo nuevo estado asignado.
     */
    public void cambiarEstado(EstadoExpediente nuevo) {
        this.estado = nuevo;
    }
}
