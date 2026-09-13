package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.IdentidadActual;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.Rol;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.ComposicionService;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.EvaluacionService;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.EvaluacionEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.AcuseResponse;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.DespachoRequest;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.EntregaDelAlumnoResponse;
import java.util.List;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.ResultadoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Evaluación")
@RestController
@RequestMapping("/teoricos/evaluaciones")
public class EvaluacionController {

    private final EvaluacionService evaluaciones;
    private final ComposicionService composicion;
    private final IdentidadActual identidad;

    public EvaluacionController(EvaluacionService evaluaciones, ComposicionService composicion,
                                IdentidadActual identidad) {
        this.evaluaciones = evaluaciones;
        this.composicion = composicion;
        this.identidad = identidad;
    }

    /**
     * El despacho del Tema 03 (CI-22). Es una llamada SINCRONICA y no un evento
     * (CI-24): el 03 necesita un bit para continuar, si aceptamos la entrega o
     * no. Con un evento, una entrega mal armada desapareceria en silencio, que
     * es la peor falla posible en este punto del ciclo.
     */
    @Operation(summary = "Recibir la entrega despachada por el Tema 03 y corregir",
            description = """
                    **Es una llamada sincrónica y no un evento** (CI-24), y es deliberado: el
                    Tema 03 necesita un bit para continuar —si aceptamos la entrega o no—. Con un
                    evento, una entrega mal armada desaparecería en silencio, que es la peor
                    falla posible en este punto del ciclo.

                    **Idempotente por `entregaId`** (CI-25). La garantía la da un UNIQUE en la
                    base y no un `if`: si el 03 reintenta, no corregimos dos veces ni emitimos
                    dos eventos.

                    **El acuse nunca lleva la nota**, ni siquiera con corrección inmediata
                    (CI-23). "Inmediata" significa "en segundos y sin que intervenga un humano",
                    no "en la misma respuesta HTTP".

                    Si el cuestionario tiene ítems de corrección humana, la evaluación queda en
                    `EN_ESPERA`, sin nota, y **el evento no sale** hasta que el profesor ponga el
                    último puntaje.
                    """)
    @SecurityRequirement(name = "despacho")
    @PostMapping
    public ResponseEntity<AcuseResponse> despachar(@Valid @RequestBody DespachoRequest req) {
        EvaluacionEntity evaluacion = evaluaciones.despachar(req.aDespacho());
        String correccion = composicion.modoDeCorreccionDe(evaluacion.getContenidoId());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AcuseResponse(evaluacion.getId(), AcuseResponse.ACEPTADA, correccion));
    }

    /**
     * Correcto / incorrecto, o NINGUNO DE LOS DOS.
     *
     * Los cuatro tipos automaticos son todo o nada, asi que ahi la pregunta
     * siempre tiene respuesta. Una correccion humana no: un desarrollo que saco
     * 48 sobre 60 no es "incorrecto", y decirle eso al alumno es peor que no
     * decirle nada. Con puntaje parcial devolvemos null y el front muestra
     * "Parcial", que es lo unico cierto.
     */
    private Boolean correcto(Integer obtenido, int puntaje) {
        if (obtenido == null) return null;
        if (obtenido == puntaje) return true;
        if (obtenido == 0) return false;
        return null;
    }

    /**
     * Reconciliacion para el Tema 03 cuando perdio el evento (CI-43), y de paso
     * la pantalla de resultado del alumno con su desglose (CI-28).
     */
    /**
     * OJO con el orden de los mapeos: esta ruta es literal y la de abajo lleva
     * {entregaId}. Spring resuelve primero la literal, asi que "historial" nunca
     * se intenta convertir a UUID. Si alguien la renombra a algo con {} en el
     * medio, se rompe en silencio.
     */
    @Operation(summary = "Todo lo que entregó este alumno, lo más reciente primero",
            description = """
                    CI-44: se guarda una corrección por entrega y **nunca se pisa**. Cada intento
                    queda, y esta lista es lo que permite verlo. Dos intentos del mismo desafío
                    pueden estar corregidos contra versiones distintas del mismo ítem, y cada uno
                    conserva la suya.
                    """)
    @SecurityRequirement(name = "token")
    @GetMapping("/historial")
    public List<EntregaDelAlumnoResponse> historial() {
        if (identidad.esServicio() || identidad.rol() != Rol.ALUMNO) {
            throw new ExcepcionDeNegocio(ClaveError.NO_AUTORIZADO, null, HttpStatus.FORBIDDEN);
        }
        List<EntregaDelAlumnoResponse> resultado = new java.util.ArrayList<>();
        for (EvaluacionEntity e : evaluaciones.historialDe(identidad.id())) {
            resultado.add(new EntregaDelAlumnoResponse(
                    e.getEntregaId(), e.getDesafioId(),
                    composicion.tituloDe(e.getContenidoId()), e.getIntento(),
                    e.getNota(), e.getEstado(), e.getCorrector(), e.getCreadaEn()));
        }
        return resultado;
    }

    @Operation(summary = "El resultado con su desglose por pregunta",
            description = """
                    Dos consumidores: el **Tema 03**, para reconciliar cuando perdió el evento
                    (CI-43), y el **front**, para la pantalla del alumno (CI-28).

                    El desglose dice cuánto sacó en cada pregunta y qué contestó, pero **nunca la
                    clave de corrección**: con reintentos ilimitados (RF-REC-04), devolverla
                    convierte el reintento en copiar.

                    No hay campo `aprobado` (H-16, CI-39). Un `obtenido` en null significa que
                    ese ítem todavía lo espera un humano; `correcto` en null con puntaje presente
                    significa puntaje parcial, que no es ni correcta ni incorrecta.
                    """)
    @SecurityRequirement(name = "token")
    @GetMapping("/{entregaId}")
    public ResultadoResponse resultado(@PathVariable UUID entregaId) {
        EvaluacionEntity e = evaluaciones.porEntrega(entregaId);

        // Un alumno solo ve lo suyo (RF-USR-08). El servicio (el Tema 03) ve todo,
        // porque la entrega es suya y la nota se la debemos.
        if (!identidad.esServicio() && identidad.rol() == Rol.ALUMNO
                && !identidad.id().equals(e.getAlumnoId())) {
            throw new ExcepcionDeNegocio(ClaveError.NO_AUTORIZADO, null, HttpStatus.FORBIDDEN);
        }

        return new ResultadoResponse(
                e.getEntregaId(), e.getDesafioId(), e.getAlumnoId(), e.getIntento(),
                e.getNota(), e.getEstado(), e.getCorrector(), e.getRevision(),
                evaluaciones.desglose(e).stream()
                        .map(d -> new ResultadoResponse.ItemCorregido(
                                d.itemVersionId(), d.orden(), d.enunciado(), d.payload(),
                                d.puntaje(),
                                d.obtenido(),
                                correcto(d.obtenido(), d.puntaje()),
                                d.obtenido() == null,
                                d.respuesta(),
                                d.devolucion()))
                        .toList());
    }
}
