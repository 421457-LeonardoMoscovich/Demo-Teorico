package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

/**
 * El resultado, con el desglose por item (CI-28, CI-43).
 *
 * NO tiene un campo `aprobado`, y eso es a proposito: el PRD no define en
 * ningun lado el umbral de aprobacion (H-16), y como el umbral maneja XP
 * (PAR-01) y vidas (RF-DES-07), es economia — y la economia vive en un solo
 * lugar, el Tema 03. Decirlo nosotros seria inventar una regla academica.
 *
 * El desglose dice cuanto saco en cada pregunta y que contesto, pero NO la
 * clave de correccion: con reintentos ilimitados (RF-REC-04), devolverla
 * convierte el reintento en copiar.
 */
public record ResultadoResponse(
        UUID entregaId,
        UUID desafioId,
        UUID alumnoId,
        int intento,
        Integer nota,
        String estado,
        String corrector,
        int revision,
        List<ItemCorregido> detalle) {

    public record ItemCorregido(
            UUID itemVersionId,
            int orden,
            String enunciado,
            int puntaje,
            int obtenido,
            boolean correcto,
            JsonNode respuesta) {
    }
}
