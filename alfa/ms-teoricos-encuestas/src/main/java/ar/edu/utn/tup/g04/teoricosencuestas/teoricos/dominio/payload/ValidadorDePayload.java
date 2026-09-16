package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.Normalizacion;
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
        validar(tipo, enunciado, payload, criterio, null);
    }

    public void validar(TipoDeItem tipo, String enunciado, PayloadDeItem payload,
                        CriterioDeCorreccion criterio, Devolucion devolucion) {

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
            case RESPUESTA_CORTA -> validarRespuestaCorta(
                    exigir(payload, PayloadDeItem.RespuestaCorta.class),
                    exigir(criterio, CriterioDeCorreccion.RespuestaCorta.class));
            case NUMERICA -> validarNumerica(
                    exigir(payload, PayloadDeItem.Numerica.class),
                    exigir(criterio, CriterioDeCorreccion.Numerica.class));
            case ABIERTA -> validarAbierta(
                    exigir(payload, PayloadDeItem.Abierta.class),
                    exigir(criterio, CriterioDeCorreccion.Abierta.class));
            default -> throw new ExcepcionDeNegocio(ClaveError.TIPO_DESCONOCIDO, "tipo");
        }

        if (devolucion != null && !devolucion.vacia()) {
            validarDevolucion(tipo, payload, devolucion);
        }
    }

    /**
     * La devolucion por opcion tiene que apuntar a opciones que existen (CI-58).
     *
     * Sin esto se puede guardar un texto atado a un id equivocado, y el efecto
     * es peor que no tener devolucion: el alumno no la ve nunca —se filtra por
     * lo que marco, y nunca marco un id inexistente— asi que el profesor cree
     * que escribio una devolucion que en la practica no existe. Falla en
     * silencio, que es la clase de bug que este validador existe para impedir.
     *
     * El texto general no se valida: cualquier texto sirve, y vacio es valido
     * (significa que el profesor solo escribio devoluciones por opcion).
     */
    private void validarDevolucion(TipoDeItem tipo, PayloadDeItem payload, Devolucion d) {
        List<Devolucion.PorOpcion> porOpcion =
                d.porOpcion() == null ? List.of() : d.porOpcion();
        if (porOpcion.isEmpty()) {
            return;
        }
        if (!(payload instanceof PayloadDeItem.OpcionMultiple p)) {
            // V/F, emparejar, ordenar y abierta no tienen opciones que elegir,
            // asi que una devolucion por opcion ahi no significa nada.
            throw new ExcepcionDeNegocio(ClaveError.DEVOLUCION_SIN_OPCIONES, "devolucion.porOpcion");
        }
        Set<String> ids = new HashSet<>();
        for (PayloadDeItem.Opcion o : p.opciones() == null ? List.<PayloadDeItem.Opcion>of() : p.opciones()) {
            ids.add(o.id());
        }
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < porOpcion.size(); i++) {
            String campo = "devolucion.porOpcion[" + i + "].id";
            String id = porOpcion.get(i).id();
            if (id == null || !ids.contains(id)) {
                throw new ExcepcionDeNegocio(ClaveError.DEVOLUCION_DE_OPCION_INEXISTENTE, campo);
            }
            if (!vistos.add(id)) {
                throw new ExcepcionDeNegocio(ClaveError.ID_DUPLICADO, campo);
            }
        }
    }

    /**
     * La rubrica es OBLIGATORIA, y esa es toda la regla de este tipo.
     *
     * En los cuatro automaticos el criterio existe para que la maquina puntue.
     * Aca existe para que un humano puntue parejo entre veinte alumnos, y sin
     * ella la correccion queda a merced de en que orden se leyeron las
     * respuestas. Es el unico caso donde el criterio no lo consume el codigo.
     */
    private void validarAbierta(PayloadDeItem.Abierta p, CriterioDeCorreccion.Abierta c) {
        if (p.consigna() == null || p.consigna().isBlank()) {
            throw new ExcepcionDeNegocio(ClaveError.CONSIGNA_REQUERIDA, "payload.consigna");
        }
        if (p.extensionMaxima() != null && p.extensionMaxima() <= 0) {
            throw new ExcepcionDeNegocio(ClaveError.EXTENSION_NO_POSITIVA, "payload.extensionMaxima");
        }
        if (c.rubrica() == null || c.rubrica().isBlank()) {
            throw new ExcepcionDeNegocio(ClaveError.RUBRICA_REQUERIDA, "criterio.rubrica");
        }
    }

    /**
     * Exactamente una regla de fondo: tiene que haber una respuesta que valga el
     * 100%.
     *
     * Sin ella el item es incorregible en el peor sentido —se puede contestar
     * perfecto y no sacar el puntaje completo—, y el peso que el profesor
     * eligio al componer deja de significar lo que dice. Es la misma regla que
     * ya aplica el puntaje parcial por opcion (CI-55), por la misma razon.
     *
     * Las repetidas se rechazan sobre el texto YA normalizado con los
     * interruptores del propio criterio, y con la MISMA funcion que usa el
     * corrector: con la comparacion laxa prendida,
     * "paris" y "PARIS" son la misma respuesta escrita dos veces, y la segunda
     * es inalcanzable. Dejarla pasar seria guardar una regla que no se aplica
     * nunca — el tipo de cosa que despues nadie entiende por que no funciona.
     */
    private void validarRespuestaCorta(PayloadDeItem.RespuestaCorta p,
                                       CriterioDeCorreccion.RespuestaCorta c) {
        if (p.consigna() == null || p.consigna().isBlank()) {
            throw new ExcepcionDeNegocio(ClaveError.CONSIGNA_REQUERIDA, "payload.consigna");
        }
        List<CriterioDeCorreccion.Aceptada> aceptadas =
                c.aceptadas() == null ? List.of() : c.aceptadas();
        if (aceptadas.isEmpty()) {
            throw new ExcepcionDeNegocio(ClaveError.SIN_RESPUESTA_ACEPTADA, "criterio.aceptadas");
        }

        Set<String> vistas = new HashSet<>();
        boolean hayUnaAlCien = false;
        for (int i = 0; i < aceptadas.size(); i++) {
            CriterioDeCorreccion.Aceptada a = aceptadas.get(i);
            String campo = "criterio.aceptadas[" + i + "]";
            if (a.texto() == null || a.texto().isBlank()) {
                throw new ExcepcionDeNegocio(ClaveError.RESPUESTA_ACEPTADA_VACIA, campo + ".texto");
            }
            if (a.porcentaje() <= 0 || a.porcentaje() > 100) {
                throw new ExcepcionDeNegocio(ClaveError.PORCENTAJE_ACEPTADA_FUERA_DE_RANGO,
                        campo + ".porcentaje");
            }
            if (!vistas.add(Normalizacion.paraComparar(
                    a.texto(), c.distingueMayusculas(), c.distingueAcentos()))) {
                throw new ExcepcionDeNegocio(ClaveError.RESPUESTA_ACEPTADA_REPETIDA, campo + ".texto");
            }
            hayUnaAlCien |= a.porcentaje() == 100;
        }
        if (!hayUnaAlCien) {
            throw new ExcepcionDeNegocio(ClaveError.SIN_ACEPTADA_AL_100, "criterio.aceptadas");
        }
    }

    /**
     * La tolerancia negativa se rechaza en vez de tomarle el valor absoluto:
     * escribir -0.1 es un error de carga del profesor, y corregirselo en
     * silencio esconde que no entendio el campo.
     */
    private void validarNumerica(PayloadDeItem.Numerica p, CriterioDeCorreccion.Numerica c) {
        if (p.consigna() == null || p.consigna().isBlank()) {
            throw new ExcepcionDeNegocio(ClaveError.CONSIGNA_REQUERIDA, "payload.consigna");
        }
        if (c.valor() == null || !Double.isFinite(c.valor())) {
            throw new ExcepcionDeNegocio(ClaveError.VALOR_NUMERICO_REQUERIDO, "criterio.valor");
        }
        if (c.tolerancia() != null && (!Double.isFinite(c.tolerancia()) || c.tolerancia() < 0)) {
            throw new ExcepcionDeNegocio(ClaveError.TOLERANCIA_NEGATIVA, "criterio.tolerancia");
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
        if (c.tienePesos()) {
            validarPesos(c, ids, correctas);
        }
    }

    /**
     * El puntaje parcial por opcion (CI-55).
     *
     * Las dos reglas centrales son las mismas que Moodle aplica y muestra como
     * error al pie del formulario:
     *
     * <ul>
     *   <li><b>los positivos suman 100%</b> — si sumaran 80, el alumno perfecto
     *       nunca podria sacar el puntaje completo del item, y el peso que el
     *       profesor eligio al componer dejaria de significar lo que dice;</li>
     *   <li><b>las marcadas correctas suman 100% entre ellas</b> — ata las dos
     *       representaciones del criterio para que no puedan contradecirse. Sin
     *       esto se puede guardar un item donde `correctas` dice una cosa y los
     *       porcentajes pagan otra, y cual de las dos gana depende de si el
     *       criterio tiene pesos: exactamente el bug silencioso que esta clase
     *       existe para hacer imposible.</li>
     * </ul>
     *
     * Una opcion sin porcentaje vale 0, como en Moodle: no hace falta
     * enumerarlas todas para dejar las distractoras en cero.
     */
    private void validarPesos(CriterioDeCorreccion.OpcionMultiple c,
                              Set<String> ids, List<String> correctas) {
        Set<String> vistos = new HashSet<>();
        int positivos = 0;
        for (int i = 0; i < c.pesos().size(); i++) {
            CriterioDeCorreccion.Ponderada peso = c.pesos().get(i);
            String campo = "criterio.pesos[" + i + "]";
            if (peso.id() == null || !ids.contains(peso.id())) {
                throw new ExcepcionDeNegocio(ClaveError.PESO_DE_OPCION_INEXISTENTE, campo + ".id");
            }
            if (!vistos.add(peso.id())) {
                throw new ExcepcionDeNegocio(ClaveError.ID_DUPLICADO, campo + ".id");
            }
            if (peso.porcentaje() < -100 || peso.porcentaje() > 100) {
                throw new ExcepcionDeNegocio(ClaveError.PESO_FUERA_DE_RANGO, campo + ".porcentaje");
            }
            if (peso.porcentaje() > 0) {
                positivos += peso.porcentaje();
            }
        }
        if (positivos != 100) {
            throw new ExcepcionDeNegocio(ClaveError.POSITIVOS_NO_SUMAN_100, "criterio.pesos");
        }

        int sumaDeLasCorrectas = 0;
        for (CriterioDeCorreccion.Ponderada peso : c.pesos()) {
            if (correctas.contains(peso.id())) {
                sumaDeLasCorrectas += peso.porcentaje();
            }
        }
        if (sumaDeLasCorrectas != 100) {
            throw new ExcepcionDeNegocio(ClaveError.CORRECTAS_NO_SUMAN_100, "criterio.correctas");
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
