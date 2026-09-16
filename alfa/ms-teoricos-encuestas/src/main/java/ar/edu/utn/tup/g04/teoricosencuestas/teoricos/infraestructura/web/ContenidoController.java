package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.IdentidadActual;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.ComposicionService;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.ValeDeLectura;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.MapeadorJson;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.ComponerContenidoRequest;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.ContenidoRefResponse;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.MiContenidoResponse;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.VistaAlumnoResponse;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.VistaProfesorResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Composición")
@SecurityRequirement(name = "token")
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
    @Operation(summary = "Componer un cuestionario y emitir la ficha de cinco campos",
            description = """
                    Devuelve la **ficha** que el front le pasa después al Tema 03 al crear el
                    desafío (CI-03). El orden importa: primero acá, después allá. Así no existe
                    ningún contrato de composición entre el Tema 03 y nosotros, y se puede armar
                    contenido aunque el 03 esté caído.

                    `correccion` es **derivado, no elegido** (CI-07): sale INMEDIATA si todos los
                    ítems se corrigen solos, y DIFERIDA si alcanza uno que espere a un humano.
                    El Tema 03 recibe esa consecuencia y no el modo de cada ítem.

                    `resumen` es una cadena que el Tema 03 **pinta en pantalla y no interpreta**.
                    Existe para que nadie termine pidiendo `cantidadDeItems` y `puntajeTotal` por
                    separado, que sí serían conocimiento de contenido.
                    """)
    @PostMapping
    public ResponseEntity<ContenidoRefResponse> componer(
            @Valid @RequestBody ComponerContenidoRequest req) {
        ContenidoEntity contenido = composicion.componer(
                identidad.id(), req.cursoCohorteId(), req.titulo(), req.escala(),
                req.navegacionOEstandar(), req.aLineas(), req.reglaODada());
        return ResponseEntity.status(HttpStatus.CREATED).body(ficha(contenido));
    }

    @Operation(summary = "Los cuestionarios que armó este profesor",
            description = """
                    Para reutilizar uno en otra unidad o en otra cohorte. **No devuelve la ficha
                    pelada**: lleva el título, que es lo que a la ficha le falta para que un
                    humano distinga un cuestionario de otro —y que la ficha no tiene a propósito,
                    porque cruzaría la frontera hacia el Tema 03—.

                    La ficha viaja anidada y entera, para que el front la pase tal cual al crear
                    el desafío: no se rearma ni se le agrega nada.
                    """)
    @GetMapping
    public List<MiContenidoResponse> mios() {
        return composicion.listarDelProfesor(identidad.id()).stream()
                .map(c -> new MiContenidoResponse(
                        c.getId(), c.getTitulo(), c.getCursoCohorteId(), c.getEscala(),
                        c.getNavegacion(), c.getCreadoEn(), ficha(c)))
                .toList();
    }

    @Operation(summary = "El cuestionario como lo ve su dueña, con las claves de corrección",
            description = """
                    Lista las preguntas **fijas**, las que reciben todos. Si el cuestionario tiene
                    un **sorteo**, las sorteadas no se listan y no es un olvido: no existen hasta
                    que hay un alumno. Lo que se devuelve de ellas es la regla y **cuántos
                    candidatos hay hoy** en esa etiqueta, que es lo único que la profesora puede
                    mirar para saber qué tan variado es el examen que está entregando.
                    """)
    @GetMapping("/{id}/vista-profesor")
    public VistaProfesorResponse vistaProfesor(@PathVariable UUID id) {
        ContenidoEntity contenido = composicion.exigirPropio(identidad.id(), id);
        List<ComposicionService.ItemResuelto> resueltos = composicion.resolver(id);
        return new VistaProfesorResponse(
                contenido.getId(), contenido.getTitulo(), contenido.getVersion(),
                contenido.getEscala(), composicion.puntajeTotalDe(id),
                composicion.modoDeCorreccionDe(id), composicion.resumenDe(id),
                reglaDe(contenido, resueltos),
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
     * CI-19, en su version corregida: la lectura SIGUE SIN TENER ESTADO, aunque
     * cada alumno vea las preguntas en un orden distinto. El orden se DERIVA de
     * (contenidoId, alumnoId) y no se guarda: recargar da lo mismo y reconstruirlo
     * despues, al armar el desglose, tambien. No escribimos nada al servir.
     */
    @Tag(name = "Lectura del alumno")
    @Operation(summary = "El cuestionario como lo ve el alumno, sin las claves de corrección",
            description = """
                    Pide **dos credenciales distintas** y no es redundancia: el token dice quién
                    sos, y el vale dice que el Tema 03 te habilitó a leer este contenido ahora.

                    El problema que resuelve el vale: este endpoint recibe un `contenidoId` y no
                    un `desafioId`, y eso es justamente lo que nos mantiene libres del Tema 03.
                    Pero deja una pregunta sin dueño —quién verifica que este alumno pueda leer
                    esto ahora—, porque las ventanas de apertura son del 03 y decidimos no
                    mirarlas (CI-01). Sin vale, cualquiera con el `contenidoId` lee las preguntas
                    antes de que el desafío abra.

                    **El orden de las preguntas es distinto para cada alumno** y se deriva de
                    `contenidoId` + `alumnoId`: es estable entre recargas y no se guarda en
                    ningún lado. El `orden` que se devuelve es la posición que ve ESTE alumno,
                    no la que le puso el profesor.

                    La respuesta **no tiene un campo criterio**, y la garantía la da el tipo: es
                    un DTO aparte y no la vista del profesor filtrada (CI-17). Lo prueba
                    `VistaAlumnoSinCriterioIT` sobre el JSON crudo.
                    """)
    @SecurityRequirement(name = "vale")
    @GetMapping("/{id}/vista-alumno")
    public VistaAlumnoResponse vistaAlumno(
            @PathVariable UUID id,
            @RequestHeader(value = ValeDeLectura.HEADER, required = false) String vale) {
        ValeDeLectura.Contenido delVale = vales.validar(vale, id);
        ContenidoEntity contenido = composicion.exigir(id);
        // resolverPara y no resolver: si el cuestionario tiene sorteo, ESTE
        // alumno recibe su propio subconjunto, derivado y no guardado.
        List<ComposicionService.ItemResuelto> resueltos = composicion.resolverPara(id, delVale.alumnoId());

        // El puntaje total sale del cuestionario y no de lo que le toco: con
        // sorteo los dos numeros coinciden igual —el peso de las sorteadas es
        // uniforme—, y asi el alumno ve el mismo "sobre 100" que sus companeros.
        int total = composicion.puntajeTotalDe(id);
        List<ComposicionService.ItemResuelto> paraEl =
                composicion.enOrdenPara(resueltos, id, delVale.alumnoId());

        List<VistaAlumnoResponse.ItemParaAlumno> items = new java.util.ArrayList<>();
        for (int i = 0; i < paraEl.size(); i++) {
            ComposicionService.ItemResuelto r = paraEl.get(i);
            items.add(new VistaAlumnoResponse.ItemParaAlumno(
                    r.version().getId(), r.item().getTipo(), r.version().getEnunciado(),
                    // El numero que ve el alumno es su POSICION, no el orden que
                    // le puso el profesor: para el, esta es la pregunta 1.
                    i + 1, r.puntaje(),
                    json.leerPayload(r.item().getTipo(), json.aNodo(r.version().getPayload()))));
        }
        return new VistaAlumnoResponse(contenido.getId(), contenido.getTitulo(),
                contenido.getVersion(), total, contenido.getNavegacion(), items);
    }

    private ContenidoRefResponse ficha(ContenidoEntity contenido) {
        return new ContenidoRefResponse(
                ContenidoRefResponse.TIPO_TEORICO, contenido.getId(), contenido.getVersion(),
                composicion.resumenDe(contenido.getId()),
                composicion.modoDeCorreccionDe(contenido.getId()));
    }

    /** La regla con cuantos candidatos tiene HOY, que es lo que cambia sola. */
    private VistaProfesorResponse.ReglaResponse reglaDe(
            ContenidoEntity contenido, List<ComposicionService.ItemResuelto> fijas) {
        return composicion.reglaDe(contenido.getId())
                .map(r -> new VistaProfesorResponse.ReglaResponse(
                        r.getEtiqueta(), r.getCuantos(), r.getPuntaje(),
                        composicion.candidatos(contenido.getProfesorId(), r.getEtiqueta(),
                                fijas.stream().map(f -> f.item().getId())
                                        .collect(java.util.stream.Collectors.toSet())).size()))
                .orElse(null);
    }
}
