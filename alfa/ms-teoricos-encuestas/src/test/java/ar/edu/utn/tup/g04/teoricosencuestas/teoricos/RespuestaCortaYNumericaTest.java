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
 * Los dos tipos que entraron del catalogo de Moodle.
 *
 * Lo que este test protege no son los casos felices —esos son obvios— sino las
 * tres decisiones que se pueden revertir sin querer: que la comparacion sea laxa
 * por defecto, que gane la primera aceptada y no la que mas paga, y que los
 * bordes de la tolerancia numerica entren.
 */
class RespuestaCortaYNumericaTest {

    private final CorrectorAutomatico corrector = new CorrectorAutomatico();
    private final ValidadorDePayload validador = new ValidadorDePayload();

    private CriterioDeCorreccion.RespuestaCorta laxa(CriterioDeCorreccion.Aceptada... aceptadas) {
        return new CriterioDeCorreccion.RespuestaCorta(List.of(aceptadas), false, false);
    }

    private int corregirCorta(CriterioDeCorreccion criterio, String escrito, int puntaje) {
        return corrector.corregir(TipoDeItem.RESPUESTA_CORTA, criterio,
                new RespuestaDeItem.RespuestaCorta(escrito), puntaje);
    }

    @Nested
    @DisplayName("Respuesta corta")
    class Corta {

        private final CriterioDeCorreccion criterio =
                laxa(new CriterioDeCorreccion.Aceptada("circuit breaker", 100),
                     new CriterioDeCorreccion.Aceptada("breaker", 60));

        @Test
        void la_respuesta_exacta_da_el_puntaje_entero() {
            assertThat(corregirCorta(criterio, "circuit breaker", 20)).isEqualTo(20);
        }

        @Test
        void una_aceptada_parcial_paga_su_porcentaje() {
            assertThat(corregirCorta(criterio, "breaker", 20)).isEqualTo(12);
        }

        @Test
        void por_defecto_no_distingue_mayusculas_ni_acentos() {
            // Es LA decision del tipo: escribir "Circuit Breaker" no es un error
            // de concepto, y castigarlo convierte la pregunta en un dictado.
            assertThat(corregirCorta(criterio, "Circuit Breaker", 20)).isEqualTo(20);
            assertThat(corregirCorta(laxa(new CriterioDeCorreccion.Aceptada("parís", 100)),
                    "paris", 10)).isEqualTo(10);
        }

        @Test
        void los_espacios_de_mas_no_cuentan() {
            assertThat(corregirCorta(criterio, "  circuit   breaker  ", 20)).isEqualTo(20);
        }

        @Test
        void la_enie_no_es_un_acento_y_sobrevive_a_la_normalizacion() {
            // Si la ñ se normalizara como una vocal acentuada, "ano" valdria
            // como "año" y el tipo seria inusable en castellano.
            CriterioDeCorreccion c = laxa(new CriterioDeCorreccion.Aceptada("año", 100));
            assertThat(corregirCorta(c, "año", 10)).isEqualTo(10);
            assertThat(corregirCorta(c, "ano", 10)).isZero();
        }

        @Test
        void prendiendo_el_interruptor_si_distingue() {
            CriterioDeCorreccion estricta = new CriterioDeCorreccion.RespuestaCorta(
                    List.of(new CriterioDeCorreccion.Aceptada("HTTP", 100)), true, true);
            assertThat(corregirCorta(estricta, "HTTP", 10)).isEqualTo(10);
            assertThat(corregirCorta(estricta, "http", 10)).isZero();
        }

        @Test
        void gana_la_primera_que_coincide_y_no_la_que_mas_paga() {
            // Con la comparacion laxa las dos coinciden. Si evaluaramos todas y
            // nos quedaramos con la mejor, el 60 no se aplicaria NUNCA y el
            // profesor no tendria forma de castigar una variante.
            CriterioDeCorreccion c = new CriterioDeCorreccion.RespuestaCorta(
                    List.of(new CriterioDeCorreccion.Aceptada("Paris", 60),
                            new CriterioDeCorreccion.Aceptada("parís", 100)),
                    false, false);
            assertThat(corregirCorta(c, "paris", 10)).isEqualTo(6);
        }

        @Test
        void en_blanco_da_cero() {
            assertThat(corregirCorta(criterio, "   ", 20)).isZero();
            assertThat(corregirCorta(criterio, null, 20)).isZero();
        }

        @Test
        void lo_que_no_esta_en_la_lista_da_cero() {
            assertThat(corregirCorta(criterio, "retry", 20)).isZero();
        }
    }

    @Nested
    @DisplayName("Numerica")
    class Numerica {

        private final CriterioDeCorreccion criterio =
                new CriterioDeCorreccion.Numerica(9.8, 0.1);

        private int corregir(Double valor, int puntaje) {
            return corrector.corregir(TipoDeItem.NUMERICA, criterio,
                    new RespuestaDeItem.Numerica(valor), puntaje);
        }

        @Test
        void el_valor_exacto_da_el_puntaje() {
            assertThat(corregir(9.8, 15)).isEqualTo(15);
        }

        @Test
        void los_bordes_de_la_tolerancia_entran() {
            assertThat(corregir(9.7, 15)).isEqualTo(15);
            assertThat(corregir(9.9, 15)).isEqualTo(15);
        }

        @Test
        void afuera_de_la_tolerancia_da_cero() {
            assertThat(corregir(9.6, 15)).isZero();
            assertThat(corregir(10.0, 15)).isZero();
        }

        @Test
        void el_borde_sobrevive_al_error_de_coma_flotante() {
            // 0.3 - 0.2 no da 0.1 exacto en binario. Sin el epsilon del
            // corrector este caso falla, y falla justo en el borde, que es el
            // unico lugar donde alguien lo mira.
            CriterioDeCorreccion c = new CriterioDeCorreccion.Numerica(0.2, 0.1);
            assertThat(corrector.corregir(TipoDeItem.NUMERICA, c,
                    new RespuestaDeItem.Numerica(0.3), 10)).isEqualTo(10);
        }

        @Test
        void sin_tolerancia_es_exacto() {
            CriterioDeCorreccion c = new CriterioDeCorreccion.Numerica(0.0, null);
            assertThat(corrector.corregir(TipoDeItem.NUMERICA, c,
                    new RespuestaDeItem.Numerica(0.0), 10)).isEqualTo(10);
            assertThat(corrector.corregir(TipoDeItem.NUMERICA, c,
                    new RespuestaDeItem.Numerica(0.5), 10)).isZero();
        }

        @Test
        void en_blanco_da_cero() {
            assertThat(corregir(null, 15)).isZero();
        }
    }

    @Nested
    @DisplayName("Validacion")
    class Validacion {

        private void validarCorta(CriterioDeCorreccion c) {
            validador.validar(TipoDeItem.RESPUESTA_CORTA, "Un enunciado",
                    new PayloadDeItem.RespuestaCorta("Una o dos palabras."), c);
        }

        @Test
        void el_criterio_valido_pasa() {
            validarCorta(laxa(new CriterioDeCorreccion.Aceptada("saga", 100)));
        }

        @Test
        void sin_ninguna_al_100_se_rechaza() {
            // Contestar perfecto tiene que dar el puntaje completo del item; si
            // el maximo fuera 80, el peso elegido al componer dejaria de
            // significar lo que dice.
            assertThatThrownBy(() -> validarCorta(laxa(new CriterioDeCorreccion.Aceptada("saga", 80))))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.SIN_ACEPTADA_AL_100);
        }

        @Test
        void sin_aceptadas_se_rechaza() {
            assertThatThrownBy(() -> validarCorta(laxa()))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.SIN_RESPUESTA_ACEPTADA);
        }

        @Test
        void una_repetida_bajo_la_comparacion_laxa_se_rechaza() {
            // "saga" y "SAGA" son la misma respuesta con el interruptor apagado,
            // y la segunda no se alcanzaria nunca.
            assertThatThrownBy(() -> validarCorta(
                    laxa(new CriterioDeCorreccion.Aceptada("saga", 100),
                         new CriterioDeCorreccion.Aceptada("SAGA", 50))))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.RESPUESTA_ACEPTADA_REPETIDA);
        }

        @Test
        void la_misma_pareja_con_el_interruptor_prendido_es_valida() {
            validador.validar(TipoDeItem.RESPUESTA_CORTA, "Un enunciado",
                    new PayloadDeItem.RespuestaCorta("Una palabra."),
                    new CriterioDeCorreccion.RespuestaCorta(
                            List.of(new CriterioDeCorreccion.Aceptada("saga", 100),
                                    new CriterioDeCorreccion.Aceptada("SAGA", 50)),
                            true, true));
        }

        @Test
        void un_porcentaje_fuera_de_rango_se_rechaza() {
            assertThatThrownBy(() -> validarCorta(
                    laxa(new CriterioDeCorreccion.Aceptada("saga", 100),
                         new CriterioDeCorreccion.Aceptada("otra", 0))))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.PORCENTAJE_ACEPTADA_FUERA_DE_RANGO);
        }

        @Test
        void la_numerica_necesita_un_valor() {
            assertThatThrownBy(() -> validador.validar(TipoDeItem.NUMERICA, "Un enunciado",
                    new PayloadDeItem.Numerica("Un número.", null),
                    new CriterioDeCorreccion.Numerica(null, 0.1)))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.VALOR_NUMERICO_REQUERIDO);
        }

        @Test
        void la_tolerancia_negativa_se_rechaza_en_vez_de_corregirse_sola() {
            assertThatThrownBy(() -> validador.validar(TipoDeItem.NUMERICA, "Un enunciado",
                    new PayloadDeItem.Numerica("Un número.", null),
                    new CriterioDeCorreccion.Numerica(9.8, -0.1)))
                    .isInstanceOf(ExcepcionDeNegocio.class)
                    .extracting(e -> ((ExcepcionDeNegocio) e).clave())
                    .isEqualTo(ClaveError.TOLERANCIA_NEGATIVA);
        }

        @Test
        void los_dos_tipos_son_autocorregibles() {
            // Si alguno dejara de serlo, un cuestionario que los use saldria con
            // correccion DIFERIDA y le abriria cola al profesor sin motivo (CI-07).
            assertThat(TipoDeItem.RESPUESTA_CORTA.esAutocorregible()).isTrue();
            assertThat(TipoDeItem.NUMERICA.esAutocorregible()).isTrue();
        }
    }
}
