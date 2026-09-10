package ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * El envelope estandar, obligatorio para todos los microservicios de la
 * plataforma. Nuestro producer es siempre tema-04-teoricos-encuestas y el
 * eventType va en MAYUSCULA_SNAKE.
 */
public record EventoSobre(
        UUID eventId,
        String eventType,
        Instant timestamp,
        String producer,
        Map<String, Object> payload) {

    public static final String PRODUCER = "tema-04-teoricos-encuestas";

    public static EventoSobre de(String eventType, Map<String, Object> payload) {
        return new EventoSobre(UUID.randomUUID(), eventType, Instant.now(), PRODUCER, payload);
    }
}
