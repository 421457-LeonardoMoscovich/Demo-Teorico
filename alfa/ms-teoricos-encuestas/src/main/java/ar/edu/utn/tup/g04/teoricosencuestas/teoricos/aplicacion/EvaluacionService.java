package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos.EventoSobre;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos.PublicadorDeEventos;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.Corrector;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.MapeadorJson;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoItemEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoItemRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.EvaluacionDetalleEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.EvaluacionDetalleRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.EvaluacionEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.EvaluacionRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemVersionEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemVersionRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.RespuestaEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.RespuestaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Recibe el despacho del Tema 03 (CI-22) y corrige.
 *
 * Dos reglas gobiernan este servicio:
 *
 *  - Idempotencia por entregaId (CI-25). Si el 03 reintenta el despacho no
 *    corregimos dos veces ni emitimos dos eventos. La garantia primaria es el
 *    UNIQUE de la base; el chequeo de aca solo evita el viaje.
 *
 *  - Se corrige por LA ESTAMPA de cada respuesta, no por la version vigente
 *    del item (CI-53). Si el profesor edito mientras el alumno contestaba, lo
 *    que vale es lo que el alumno vio. Corolario: no existe carrera entre el
 *    profesor editando y nosotros corrigiendo.
 */
@Service
public class EvaluacionService {

    public static final String TOPICO = "desafios.resultados";
    public static final String EVENTO = "TEORICO_CORREGIDO";

    private final EvaluacionRepository evaluaciones;
    private final EvaluacionDetalleRepository detalles;
    private final RespuestaRepository respuestas;
    private final ContenidoItemRepository lineas;
    private final ItemRepository items;
    private final ItemVersionRepository versiones;
    private final ComposicionService composicion;
    private final List<Corrector> correctores;
    private final PublicadorDeEventos publicador;
    private final MapeadorJson json;

    public EvaluacionService(EvaluacionRepository evaluaciones, EvaluacionDetalleRepository detalles,
                             RespuestaRepository respuestas, ContenidoItemRepository lineas,
                             ItemRepository items, ItemVersionRepository versiones,
                             ComposicionService composicion, List<Corrector> correctores,
                             PublicadorDeEventos publicador, MapeadorJson json) {
        this.evaluaciones = evaluaciones;
        this.detalles = detalles;
        this.respuestas = respuestas;
        this.lineas = lineas;
        this.items = items;
        this.versiones = versiones;
        this.composicion = composicion;
        this.correctores = correctores;
        this.publicador = publicador;
        this.json = json;
    }

    /** Una respuesta del alumno, ya estampada con la version que vio. */
    public record RespuestaRecibida(UUID itemVersionId, JsonNode contenido) {}

    public record Despacho(UUID entregaId, UUID desafioId, UUID alumnoId, UUID cursoCohorteId,
                           int intento, UUID contenidoId, List<RespuestaRecibida> respuestas) {}

    @Transactional
    public EvaluacionEntity despachar(Despacho despacho) {
        var yaExiste = evaluaciones.findByEntregaId(despacho.entregaId());
        if (yaExiste.isPresent()) {
            return yaExiste.get();
        }

        composicion.exigir(despacho.contenidoId());

        Map<UUID, ContenidoItemEntity> porItem = new LinkedHashMap<>();
        for (ContenidoItemEntity linea : lineas.findByClaveContenidoIdOrderByOrdenAsc(despacho.contenidoId())) {
            porItem.put(linea.getItemId(), linea);
        }

        Map<UUID, ItemVersionEntity> versionPorItem = new HashMap<>();
        Map<UUID, RespuestaRecibida> respuestaPorItem = new HashMap<>();
        Set<UUID> cubiertos = new HashSet<>();

        for (RespuestaRecibida recibida : despacho.respuestas()) {
            ItemVersionEntity version = versiones.findById(recibida.itemVersionId())
                    .orElseThrow(() -> new ExcepcionDeNegocio(
                            ClaveError.RESPUESTA_DE_ITEM_AJENO, "respuestas"));
            if (!porItem.containsKey(version.getItemId())) {
                throw new ExcepcionDeNegocio(ClaveError.RESPUESTA_DE_ITEM_AJENO, "respuestas");
            }
            if (!cubiertos.add(version.getItemId())) {
                throw new ExcepcionDeNegocio(ClaveError.RESPUESTA_DE_ITEM_AJENO, "respuestas");
            }
            versionPorItem.put(version.getItemId(), version);
            respuestaPorItem.put(version.getItemId(), recibida);
        }

        if (!cubiertos.equals(porItem.keySet())) {
            throw new ExcepcionDeNegocio(ClaveError.RESPUESTAS_INCOMPLETAS, "respuestas");
        }

        EvaluacionEntity evaluacion = evaluaciones.save(new EvaluacionEntity(
                UUID.randomUUID(), despacho.entregaId(), despacho.desafioId(), despacho.alumnoId(),
                despacho.cursoCohorteId(), despacho.intento(), despacho.contenidoId(), Instant.now()));

        int nota = 0;
        String corrector = "AUTOMATICO";

        for (ContenidoItemEntity linea : porItem.values()) {
            ItemVersionEntity version = versionPorItem.get(linea.getItemId());
            RespuestaRecibida recibida = respuestaPorItem.get(linea.getItemId());
            ItemEntity item = items.findByIdAndBajaLogicaIsNull(linea.getItemId())
                    .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.ITEM_INEXISTENTE, "respuestas"));

            respuestas.save(new RespuestaEntity(UUID.randomUUID(), evaluacion.getId(),
                    version.getId(), json.escribir(recibida.contenido()), Instant.now()));

            int obtenido = corregirUno(item.getTipo(), version, recibida, linea.getPuntaje());
            nota += obtenido;

            detalles.save(new EvaluacionDetalleEntity(UUID.randomUUID(), evaluacion.getId(),
                    version.getId(), linea.getOrden(), linea.getPuntaje(), obtenido));
        }

        evaluacion.cerrar(nota, corrector);
        evaluaciones.save(evaluacion);
        publicar(evaluacion);
        return evaluacion;
    }

    private int corregirUno(TipoDeItem tipo, ItemVersionEntity version,
                            RespuestaRecibida recibida, int puntaje) {
        Corrector corrector = correctores.stream()
                .filter(c -> c.atiende(tipo))
                .findFirst()
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.TIPO_FUERA_DE_ALFA, "tipo"));
        var criterio = json.leerCriterio(tipo, json.aNodo(version.getCriterio()));
        var respuesta = json.leerRespuesta(tipo, recibida.contenido());
        return corrector.corregir(tipo, criterio, respuesta, puntaje);
    }

    /**
     * CI-38, CI-39, CI-28: al Tema 03 le van la nota y el estado, y nada mas.
     * Salen corrector (no le cambia el XP), aprobado (el umbral no lo define
     * ningun documento del PRD, ver H-16) y el desglose por item (es
     * conocimiento de contenido, y va al front).
     *
     * CI-42: la clave de particion es desafioId + alumnoId, para que los dos
     * intentos de un mismo alumno no caigan en particiones distintas y el 03
     * termine quedandose con la nota vieja.
     */
    private void publicar(EvaluacionEntity e) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entregaId", e.getEntregaId());
        payload.put("desafioId", e.getDesafioId());
        payload.put("alumnoId", e.getAlumnoId());
        payload.put("cursoCohorteId", e.getCursoCohorteId());
        payload.put("intento", e.getIntento());
        payload.put("nota", e.getNota());
        payload.put("estado", e.getEstado());
        payload.put("revision", e.getRevision());
        publicador.publicar(TOPICO, e.getDesafioId() + ":" + e.getAlumnoId(),
                EventoSobre.de(EVENTO, payload));
    }

    public record Detalle(UUID itemVersionId, int orden, String enunciado,
                          int puntaje, int obtenido, JsonNode respuesta) {}

    @Transactional(readOnly = true)
    public EvaluacionEntity porEntrega(UUID entregaId) {
        return evaluaciones.findByEntregaId(entregaId)
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.EVALUACION_INEXISTENTE, "entregaId"));
    }

    /**
     * El desglose se arma sobre la estampa: el enunciado que sale de aca es el
     * que el alumno vio, no el que el item tiene hoy.
     */
    @Transactional(readOnly = true)
    public List<Detalle> desglose(UUID evaluacionId) {
        Map<UUID, String> respuestaPorVersion = new HashMap<>();
        for (RespuestaEntity r : respuestas.findByEvaluacionId(evaluacionId)) {
            respuestaPorVersion.put(r.getItemVersionId(), r.getContenido());
        }

        List<Detalle> resultado = new ArrayList<>();
        for (EvaluacionDetalleEntity d : detalles.findByEvaluacionIdOrderByOrdenAsc(evaluacionId)) {
            String enunciado = versiones.findById(d.getItemVersionId())
                    .map(ItemVersionEntity::getEnunciado)
                    .orElse(null);
            String contestado = respuestaPorVersion.get(d.getItemVersionId());
            resultado.add(new Detalle(d.getItemVersionId(), d.getOrden(), enunciado,
                    d.getPuntaje(), d.getObtenido(),
                    contestado == null ? null : json.aNodo(contestado)));
        }
        return resultado;
    }
}
