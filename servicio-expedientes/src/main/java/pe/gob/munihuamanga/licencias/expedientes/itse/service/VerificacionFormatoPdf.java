package pe.gob.munihuamanga.licencias.expedientes.itse.service;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;

import java.util.Optional;

/**
 * Los anexos firmados deben presentarse en formato PDF.
 */
@Component
@Order(2)
public class VerificacionFormatoPdf implements VerificacionAnexoItse {

    private static final String TIPO_PDF = "application/pdf";

    @Override
    public Optional<String> verificar(TipoAnexoItse anexo, Optional<DocumentoVersionado> documento) {
        return documento
                .filter(doc -> !TIPO_PDF.equalsIgnoreCase(doc.getTipoContenido()))
                .map(doc -> anexo.getDescripcion() + ": debe cargarse en formato PDF.");
    }
}
