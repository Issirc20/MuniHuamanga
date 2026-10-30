package pe.gob.munihuamanga.licencias.expedientes.nucleo.aplicacion;

import pe.gob.munihuamanga.licencias.expedientes.nucleo.dominio.Actor;

/**
 * Puerto que entrega el actor de la operación en curso.
 * Cuando no existe una sesión autenticada (procesos programados) devuelve el actor del sistema.
 */
public interface ProveedorActor {

    Actor actual();
}
