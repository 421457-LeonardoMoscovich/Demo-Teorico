package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.ProveedorDeIdentidadFalso;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.EstadoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Siembra un banco de items para la demo.
 *
 * Existe por un motivo muy concreto: cargar preguntas a mano delante de la
 * catedra es la forma mas facil de que la demo se caiga. Con el banco ya
 * poblado, el recorrido empieza donde tiene que empezar — en el curso — y no
 * en un formulario vacio.
 *
 * Tres cuidados:
 *  - corre SOLO con `app.demo.sembrar=true`, que los tests ponen en false;
 *  - siembra solo si el banco esta vacio, asi no duplica en cada arranque;
 *  - los items son de la profesora del adaptador falso, porque el banco es de
 *    su dueno (RF-USR-07) y no existe un banco "del sistema".
 *
 * Se borra entero cuando la alfa deje de ser una demo.
 */
@Component
@ConditionalOnProperty(name = "app.demo.sembrar", havingValue = "true")
public class SembradorDeDemo {

    private static final Logger log = LoggerFactory.getLogger(SembradorDeDemo.class);

    private final BancoDeItemsService banco;
    private final ObjectMapper json;

    public SembradorDeDemo(BancoDeItemsService banco, ObjectMapper json) {
        this.banco = banco;
        this.json = json;
    }

    private record Semilla(TipoDeItem tipo, String enunciado, String payload, String criterio,
                           String devolucion) {

        /** La mayoria de las semillas no lleva devolucion. */
        Semilla(TipoDeItem tipo, String enunciado, String payload, String criterio) {
            this(tipo, enunciado, payload, criterio, null);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sembrar() {
        var profesorId = ProveedorDeIdentidadFalso.ID_PROFESOR;

        if (!banco.listar(profesorId, null).isEmpty()) {
            log.info("[demo] el banco ya tiene items, no se siembra nada");
            return;
        }

        for (Semilla s : SEMILLAS) {
            banco.crear(profesorId, s.tipo(), new BancoDeItemsService.Contenido(
                    s.enunciado(), leer(s.payload()), leer(s.criterio()),
                    s.devolucion() == null ? null : leer(s.devolucion()),
                    // El banco sembrado nace listo: es material terminado.
                    EstadoDeItem.LISTO));
        }
        log.info("[demo] sembrados {} items en el banco de la profesora", SEMILLAS.size());
    }

    private JsonNode leer(String texto) {
        try {
            return json.readTree(texto);
        } catch (Exception e) {
            throw new IllegalStateException("Semilla de demo mal armada: " + texto, e);
        }
    }

    /** Contenido real de la materia: en una demo, el relleno se nota. */
    private static final List<Semilla> SEMILLAS = List.of(

            new Semilla(TipoDeItem.OPCION_MULTIPLE,
                    "¿Cuál de las siguientes NO es una característica de la arquitectura en capas?",
                    """
                    {"opciones":[
                      {"id":"a","texto":"Cada capa se comunica solo con la inmediata inferior"},
                      {"id":"b","texto":"Las capas superiores conocen los detalles de implementación de las inferiores"},
                      {"id":"c","texto":"Una capa puede reemplazarse sin afectar a las demás si respeta el contrato"},
                      {"id":"d","texto":"La dependencia va siempre en una sola dirección"}],
                     "multiple":false}
                    """,
                    """
                    {"correctas":["b"]}
                    """),

            // La unica semilla con puntaje parcial (CI-55) y retroalimentacion
            // (CI-58). Es a proposito que sea una sola: en la demo se compara
            // contra las otras, que siguen siendo todo-o-nada y mudas.
            new Semilla(TipoDeItem.OPCION_MULTIPLE,
                    "¿Qué problemas resuelve un API Gateway en una arquitectura de microservicios?",
                    """
                    {"opciones":[
                      {"id":"a","texto":"Da un único punto de entrada a los clientes"},
                      {"id":"b","texto":"Centraliza autenticación y limitación de tasa"},
                      {"id":"c","texto":"Garantiza la consistencia transaccional entre servicios"},
                      {"id":"d","texto":"Evita que el cliente tenga que conocer la topología interna"}],
                     "multiple":true}
                    """,
                    """
                    {"correctas":["a","b","d"],
                     "pesos":[
                       {"id":"a","porcentaje":34},
                       {"id":"b","porcentaje":33},
                       {"id":"d","porcentaje":33},
                       {"id":"c","porcentaje":-34}]}
                    """,
                    """
                    {"general":"El gateway resuelve problemas de BORDE: entrada, identidad y acoplamiento del cliente. Nada de lo que resuelve tiene que ver con la consistencia de los datos.",
                     "porOpcion":[
                       {"id":"c","texto":"Esta es la trampa de la pregunta. Un gateway enruta pedidos; no tiene forma de coordinar transacciones entre servicios que no comparten base. Para eso hacen falta sagas."},
                       {"id":"a","texto":"Sí: el cliente habla con una sola dirección en vez de con doce."},
                       {"id":"b","texto":"Sí, y es la razón más fuerte: si cada servicio validara el token por su cuenta, la regla viviría en doce lugares."},
                       {"id":"d","texto":"Sí: el cliente deja de romperse cuando un servicio se parte en dos."}]}
                    """),

            new Semilla(TipoDeItem.VERDADERO_FALSO,
                    "Base de datos por servicio",
                    """
                    {"afirmacion":"En un microservicio, la base de datos debe compartirse con los otros servicios del mismo dominio."}
                    """,
                    """
                    {"esVerdadero":false}
                    """),

            new Semilla(TipoDeItem.VERDADERO_FALSO,
                    "Idempotencia en el consumo de eventos",
                    """
                    {"afirmacion":"Un consumidor de eventos tiene que ser idempotente porque el mismo mensaje puede llegarle más de una vez."}
                    """,
                    """
                    {"esVerdadero":true}
                    """),

            new Semilla(TipoDeItem.EMPAREJAR,
                    "Emparejá cada patrón con el problema que resuelve.",
                    """
                    {"izquierda":[
                       {"id":"i1","texto":"Circuit breaker"},
                       {"id":"i2","texto":"Saga"},
                       {"id":"i3","texto":"Outbox"}],
                     "derecha":[
                       {"id":"d1","texto":"Transacción distribuida sin bloqueo global"},
                       {"id":"d2","texto":"Dejar de golpear un servicio que ya está caído"},
                       {"id":"d3","texto":"Publicar un evento y guardar el dato en la misma transacción"}]}
                    """,
                    """
                    {"pares":[["i1","d2"],["i2","d1"],["i3","d3"]]}
                    """),

            new Semilla(TipoDeItem.EMPAREJAR,
                    "Emparejá cada nivel de aislamiento con la anomalía que todavía permite.",
                    """
                    {"izquierda":[
                       {"id":"i1","texto":"Read committed"},
                       {"id":"i2","texto":"Repeatable read"},
                       {"id":"i3","texto":"Serializable"}],
                     "derecha":[
                       {"id":"d1","texto":"Ninguna de las tres"},
                       {"id":"d2","texto":"Lectura no repetible"},
                       {"id":"d3","texto":"Lectura fantasma"}]}
                    """,
                    """
                    {"pares":[["i1","d2"],["i2","d3"],["i3","d1"]]}
                    """),

            new Semilla(TipoDeItem.ORDENAR,
                    "Ordená las fases del ciclo de vida de una request que entra por el gateway.",
                    """
                    {"elementos":[
                       {"id":"e1","texto":"El gateway recibe la request"},
                       {"id":"e2","texto":"Valida el token con el servicio de identidad"},
                       {"id":"e3","texto":"Resuelve la instancia del servicio en el registro"},
                       {"id":"e4","texto":"Enruta la request al microservicio"},
                       {"id":"e5","texto":"Devuelve la respuesta al cliente"}]}
                    """,
                    """
                    {"secuencia":["e1","e2","e3","e4","e5"]}
                    """),

            new Semilla(TipoDeItem.ORDENAR,
                    "Ordená los pasos de una saga coreografiada que falla en el segundo servicio.",
                    """
                    {"elementos":[
                       {"id":"e1","texto":"El servicio A confirma su paso y publica el evento"},
                       {"id":"e2","texto":"El servicio B consume el evento y falla"},
                       {"id":"e3","texto":"El servicio B publica el evento de fallo"},
                       {"id":"e4","texto":"El servicio A ejecuta su compensación"}]}
                    """,
                    """
                    {"secuencia":["e1","e2","e3","e4"]}
                    """),

            // Las dos abiertas son las que obligan a que exista la cola: no las
            // puede puntuar nadie mas que la profesora. La rubrica va con la
            // clave de correccion y NUNCA sale por la vista del alumno.
            new Semilla(TipoDeItem.ABIERTA,
                    "Explicá por qué un microservicio no debería leer la base de datos de otro.",
                    """
                    {"consigna":"Desarrollá en no más de 200 palabras. Mencioná al menos una consecuencia concreta sobre el despliegue.",
                     "extensionMaxima":200}
                    """,
                    """
                    {"rubrica":"Completo (todo el puntaje): nombra el acoplamiento de esquema y una consecuencia sobre el despliegue independiente. Parcial (mitad): nombra el acoplamiento pero no la consecuencia. Nulo: describe la separación sin explicar por qué importa."}
                    """),

            new Semilla(TipoDeItem.ABIERTA,
                    "¿Cuándo elegirías coreografía por sobre orquestación? Justificá con un caso.",
                    """
                    {"consigna":"Desarrollá en no más de 150 palabras. Tiene que haber un caso concreto, no solo la definición.",
                     "extensionMaxima":150}
                    """,
                    """
                    {"rubrica":"Completo: distingue los dos estilos Y trae un caso donde el acoplamiento del orquestador sería el problema. Parcial: distingue los estilos sin caso, o trae un caso que no discrimina. Nulo: repite las definiciones."}
                    """));
}
