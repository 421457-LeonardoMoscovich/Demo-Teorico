package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload;

import java.util.List;

/**
 * La clave de correccion. NUNCA viaja en una respuesta consumida por un alumno:
 * por eso la vista del alumno es un DTO aparte y no esta clase filtrada (CI-17).
 */
public sealed interface CriterioDeCorreccion {

    record OpcionMultiple(List<String> correctas) implements CriterioDeCorreccion {}

    record VerdaderoFalso(Boolean esVerdadero) implements CriterioDeCorreccion {}

    /** Cada par es [idIzquierda, idDerecha]. */
    record Emparejar(List<List<String>> pares) implements CriterioDeCorreccion {}

    record Ordenar(List<String> secuencia) implements CriterioDeCorreccion {}

    /** Rubrica en texto para el corrector humano o el LLM. Fuera de la alfa. */
    record Abierta(String rubrica) implements CriterioDeCorreccion {}
}
