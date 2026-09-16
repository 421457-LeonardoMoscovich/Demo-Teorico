package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.CriterioDeCorreccion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.MapeadorJson;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.RespuestaDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La cola de correccion humana (D-01).
 *
 * Esto NO es un adaptador del puerto `Corrector`, y la distincion importa: la
 * firma del puerto es `int corregir(criterio, respuesta, puntaje)`, una funcion
 * pura que el sistema invoca cuando quiere. Una correccion humana no es eso —es
 * un trabajo pendiente que espera a que alguien aparezca—. Meterla adentro del
 * puerto obligaria a que `corregir` a veces no devuelva nada, y ahi el puerto
 * deja de significar algo.
 *
 * Asi que el puerto se queda como esta, para lo que el sistema SI puede
 * ejecutar (lo automatico hoy, el LLM despues), y la espera humana vive aca,
 * como lo que es: una cola.
 */
@Service
public class CorreccionManualService {

    private final EvaluacionDetalleRepository detalles;
    private final EvaluacionRepository evaluaciones;
    private final RespuestaRepository respuestas;
    private final ItemVersionRepository versiones;
    private final ItemRepository items;
    private final ContenidoRepository contenidos;
    private final EvaluacionService evaluacionService;
    private final MapeadorJson json;

    public CorreccionManualService(EvaluacionDetalleRepository detalles,
                                   EvaluacionRepository evaluaciones,
                                   RespuestaRepository respuestas,
                                   ItemVersionRepository versiones,
                                   ItemRepository items,
                                   ContenidoRepository contenidos,
                                   EvaluacionService evaluacionService,
                                   MapeadorJson json) {
        this.detalles = detalles;
        this.evaluaciones = evaluaciones;
        this.respuestas = respuestas;
        this.versiones = versiones;
        this.items = items;
        this.contenidos = contenidos;
        this.evaluacionService = evaluacionService;
        this.json = json;
    }

    /**
     * Un item esperando correccion, con todo lo que hace falta para puntuarlo
     * sin salir de la pantalla: la consigna que vio el alumno, lo que escribio,
     * y la rubrica que el profesor se escribio a si mismo cuando creo el item.
     */
    public record Pendiente(UUID detalleId, UUID evaluacionId, UUID alumnoId, int intento,
                            String cuestionario, String enunciado, String consigna,
                            String rubrica, int puntaje, String respuesta, Instant entregadaEn) {}

    @Transactional(readOnly = true)
    public List<Pendiente> pendientesDe(UUID profesorId) {
        List<Pendiente> resultado = new ArrayList<>();
        for (EvaluacionDetalleEntity d : detalles.pendientesDelProfesor(profesorId)) {
            evaluaciones.findById(d.getEvaluacionId()).ifPresent(e ->
                    resultado.add(armar(d, e)));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public long cuantasPendientes(UUID profesorId) {
        return detalles.cuantasPendientes(profesorId);
    }

    /**
     * El profesor pone el puntaje. Si era el ultimo pendiente, la evaluacion se
     * cierra y RECIEN AHI sale el evento hacia el Tema 03 — que es lo que hace
     * visible que el contrato con el 03 siempre fue asincronico.
     */
    @Transactional
    public EvaluacionEntity puntuar(UUID profesorId, UUID detalleId, int obtenido) {
        EvaluacionDetalleEntity detalle = detalles.findById(detalleId)
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.DETALLE_INEXISTENTE, "detalleId"));

        EvaluacionEntity evaluacion = evaluaciones.findById(detalle.getEvaluacionId())
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.EVALUACION_INEXISTENTE, "detalleId"));

        // Que el cuestionario sea de este profesor. Mismo criterio que el banco:
        // si no es suyo, el mensaje es el de "no existe" y no confirmamos nada.
        contenidos.findByIdAndProfesorIdAndBajaLogicaIsNull(evaluacion.getContenidoId(), profesorId)
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.DETALLE_INEXISTENTE, "detalleId"));

        if (!detalle.estaPendiente()) {
            throw new ExcepcionDeNegocio(ClaveError.DETALLE_YA_CORREGIDO, "detalleId");
        }
        if (obtenido < 0 || obtenido > detalle.getPuntaje()) {
            throw new ExcepcionDeNegocio(ClaveError.PUNTAJE_FUERA_DE_RANGO, "obtenido");
        }

        detalle.puntuar(obtenido, profesorId, Instant.now());
        detalles.save(detalle);

        boolean quedaAlgo = detalles.findByEvaluacionIdOrderByOrdenAsc(evaluacion.getId()).stream()
                .anyMatch(EvaluacionDetalleEntity::estaPendiente);
        if (quedaAlgo) {
            return evaluacion;
        }
        return evaluacionService.cerrarYPublicar(evaluacion, "HUMANO");
    }

    private Pendiente armar(EvaluacionDetalleEntity d, EvaluacionEntity e) {
        ItemVersionEntity version = versiones.findById(d.getItemVersionId()).orElse(null);
        String enunciado = version == null ? null : version.getEnunciado();
        String consigna = null;
        String rubrica = null;

        if (version != null) {
            TipoDeItem tipo = items.findByIdAndBajaLogicaIsNull(version.getItemId())
                    .map(ItemEntity::getTipo).orElse(null);
            if (tipo == TipoDeItem.ABIERTA) {
                var payload = json.leerPayload(tipo, json.aNodo(version.getPayload()));
                if (payload instanceof ar.edu.utn.tup.g04.teoricosencuestas.teoricos
                        .dominio.payload.PayloadDeItem.Abierta a) {
                    consigna = a.consigna();
                }
                var criterio = json.leerCriterio(tipo, json.aNodo(version.getCriterio()));
                if (criterio instanceof CriterioDeCorreccion.Abierta c) {
                    rubrica = c.rubrica();
                }
            }
        }

        String texto = null;
        for (RespuestaEntity r : respuestas.findByEvaluacionId(e.getId())) {
            if (r.getItemVersionId().equals(d.getItemVersionId())) {
                var respuesta = json.leerRespuesta(TipoDeItem.ABIERTA, json.aNodo(r.getContenido()));
                if (respuesta instanceof RespuestaDeItem.Abierta a) {
                    texto = a.texto();
                }
            }
        }

        String cuestionario = contenidos.findByIdAndBajaLogicaIsNull(e.getContenidoId())
                .map(ContenidoEntity::getTitulo).orElse(null);

        return new Pendiente(d.getId(), e.getId(), e.getAlumnoId(), e.getIntento(),
                cuestionario, enunciado, consigna, rubrica, d.getPuntaje(), texto, e.getCreadaEn());
    }
}
