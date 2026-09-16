package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.BancoDeItemsService;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.EstadoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

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
        EstadoDeItem estado,
        /**
         * Opcional. AUSENTE y VACIA no son lo mismo: ausente deja las que el
         * item ya tiene —asi un cliente viejo no las borra sin querer— y vacia
         * las saca todas.
         */
        List<String> etiquetas) {

    public BancoDeItemsService.Contenido aContenido() {
        return new BancoDeItemsService.Contenido(
                enunciado, payload, ausente(criterio), ausente(devolucion), estado, etiquetas);
    }

    /**
     * `"devolucion": null` y no mandar el campo son lo MISMO para quien escribe
     * el JSON, pero Jackson los distingue: el primero llega como NullNode y no
     * como null, asi que se cuela por todos los `== null` del servicio y termina
     * en un PAYLOAD_INVALIDO —o, peor, en un `"null"` guardado en el jsonb que
     * recien explota al corregir—. Se normaliza en el borde y no en cada uso.
     */
    private static JsonNode ausente(JsonNode nodo) {
        return nodo == null || nodo.isNull() ? null : nodo;
    }
}
