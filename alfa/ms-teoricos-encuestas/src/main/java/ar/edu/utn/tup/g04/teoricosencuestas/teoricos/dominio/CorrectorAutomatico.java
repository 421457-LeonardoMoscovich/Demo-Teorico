package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.CriterioDeCorreccion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.RespuestaDeItem;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Los cuatro tipos autocorregibles.
 *
 * <p><b>Tres de los cuatro son todo o nada. Opcion multiple ya no (CI-55).</b>
 * Este comentario decia que el puntaje parcial era "inventar una regla
 * academica que nadie pidio", y ese argumento se cayo: Moodle —el sistema que
 * la catedra usa— pondera cada opcion por separado desde siempre, con
 * porcentajes negativos incluidos. No es una regla que inventamos nosotros, es
 * la que el profesor ya tiene en la mano. Ver CI-55 para la reversion completa.
 *
 * <p>Emparejar y ordenar siguen siendo todo o nada, y eso tambien es una
 * decision: en ordenar, un elemento fuera de lugar corre a todos los demas, asi
 * que "cuantos acerto" no significa nada; en emparejar, el parcial es
 * defendible pero nadie lo pidio y no queriamos dos reglas nuevas en la misma
 * tanda.
 *
 * <p>El item dejado en blanco llega como respuesta vacia y saca 0 por el mismo
 * camino que una respuesta equivocada. El corrector no tiene que distinguir
 * "no contesto" de "no llego el dato".
 */
@Component
public class CorrectorAutomatico implements Corrector {

    @Override
    public boolean atiende(TipoDeItem tipo) {
        return tipo.esAutocorregible();
    }

    @Override
    public String nombre() {
        return "AUTOMATICO";
    }

    @Override
    public int corregir(TipoDeItem tipo, CriterioDeCorreccion criterio,
                        RespuestaDeItem respuesta, int puntaje) {
        return switch (tipo) {
            case OPCION_MULTIPLE -> opcionMultiple(criterio, respuesta, puntaje);
            case VERDADERO_FALSO -> verdaderoFalso(criterio, respuesta) ? puntaje : 0;
            case EMPAREJAR -> emparejar(criterio, respuesta) ? puntaje : 0;
            case ORDENAR -> ordenar(criterio, respuesta) ? puntaje : 0;
            case ABIERTA, CONVERSACION, DEBATE -> 0;
        };
    }

    /**
     * Sin pesos, el todo o nada de siempre. Con pesos, la suma de lo que vale
     * cada opcion marcada.
     *
     * <p>Los dos recortes importan y no son lo mismo: el de la suma evita que
     * un criterio con positivos mal cargados pague mas del 100%, y el del
     * resultado evita que los negativos arrastren el item por debajo de cero.
     * Un item no le puede restar puntos al cuestionario: el piso de una
     * pregunta es no haberla contestado.
     */
    private int opcionMultiple(CriterioDeCorreccion criterio, RespuestaDeItem respuesta,
                               int puntaje) {
        if (!(criterio instanceof CriterioDeCorreccion.OpcionMultiple c)
                || !(respuesta instanceof RespuestaDeItem.OpcionMultiple r)) {
            return 0;
        }
        if (!c.tienePesos()) {
            return conjunto(c.correctas()).equals(conjunto(r.seleccionadas())) ? puntaje : 0;
        }

        Set<String> marcadas = conjunto(r.seleccionadas());
        int porcentaje = 0;
        for (CriterioDeCorreccion.Ponderada p : c.pesos()) {
            if (marcadas.contains(p.id())) {
                porcentaje += p.porcentaje();
            }
        }
        porcentaje = Math.min(100, porcentaje);

        // Redondeo al entero mas cercano: la nota del cuestionario es entera
        // (CI-06, escala 0-100) y el puntaje de un item tambien.
        int obtenido = Math.round(puntaje * porcentaje / 100f);
        return Math.max(0, Math.min(puntaje, obtenido));
    }

    private boolean verdaderoFalso(CriterioDeCorreccion criterio, RespuestaDeItem respuesta) {
        if (!(criterio instanceof CriterioDeCorreccion.VerdaderoFalso c)
                || !(respuesta instanceof RespuestaDeItem.VerdaderoFalso r)) {
            return false;
        }
        return c.esVerdadero() != null && c.esVerdadero().equals(r.valor());
    }

    private boolean emparejar(CriterioDeCorreccion criterio, RespuestaDeItem respuesta) {
        if (!(criterio instanceof CriterioDeCorreccion.Emparejar c)
                || !(respuesta instanceof RespuestaDeItem.Emparejar r)) {
            return false;
        }
        // Conjunto de pares y no lista: el orden en que el alumno armo los pares
        // no es parte de lo que se evalua.
        return paresComoConjunto(c.pares()).equals(paresComoConjunto(r.pares()));
    }

    private boolean ordenar(CriterioDeCorreccion criterio, RespuestaDeItem respuesta) {
        if (!(criterio instanceof CriterioDeCorreccion.Ordenar c)
                || !(respuesta instanceof RespuestaDeItem.Ordenar r)) {
            return false;
        }
        // Aca SI importa el orden: es lo unico que el item evalua.
        return c.secuencia() != null && c.secuencia().equals(r.secuencia());
    }

    private Set<String> conjunto(List<String> valores) {
        return valores == null ? Set.of() : new HashSet<>(valores);
    }

    private Set<String> paresComoConjunto(List<List<String>> pares) {
        if (pares == null) return Set.of();
        Set<String> normalizados = new HashSet<>();
        for (List<String> par : pares) {
            if (par != null && par.size() == 2) {
                normalizados.add(par.get(0) + "->" + par.get(1));
            }
        }
        return normalizados;
    }
}
