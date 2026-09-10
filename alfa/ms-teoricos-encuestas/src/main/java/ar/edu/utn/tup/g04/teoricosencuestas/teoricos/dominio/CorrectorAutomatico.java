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
 * Todo o nada por item, sin puntaje parcial: el peso del item ya es la unidad
 * de gradacion que eligio el profesor, y repartir de nuevo adentro de un item
 * es inventar una regla academica que nadie pidio.
 *
 * El item dejado en blanco llega como respuesta vacia y saca 0 por el mismo
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
        boolean acerto = switch (tipo) {
            case OPCION_MULTIPLE -> opcionMultiple(criterio, respuesta);
            case VERDADERO_FALSO -> verdaderoFalso(criterio, respuesta);
            case EMPAREJAR -> emparejar(criterio, respuesta);
            case ORDENAR -> ordenar(criterio, respuesta);
            case ABIERTA, CONVERSACION, DEBATE -> false;
        };
        return acerto ? puntaje : 0;
    }

    private boolean opcionMultiple(CriterioDeCorreccion criterio, RespuestaDeItem respuesta) {
        if (!(criterio instanceof CriterioDeCorreccion.OpcionMultiple c)
                || !(respuesta instanceof RespuestaDeItem.OpcionMultiple r)) {
            return false;
        }
        return conjunto(c.correctas()).equals(conjunto(r.seleccionadas()));
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
