package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

/** La vista completa, con la clave de correccion. Solo para el profesor dueno. */
public record VistaProfesorResponse(
        UUID contenidoId,
        String titulo,
        int version,
        String escala,
        int puntajeTotal,
        String correccion,
        String resumen,
        /**
         * La regla de sorteo, o null si el cuestionario es todo fijo. Lleva
         * `candidatos`, que es la unica forma que tiene la profesora de ver que
         * tan grande es la bolsa de la que sale el examen de cada alumno.
         */
        ReglaResponse regla,
        List<ItemParaProfesor> items) {

    public record ReglaResponse(String etiqueta, int cuantos, int puntaje, int candidatos) {}

    public record ItemParaProfesor(
            UUID itemId,
            UUID itemVersionId,
            TipoDeItem tipo,
            String enunciado,
            int version,
            int orden,
            int puntaje,
            JsonNode payload,
            JsonNode criterio) {
    }
}
