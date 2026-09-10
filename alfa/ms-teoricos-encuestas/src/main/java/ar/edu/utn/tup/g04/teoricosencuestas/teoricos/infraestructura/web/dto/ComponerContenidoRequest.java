package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.ComposicionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ComponerContenidoRequest(
        @NotNull UUID cursoCohorteId,
        @NotBlank String titulo,
        /** PORCENTUAL exige que los pesos sumen 100. LIBRE no. */
        @NotBlank String escala,
        @NotEmpty List<LineaRequest> items) {

    public record LineaRequest(@NotNull UUID itemId, int orden, int puntaje) {}

    public List<ComposicionService.Linea> aLineas() {
        return items.stream()
                .map(l -> new ComposicionService.Linea(l.itemId(), l.orden(), l.puntaje()))
                .toList();
    }
}
