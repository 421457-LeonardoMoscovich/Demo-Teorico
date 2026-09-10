package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoItemEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoItemRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemVersionEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Arma el cuestionario. Con CI-03 el que llama es EL FRONT, no el Tema 03:
 * primero se compone el contenido aca, que devuelve un contenidoId, y despues
 * el front crea el desafio en el 03 con la referencia ya formada. Por eso no
 * existe ningun contrato de composicion entre el 03 y nosotros.
 */
@Service
public class ComposicionService {

    public static final String ESCALA_PORCENTUAL = "PORCENTUAL";
    public static final String ESCALA_LIBRE = "LIBRE";
    private static final int TOTAL_PORCENTUAL = 100;

    private final ContenidoRepository contenidos;
    private final ContenidoItemRepository lineas;
    private final BancoDeItemsService banco;

    public ComposicionService(ContenidoRepository contenidos, ContenidoItemRepository lineas,
                              BancoDeItemsService banco) {
        this.contenidos = contenidos;
        this.lineas = lineas;
        this.banco = banco;
    }

    public record Linea(UUID itemId, int orden, int puntaje) {}

    /** Un item resuelto a su version VIGENTE. La resolucion se hace en cada lectura (CI-13). */
    public record ItemResuelto(ItemEntity item, ItemVersionEntity version, int orden, int puntaje) {}

    @Transactional
    public ContenidoEntity componer(UUID profesorId, UUID cursoCohorteId, String titulo,
                                    String escala, List<Linea> pedidas) {
        validar(profesorId, escala, pedidas);

        ContenidoEntity contenido = contenidos.save(new ContenidoEntity(
                UUID.randomUUID(), profesorId, cursoCohorteId, titulo, escala, Instant.now()));

        for (Linea linea : pedidas) {
            lineas.save(new ContenidoItemEntity(
                    contenido.getId(), linea.itemId(), linea.orden(), linea.puntaje()));
        }
        return contenido;
    }

    private void validar(UUID profesorId, String escala, List<Linea> pedidas) {
        if (pedidas == null || pedidas.isEmpty()) {
            throw new ExcepcionDeNegocio(ClaveError.CONTENIDO_SIN_ITEMS, "items");
        }

        Set<UUID> vistos = new HashSet<>();
        Set<Integer> ordenes = new HashSet<>();
        int suma = 0;

        for (int i = 0; i < pedidas.size(); i++) {
            Linea linea = pedidas.get(i);
            if (linea.puntaje() <= 0) {
                throw new ExcepcionDeNegocio(ClaveError.PUNTAJE_NO_POSITIVO, "items[" + i + "].puntaje");
            }
            if (!vistos.add(linea.itemId())) {
                throw new ExcepcionDeNegocio(ClaveError.ITEM_REPETIDO, "items[" + i + "].itemId");
            }
            if (!ordenes.add(linea.orden())) {
                throw new ExcepcionDeNegocio(ClaveError.ORDEN_NO_CONSECUTIVO, "items[" + i + "].orden");
            }
            // Que el item exista, este vigente y sea de este profesor. Si no lo es,
            // el mensaje es el mismo que si no existiera: no confirmamos items ajenos.
            banco.exigirPropio(profesorId, linea.itemId());
            suma += linea.puntaje();
        }

        for (int esperado = 1; esperado <= pedidas.size(); esperado++) {
            if (!ordenes.contains(esperado)) {
                throw new ExcepcionDeNegocio(ClaveError.ORDEN_NO_CONSECUTIVO, "items");
            }
        }

        // La regla de los 100 vive bajo bandera, no como invariante del dominio:
        // el desafio de recuperacion de vida (RF-REC-04) usa otras escalas.
        if (ESCALA_PORCENTUAL.equals(escala) && suma != TOTAL_PORCENTUAL) {
            throw new ExcepcionDeNegocio(ClaveError.PESOS_NO_SUMAN_100, "items");
        }

        // TODO CI-47: cuando entren los items de correccion humana, rechazar la
        // composicion si el desafio admite reintentos ilimitados. En la alfa no
        // hay items de correccion humana, asi que la regla no tiene que rechazar
        // nada todavia; el hueco queda marcado, no olvidado.
    }

    @Transactional(readOnly = true)
    public ContenidoEntity exigir(UUID contenidoId) {
        return contenidos.findByIdAndBajaLogicaIsNull(contenidoId)
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.CONTENIDO_INEXISTENTE, "contenidoId"));
    }

    @Transactional(readOnly = true)
    public ContenidoEntity exigirPropio(UUID profesorId, UUID contenidoId) {
        return contenidos.findByIdAndProfesorIdAndBajaLogicaIsNull(contenidoId, profesorId)
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.CONTENIDO_INEXISTENTE, "contenidoId"));
    }

    @Transactional(readOnly = true)
    public List<ContenidoEntity> listarDelProfesor(UUID profesorId) {
        return contenidos.findByProfesorIdAndBajaLogicaIsNullOrderByCreadoEnDesc(profesorId);
    }

    /**
     * Resuelve cada linea a la version vigente de su item, AHORA.
     * Dos alumnos que abren el mismo cuestionario en momentos distintos pueden
     * recibir versiones distintas, y eso es exactamente lo que RF-CUR-05 pide.
     */
    @Transactional(readOnly = true)
    public List<ItemResuelto> resolver(UUID contenidoId) {
        List<ItemResuelto> resueltos = new ArrayList<>();
        for (ContenidoItemEntity linea : lineas.findByClaveContenidoIdOrderByOrdenAsc(contenidoId)) {
            ItemEntity item = banco.exigirVigente(linea.getItemId());
            resueltos.add(new ItemResuelto(item, banco.versionVigente(item),
                    linea.getOrden(), linea.getPuntaje()));
        }
        return resueltos;
    }

    public int puntajeTotal(List<ItemResuelto> resueltos) {
        return resueltos.stream().mapToInt(ItemResuelto::puntaje).sum();
    }

    /**
     * CI-07: el modo de correccion es CALCULADO, no elegido. Si todos los items
     * se corrigen solos es INMEDIATA; alcanza uno que espere al profesor o a la
     * IA para que sea DIFERIDA. Al Tema 03 le llega solo esta consecuencia, no
     * el modo de cada item.
     */
    public String modoDeCorreccion(List<ItemResuelto> resueltos) {
        boolean todosAutomaticos = resueltos.stream()
                .allMatch(r -> r.item().getTipo().esAutocorregible());
        return todosAutomaticos ? "INMEDIATA" : "DIFERIDA";
    }

    /**
     * El resumen que el 03 PINTA EN PANTALLA y no interpreta. Existe para que
     * nadie termine pidiendo cantidadDeItems y puntajeTotal por separado, que
     * si serian conocimiento de contenido.
     */
    public String resumen(List<ItemResuelto> resueltos) {
        int cantidad = resueltos.size();
        String preguntas = cantidad == 1 ? "1 pregunta" : cantidad + " preguntas";
        return preguntas + " · " + puntajeTotal(resueltos) + " puntos";
    }
}
