package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Una linea del historial del alumno. Sin desglose: para eso esta el resultado
 * completo, y traer el de todos los intentos para pintar una lista seria caro
 * y ademas mostraria mas de lo que la pantalla necesita.
 */
public record EntregaDelAlumnoResponse(
        UUID entregaId,
        UUID desafioId,
        String cuestionario,
        int intento,
        Integer nota,
        String estado,
        String corrector,
        Instant entregadaEn) {
}
