package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload;

import java.util.List;

/**
 * La retroalimentacion de un item: lo que el alumno lee DESPUES de que su
 * respuesta ya esta corregida (CI-58).
 *
 * <p>No es jerarquia sellada como {@link PayloadDeItem} y {@link
 * CriterioDeCorreccion}, y eso es a proposito: aquellos tienen una forma
 * distinta por tipo de item y el compilador tiene que exigir tratarlos todos.
 * La devolucion tiene una sola forma para los cinco — un texto general, y una
 * lista por opcion que solo los tipos con opciones usan.
 *
 * <p><b>Por que no viaja en la vista del alumno:</b> por la misma razon que el
 * criterio (CI-17). La devolucion de una opcion dice por que esa opcion esta
 * bien o mal; servirla antes de contestar es servir la respuesta. Como la vista
 * del alumno es una proyeccion aparte y no un filtro sobre esta, la fuga es
 * imposible por construccion y no por disciplina.
 */
public record Devolucion(String general, List<PorOpcion> porOpcion) {

    /** El texto de UNA opcion. `id` es el de la opcion en el payload. */
    public record PorOpcion(String id, String texto) {}

    public boolean vacia() {
        return (general == null || general.isBlank())
                && (porOpcion == null || porOpcion.isEmpty());
    }

    /**
     * El texto de las opciones que el alumno efectivamente marco.
     *
     * <p><b>Solo las marcadas, nunca todas.</b> Si devolvieramos la lista
     * completa, el alumno leeria "correcto, esta es la definicion de
     * atomicidad" al lado de una opcion que no eligio, y eso es la clave de
     * correccion contada con otras palabras. Es la misma regla por la que
     * {@code ResultadoResponse} no devuelve el criterio: con reintentos
     * ilimitados (RF-REC-04), regalar la respuesta convierte el reintento en
     * copiar.
     */
    public List<PorOpcion> paraLoMarcado(List<String> marcadas) {
        if (porOpcion == null || marcadas == null) {
            return List.of();
        }
        return porOpcion.stream()
                .filter(p -> marcadas.contains(p.id()))
                .filter(p -> p.texto() != null && !p.texto().isBlank())
                .toList();
    }
}
