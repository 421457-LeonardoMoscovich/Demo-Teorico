package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.CriterioDeCorreccion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.Devolucion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.PayloadDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.ValidadorDePayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La retroalimentacion del item (CI-58).
 *
 * Lo que fija este test no es que la devolucion llegue: es <b>cuanta llega</b>.
 * La devolucion de una opcion dice si esa opcion estaba bien, asi que servir la
 * lista entera es servir la clave de correccion con otras palabras — y con
 * reintentos ilimitados (RF-REC-04) eso convierte el reintento en copiar. El
 * recorte a lo que el alumno marco es la regla, y vive en el dominio para que
 * no dependa de que la pantalla se acuerde de filtrar.
 */
class DevolucionRecortadaTest {

    private final ValidadorDePayload validador = new ValidadorDePayload();

    private static final Devolucion DEVOLUCION = new Devolucion(
            "El gateway resuelve problemas de borde.",
            List.of(new Devolucion.PorOpcion("a", "Si: un solo punto de entrada."),
                    new Devolucion.PorOpcion("b", "Si, y es la razon mas fuerte."),
                    new Devolucion.PorOpcion("c", "No: un gateway no coordina transacciones.")));

    private static PayloadDeItem.OpcionMultiple payload() {
        return new PayloadDeItem.OpcionMultiple(
                List.of(new PayloadDeItem.Opcion("a", "Punto de entrada unico"),
                        new PayloadDeItem.Opcion("b", "Autenticacion centralizada"),
                        new PayloadDeItem.Opcion("c", "Consistencia transaccional")),
                true);
    }

    @Nested
    @DisplayName("El recorte")
    class Recorte {

        @Test
        void solo_devuelve_el_texto_de_lo_que_el_alumno_marco() {
            List<Devolucion.PorOpcion> visto = DEVOLUCION.paraLoMarcado(List.of("a"));
            assertThat(visto).extracting(Devolucion.PorOpcion::id).containsExactly("a");
        }

        @Test
        void no_se_filtra_la_devolucion_de_una_opcion_que_no_eligio() {
            // Es la razon de ser de la clase: "No: un gateway no coordina
            // transacciones" al lado de una opcion que no marco le estaria
            // diciendo cual era la trampa sin que la haya pisado.
            List<Devolucion.PorOpcion> visto = DEVOLUCION.paraLoMarcado(List.of("a", "b"));
            assertThat(visto).extracting(Devolucion.PorOpcion::id).doesNotContain("c");
        }

        @Test
        void el_que_no_marco_nada_no_recibe_ninguna() {
            assertThat(DEVOLUCION.paraLoMarcado(List.of())).isEmpty();
        }

        @Test
        void el_que_marco_la_equivocada_recibe_la_suya() {
            // La devolucion de lo que SI marco es justamente lo que tiene valor:
            // le explica el error que cometio, no los que no cometio.
            assertThat(DEVOLUCION.paraLoMarcado(List.of("c")))
                    .extracting(Devolucion.PorOpcion::texto)
                    .containsExactly("No: un gateway no coordina transacciones.");
        }

        @Test
        void una_devolucion_vacia_no_ocupa_lugar() {
            assertThat(new Devolucion(null, List.of()).vacia()).isTrue();
            assertThat(new Devolucion("  ", null).vacia()).isTrue();
            assertThat(new Devolucion("algo", null).vacia()).isFalse();
        }

        @Test
        void los_textos_en_blanco_se_descartan() {
            Devolucion conHuecos = new Devolucion(null,
                    List.of(new Devolucion.PorOpcion("a", "   ")));
            assertThat(conHuecos.paraLoMarcado(List.of("a"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Validacion")
    class Validacion {

        private void validar(Devolucion d) {
            validador.validar(TipoDeItem.OPCION_MULTIPLE, "Un enunciado", payload(),
                    new CriterioDeCorreccion.OpcionMultiple(List.of("a", "b")), d);
        }

        @Test
        void una_devolucion_bien_armada_pasa() {
            validar(DEVOLUCION);
        }

        @Test
        void el_item_sin_devolucion_sigue_siendo_valido() {
            // La compatibilidad: los items que ya estan en la base no tienen.
            validar(null);
        }

        @Test
        void no_puede_apuntar_a_una_opcion_inexistente() {
            // Sin esta regla el error es invisible: como la devolucion se filtra
            // por lo que el alumno marco, y nadie puede marcar un id que no
            // existe, el texto no se mostraria nunca y el profesor creeria que
            // lo escribio.
            Devolucion fantasma = new Devolucion(null,
                    List.of(new Devolucion.PorOpcion("zzz", "un texto huerfano")));
            assertThatThrownBy(() -> validar(fantasma))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.DEVOLUCION_DE_OPCION_INEXISTENTE);
        }

        @Test
        void un_tipo_sin_opciones_no_admite_devolucion_por_opcion() {
            Devolucion porOpcion = new Devolucion(null,
                    List.of(new Devolucion.PorOpcion("a", "texto")));
            assertThatThrownBy(() -> validador.validar(TipoDeItem.VERDADERO_FALSO, "Un enunciado",
                    new PayloadDeItem.VerdaderoFalso("Una afirmacion"),
                    new CriterioDeCorreccion.VerdaderoFalso(true), porOpcion))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.DEVOLUCION_SIN_OPCIONES);
        }

        @Test
        void un_tipo_sin_opciones_si_admite_la_general() {
            // Una respuesta abierta o un V/F pueden explicar por que, aunque no
            // tengan opciones que comentar de a una.
            validador.validar(TipoDeItem.VERDADERO_FALSO, "Un enunciado",
                    new PayloadDeItem.VerdaderoFalso("Una afirmacion"),
                    new CriterioDeCorreccion.VerdaderoFalso(true),
                    new Devolucion("Compartir base acopla el despliegue.", null));
        }
    }
}
