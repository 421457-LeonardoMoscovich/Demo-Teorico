package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.PayloadDeItem;

import java.util.List;
import java.util.UUID;

/**
 * Lo que ve el alumno. Es un DTO APARTE, no la vista del profesor filtrada por
 * rol (CI-17): es una frontera de seguridad, no de estilo.
 *
 * La clase NO TIENE un campo criterio. La garantia la da el tipo, no un
 * @JsonIgnore que alguien puede borrar sin darse cuenta de lo que rompe.
 *
 * El payload se re-serializa desde el record del dominio y no se reenvia el
 * jsonb crudo: asi, el dia que alguien meta un campo esCorrecta adentro de las
 * opciones para simplificar algo, no se filtra por este endpoint.
 */
public record VistaAlumnoResponse(
        UUID contenidoId,
        String titulo,
        int version,
        int puntajeTotal,
        List<ItemParaAlumno> items) {

    public record ItemParaAlumno(
            UUID itemVersionId,
            TipoDeItem tipo,
            String enunciado,
            int orden,
            int puntaje,
            PayloadDeItem payload) {
    }
}
