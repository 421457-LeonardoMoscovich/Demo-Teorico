package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.CorrectorAutomatico;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.CriterioDeCorreccion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.RespuestaDeItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los cuatro tipos autocorregibles, con su caso perfecto, su caso cero y su
 * caso limite. Es un test de dominio puro: no necesita base ni contexto.
 */
class CorreccionDeLosCuatroTiposTest {

    private final CorrectorAutomatico corrector = new CorrectorAutomatico();

    @Nested
    @DisplayName("Opcion multiple")
    class OpcionMultiple {

        private final CriterioDeCorreccion criterio =
                new CriterioDeCorreccion.OpcionMultiple(List.of("a", "c"));

        @Test
        void acierto_exacto_da_el_puntaje_entero() {
            int obtenido = corrector.corregir(TipoDeItem.OPCION_MULTIPLE, criterio,
                    new RespuestaDeItem.OpcionMultiple(List.of("c", "a")), 40);
            assertThat(obtenido).isEqualTo(40);
        }

        @Test
        void el_orden_de_lo_seleccionado_no_importa() {
            assertThat(corrector.corregir(TipoDeItem.OPCION_MULTIPLE, criterio,
                    new RespuestaDeItem.OpcionMultiple(List.of("a", "c")), 40)).isEqualTo(40);
        }

        @Test
        void acertar_solo_una_de_dos_da_cero_y_no_la_mitad() {
            // Todo o nada por item: el peso del item ya es la unidad de gradacion
            // que eligio el profesor.
            assertThat(corrector.corregir(TipoDeItem.OPCION_MULTIPLE, criterio,
                    new RespuestaDeItem.OpcionMultiple(List.of("a")), 40)).isZero();
        }

        @Test
        void marcar_una_de_mas_da_cero() {
            assertThat(corrector.corregir(TipoDeItem.OPCION_MULTIPLE, criterio,
                    new RespuestaDeItem.OpcionMultiple(List.of("a", "b", "c")), 40)).isZero();
        }

        @Test
        void el_item_en_blanco_da_cero_por_el_mismo_camino() {
            assertThat(corrector.corregir(TipoDeItem.OPCION_MULTIPLE, criterio,
                    new RespuestaDeItem.OpcionMultiple(List.of()), 40)).isZero();
        }
    }

    @Nested
    @DisplayName("Verdadero / falso")
    class VerdaderoFalso {

        private final CriterioDeCorreccion criterio = new CriterioDeCorreccion.VerdaderoFalso(true);

        @Test
        void acierto() {
            assertThat(corrector.corregir(TipoDeItem.VERDADERO_FALSO, criterio,
                    new RespuestaDeItem.VerdaderoFalso(true), 30)).isEqualTo(30);
        }

        @Test
        void error() {
            assertThat(corrector.corregir(TipoDeItem.VERDADERO_FALSO, criterio,
                    new RespuestaDeItem.VerdaderoFalso(false), 30)).isZero();
        }

        @Test
        void sin_contestar_da_cero_y_no_explota() {
            assertThat(corrector.corregir(TipoDeItem.VERDADERO_FALSO, criterio,
                    new RespuestaDeItem.VerdaderoFalso(null), 30)).isZero();
        }
    }

    @Nested
    @DisplayName("Emparejar")
    class Emparejar {

        private final CriterioDeCorreccion criterio = new CriterioDeCorreccion.Emparejar(
                List.of(List.of("i1", "d2"), List.of("i2", "d1")));

        @Test
        void acierto_con_los_pares_en_otro_orden() {
            // El orden en que el alumno armo los pares no es parte de lo evaluado.
            assertThat(corrector.corregir(TipoDeItem.EMPAREJAR, criterio,
                    new RespuestaDeItem.Emparejar(List.of(List.of("i2", "d1"), List.of("i1", "d2"))),
                    20)).isEqualTo(20);
        }

        @Test
        void un_par_cruzado_da_cero() {
            assertThat(corrector.corregir(TipoDeItem.EMPAREJAR, criterio,
                    new RespuestaDeItem.Emparejar(List.of(List.of("i1", "d1"), List.of("i2", "d2"))),
                    20)).isZero();
        }

        @Test
        void faltando_un_par_da_cero() {
            assertThat(corrector.corregir(TipoDeItem.EMPAREJAR, criterio,
                    new RespuestaDeItem.Emparejar(List.of(List.of("i1", "d2"))), 20)).isZero();
        }
    }

    @Nested
    @DisplayName("Ordenar")
    class Ordenar {

        private final CriterioDeCorreccion criterio =
                new CriterioDeCorreccion.Ordenar(List.of("e1", "e2", "e3"));

        @Test
        void secuencia_exacta() {
            assertThat(corrector.corregir(TipoDeItem.ORDENAR, criterio,
                    new RespuestaDeItem.Ordenar(List.of("e1", "e2", "e3")), 10)).isEqualTo(10);
        }

        @Test
        void aca_el_orden_si_importa() {
            assertThat(corrector.corregir(TipoDeItem.ORDENAR, criterio,
                    new RespuestaDeItem.Ordenar(List.of("e2", "e1", "e3")), 10)).isZero();
        }

        @Test
        void secuencia_incompleta_da_cero() {
            assertThat(corrector.corregir(TipoDeItem.ORDENAR, criterio,
                    new RespuestaDeItem.Ordenar(List.of("e1", "e2")), 10)).isZero();
        }
    }

    @Test
    void los_tipos_de_correccion_humana_no_los_atiende_el_automatico() {
        assertThat(corrector.atiende(TipoDeItem.ABIERTA)).isFalse();
        assertThat(corrector.atiende(TipoDeItem.CONVERSACION)).isFalse();
        assertThat(corrector.atiende(TipoDeItem.OPCION_MULTIPLE)).isTrue();
    }
}
