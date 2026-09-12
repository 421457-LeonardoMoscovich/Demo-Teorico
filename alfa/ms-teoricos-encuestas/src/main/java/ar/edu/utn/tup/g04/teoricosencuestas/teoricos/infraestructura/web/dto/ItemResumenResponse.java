package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.EstadoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;

import java.util.List;
import java.util.UUID;

/** Lo que se lista en el banco. Sin payload y sin criterio. */
public record ItemResumenResponse(
        UUID id,
        TipoDeItem tipo,
        String enunciado,
        int version,
        boolean autocorregible,
        /** BORRADOR | LISTO (CI-59). Un borrador no entra a ningun cuestionario. */
        EstadoDeItem estado,
        /** Como el profesor organiza SU banco. No es un vinculo a un curso (CI-01). */
        List<String> etiquetas) {
}
