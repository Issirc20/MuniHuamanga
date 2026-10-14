package pe.gob.munihuamanga.licencias.expedientes.borradores.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.gob.munihuamanga.licencias.common.enums.EstadoBorrador;
import pe.gob.munihuamanga.licencias.common.enums.ModalidadTramite;
import pe.gob.munihuamanga.licencias.common.enums.NivelRiesgo;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.establecimientos.model.Establecimiento;
import pe.gob.munihuamanga.licencias.expedientes.model.Expediente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "solicitudes_borrador")
public class SolicitudBorrador {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "establecimiento_id", nullable = false)
    private Establecimiento establecimiento;

    @Column(name = "creador_id", nullable = false)
    private UUID creadorId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoBorrador estado;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_tramite", length = 50)
    private ModalidadTramite modalidadTramite;

    @Column(name = "nombre_comercial_declarado")
    private String nombreComercialDeclarado;

    @Column(name = "giro_negocio_declarado")
    private String giroNegocioDeclarado;

    @Column(name = "actividad_detallada", columnDefinition = "TEXT")
    private String actividadDetallada;

    @Column(name = "area_metros_cuadrados", precision = 10, scale = 2)
    private BigDecimal areaMetrosCuadrados;

    @Column(name = "aforo_personas")
    private Integer aforoPersonas;

    @Column(name = "numero_pisos")
    private Integer numeroPisos;

    @Column(name = "horario_inicio", length = 10)
    private String horarioInicio;

    @Column(name = "horario_fin", length = 10)
    private String horarioFin;

    @Column(name = "telefono_contacto", length = 20)
    private String telefonoContacto;

    @Column(name = "correo_notificacion", length = 150)
    private String correoNotificacion;

    @Column(name = "autoriza_notificacion")
    private Boolean autorizaNotificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo_declarado", length = 20)
    private NivelRiesgo nivelRiesgoDeclarado;

    // Datos del comprobante SAT registrado (sujeto a validación manual)
    @Column(name = "numero_operacion_sat", length = 50)
    private String numeroOperacionSat;

    @Column(name = "fecha_pago_sat")
    private LocalDate fechaPagoSat;

    @Column(name = "monto_tasa", precision = 10, scale = 2)
    private BigDecimal montoTasa;

    @Column(name = "voucher_id", length = 50)
    private String voucherId;

    // Relación con el expediente una vez formalizado (M06)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id")
    private Expediente expediente;

    @Column(name = "motivo_descarte", columnDefinition = "TEXT")
    private String motivoDescarte;

    @Column(name = "fecha_descarte")
    private LocalDateTime fechaDescarte;

    @Column(name = "usuario_descarte")
    private UUID usuarioDescarte;

    @Builder.Default
    @OneToMany(mappedBy = "borrador", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoBorrador> documentos = new ArrayList<>();
}
