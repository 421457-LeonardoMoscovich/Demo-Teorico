package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.BancoDeItemsService;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.EstadoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * El enunciado NO lleva @NotBlank aca a proposito: lo valida
 * ValidadorDePayload, junto con el resto de las reglas del tipo, para que el
 * error salga siempre por el mismo camino y con el mismo formato de campo.
 */
public record CrearItemRequest(
        @NotNull TipoDeItem tipo,
        String enunciado,
        @NotNull JsonNode payload,
        JsonNode criterio,
        /** Opcional (CI-58). Ausente = el item no devuelve nada despues de corregir. */
        JsonNode devolucion,
        /** Opcional (CI-59). Ausente = LISTO: el que no dice nada, publica. */
        EstadoDeItem estado) {

    public BancoDeItemsService.Contenido aContenido() {
        return new BancoDeItemsService.Contenido(enunciado, payload, criterio, devolucion, estado);
    }
}
