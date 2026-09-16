package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoItemEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoItemRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoReglaEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoReglaRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ContenidoRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemVersionEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.BarajadorDeterministico;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.Normalizacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /** El alumno puede ir y volver por las consignas sin contestarlas. */
    public static final String NAVEGACION_LIBRE = "LIBRE";
    /** Una vez que paso a la siguiente, no vuelve. Avanzar sin contestar SI se permite. */
    public static final String NAVEGACION_SECUENCIAL = "SECUENCIAL";
    private static final int TOTAL_PORCENTUAL = 100;

    private final ContenidoRepository contenidos;
    private final ContenidoItemRepository lineas;
    private final ContenidoReglaRepository reglas;
    private final BancoDeItemsService banco;

    /**
     * Interruptor del barajado. Esta encendido: es una funcion del producto y no
     * un experimento. Existe la propiedad igual porque una funcion que no se
     * puede apagar es una funcion que no se puede diagnosticar.
     */
    private final boolean barajar;

    public ComposicionService(ContenidoRepository contenidos, ContenidoItemRepository lineas,
                              ContenidoReglaRepository reglas, BancoDeItemsService banco,
                              @Value("${app.barajado.habilitado:true}") boolean barajar) {
        this.contenidos = contenidos;
        this.lineas = lineas;
        this.reglas = reglas;
        this.banco = banco;
        this.barajar = barajar;
    }

    public record Linea(UUID itemId, int orden, int puntaje) {}

    /**
     * "N al azar de esta etiqueta, cada una vale esto." Lo que NO es: una
     * tirada que se resuelve al armar. Cada alumno recibe su propio subconjunto
     * y se deriva al leer (V10).
     */
    public record Regla(String etiqueta, int cuantos, int puntaje) {

        /** Lo que aporta al total, sin saber quien pregunta. */
        public int puntajeTotal() { return cuantos * puntaje; }
    }

    /** Un item resuelto a su version VIGENTE. La resolucion se hace en cada lectura (CI-13). */
    public record ItemResuelto(ItemEntity item, ItemVersionEntity version, int orden, int puntaje) {}

    @Transactional
    public ContenidoEntity componer(UUID profesorId, UUID cursoCohorteId, String titulo,
                                    String escala, String navegacion, List<Linea> pedidas,
                                    Regla regla) {
        Regla normalizada = normalizar(regla);
        validar(profesorId, escala, navegacion, pedidas, normalizada);

        ContenidoEntity contenido = contenidos.save(new ContenidoEntity(
                UUID.randomUUID(), profesorId, cursoCohorteId, titulo, escala,
                navegacion, Instant.now()));

        for (Linea linea : pedidas) {
            lineas.save(new ContenidoItemEntity(
                    contenido.getId(), linea.itemId(), linea.orden(), linea.puntaje()));
        }
        if (normalizada != null) {
            reglas.save(new ContenidoReglaEntity(contenido.getId(), normalizada.etiqueta(),
                    normalizada.cuantos(), normalizada.puntaje()));
        }
        return contenido;
    }

    /** La etiqueta viaja como la escribio el profesor; se guarda normalizada. */
    private Regla normalizar(Regla regla) {
        if (regla == null) {
            return null;
        }
        return new Regla(Normalizacion.etiqueta(regla.etiqueta()), regla.cuantos(), regla.puntaje());
    }

    private void validar(UUID profesorId, String escala, String navegacion, List<Linea> pedidas,
                         Regla regla) {
        // Se valida aca y no con una constraint en la base: el mensaje de error
        // tiene que decir QUE campo esta mal, y una violacion de CHECK llega sin
        // esa informacion.
        if (!NAVEGACION_LIBRE.equals(navegacion) && !NAVEGACION_SECUENCIAL.equals(navegacion)) {
            throw new ExcepcionDeNegocio(ClaveError.NAVEGACION_INVALIDA, "navegacion");
        }

        // Sin items Y sin regla no hay cuestionario. Con regla sola si lo hay:
        // "cinco al azar de microservicios" es un cuestionario completo.
        if ((pedidas == null || pedidas.isEmpty()) && regla == null) {
            throw new ExcepcionDeNegocio(ClaveError.CONTENIDO_SIN_ITEMS, "items");
        }
        if (pedidas == null) {
            pedidas = List.of();
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
            // Que el item este LISTO, ademas de existir (CI-59). Es la unica
            // barrera: una vez compuesto, el borrador ya no puede volver.
            if (!banco.exigirPropio(profesorId, linea.itemId()).getEstado().sePuedeComponer()) {
                throw new ExcepcionDeNegocio(ClaveError.ITEM_EN_BORRADOR, "items[" + i + "].itemId");
            }
            suma += linea.puntaje();
        }

        for (int esperado = 1; esperado <= pedidas.size(); esperado++) {
            if (!ordenes.contains(esperado)) {
                throw new ExcepcionDeNegocio(ClaveError.ORDEN_NO_CONSECUTIVO, "items");
            }
        }

        if (regla != null) {
            validarRegla(profesorId, regla, vistos);
            suma += regla.puntajeTotal();
        }

        // La regla de los 100 vive bajo bandera, no como invariante del dominio:
        // el desafio de recuperacion de vida (RF-REC-04) usa otras escalas.
        //
        // Con sorteo, la suma incluye lo que la regla APORTARA: cuantos x
        // puntaje. Es la unica forma de saber sobre cuanto rinde el alumno
        // antes de saber quien es, y es tambien por que el puntaje de las
        // sorteadas es uniforme.
        if (ESCALA_PORCENTUAL.equals(escala) && suma != TOTAL_PORCENTUAL) {
            throw new ExcepcionDeNegocio(ClaveError.PESOS_NO_SUMAN_100, "items");
        }

        // CI-47 NO se valida aca, y eso es una conclusion, no un olvido.
        //
        // La regla es "un desafio con reintentos ilimitados no admite items de
        // correccion humana". Estuvo anotada como TODO en este metodo y era el
        // lugar equivocado: por CI-03 esta composicion ocurre ANTES de que el
        // desafio exista, asi que aca no hay reintentos que mirar, y pedirselos
        // a quien llama seria hacer que el Tema 04 conozca un concepto que es
        // del 03. La consecuencia que el 03 necesita ya viaja en la ficha como
        // `correccion: DIFERIDA` (CI-07), y es el 03 quien rechaza.
    }

    private void validarRegla(UUID profesorId, Regla regla, Set<UUID> fijos) {
        if (regla.etiqueta() == null || regla.etiqueta().isBlank()) {
            throw new ExcepcionDeNegocio(ClaveError.ETIQUETA_VACIA, "regla.etiqueta");
        }
        if (regla.cuantos() <= 0) {
            throw new ExcepcionDeNegocio(ClaveError.SORTEO_SIN_PREGUNTAS, "regla.cuantos");
        }
        if (regla.puntaje() <= 0) {
            throw new ExcepcionDeNegocio(ClaveError.PUNTAJE_NO_POSITIVO, "regla.puntaje");
        }

        // Que HOY alcance. No garantiza que alcance manana —la poblacion es
        // viva y el profesor puede dar de baja preguntas—, pero publicar un
        // cuestionario que ya nace roto no tiene defensa posible.
        int disponibles = candidatos(profesorId, regla.etiqueta(), fijos).size();
        if (disponibles < regla.cuantos()) {
            throw new ExcepcionDeNegocio(ClaveError.SORTEO_SIN_CANDIDATOS, "regla.cuantos");
        }
    }

    /**
     * La poblacion del sorteo, HOY: los items LISTOS de este profesor con esa
     * etiqueta, menos los que ya entran fijos al cuestionario —que una pregunta
     * aparezca dos veces en el mismo examen es un error visible—.
     *
     * Ordenada por id y no por tipo: el orden tiene que ser ESTABLE o el sorteo
     * deja de ser reproducible entre dos lecturas.
     */
    @Transactional(readOnly = true)
    public List<ItemEntity> candidatos(UUID profesorId, String etiqueta, Set<UUID> excluidos) {
        return banco.listar(profesorId, null, etiqueta).stream()
                .filter(i -> i.getEstado().sePuedeComponer())
                .filter(i -> !excluidos.contains(i.getId()))
                .sorted(Comparator.comparing(i -> i.getId().toString()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ContenidoReglaEntity> reglaDe(UUID contenidoId) {
        return reglas.findByContenidoId(contenidoId);
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

    /** El titulo, o null si el contenido se dio de baja. Para listas, no para contratos. */
    @Transactional(readOnly = true)
    public String tituloDe(UUID contenidoId) {
        return contenidos.findByIdAndBajaLogicaIsNull(contenidoId)
                .map(ContenidoEntity::getTitulo)
                .orElse(null);
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

    /**
     * El orden en que ve las preguntas ESTE alumno.
     *
     * Vive aca y no en el controlador porque la misma permutacion hay que poder
     * reconstruirla al armar el desglose: si el alumno contesto la que para el
     * era la 3, el resultado tiene que decirle 3. Es una funcion pura de sus dos
     * argumentos, asi que reconstruirla es gratis y no hay nada que guardar.
     */
    public <T> List<T> enOrdenPara(List<T> enOrdenDelProfesor, UUID contenidoId, UUID alumnoId) {
        if (!barajar) {
            return enOrdenDelProfesor;
        }
        return BarajadorDeterministico.barajar(enOrdenDelProfesor, contenidoId, alumnoId);
    }

    /**
     * El cuestionario COMO LO RECIBE ESTE ALUMNO: las fijas, mas su sorteo.
     *
     * Nada de esto se guarda. El subconjunto se deriva de (contenidoId,
     * alumnoId) igual que el orden, asi que recargar devuelve lo mismo y
     * reconstruirlo al corregir tambien: la lectura sigue sin estado (CI-19).
     *
     * El orden que se devuelve es el BASE —fijas por su orden, sorteadas
     * despues—, no el que va a ver el alumno: barajarlo es responsabilidad de
     * {@link #enOrdenPara}, y tiene que seguir siendo un paso aparte para que
     * el desglose pueda reconstruir la misma permutacion sobre los detalles ya
     * guardados.
     */
    @Transactional(readOnly = true)
    public List<ItemResuelto> resolverPara(UUID contenidoId, UUID alumnoId) {
        List<ItemResuelto> resueltos = new ArrayList<>(resolver(contenidoId));

        ContenidoReglaEntity regla = reglas.findByContenidoId(contenidoId).orElse(null);
        if (regla == null) {
            return resueltos;
        }

        ContenidoEntity contenido = exigir(contenidoId);
        Set<UUID> fijos = resueltos.stream().map(r -> r.item().getId()).collect(Collectors.toSet());
        List<ItemEntity> poblacion = candidatos(contenido.getProfesorId(), regla.getEtiqueta(), fijos);

        // La poblacion se achico despues de publicar: el profesor dio de baja
        // preguntas o las volvio a borrador. Se falla en vez de servir menos
        // preguntas, y no es rigidez: servir cuatro de cinco dejaria al alumno
        // rindiendo sobre 80 sin que nadie se lo haya dicho, y la nota
        // publicada al Tema 03 seria incomparable con la de sus companeros.
        if (poblacion.size() < regla.getCuantos()) {
            throw new ExcepcionDeNegocio(ClaveError.SORTEO_SIN_CANDIDATOS, "contenidoId");
        }

        List<ItemEntity> sorteadas = BarajadorDeterministico.elegir(
                poblacion, contenidoId, alumnoId, regla.getCuantos());

        int orden = resueltos.size();
        for (ItemEntity item : sorteadas) {
            orden++;
            resueltos.add(new ItemResuelto(item, banco.versionVigente(item), orden,
                    regla.getPuntaje()));
        }
        return resueltos;
    }

    /**
     * El puntaje total del cuestionario SIN saber quien pregunta: lo que suman
     * las fijas, mas lo que la regla va a aportar. Es el numero que viaja en la
     * ficha y el que se le muestra al alumno arriba de todo.
     */
    @Transactional(readOnly = true)
    public int puntajeTotalDe(UUID contenidoId) {
        return puntajeTotal(resolver(contenidoId))
                + reglas.findByContenidoId(contenidoId)
                        .map(ContenidoReglaEntity::puntajeTotal).orElse(0);
    }

    /** Cuantas preguntas recibe cualquier alumno: las fijas mas las sorteadas. */
    @Transactional(readOnly = true)
    public int cuantasPreguntasDe(UUID contenidoId) {
        return resolver(contenidoId).size()
                + reglas.findByContenidoId(contenidoId)
                        .map(ContenidoReglaEntity::getCuantos).orElse(0);
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
    /**
     * El modo de correccion de un cuestionario con sorteo, y es el punto mas
     * delicado del sorteo entero.
     *
     * La ficha sale UNA vez, al componer, y el Tema 03 no puede recibir una
     * respuesta distinta por alumno: si a uno le toca una pregunta abierta y a
     * otro no, el modo no puede ser "depende". Asi que se mira TODA la
     * poblacion candidata y no el sorteo de nadie: alcanza que UNA de las
     * preguntas que podrian salir espere a un humano para que el cuestionario
     * entero sea DIFERIDO.
     *
     * Lo que esto no puede prometer: la poblacion es viva. Si despues de
     * publicar se agrega una pregunta abierta a esa etiqueta, la ficha que el
     * 03 ya guardo dice INMEDIATA y el alumno al que le toque va a quedar
     * esperando correccion igual. El sistema no se rompe —el evento sale cuando
     * la nota existe, que es lo que CorreccionHumanaIT fija—, pero el 03
     * mostro una promesa que dejo de ser cierta.
     */
    @Transactional(readOnly = true)
    public String modoDeCorreccionDe(UUID contenidoId) {
        List<ItemResuelto> fijas = resolver(contenidoId);
        if ("DIFERIDA".equals(modoDeCorreccion(fijas))) {
            return "DIFERIDA";
        }
        ContenidoReglaEntity regla = reglas.findByContenidoId(contenidoId).orElse(null);
        if (regla == null) {
            return "INMEDIATA";
        }
        ContenidoEntity contenido = exigir(contenidoId);
        Set<UUID> fijos = fijas.stream().map(r -> r.item().getId()).collect(Collectors.toSet());
        boolean todasAutomaticas = candidatos(contenido.getProfesorId(), regla.getEtiqueta(), fijos)
                .stream().allMatch(i -> i.getTipo().esAutocorregible());
        return todasAutomaticas ? "INMEDIATA" : "DIFERIDA";
    }

    /** El resumen que pinta el Tema 03, contando lo que la regla va a aportar. */
    @Transactional(readOnly = true)
    public String resumenDe(UUID contenidoId) {
        int cantidad = cuantasPreguntasDe(contenidoId);
        String preguntas = cantidad == 1 ? "1 pregunta" : cantidad + " preguntas";
        return preguntas + " · " + puntajeTotalDe(contenidoId) + " puntos";
    }

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
