package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Traduce entre el jsonb de la base y los records del dominio.
 *
 * La clase que hay que construir la dice el `tipo` del item, no un
 * discriminador adentro del JSON: el tipo es inmutable entre versiones, asi que
 * es una fuente mas confiable que un campo que alguien puede olvidarse de poner.
 */
@Component
public class MapeadorJson {

    private final ObjectMapper mapper;

    public MapeadorJson(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String escribir(Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar " + valor, e);
        }
    }

    public JsonNode aNodo(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new ExcepcionDeNegocio(ClaveError.PAYLOAD_INVALIDO, "payload");
        }
    }

    public PayloadDeItem leerPayload(TipoDeItem tipo, JsonNode json) {
        Class<? extends PayloadDeItem> clase = switch (tipo) {
            case OPCION_MULTIPLE -> PayloadDeItem.OpcionMultiple.class;
            case VERDADERO_FALSO -> PayloadDeItem.VerdaderoFalso.class;
            case EMPAREJAR       -> PayloadDeItem.Emparejar.class;
            case ORDENAR         -> PayloadDeItem.Ordenar.class;
            case ABIERTA         -> PayloadDeItem.Abierta.class;
            case CONVERSACION    -> PayloadDeItem.Conversacion.class;
            case DEBATE          -> PayloadDeItem.Debate.class;
        };
        return convertir(json, clase, "payload");
    }

    public CriterioDeCorreccion leerCriterio(TipoDeItem tipo, JsonNode json) {
        Class<? extends CriterioDeCorreccion> clase = switch (tipo) {
            case OPCION_MULTIPLE -> CriterioDeCorreccion.OpcionMultiple.class;
            case VERDADERO_FALSO -> CriterioDeCorreccion.VerdaderoFalso.class;
            case EMPAREJAR       -> CriterioDeCorreccion.Emparejar.class;
            case ORDENAR         -> CriterioDeCorreccion.Ordenar.class;
            case ABIERTA         -> CriterioDeCorreccion.Abierta.class;
            case CONVERSACION, DEBATE ->
                    throw new ExcepcionDeNegocio(ClaveError.TIPO_DIFERIDO, "tipo");
        };
        return convertir(json, clase, "criterio");
    }

    /**
     * La devolucion no depende del tipo: una sola forma para los cinco. Por eso
     * no hay switch aca, a diferencia de payload, criterio y respuesta.
     */
    public Devolucion leerDevolucion(JsonNode json) {
        return convertir(json, Devolucion.class, "devolucion");
    }

    public RespuestaDeItem leerRespuesta(TipoDeItem tipo, JsonNode json) {
        Class<? extends RespuestaDeItem> clase = switch (tipo) {
            case OPCION_MULTIPLE -> RespuestaDeItem.OpcionMultiple.class;
            case VERDADERO_FALSO -> RespuestaDeItem.VerdaderoFalso.class;
            case EMPAREJAR       -> RespuestaDeItem.Emparejar.class;
            case ORDENAR         -> RespuestaDeItem.Ordenar.class;
            case ABIERTA         -> RespuestaDeItem.Abierta.class;
            case CONVERSACION, DEBATE ->
                    throw new ExcepcionDeNegocio(ClaveError.TIPO_DIFERIDO, "tipo");
        };
        return convertir(json, clase, "respuesta");
    }

    private <T> T convertir(JsonNode json, Class<T> clase, String campo) {
        if (json == null || json.isNull()) {
            throw new ExcepcionDeNegocio(ClaveError.PAYLOAD_INVALIDO, campo);
        }
        try {
            return mapper.treeToValue(json, clase);
        } catch (Exception e) {
            throw new ExcepcionDeNegocio(ClaveError.PAYLOAD_INVALIDO, campo);
        }
    }
}
