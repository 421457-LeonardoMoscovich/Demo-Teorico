package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload;

import java.util.List;

/**
 * Lo que contesto el alumno. Se guarda tal cual en `respuesta.contenido`, junto
 * con la estampa de la version que vio (CI-12).
 *
 * El item dejado en blanco llega como una respuesta vacia y se persiste igual:
 * asi el corrector puntua 0 explicitamente y no tiene que distinguir
 * "no contesto" de "no llego el dato".
 */
public sealed interface RespuestaDeItem {

    record OpcionMultiple(List<String> seleccionadas) implements RespuestaDeItem {}

    record VerdaderoFalso(Boolean valor) implements RespuestaDeItem {}

    record Emparejar(List<List<String>> pares) implements RespuestaDeItem {}

    record Ordenar(List<String> secuencia) implements RespuestaDeItem {}

    record Abierta(String texto) implements RespuestaDeItem {}
}
