package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.IdentidadActual;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.Rol;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.CorreccionManualService;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.EvaluacionEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La cola de correccion del profesor.
 *
 * Es nuestra y no del Tema 03: lo que hay que mirar para puntuar —la consigna,
 * la respuesta y la rubrica— es conocimiento de contenido, y el 03 no lo tiene
 * ni le hace falta. El unico dato que cruza la frontera sigue siendo la nota,
 * y sale por el evento cuando el ultimo item queda corregido.
 */
@Tag(name = "Corrección humana")
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/teoricos/correcciones")
public class CorreccionController {

    private final CorreccionManualService correcciones;
    private final IdentidadActual identidad;

    public CorreccionController(CorreccionManualService correcciones, IdentidadActual identidad) {
        this.correcciones = correcciones;
        this.identidad = identidad;
    }

    private UUID profesorActual() {
        if (identidad.esServicio() || identidad.rol() != Rol.PROFESOR) {
            throw new ExcepcionDeNegocio(ClaveError.NO_AUTORIZADO, null, HttpStatus.FORBIDDEN);
        }
        return identidad.id();
    }

    @GetMapping("/pendientes")
    public List<CorreccionManualService.Pendiente> pendientes() {
        return correcciones.pendientesDe(profesorActual());
    }

    /**
     * Solo el numero, para el badge de la barra. Endpoint aparte del listado
     * porque el front lo sondea cada pocos segundos y no tiene sentido que se
     * traiga las respuestas completas de todos los alumnos para pintar un "3".
     */
    @GetMapping("/pendientes/cuantas")
    public Map<String, Long> cuantas() {
        return Map.of("pendientes", correcciones.cuantasPendientes(profesorActual()));
    }

    public record PuntuarRequest(@NotNull Integer obtenido) {}

    @PostMapping("/{detalleId}")
    public Map<String, Object> puntuar(@PathVariable UUID detalleId,
                                       @Valid @RequestBody PuntuarRequest req) {
        EvaluacionEntity e = correcciones.puntuar(profesorActual(), detalleId, req.obtenido());
        // Se devuelve el estado de la evaluacion para que el front sepa si con
        // esta correccion se cerro el cuestionario o todavia le falta.
        return Map.of("evaluacionId", e.getId(), "estado", e.getEstado(),
                "cerrada", !e.esperaCorreccion());
    }
}
