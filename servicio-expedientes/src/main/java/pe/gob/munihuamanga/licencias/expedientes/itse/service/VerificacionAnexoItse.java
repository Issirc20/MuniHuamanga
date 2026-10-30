package pe.gob.munihuamanga.licencias.expedientes.itse.service;

import pe.gob.munihuamanga.licencias.common.enums.TipoAnexoItse;
import pe.gob.munihuamanga.licencias.expedientes.documentos.model.DocumentoVersionado;

import java.util.Optional;

/**
 * Verificación automática previa de un anexo ITSE cargado. Solo detecta defectos técnicos del archivo;
 * la firma, el sello y la coherencia de los datos los confirma el funcionario.
 *
 * @return descripción del hallazgo, o vacío si el anexo supera la verificación.
 */
public interface VerificacionAnexoItse {

    Optional<String> verificar(TipoAnexoItse anexo, Optional<DocumentoVersionado> documento);
}
