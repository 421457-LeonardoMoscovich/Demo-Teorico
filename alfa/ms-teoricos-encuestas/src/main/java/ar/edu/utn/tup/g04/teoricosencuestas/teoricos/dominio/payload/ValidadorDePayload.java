package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * La coherencia entre payload y criterio es una REGLA DE DOMINIO, no una
 * anotacion de Bean Validation.
 *
 * Validar el payload solo en el DTO deja el criterio sin verificar contra el:
 * se puede crear un item cuyo criterio apunte a una opcion inexistente, y el
 * corrector lo puntua mal en silencio. Ese es el bug que esta clase existe para
 * hacer imposible.
 *
 * Cada rechazo nombra un campo NAVEGABLE (criterio.correctas[0]), porque el
 * front lo usa para marcar el input exacto.
 */
@Component
public class ValidadorDePayload {

    public void validar(TipoDeItem tipo, String enunciado,
                        PayloadDeItem payload, CriterioDeCorreccion criterio) {

        if (tipo.estaDiferido()) {
            throw new ExcepcionDeNegocio(ClaveError.TIPO_DIFERIDO, "tipo", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (!tipo.estaEnLaAlfa()) {
            throw new ExcepcionDeNegocio(ClaveError.TIPO_FUERA_DE_ALFA, "tipo", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (enunciado == null || enunciado.isBlank()) {
            throw new ExcepcionDeNegocio(ClaveError.ENUNCIADO_REQUERIDO, "enunciado");
        }
        if (criterio == null) {
            throw new ExcepcionDeNegocio(ClaveError.CRITERIO_REQUERIDO, "criterio");
        }

        switch (tipo) {
            case OPCION_MULTIPLE -> validarOpcionMultiple(
                    exigir(payload, PayloadDeItem.OpcionMultiple.class),
                    exigir(criterio, CriterioDeCorreccion.OpcionMultiple.class));
            case VERDADERO_FALSO -> validarVerdaderoFalso(
                    exigir(payload, PayloadDeItem.VerdaderoFalso.class),
                    exigir(criterio, CriterioDeCorreccion.VerdaderoFalso.class));
            case EMPAREJAR -> validarEmparejar(
                    exigir(payload, PayloadDeItem.Emparejar.class),
                    exigir(criterio, CriterioDeCorreccion.Emparejar.class));
            case ORDENAR -> validarOrdenar(
                    exigir(payload, PayloadDeItem.Ordenar.class),
                    exigir(criterio, CriterioDeCorreccion.Ordenar.class));
            default -> throw new ExcepcionDeNegocio(ClaveError.TIPO_DESCONOCIDO, "tipo");
        }
    }

    private void validarOpcionMultiple(PayloadDeItem.OpcionMultiple p,
                                       CriterioDeCorreccion.OpcionMultiple c) {
        List<PayloadDeItem.Opcion> opciones = p.opciones() == null ? List.of() : p.opciones();
        if (opciones.size() < 2) {
            throw new ExcepcionDeNegocio(ClaveError.OPCIONES_INSUFICIENTES, "payload.opciones");
        }
        Set<String> ids = idsUnicos(opciones, "payload.opciones");
        for (int i = 0; i < opciones.size(); i++) {
            String texto = opciones.get(i).texto();
            if (texto == null || texto.isBlank()) {
                throw new ExcepcionDeNegocio(ClaveError.PAYLOAD_INVALIDO,
                        "payload.opciones[" + i + "].texto");
            }
        }

        List<String> correctas = c.correctas() == null ? List.of() : c.correctas();
        if (correctas.isEmpty()) {
            throw new ExcepcionDeNegocio(ClaveError.SIN_OPCION_CORRECTA, "criterio.correctas");
        }
        for (int i = 0; i < correctas.size(); i++) {
            if (!ids.contains(correctas.get(i))) {
                throw new ExcepcionDeNegocio(ClaveError.CORRECTA_INEXISTENTE,
                        "criterio.correctas[" + i + "]");
            }
        }
        if (!p.multiple() && correctas.size() > 1) {
            throw new ExcepcionDeNegocio(ClaveError.UNICA_CORRECTA_ESPERADA, "criterio.correctas");
        }
    }

    private void validarVerdaderoFalso(PayloadDeItem.VerdaderoFalso p,
                                       CriterioDeCorreccion.VerdaderoFalso c) {
        if (p.afirmacion() == null || p.afirmacion().isBlank()) {
            throw new ExcepcionDeNegocio(ClaveError.AFIRMACION_REQUERIDA, "payload.afirmacion");
        }
        if (c.esVerdadero() == null) {
            throw new ExcepcionDeNegocio(ClaveError.VALOR_VERDADERO_REQUERIDO, "criterio.esVerdadero");
        }
    }

    private void validarEmparejar(PayloadDeItem.Emparejar p, CriterioDeCorreccion.Emparejar c) {
        List<PayloadDeItem.Opcion> izq = p.izquierda() == null ? List.of() : p.izquierda();
        List<PayloadDeItem.Opcion> der = p.derecha() == null ? List.of() : p.derecha();
        if (izq.size() < 2) {
            throw new ExcepcionDeNegocio(ClaveError.ELEMENTOS_INSUFICIENTES, "payload.izquierda");
        }
        if (der.size() < 2) {
            throw new ExcepcionDeNegocio(ClaveError.ELEMENTOS_INSUFICIENTES, "payload.derecha");
        }
        Set<String> idsIzq = idsUnicos(izq, "payload.izquierda");
        Set<String> idsDer = idsUnicos(der, "payload.derecha");

        List<List<String>> pares = c.pares() == null ? List.of() : c.pares();
        Set<String> izquierdasUsadas = new HashSet<>();
        for (int i = 0; i < pares.size(); i++) {
            List<String> par = pares.get(i);
            if (par == null || par.size() != 2) {
                throw new ExcepcionDeNegocio(ClaveError.PAYLOAD_INVALIDO, "criterio.pares[" + i + "]");
            }
            if (!idsIzq.contains(par.get(0))) {
                throw new ExcepcionDeNegocio(ClaveError.PAR_INEXISTENTE, "criterio.pares[" + i + "][0]");
            }
            if (!idsDer.contains(par.get(1))) {
                throw new ExcepcionDeNegocio(ClaveError.PAR_INEXISTENTE, "criterio.pares[" + i + "][1]");
            }
            if (!izquierdasUsadas.add(par.get(0))) {
                throw new ExcepcionDeNegocio(ClaveError.PAR_IZQUIERDA_REPETIDA,
                        "criterio.pares[" + i + "][0]");
            }
        }
        if (izquierdasUsadas.size() != idsIzq.size()) {
            throw new ExcepcionDeNegocio(ClaveError.PARES_INCOMPLETOS, "criterio.pares");
        }
    }

    private void validarOrdenar(PayloadDeItem.Ordenar p, CriterioDeCorreccion.Ordenar c) {
        List<PayloadDeItem.Opcion> elementos = p.elementos() == null ? List.of() : p.elementos();
        if (elementos.size() < 2) {
            throw new ExcepcionDeNegocio(ClaveError.ELEMENTOS_INSUFICIENTES, "payload.elementos");
        }
        Set<String> ids = idsUnicos(elementos, "payload.elementos");

        List<String> secuencia = c.secuencia() == null ? List.of() : c.secuencia();
        // Permutacion exacta: ni repetido ni faltante. Comparar el conjunto con el
        // de ids y ademas el tamanio lo cubre entero: el conjunto atrapa al que
        // falta y al que sobra, el tamanio atrapa al repetido.
        Set<String> enLaSecuencia = new HashSet<>(secuencia);
        if (secuencia.size() != elementos.size() || !enLaSecuencia.equals(ids)) {
            throw new ExcepcionDeNegocio(ClaveError.SECUENCIA_NO_ES_PERMUTACION, "criterio.secuencia");
        }
    }

    private Set<String> idsUnicos(List<PayloadDeItem.Opcion> opciones, String campo) {
        Set<String> ids = new HashSet<>();
        List<String> repetidos = new ArrayList<>();
        for (int i = 0; i < opciones.size(); i++) {
            String id = opciones.get(i).id();
            if (id == null || id.isBlank()) {
                throw new ExcepcionDeNegocio(ClaveError.PAYLOAD_INVALIDO, campo + "[" + i + "].id");
            }
            if (!ids.add(id)) {
                repetidos.add(id);
            }
        }
        if (!repetidos.isEmpty()) {
            throw new ExcepcionDeNegocio(ClaveError.ID_DUPLICADO, campo);
        }
        return ids;
    }

    @SuppressWarnings("unchecked")
    private <T> T exigir(Object valor, Class<T> clase) {
        if (!clase.isInstance(valor)) {
            throw new ExcepcionDeNegocio(ClaveError.PAYLOAD_INVALIDO, "payload");
        }
        return (T) valor;
    }
}
