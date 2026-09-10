package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.BancoDeItemsService;
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
        JsonNode criterio) {

    public BancoDeItemsService.Contenido aContenido() {
        return new BancoDeItemsService.Contenido(enunciado, payload, criterio);
    }
}
