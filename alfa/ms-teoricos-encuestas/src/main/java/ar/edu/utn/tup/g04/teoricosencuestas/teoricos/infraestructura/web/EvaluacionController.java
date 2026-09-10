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
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.ResultadoResponse;
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
    @PostMapping
    public ResponseEntity<AcuseResponse> despachar(@Valid @RequestBody DespachoRequest req) {
        EvaluacionEntity evaluacion = evaluaciones.despachar(req.aDespacho());
        String correccion = composicion.modoDeCorreccion(
                composicion.resolver(evaluacion.getContenidoId()));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new AcuseResponse(evaluacion.getId(), AcuseResponse.ACEPTADA, correccion));
    }

    /**
     * Reconciliacion para el Tema 03 cuando perdio el evento (CI-43), y de paso
     * la pantalla de resultado del alumno con su desglose (CI-28).
     */
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
                evaluaciones.desglose(e.getId()).stream()
                        .map(d -> new ResultadoResponse.ItemCorregido(
                                d.itemVersionId(), d.orden(), d.enunciado(),
                                d.puntaje(), d.obtenido(), d.obtenido() == d.puntaje(),
                                d.respuesta()))
                        .toList());
    }
}
