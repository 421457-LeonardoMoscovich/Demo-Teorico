package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload;

import java.util.List;

/**
 * La clave de correccion. NUNCA viaja en una respuesta consumida por un alumno:
 * por eso la vista del alumno es un DTO aparte y no esta clase filtrada (CI-17).
 */
public sealed interface CriterioDeCorreccion {

    /**
     * Cuanto vale marcar una opcion, en porcentaje del peso del item.
     *
     * Negativo a proposito: es lo que castiga marcar de mas en un item de
     * respuesta multiple. Sin eso, marcar las cinco opciones garantiza el 100%
     * y el item deja de evaluar nada.
     */
    record Ponderada(String id, int porcentaje) {}

    /**
     * `pesos` es OPCIONAL y ese es todo el mecanismo de compatibilidad.
     *
     * En null —que es como quedan los criterios ya guardados en la base— el
     * corrector se comporta como siempre: todo o nada contra `correctas`. No
     * hay migracion de datos, y las evaluaciones ya corregidas no se tocan, que
     * es lo que exigen CI-12 y CI-44.
     *
     * Cuando viene, `correctas` no desaparece: sigue diciendo cual es la
     * seleccion que saca el 100%, y el validador exige que los dos coincidan.
     * Dos representaciones de lo mismo que no se pueden contradecir.
     */
    record OpcionMultiple(List<String> correctas, List<Ponderada> pesos)
            implements CriterioDeCorreccion {

        /** El criterio todo-o-nada de siempre. */
        public OpcionMultiple(List<String> correctas) {
            this(correctas, null);
        }

        public boolean tienePesos() {
            return pesos != null && !pesos.isEmpty();
        }
    }

    record VerdaderoFalso(Boolean esVerdadero) implements CriterioDeCorreccion {}

    /** Cada par es [idIzquierda, idDerecha]. */
    record Emparejar(List<List<String>> pares) implements CriterioDeCorreccion {}

    record Ordenar(List<String> secuencia) implements CriterioDeCorreccion {}

    /** Rubrica en texto para el corrector humano o el LLM. Fuera de la alfa. */
    record Abierta(String rubrica) implements CriterioDeCorreccion {}
}
