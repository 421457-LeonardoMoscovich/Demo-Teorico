package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * SOLO para el profesor dueno del item: lleva el criterio.
 * Este DTO no aparece en ningun endpoint que pueda consumir un alumno.
 */
public record ItemDetalleResponse(
        UUID id,
        TipoDeItem tipo,
        String enunciado,
        int version,
        UUID itemVersionId,
        JsonNode payload,
        JsonNode criterio) {
}
