package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.ComposicionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ComponerContenidoRequest(
        @NotNull UUID cursoCohorteId,
        @NotBlank String titulo,
        /** PORCENTUAL exige que los pesos sumen 100. LIBRE no. */
        @NotBlank String escala,
        /**
         * LIBRE | SECUENCIAL. Opcional: si no viene, LIBRE. Es opcional a
         * proposito —un cliente escrito antes de que esto existiera sigue
         * componiendo igual y obtiene el comportamiento que ya tenia—.
         */
        String navegacion,
        /**
         * Las preguntas FIJAS: las que reciben todos. Puede venir vacia si hay
         * regla —un cuestionario puede ser solo sorteo—, y por eso ya no es
         * @NotEmpty: que no haya ni items ni regla lo rechaza el dominio, que
         * es quien sabe que las dos cosas juntas son lo que forma un
         * cuestionario.
         */
        List<LineaRequest> items,
        /** El sorteo por etiqueta, opcional. */
        ReglaRequest regla) {

    public record ReglaRequest(@NotBlank String etiqueta, int cuantos, int puntaje) {

        public ComposicionService.Regla aRegla() {
            return new ComposicionService.Regla(etiqueta, cuantos, puntaje);
        }
    }

    public ComposicionService.Regla reglaODada() {
        return regla == null ? null : regla.aRegla();
    }

    /** Nunca null: lo que no se elige es la navegacion de siempre. */
    public String navegacionOEstandar() {
        return navegacion == null || navegacion.isBlank()
                ? ComposicionService.NAVEGACION_LIBRE
                : navegacion;
    }

    public record LineaRequest(@NotNull UUID itemId, int orden, int puntaje) {}

    public List<ComposicionService.Linea> aLineas() {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(l -> new ComposicionService.Linea(l.itemId(), l.orden(), l.puntaje()))
                .toList();
    }
}
