package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;

import java.util.UUID;

/** Lo que se lista en el banco. Sin payload y sin criterio. */
public record ItemResumenResponse(
        UUID id,
        TipoDeItem tipo,
        String enunciado,
        int version,
        boolean autocorregible) {
}
