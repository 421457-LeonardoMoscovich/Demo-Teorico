package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.EvaluacionService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * El mensaje de despacho de CI-22. Es IDENTICO al que recibe el Tema 05 salvo
 * el campo respuesta, que para el 03 es opaco: por eso viaja como JSON crudo y
 * no como una estructura que el 03 tenga que entender.
 *
 * contenidoBinding lleva DOS campos, no cinco: tipo ya se consumio en el ruteo
 * del 03, y resumen y correccion los produjimos nosotros, asi que seria absurdo
 * que nos vuelvan.
 *
 * alumnoId SI viene en el body, a diferencia del resto de nuestros endpoints:
 * aca el llamador es el Tema 03, que es el dueno del dato. Lo que reemplaza a
 * la garantia del token es la credencial de servicio del despacho.
 */
public record DespachoRequest(
        @NotNull UUID entregaId,
        @NotNull UUID desafioId,
        @NotNull UUID alumnoId,
        @NotNull UUID cursoCohorteId,
        int intento,
        @NotNull ContenidoBinding contenidoBinding,
        @NotEmpty List<RespuestaRequest> respuestas) {

    public record ContenidoBinding(@NotNull UUID contenidoId, int version) {}

    /** La estampa: sobre que version de cada item contesto el alumno (CI-12). */
    public record RespuestaRequest(@NotNull UUID itemVersionId, @NotNull JsonNode contenido) {}

    public EvaluacionService.Despacho aDespacho() {
        return new EvaluacionService.Despacho(
                entregaId, desafioId, alumnoId, cursoCohorteId, intento,
                contenidoBinding.contenidoId(),
                respuestas.stream()
                        .map(r -> new EvaluacionService.RespuestaRecibida(r.itemVersionId(), r.contenido()))
                        .toList());
    }
}
