package pe.gob.munihuamanga.licencias.expedientes.itse.service;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;

import java.util.Optional;

/**
 * Cada uno de los cuatro anexos debe haber sido cargado.
 */
@Component
@Order(1)
public class VerificacionPresenciaAnexo implements VerificacionAnexoItse {

    @Override
    public Optional<String> verificar(TipoAnexoItse anexo, Optional<DocumentoVersionado> documento) {
        return documento.isPresent()
                ? Optional.empty()
                : Optional.of(anexo.getDescripcion() + ": no ha sido cargado.");
    }
}
