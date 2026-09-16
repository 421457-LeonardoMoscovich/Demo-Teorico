package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Cada alumno ve las preguntas en un orden distinto, y el orden NO SE GUARDA
 * EN NINGUN LADO.
 *
 * Esa es toda la idea, y es lo que permite tener barajado sin romper CI-19.
 * El razonamiento original decia que barajar obligaba a persistir el orden por
 * alumno y por intento —porque si no, el alumno recarga y ve otra cosa— y que
 * por lo tanto la lectura dejaba de ser pura y pasaba a escribir. Eso vale para
 * un barajado con `Math.random()`. No vale para uno DERIVADO: la permutacion
 * sale de (contenidoId, alumnoId), asi que es la misma cada vez que se calcula.
 * Recargar da lo mismo, reconstruirla al mostrar el resultado da lo mismo, y
 * dos servidores distintos dan lo mismo.
 *
 * O sea: la lectura sigue sin tener estado. Lo unico que cambio es que la
 * respuesta ahora depende de QUIEN pregunta, y eso ya lo sabiamos porque el
 * vale trae el alumnoId.
 *
 * Por que la semilla NO incluye el intento: el vale no lo lleva, y meterlo
 * obligaria a cambiar el contrato con el Tema 03 por una ganancia discutible
 * —que el alumno que reintenta vea otro orden—. Si alguna vez hace falta, entra
 * ahi y no cambia nada mas.
 *
 * SHA-256 y no `hashCode()`: `hashCode` de String no tiene garantia de
 * estabilidad entre versiones de la JVM, y este orden tiene que sobrevivir a un
 * redeploy o el desglose deja de coincidir con lo que el alumno vio.
 */
public final class BarajadorDeterministico {

    private BarajadorDeterministico() {
    }

    /**
     * Devuelve la lista permutada para este alumno. La misma entrada da siempre
     * la misma salida; no toca la lista original.
     */
    public static <T> List<T> barajar(List<T> original, UUID contenidoId, UUID alumnoId) {
        if (original == null || original.size() < 2) {
            return original;
        }
        List<T> copia = new ArrayList<>(original);
        Collections.shuffle(copia, new Random(semilla(contenidoId, alumnoId)));
        return copia;
    }

    /**
     * Elige {@code cuantos} de la lista, para ESTE alumno. Misma maquina y
     * misma semilla que el barajado, por la misma razon: el subconjunto se
     * deriva, no se guarda, asi que recargar devuelve el mismo y reconstruirlo
     * al corregir tambien.
     *
     * La lista de entrada tiene que llegar en un orden ESTABLE —la poblacion
     * ordenada por id— o el sorteo dejaria de ser reproducible: la misma
     * semilla sobre dos listas con distinto orden da subconjuntos distintos.
     */
    public static <T> List<T> elegir(List<T> poblacion, UUID contenidoId, UUID alumnoId,
                                     int cuantos) {
        if (poblacion == null || poblacion.isEmpty() || cuantos <= 0) {
            return List.of();
        }
        List<T> mezclada = barajar(poblacion, contenidoId, alumnoId);
        return List.copyOf(mezclada.subList(0, Math.min(cuantos, mezclada.size())));
    }

    static long semilla(UUID contenidoId, UUID alumnoId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((contenidoId + ":" + alumnoId).getBytes(StandardCharsets.UTF_8));
            long semilla = 0L;
            for (int i = 0; i < Long.BYTES; i++) {
                semilla = (semilla << 8) | (digest[i] & 0xFF);
            }
            return semilla;
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 tiene que existir en cualquier JVM", e);
        }
    }
}
