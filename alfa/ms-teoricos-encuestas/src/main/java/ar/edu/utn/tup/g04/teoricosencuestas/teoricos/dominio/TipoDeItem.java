package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio;

/**
 * Los siete tipos del PRD. No son homogeneos y el enum lo dice:
 *
 *  - cuatro se corrigen solos y son los que implementa la alfa;
 *  - ABIERTA se corrige a mano o por IA (D-01): modelada, fuera de la alfa;
 *  - CONVERSACION y DEBATE estan DIFERIDOS (D-09), no descartados: son dos
 *    formas de interaccion que el modelo de entrega unica no soporta.
 *
 * El tipo vive en `item` y no en `item_version`: asi no puede cambiar entre
 * versiones, y eso es mejor que validarlo.
 */
public enum TipoDeItem {

    OPCION_MULTIPLE(true, false, true),
    VERDADERO_FALSO(true, false, true),
    EMPAREJAR(true, false, true),
    ORDENAR(true, false, true),
    ABIERTA(false, false, false),
    CONVERSACION(false, true, false),
    DEBATE(false, true, false);

    private final boolean autocorregible;
    private final boolean diferido;
    private final boolean enLaAlfa;

    TipoDeItem(boolean autocorregible, boolean diferido, boolean enLaAlfa) {
        this.autocorregible = autocorregible;
        this.diferido = diferido;
        this.enLaAlfa = enLaAlfa;
    }

    /** Determina si el modo de correccion del contenido es INMEDIATA o DIFERIDA (CI-07). */
    public boolean esAutocorregible() { return autocorregible; }

    /** D-09: previsto en el modelo, no implementado. Se rechaza con 422, no con 400. */
    public boolean estaDiferido() { return diferido; }

    public boolean estaEnLaAlfa() { return enLaAlfa; }
}
