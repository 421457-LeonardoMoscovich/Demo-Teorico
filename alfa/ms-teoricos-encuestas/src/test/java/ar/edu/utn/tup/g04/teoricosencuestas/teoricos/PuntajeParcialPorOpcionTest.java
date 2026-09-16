package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.CorrectorAutomatico;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.CriterioDeCorreccion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.PayloadDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.RespuestaDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.ValidadorDePayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El puntaje parcial por opcion (CI-55).
 *
 * Fija la reversion del todo o nada en opcion multiple, y sobre todo fija que
 * la reversion es COMPATIBLE: un criterio sin `pesos` —que es como quedaron
 * todos los que ya estan en la base— se corrige exactamente igual que antes.
 * Si ese caso se rompiera, CI-12 y CI-44 quedarian violados en silencio: notas
 * ya emitidas cambiarian de valor sin que nadie lo decidiera.
 */
class PuntajeParcialPorOpcionTest {

    private final CorrectorAutomatico corrector = new CorrectorAutomatico();
    private final ValidadorDePayload validador = new ValidadorDePayload();

    /** Dos correctas al 50% y una distractora que descuenta. */
    private static CriterioDeCorreccion.OpcionMultiple criterioPonderado() {
        return new CriterioDeCorreccion.OpcionMultiple(
                List.of("a", "c"),
                List.of(new CriterioDeCorreccion.Ponderada("a", 50),
                        new CriterioDeCorreccion.Ponderada("c", 50),
                        new CriterioDeCorreccion.Ponderada("b", -25)));
    }

    private static PayloadDeItem.OpcionMultiple payload() {
        return new PayloadDeItem.OpcionMultiple(
                List.of(new PayloadDeItem.Opcion("a", "Primera"),
                        new PayloadDeItem.Opcion("b", "Distractora"),
                        new PayloadDeItem.Opcion("c", "Segunda")),
                true);
    }

    private int corregir(CriterioDeCorreccion criterio, List<String> marcadas) {
        return corrector.corregir(TipoDeItem.OPCION_MULTIPLE, criterio,
                new RespuestaDeItem.OpcionMultiple(marcadas), 40);
    }

    @Nested
    @DisplayName("Correccion")
    class Correccion {

        @Test
        void marcar_las_dos_correctas_da_el_puntaje_entero() {
            assertThat(corregir(criterioPonderado(), List.of("a", "c"))).isEqualTo(40);
        }

        @Test
        void marcar_una_sola_da_la_mitad_y_ya_no_cero() {
            // Es la diferencia observable con el comportamiento anterior.
            assertThat(corregir(criterioPonderado(), List.of("a"))).isEqualTo(20);
        }

        @Test
        void la_distractora_descuenta_sobre_lo_acertado() {
            // 50 - 25 = 25% de 40 = 10.
            assertThat(corregir(criterioPonderado(), List.of("a", "b"))).isEqualTo(10);
        }

        @Test
        void marcarlo_todo_no_garantiza_el_puntaje_completo() {
            // Es para esto que existen los porcentajes negativos: sin el -25, la
            // estrategia de marcar las tres pagaria 100% y el item no evaluaria
            // nada. 50 + 50 - 25 = 75% de 40 = 30.
            assertThat(corregir(criterioPonderado(), List.of("a", "b", "c"))).isEqualTo(30);
        }

        @Test
        void los_negativos_nunca_dejan_el_item_por_debajo_de_cero() {
            // El piso de una pregunta es no haberla contestado: un item no le
            // puede restar puntos a los demas.
            assertThat(corregir(criterioPonderado(), List.of("b"))).isZero();
        }

        @Test
        void el_item_en_blanco_sigue_dando_cero() {
            assertThat(corregir(criterioPonderado(), List.of())).isZero();
        }

        @Test
        void redondea_al_entero_mas_cercano() {
            // Tres correctas al 33% cada una: una sola paga 33% de 40 = 13,2.
            CriterioDeCorreccion tercios = new CriterioDeCorreccion.OpcionMultiple(
                    List.of("a", "b", "c"),
                    List.of(new CriterioDeCorreccion.Ponderada("a", 34),
                            new CriterioDeCorreccion.Ponderada("b", 33),
                            new CriterioDeCorreccion.Ponderada("c", 33)));
            assertThat(corregir(tercios, List.of("b"))).isEqualTo(13);
        }

        @Test
        void un_criterio_sin_pesos_se_corrige_como_siempre() {
            // La compatibilidad, dicha como test: es lo que hace que CI-55 no
            // recalcule nada de lo ya corregido.
            CriterioDeCorreccion viejo = new CriterioDeCorreccion.OpcionMultiple(List.of("a", "c"));
            assertThat(corregir(viejo, List.of("a", "c"))).isEqualTo(40);
            assertThat(corregir(viejo, List.of("a"))).isZero();
        }
    }

    @Nested
    @DisplayName("Validacion del criterio")
    class Validacion {

        private void validar(CriterioDeCorreccion criterio) {
            validador.validar(TipoDeItem.OPCION_MULTIPLE, "Un enunciado", payload(), criterio);
        }

        @Test
        void un_criterio_ponderado_bien_armado_pasa() {
            validar(criterioPonderado());
        }

        @Test
        void los_positivos_tienen_que_sumar_100() {
            // El error que Moodle muestra al pie del formulario.
            CriterioDeCorreccion flojo = new CriterioDeCorreccion.OpcionMultiple(
                    List.of("a", "c"),
                    List.of(new CriterioDeCorreccion.Ponderada("a", 50),
                            new CriterioDeCorreccion.Ponderada("c", 30)));
            assertThatThrownBy(() -> validar(flojo))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.POSITIVOS_NO_SUMAN_100);
        }

        @Test
        void las_marcadas_correctas_tienen_que_sumar_100_entre_ellas() {
            // Positivos coherentes (50 + 50), pero `correctas` nombra una sola:
            // las dos representaciones del criterio se contradicen.
            CriterioDeCorreccion incoherente = new CriterioDeCorreccion.OpcionMultiple(
                    List.of("a"),
                    List.of(new CriterioDeCorreccion.Ponderada("a", 50),
                            new CriterioDeCorreccion.Ponderada("c", 50)));
            assertThatThrownBy(() -> validar(incoherente))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.CORRECTAS_NO_SUMAN_100);
        }

        @Test
        void un_peso_no_puede_apuntar_a_una_opcion_inexistente() {
            CriterioDeCorreccion fantasma = new CriterioDeCorreccion.OpcionMultiple(
                    List.of("a", "c"),
                    List.of(new CriterioDeCorreccion.Ponderada("zzz", 100)));
            assertThatThrownBy(() -> validar(fantasma))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.PESO_DE_OPCION_INEXISTENTE);
        }

        @Test
        void el_porcentaje_no_puede_pasarse_de_100() {
            CriterioDeCorreccion excedido = new CriterioDeCorreccion.OpcionMultiple(
                    List.of("a"),
                    List.of(new CriterioDeCorreccion.Ponderada("a", 140)));
            assertThatThrownBy(() -> validar(excedido))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.PESO_FUERA_DE_RANGO);
        }

        @Test
        void el_campo_del_error_es_navegable() {
            // El front lo usa para marcar el input exacto, igual que con el
            // resto de los rechazos de esta clase.
            CriterioDeCorreccion fantasma = new CriterioDeCorreccion.OpcionMultiple(
                    List.of("a", "c"),
                    List.of(new CriterioDeCorreccion.Ponderada("a", 50),
                            new CriterioDeCorreccion.Ponderada("c", 50),
                            new CriterioDeCorreccion.Ponderada("zzz", -10)));
            assertThatThrownBy(() -> validar(fantasma))
                    .extracting(e -> ((ExcepcionDeNegocio) e).campo())
                    .isEqualTo("criterio.pesos[2].id");
        }
    }
}
