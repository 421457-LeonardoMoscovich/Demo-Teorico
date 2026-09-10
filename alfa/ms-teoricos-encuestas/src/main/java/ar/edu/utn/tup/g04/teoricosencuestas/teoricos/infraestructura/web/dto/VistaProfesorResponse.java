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
        List<ItemParaProfesor> items) {

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
