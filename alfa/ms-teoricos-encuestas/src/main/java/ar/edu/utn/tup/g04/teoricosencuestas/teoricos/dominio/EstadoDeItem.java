package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio;

/**
 * Si el item ya se puede usar en un cuestionario (CI-59).
 *
 * <p>Dos estados y no tres: no hay "archivado", porque eso ya lo dice la baja
 * logica, ni "en revision", porque no hay nadie que revise items mas que su
 * propio autor.
 *
 * <p><b>Es del item y no de la version.</b> La version es inmutable (D-04): si
 * el estado viviera ahi, pasar de borrador a listo obligaria a publicar una
 * version nueva — y no lo es, porque la pregunta no cambio. Lo que cambio es la
 * decision del profesor sobre si ya se puede usar.
 */
public enum EstadoDeItem {

    /** A medio cargar. Existe, se edita, pero no entra a ningun cuestionario. */
    BORRADOR,

    /** Terminado. Es el estado por defecto y el unico que se puede componer. */
    LISTO;

    public boolean sePuedeComponer() {
        return this == LISTO;
    }
}
