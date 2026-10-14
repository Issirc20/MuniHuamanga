package pe.gob.munihuamanga.licencias.expedientes.resoluciones.model;

import jakarta.persistence.Column;
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
import pe.gob.munihuamanga.licencias.common.enums.EstadoOperacionFirma;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de trazabilidad e idempotencia para operaciones remotas de firma digital institucional (M09 - I08.1).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operaciones_firma")
public class OperacionFirma {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolucion_id", nullable = false)
    private ResolucionAdministrativa resolucion;

    @Column(name = "id_transaccion_remota", nullable = false, length = 100)
    private String idTransaccionRemota;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoOperacionFirma estado;

    @Column(name = "hash_pre_firma", nullable = false, length = 64)
    private String hashPreFirma;

    @Column(name = "hash_post_firma", length = 64)
    private String hashPostFirma;

    @Column(name = "intentos", nullable = false)
    private Integer intentos;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "codigo_error", length = 50)
    private String codigoError;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    @Column(name = "creado_por", nullable = false, length = 100)
    private String creadoPor;
}
