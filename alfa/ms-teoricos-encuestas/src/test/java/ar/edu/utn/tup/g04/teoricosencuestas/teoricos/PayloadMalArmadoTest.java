package ar.edu.utn.tup.g04.teoricosencuestas.teoricos;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.CriterioDeCorreccion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.PayloadDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.ValidadorDePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Cada caso invalido afirma DOS cosas: la clave del error y el campo exacto que
 * nombra. El campo es lo que el front usa para marcar el input, asi que un
 * mensaje correcto apuntando al campo equivocado es un bug.
 */
class PayloadMalArmadoTest {

    private final ValidadorDePayload validador = new ValidadorDePayload();

    private static PayloadDeItem.Opcion o(String id, String texto) {
        return new PayloadDeItem.Opcion(id, texto);
    }

    static Stream<Arguments> casosInvalidos() {
        var opciones = List.of(o("a", "Paris"), o("b", "Roma"));
        var elementos = List.of(o("e1", "uno"), o("e2", "dos"));
        var izq = List.of(o("i1", "A"), o("i2", "B"));
        var der = List.of(o("d1", "X"), o("d2", "Y"));

        return Stream.of(
                Arguments.of("enunciado en blanco",
                        TipoDeItem.OPCION_MULTIPLE, "   ",
                        new PayloadDeItem.OpcionMultiple(opciones, false),
                        new CriterioDeCorreccion.OpcionMultiple(List.of("a")),
                        ClaveError.ENUNCIADO_REQUERIDO, "enunciado"),

                Arguments.of("una sola opcion",
                        TipoDeItem.OPCION_MULTIPLE, "e",
                        new PayloadDeItem.OpcionMultiple(List.of(o("a", "Paris")), false),
                        new CriterioDeCorreccion.OpcionMultiple(List.of("a")),
                        ClaveError.OPCIONES_INSUFICIENTES, "payload.opciones"),

                Arguments.of("ninguna correcta",
                        TipoDeItem.OPCION_MULTIPLE, "e",
                        new PayloadDeItem.OpcionMultiple(opciones, false),
                        new CriterioDeCorreccion.OpcionMultiple(List.of()),
                        ClaveError.SIN_OPCION_CORRECTA, "criterio.correctas"),

                Arguments.of("correcta que no existe entre las opciones",
                        TipoDeItem.OPCION_MULTIPLE, "e",
                        new PayloadDeItem.OpcionMultiple(opciones, false),
                        new CriterioDeCorreccion.OpcionMultiple(List.of("zzz")),
                        ClaveError.CORRECTA_INEXISTENTE, "criterio.correctas[0]"),

                Arguments.of("dos correctas con multiple en false",
                        TipoDeItem.OPCION_MULTIPLE, "e",
                        new PayloadDeItem.OpcionMultiple(opciones, false),
                        new CriterioDeCorreccion.OpcionMultiple(List.of("a", "b")),
                        ClaveError.UNICA_CORRECTA_ESPERADA, "criterio.correctas"),

                Arguments.of("ids de opcion repetidos",
                        TipoDeItem.OPCION_MULTIPLE, "e",
                        new PayloadDeItem.OpcionMultiple(List.of(o("a", "X"), o("a", "Y")), false),
                        new CriterioDeCorreccion.OpcionMultiple(List.of("a")),
                        ClaveError.ID_DUPLICADO, "payload.opciones"),

                Arguments.of("afirmacion vacia",
                        TipoDeItem.VERDADERO_FALSO, "e",
                        new PayloadDeItem.VerdaderoFalso("  "),
                        new CriterioDeCorreccion.VerdaderoFalso(true),
                        ClaveError.AFIRMACION_REQUERIDA, "payload.afirmacion"),

                Arguments.of("sin decir si es verdadera",
                        TipoDeItem.VERDADERO_FALSO, "e",
                        new PayloadDeItem.VerdaderoFalso("El cielo es azul"),
                        new CriterioDeCorreccion.VerdaderoFalso(null),
                        ClaveError.VALOR_VERDADERO_REQUERIDO, "criterio.esVerdadero"),

                Arguments.of("un concepto emparejado dos veces",
                        TipoDeItem.EMPAREJAR, "e",
                        new PayloadDeItem.Emparejar(izq, der),
                        new CriterioDeCorreccion.Emparejar(
                                List.of(List.of("i1", "d1"), List.of("i1", "d2"))),
                        ClaveError.PAR_IZQUIERDA_REPETIDA, "criterio.pares[1][0]"),

                Arguments.of("par que apunta a un id inexistente",
                        TipoDeItem.EMPAREJAR, "e",
                        new PayloadDeItem.Emparejar(izq, der),
                        new CriterioDeCorreccion.Emparejar(
                                List.of(List.of("i1", "zzz"), List.of("i2", "d2"))),
                        ClaveError.PAR_INEXISTENTE, "criterio.pares[0][1]"),

                Arguments.of("falta emparejar un concepto",
                        TipoDeItem.EMPAREJAR, "e",
                        new PayloadDeItem.Emparejar(izq, der),
                        new CriterioDeCorreccion.Emparejar(List.of(List.of("i1", "d1"))),
                        ClaveError.PARES_INCOMPLETOS, "criterio.pares"),

                Arguments.of("secuencia a la que le falta un elemento",
                        TipoDeItem.ORDENAR, "e",
                        new PayloadDeItem.Ordenar(elementos),
                        new CriterioDeCorreccion.Ordenar(List.of("e1")),
                        ClaveError.SECUENCIA_NO_ES_PERMUTACION, "criterio.secuencia"),

                Arguments.of("secuencia con un elemento repetido",
                        TipoDeItem.ORDENAR, "e",
                        new PayloadDeItem.Ordenar(elementos),
                        new CriterioDeCorreccion.Ordenar(List.of("e1", "e1")),
                        ClaveError.SECUENCIA_NO_ES_PERMUTACION, "criterio.secuencia"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("casosInvalidos")
    void rechaza_nombrando_el_campo(String caso, TipoDeItem tipo, String enunciado,
                                    PayloadDeItem payload, CriterioDeCorreccion criterio,
                                    ClaveError claveEsperada, String campoEsperado) {
        ExcepcionDeNegocio e = catchThrowableOfType(
                () -> validador.validar(tipo, enunciado, payload, criterio),
                ExcepcionDeNegocio.class);

        assertThat(e).as(caso).isNotNull();
        assertThat(e.clave()).as(caso).isEqualTo(claveEsperada);
        assertThat(e.campo()).as(caso).isEqualTo(campoEsperado);
    }

    @Test
    void los_cuatro_tipos_validos_pasan() {
        validador.validar(TipoDeItem.OPCION_MULTIPLE, "e",
                new PayloadDeItem.OpcionMultiple(List.of(o("a", "A"), o("b", "B")), false),
                new CriterioDeCorreccion.OpcionMultiple(List.of("a")));

        validador.validar(TipoDeItem.VERDADERO_FALSO, "e",
                new PayloadDeItem.VerdaderoFalso("Una afirmacion"),
                new CriterioDeCorreccion.VerdaderoFalso(false));

        validador.validar(TipoDeItem.EMPAREJAR, "e",
                new PayloadDeItem.Emparejar(List.of(o("i1", "A"), o("i2", "B")),
                        List.of(o("d1", "X"), o("d2", "Y"))),
                new CriterioDeCorreccion.Emparejar(List.of(List.of("i1", "d2"), List.of("i2", "d1"))));

        validador.validar(TipoDeItem.ORDENAR, "e",
                new PayloadDeItem.Ordenar(List.of(o("e1", "1"), o("e2", "2"))),
                new CriterioDeCorreccion.Ordenar(List.of("e2", "e1")));
    }

    @Test
    void los_tipos_diferidos_se_rechazan_con_422_y_no_con_400() {
        // D-09: que diga "diferido" y no "invalido". Quien lo reciba tiene que
        // entender que el tipo existe y que va a llegar, no que se equivoco de
        // nombre. Por eso 422 y no 400.
        assertThatThrownBy(() -> validador.validar(TipoDeItem.DEBATE, "e",
                new PayloadDeItem.Debate("c", List.of()),
                new CriterioDeCorreccion.Abierta("r")))
                .isInstanceOfSatisfying(ExcepcionDeNegocio.class, e -> {
                    assertThat(e.clave()).isEqualTo(ClaveError.TIPO_DIFERIDO);
                    assertThat(e.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                });
    }
}
