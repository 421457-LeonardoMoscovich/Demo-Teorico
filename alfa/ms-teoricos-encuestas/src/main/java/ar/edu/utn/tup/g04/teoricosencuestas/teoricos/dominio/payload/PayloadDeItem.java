package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload;

import java.util.List;

/**
 * Lo que ve el alumno de un item. Jerarquia sellada para que el compilador
 * exija tratar todos los casos en cada switch.
 *
 * Se guarda en jsonb porque los siete tipos tienen formas incompatibles entre
 * si; siete tablas con columnas nulas seria peor. Que forma tiene cada payload
 * lo dice el `tipo` del item, que es inmutable, asi que el JSON no necesita
 * llevar un discriminador adentro.
 */
public sealed interface PayloadDeItem {

    /** Un elemento con identidad propia. El id es lo que viaja en las respuestas. */
    record Opcion(String id, String texto) {}

    record OpcionMultiple(List<Opcion> opciones, boolean multiple) implements PayloadDeItem {}

    record VerdaderoFalso(String afirmacion) implements PayloadDeItem {}

    record Emparejar(List<Opcion> izquierda, List<Opcion> derecha) implements PayloadDeItem {}

    record Ordenar(List<Opcion> elementos) implements PayloadDeItem {}

    /**
     * Lo unico que ve el alumno es la consigna: las respuestas aceptadas son
     * clave de correccion y viven en el criterio (CI-17).
     */
    record RespuestaCorta(String consigna) implements PayloadDeItem {}

    /**
     * `unidad` es rotulo, no parte de la respuesta: se pinta al lado del input
     * ("kg") y el alumno escribe solo el numero. Moodle permite pedir la unidad
     * escrita y corregirla; no lo copiamos porque duplica la regla de
     * correccion —hay que decidir si "Kg" vale como "kg"— para ganar poco.
     */
    record Numerica(String consigna, String unidad) implements PayloadDeItem {}

    /** Modelada para que el jsonb ya la admita. Fuera de la alfa (D-01). */
    record Abierta(String consigna, Integer extensionMaxima) implements PayloadDeItem {}

    /** Diferidos (D-09). Existen como tipo; el validador los rechaza. */
    record Conversacion(String consigna, List<String> transcripcion) implements PayloadDeItem {}

    record Debate(String consigna, List<String> transcripcion) implements PayloadDeItem {}
}
