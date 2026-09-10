package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.IdentidadActual;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.ComposicionService;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.ValeDeLectura;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.MapeadorJson;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.ComponerContenidoRequest;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.ContenidoRefResponse;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.VistaAlumnoResponse;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.VistaProfesorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/teoricos/contenidos")
public class ContenidoController {

    private final ComposicionService composicion;
    private final ValeDeLectura vales;
    private final IdentidadActual identidad;
    private final MapeadorJson json;

    public ContenidoController(ComposicionService composicion, ValeDeLectura vales,
                               IdentidadActual identidad, MapeadorJson json) {
        this.composicion = composicion;
        this.vales = vales;
        this.identidad = identidad;
        this.json = json;
    }

    /**
     * Lo llama EL FRONT (CI-03), no el Tema 03. Devuelve la ficha de cinco
     * campos con la que el front va despues a crear el desafio en el 03.
     */
    @PostMapping
    public ResponseEntity<ContenidoRefResponse> componer(
            @Valid @RequestBody ComponerContenidoRequest req) {
        ContenidoEntity contenido = composicion.componer(
                identidad.id(), req.cursoCohorteId(), req.titulo(), req.escala(), req.aLineas());
        return ResponseEntity.status(HttpStatus.CREATED).body(ficha(contenido));
    }

    @GetMapping
    public List<ContenidoRefResponse> mios() {
        return composicion.listarDelProfesor(identidad.id()).stream().map(this::ficha).toList();
    }

    @GetMapping("/{id}/vista-profesor")
    public VistaProfesorResponse vistaProfesor(@PathVariable UUID id) {
        ContenidoEntity contenido = composicion.exigirPropio(identidad.id(), id);
        List<ComposicionService.ItemResuelto> resueltos = composicion.resolver(id);
        return new VistaProfesorResponse(
                contenido.getId(), contenido.getTitulo(), contenido.getVersion(),
                contenido.getEscala(), composicion.puntajeTotal(resueltos),
                composicion.modoDeCorreccion(resueltos), composicion.resumen(resueltos),
                resueltos.stream().map(r -> new VistaProfesorResponse.ItemParaProfesor(
                        r.item().getId(), r.version().getId(), r.item().getTipo(),
                        r.version().getEnunciado(), r.version().getVersion(),
                        r.orden(), r.puntaje(),
                        json.aNodo(r.version().getPayload()),
                        r.version().getCriterio() == null ? null : json.aNodo(r.version().getCriterio())))
                        .toList());
    }

    /**
     * La lectura del alumno. Exige el vale firmado por el Tema 03 (CI-18).
     *
     * CI-19: no tiene estado. El mismo contenido se le sirve igual a todos: no
     * necesitamos saber quien mira para armar la respuesta ni guardamos nada al
     * servirla. Verificado en el PRD: no hay barajado de preguntas ni cronometro.
     */
    @GetMapping("/{id}/vista-alumno")
    public VistaAlumnoResponse vistaAlumno(
            @PathVariable UUID id,
            @RequestHeader(value = ValeDeLectura.HEADER, required = false) String vale) {
        vales.validar(vale, id);
        ContenidoEntity contenido = composicion.exigir(id);
        List<ComposicionService.ItemResuelto> resueltos = composicion.resolver(id);
        return new VistaAlumnoResponse(
                contenido.getId(), contenido.getTitulo(), contenido.getVersion(),
                composicion.puntajeTotal(resueltos),
                resueltos.stream().map(r -> new VistaAlumnoResponse.ItemParaAlumno(
                        r.version().getId(), r.item().getTipo(), r.version().getEnunciado(),
                        r.orden(), r.puntaje(),
                        json.leerPayload(r.item().getTipo(), json.aNodo(r.version().getPayload()))))
                        .toList());
    }

    private ContenidoRefResponse ficha(ContenidoEntity contenido) {
        List<ComposicionService.ItemResuelto> resueltos = composicion.resolver(contenido.getId());
        return new ContenidoRefResponse(
                ContenidoRefResponse.TIPO_TEORICO, contenido.getId(), contenido.getVersion(),
                composicion.resumen(resueltos), composicion.modoDeCorreccion(resueltos));
    }
}
