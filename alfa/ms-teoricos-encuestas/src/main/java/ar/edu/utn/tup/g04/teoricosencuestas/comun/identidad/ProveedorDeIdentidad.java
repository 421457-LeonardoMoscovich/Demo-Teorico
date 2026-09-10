package ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad;

import java.util.Optional;
import java.util.UUID;

/**
 * El puerto de identidad.
 *
 * La identidad es del Tema 01. Mientras no exista, el adaptador es falso
 * (dos usuarios en memoria). Cuando el Tema 01 este arriba se escribe un
 * segundo adaptador y NADA mas cambia: el resto del codigo depende de esta
 * interfaz, nunca de como se autentica.
 */
public interface ProveedorDeIdentidad {

    Optional<Usuario> autenticar(String usuario, String clave);

    Optional<Usuario> porId(UUID id);
}
