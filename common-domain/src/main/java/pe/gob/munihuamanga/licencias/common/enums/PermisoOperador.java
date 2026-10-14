package pe.gob.munihuamanga.licencias.common.enums;

/**
 * Permisos explícitos asignables a un operador de cuenta para evitar credenciales compartidas
 * y elevación de privilegios (Incremento I03).
 */
public enum PermisoOperador {
    CREAR_EXPEDIENTE,
    CREAR_BORRADOR,
    PRESENTAR_EXPEDIENTE,
    CONSULTAR_EXPEDIENTE,
    SUBSANAR_EXPEDIENTE,
    GESTIONAR_ESTABLECIMIENTOS,
    ADMINISTRAR_OPERADORES
}
