package pe.gob.munihuamanga.licencias.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDto {

    private String voucherId;
    private UUID expedienteId;
    private String numeroTramite;
    private String titular;
    private String documentoIdentidad;
    private BigDecimal monto;
    private String concepto;
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaVencimiento;
    private String codigoBarrasSat;
}
