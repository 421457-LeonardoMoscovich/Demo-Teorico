package ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.IdentidadActual;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.Rol;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * ANDAMIAJE DE LA DEMO. Deja mirar los eventos que salieron hacia el Tema 03.
 *
 * Hasta ahora el envelope de `TEORICO_CORREGIDO` solo existia en el log de
 * Docker, y eso deja el punto mas fino del diseno fuera de la pantalla: que el
 * contrato con el Tema 03 SIEMPRE fue asincronico. Con los cuatro tipos
 * automaticos no se notaba, porque el evento salia en el mismo milisegundo que
 * la entrega. Con la correccion humana se ve: el alumno entrega y no pasa nada,
 * y el evento aparece recien cuando el profesor pone el ultimo puntaje.
 *
 * Se apaga con `app.demo.panel=false`, y se borra entero el dia que entre Kafka
 * — ahi los eventos se miran en el broker, que es donde se miran de verdad.
 *
 * Es del PROFESOR y no del alumno: el envelope lleva notas y alumnoId de
 * cualquiera, y esa es informacion de otros.
 */
@RestController
@RequestMapping("/teoricos/demo/eventos")
@ConditionalOnProperty(name = "app.demo.panel", havingValue = "true", matchIfMissing = true)
@Tag(name = "Demo", description = "Andamiaje de la alfa. No es parte del contrato y no está en "
        + "el Sprint 1: se borra cuando entre Kafka.")
public class EventosDemoController {

    private final PublicadorEnMemoria publicador;
    private final IdentidadActual identidad;

    public EventosDemoController(PublicadorEnMemoria publicador, IdentidadActual identidad) {
        this.publicador = publicador;
        this.identidad = identidad;
    }

    public record EventoVisible(String topico, String clave, String eventId, String eventType,
                                String timestamp, String producer, Object payload) {}

    @GetMapping
    public List<EventoVisible> ultimos() {
        if (identidad.esServicio() || identidad.rol() != Rol.PROFESOR) {
            throw new ExcepcionDeNegocio(ClaveError.NO_AUTORIZADO, null, HttpStatus.FORBIDDEN);
        }

        List<EventoVisible> visibles = new ArrayList<>();
        for (PublicadorEnMemoria.Publicado p : publicador.publicados()) {
            EventoSobre e = p.evento();
            visibles.add(new EventoVisible(p.topico(), p.clave(), String.valueOf(e.eventId()),
                    e.eventType(), String.valueOf(e.timestamp()), e.producer(), e.payload()));
        }
        // El ultimo primero: en una demo lo que interesa es lo que acaba de pasar.
        java.util.Collections.reverse(visibles);
        return visibles;
    }
}
