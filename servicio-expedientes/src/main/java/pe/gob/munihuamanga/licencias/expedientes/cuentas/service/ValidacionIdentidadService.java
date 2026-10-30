package pe.gob.munihuamanga.licencias.expedientes.cuentas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.gob.munihuamanga.licencias.common.enums.MedioValidacionIdentidad;
import pe.gob.munihuamanga.licencias.common.exception.RecursoNoEncontradoException;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.model.Cuenta;
import pe.gob.munihuamanga.licencias.expedientes.cuentas.repository.CuentaRepository;
import pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion.ProveedorActor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Validación de la identidad declarada del administrado por mesa de partes.
 * La identidad registrada en el portal es declarativa (presunción de veracidad, TUO LPAG art. 51)
 * hasta que un funcionario la coteja con el documento original o por consulta en línea.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidacionIdentidadService {

    private final CuentaRepository cuentaRepository;
    private final ProveedorActor proveedorActor;
    private final Clock reloj;

    @Transactional(readOnly = true)
    public boolean estaValidada(UUID cuentaId) {
        return cuentaRepository.findById(cuentaId)
                .map(cuenta -> Boolean.TRUE.equals(cuenta.getIdentidadValidadaOficialmente()))
                .orElse(false);
    }

    @Transactional
    public void validar(UUID cuentaId, MedioValidacionIdentidad medio, String referencia) {
        if (medio == null || referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el medio y la referencia de la validación de identidad.");
        }
        Cuenta cuenta = cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta", cuentaId));

        cuenta.setIdentidadValidadaOficialmente(true);
        cuenta.setMedioValidacionIdentidad(medio);
        cuenta.setReferenciaValidacionIdentidad(referencia.trim());
        cuenta.setIdentidadValidadaPor(proveedorActor.actual().nombreParaRegistro());
        cuenta.setFechaValidacionIdentidad(LocalDateTime.now(reloj));
        cuenta.setFechaModificacion(LocalDateTime.now(reloj));
        cuentaRepository.save(cuenta);
        log.info("[CUENTAS] Identidad de la cuenta {} validada mediante {}.", cuentaId, medio);
    }
}
