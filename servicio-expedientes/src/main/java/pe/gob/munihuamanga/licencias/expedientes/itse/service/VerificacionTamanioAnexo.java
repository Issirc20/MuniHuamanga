package pe.gob.munihuamanga.licencias.expedientes.itse.service;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;

import java.util.Optional;

/**
 * Descarta archivos vacíos o de tamaño insuficiente para contener un documento escaneado legible.
 */
@Component
@Order(3)
public class VerificacionTamanioAnexo implements VerificacionAnexoItse {

    static final long TAMANIO_MINIMO_BYTES = 1024;

    @Override
    public Optional<String> verificar(TipoAnexoItse anexo, Optional<DocumentoVersionado> documento) {
        return documento
                .filter(doc -> doc.getTamanioBytes() == null || doc.getTamanioBytes() < TAMANIO_MINIMO_BYTES)
                .map(doc -> anexo.getDescripcion() + ": el archivo está vacío o incompleto.");
    }
}
