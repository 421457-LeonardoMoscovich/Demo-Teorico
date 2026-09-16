package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Como se comparan dos textos que tienen que significar lo mismo.
 *
 * Vive en un solo lado porque la usan tres: el corrector de respuesta corta
 * —para decidir si el alumno acerto—, el validador —para detectar dos
 * respuestas aceptadas que en realidad son la misma— y el banco —para que
 * "Microservicios" y "microservicios" sean una sola etiqueta—. Tres copias de
 * una regla sutil terminan divergiendo, y el dia que diverjan el validador va a
 * dejar pasar un criterio que el corrector no puede aplicar.
 */
public final class Normalizacion {

    private Normalizacion() {
    }

    /**
     * Marcador para la enie mientras se sacan los acentos.
     *
     * La enie NO es una vocal acentuada, es otra letra: en castellano "ano" no
     * es "año", y una normalizacion que las confunda deja el tipo inusable. Pero
     * en Unicode la enie se descompone en n + tilde combinante, asi que el mismo
     * paso que borra el acento de "parís" se lleva puesta la enie. Se aparta
     * antes de descomponer y se repone despues.
     *
     * La dieresis si se saca, y esa asimetria es a proposito: "pingüino" y
     * "pinguino" son la misma palabra mal escrita, no dos palabras.
     */
    private static final char MINUSCULA = 1;   // U+0001, que no aparece en un texto escrito
    private static final char MAYUSCULA = 2;   // U+0002

    /** Recorta las puntas y colapsa los espacios del medio: dos espacios son un tipeo. */
    public static String colapsarEspacios(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }

    /** Saca los acentos descomponiendo en Unicode. Conserva la enie. */
    public static String sinAcentos(String texto) {
        if (texto == null) return "";
        String apartada = texto.replace('ñ', MINUSCULA).replace('Ñ', MAYUSCULA);
        String plana = Normalizer.normalize(apartada, Normalizer.Form.NFD)
                .replaceAll("\\p{IsM}", "");
        return Normalizer.normalize(plana, Normalizer.Form.NFC)
                .replace(MINUSCULA, 'ñ')
                .replace(MAYUSCULA, 'Ñ');
    }

    /**
     * El texto con el que se compara una respuesta corta, segun los dos
     * interruptores del criterio. Es la MISMA funcion que usa el validador para
     * detectar aceptadas repetidas: si no lo fuera, se podria guardar una
     * segunda respuesta que el corrector nunca alcanza.
     */
    public static String paraComparar(String texto, boolean distingueMayusculas,
                                      boolean distingueAcentos) {
        String limpio = colapsarEspacios(texto);
        if (!distingueMayusculas) {
            limpio = limpio.toLowerCase(Locale.ROOT);
        }
        return distingueAcentos ? limpio : sinAcentos(limpio);
    }

    /**
     * Una etiqueta del banco: siempre en minusculas y sin acentos.
     *
     * No se ofrece la variante estricta a proposito. Una etiqueta es un rotulo
     * para encontrar, no contenido: dejar que "Kafka" y "kafka" convivan rompe
     * el filtro justo cuando el banco crece, que es cuando hacia falta.
     */
    public static String etiqueta(String cruda) {
        return sinAcentos(colapsarEspacios(cruda).toLowerCase(Locale.ROOT));
    }
}
