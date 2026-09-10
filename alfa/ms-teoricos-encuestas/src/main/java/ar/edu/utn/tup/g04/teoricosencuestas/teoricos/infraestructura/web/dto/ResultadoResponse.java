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
        /** Null mientras el estado sea EN_ESPERA: la nota es del cuestionario entero o no es. */
        Integer nota,
        String estado,
        String corrector,
        int revision,
        List<ItemCorregido> detalle) {

    /**
     * `obtenido` y `correcto` en null significan que ese item todavia espera a
     * un humano (D-01). Se modela como ausencia y no como un 0 con una bandera
     * al lado: un 0 es una nota, y decirle 0 a algo que nadie corrigio todavia
     * seria mentirle al alumno.
     */
    public record ItemCorregido(
            UUID itemVersionId,
            int orden,
            String enunciado,
            /** El payload de la version que vio el alumno: sirve para traducir ids a texto. */
            JsonNode payload,
            int puntaje,
            Integer obtenido,
            Boolean correcto,
            boolean pendiente,
            JsonNode respuesta) {
    }
}
