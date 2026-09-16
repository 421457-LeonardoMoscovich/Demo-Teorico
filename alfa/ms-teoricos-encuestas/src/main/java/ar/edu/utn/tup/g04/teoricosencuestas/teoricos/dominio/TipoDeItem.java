package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio;

/**
 * Los nueve tipos. Siete son del PRD y dos —RESPUESTA_CORTA y NUMERICA— se
 * agregaron mirando el catalogo de Moodle, que es el que la catedra usa: son
 * los dos unicos de esa lista que se corrigen solos y entran en este modelo sin
 * inventarle nada (las de arrastrar necesitan archivos, las calculadas
 * necesitan que la lectura escriba, y CI-19 dice que no escribe).
 *
 * No son homogeneos y el enum lo dice:
 *
 *  - seis se corrigen solos;
 *  - ABIERTA se corrige a mano o por IA (D-01). Ya no es autocorregible pero SI
 *    esta en la alfa: es la que obliga a que exista un estado de espera, y sin
 *    ella el puerto Corrector es una promesa que nadie verifico;
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

    /**
     * Texto corto contra una lista de respuestas aceptadas (Moodle: "Respuesta
     * corta"). Se corrige sola porque el profesor enumera lo que vale; lo que no
     * previo, no vale — y esa es exactamente la razon por la que una pregunta
     * conceptual va en ABIERTA y no aca.
     */
    RESPUESTA_CORTA(true, false, true),

    /** Un numero con tolerancia (Moodle: "Numerica"). */
    NUMERICA(true, false, true),

    ABIERTA(false, false, true),
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
