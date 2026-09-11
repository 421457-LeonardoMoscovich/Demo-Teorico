package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos.EventoSobre;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.eventos.PublicadorDeEventos;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.Corrector;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.Devolucion;
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

        // Se particiona por tipo: lo autocorregible se puntua ahora, lo que espera
        // a un humano (D-01) se guarda con obtenido NULL. La respuesta del alumno
        // se persiste igual en los dos casos —el que corrige despues necesita
        // leerla, y la estampa de version tiene que quedar fijada YA.
        int pendientes = 0;

        for (ContenidoItemEntity linea : porItem.values()) {
            ItemVersionEntity version = versionPorItem.get(linea.getItemId());
            RespuestaRecibida recibida = respuestaPorItem.get(linea.getItemId());
            ItemEntity item = items.findByIdAndBajaLogicaIsNull(linea.getItemId())
                    .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.ITEM_INEXISTENTE, "respuestas"));

            respuestas.save(new RespuestaEntity(UUID.randomUUID(), evaluacion.getId(),
                    version.getId(), json.escribir(recibida.contenido()), Instant.now()));

            Integer obtenido = null;
            if (item.getTipo().esAutocorregible()) {
                obtenido = corregirUno(item.getTipo(), version, recibida, linea.getPuntaje());
            } else {
                pendientes++;
            }

            detalles.save(new EvaluacionDetalleEntity(UUID.randomUUID(), evaluacion.getId(),
                    version.getId(), linea.getOrden(), linea.getPuntaje(), obtenido));
        }

        if (pendientes > 0) {
            // Ni nota ni evento. El Tema 03 se entera cuando HAY nota, y todavia
            // no la hay: la nota es del cuestionario entero o no es.
            evaluacion.esperarCorreccionHumana();
            evaluaciones.save(evaluacion);
            return evaluacion;
        }

        cerrarYPublicar(evaluacion, "AUTOMATICO");
        return evaluacion;
    }

    /**
     * Suma el desglose y cierra. Es el unico lugar donde una evaluacion pasa a
     * FINAL y el unico que publica, venga de un despacho todo-automatico o del
     * profesor poniendo el ultimo puntaje a mano.
     */
    @Transactional
    public EvaluacionEntity cerrarYPublicar(EvaluacionEntity evaluacion, String corrector) {
        int nota = 0;
        for (EvaluacionDetalleEntity d : detalles.findByEvaluacionIdOrderByOrdenAsc(evaluacion.getId())) {
            if (d.estaPendiente()) {
                throw new IllegalStateException(
                        "No se puede cerrar una evaluacion con items sin corregir: " + evaluacion.getId());
            }
            nota += d.getObtenido();
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

    /**
     * `obtenido` en null = todavia lo espera un humano.
     *
     * `payload` es el de LA VERSION QUE EL ALUMNO VIO, no el vigente. Va para que
     * el front pueda mostrar "Contestaste: Paris" en vez de "Contestaste: a" —las
     * respuestas viajan por id, y un id no le dice nada a nadie. Sacarlo de la
     * estampa y no del item de hoy es lo que hace que el desglose siga siendo
     * legible despues de que el profesor edite la pregunta (CI-13).
     *
     * No lleva el criterio: con reintentos ilimitados eso convierte el reintento
     * en copiar.
     *
     * `devolucion` (CI-58) SI viaja, y no contradice lo anterior porque viene
     * recortada: solo el texto general y el de las opciones que este alumno
     * marco. La devolucion de una opcion que no eligio diria si esa opcion era
     * la correcta, que es el criterio contado de otra forma.
     */
    public record Detalle(UUID itemVersionId, int orden, String enunciado, JsonNode payload,
                          int puntaje, Integer obtenido, JsonNode respuesta,
                          Devolucion devolucion) {}

    /**
     * Todo lo que este alumno entrego, lo mas reciente primero.
     *
     * CI-44 dice que se guarda una correccion por entrega y NUNCA se pisa: cada
     * intento queda. Esta consulta es lo que hace que eso se pueda ver — y de
     * paso muestra el versionado, porque dos intentos del mismo desafio pueden
     * haberse corregido contra versiones distintas del mismo item.
     */
    @Transactional(readOnly = true)
    public List<EvaluacionEntity> historialDe(UUID alumnoId) {
        return evaluaciones.findByAlumnoIdOrderByCreadaEnDesc(alumnoId);
    }

    @Transactional(readOnly = true)
    public EvaluacionEntity porEntrega(UUID entregaId) {
        return evaluaciones.findByEntregaId(entregaId)
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.EVALUACION_INEXISTENTE, "entregaId"));
    }

    /**
     * El desglose se arma sobre la estampa: el enunciado que sale de aca es el
     * que el alumno vio, no el que el item tiene hoy.
     *
     * Y sale EN EL ORDEN EN QUE EL LO VIO. Como cada alumno recibe las preguntas
     * barajadas, mostrarle el resultado en el orden del profesor lo haria buscar
     * a mano cual era cual. La permutacion no esta guardada: se reconstruye con
     * la misma semilla, que es exactamente lo que la hace barata.
     */
    @Transactional(readOnly = true)
    public List<Detalle> desglose(EvaluacionEntity evaluacion) {
        UUID evaluacionId = evaluacion.getId();
        Map<UUID, String> respuestaPorVersion = new HashMap<>();
        for (RespuestaEntity r : respuestas.findByEvaluacionId(evaluacionId)) {
            respuestaPorVersion.put(r.getItemVersionId(), r.getContenido());
        }

        List<Detalle> resultado = new ArrayList<>();
        for (EvaluacionDetalleEntity d : detalles.findByEvaluacionIdOrderByOrdenAsc(evaluacionId)) {
            ItemVersionEntity version = versiones.findById(d.getItemVersionId()).orElse(null);
            String contestado = respuestaPorVersion.get(d.getItemVersionId());
            JsonNode respuesta = contestado == null ? null : json.aNodo(contestado);
            resultado.add(new Detalle(
                    d.getItemVersionId(), d.getOrden(),
                    version == null ? null : version.getEnunciado(),
                    version == null ? null : json.aNodo(version.getPayload()),
                    d.getPuntaje(), d.getObtenido(),
                    respuesta,
                    devolucionDe(version, respuesta, d.getObtenido())));
        }

        // Se reordena como lo vio el alumno y se renumera: para el, la primera
        // que contesto es la 1.
        List<Detalle> comoLoVio = composicion.enOrdenPara(
                resultado, evaluacion.getContenidoId(), evaluacion.getAlumnoId());
        List<Detalle> numerado = new ArrayList<>();
        for (int i = 0; i < comoLoVio.size(); i++) {
            Detalle d = comoLoVio.get(i);
            numerado.add(new Detalle(d.itemVersionId(), i + 1, d.enunciado(), d.payload(),
                    d.puntaje(), d.obtenido(), d.respuesta(), d.devolucion()));
        }
        return numerado;
    }

    /**
     * La devolucion de un item, lista para mostrar (CI-58).
     *
     * Dos recortes, y los dos son de fondo:
     *
     * <ul>
     *   <li><b>solo si el item ya esta corregido</b> — mientras `obtenido` sea
     *       null el item espera a un humano, y explicarle al alumno por que su
     *       respuesta esta bien antes de que nadie la haya leido no tiene
     *       sentido;</li>
     *   <li><b>solo las opciones que marco</b> — lo hace {@code paraLoMarcado},
     *       y es lo que impide que la devolucion se convierta en la clave de
     *       correccion contada con otras palabras.</li>
     * </ul>
     */
    private Devolucion devolucionDe(ItemVersionEntity version, JsonNode respuesta,
                                    Integer obtenido) {
        if (version == null || version.getDevolucion() == null || obtenido == null) {
            return null;
        }
        Devolucion cruda = json.leerDevolucion(json.aNodo(version.getDevolucion()));
        Devolucion recortada = new Devolucion(cruda.general(), cruda.paraLoMarcado(marcadas(respuesta)));
        return recortada.vacia() ? null : recortada;
    }

    /**
     * Lo que el alumno marco, leido del jsonb de la respuesta sin mirar el tipo.
     *
     * Los tipos que no tienen `seleccionadas` devuelven la lista vacia, y con
     * eso la devolucion por opcion desaparece sola: no hace falta preguntar por
     * el tipo para saber que en un V/F no hay opciones que comentar.
     */
    private List<String> marcadas(JsonNode respuesta) {
        if (respuesta == null || !respuesta.hasNonNull("seleccionadas")) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (JsonNode n : respuesta.get("seleccionadas")) {
            ids.add(n.asText());
        }
        return ids;
    }
}
