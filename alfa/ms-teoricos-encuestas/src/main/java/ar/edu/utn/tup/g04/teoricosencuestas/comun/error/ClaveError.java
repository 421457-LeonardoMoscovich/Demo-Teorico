package ar.edu.utn.tup.g04.teoricosencuestas.comun.error;

/**
 * Catalogo de errores. En la alfa la clave lleva su texto al lado; en el sprint
 * el texto sale de messages_*.properties (G04-HU04) y esta clase se queda solo
 * con la clave. Lo que ya vale ahora: el codigo nunca construye un mensaje
 * suelto, y todo error nombra un campo navegable.
 */
public enum ClaveError {

    ENUNCIADO_REQUERIDO("El enunciado no puede estar vacio"),
    TIPO_DIFERIDO("El tipo de item esta previsto en el modelo pero diferido en esta version (D-09)"),
    TIPO_FUERA_DE_ALFA("El tipo de item existe en el modelo pero no entra en la alfa (respuesta abierta: D-01)"),
    TIPO_DESCONOCIDO("Tipo de item desconocido"),
    PAYLOAD_INVALIDO("El payload del item no es valido para su tipo"),
    CRITERIO_REQUERIDO("El item necesita una clave de correccion"),
    OPCIONES_INSUFICIENTES("Un item de opcion multiple necesita al menos dos opciones"),
    SIN_OPCION_CORRECTA("Hay que marcar al menos una opcion correcta"),
    CORRECTA_INEXISTENTE("Una opcion marcada como correcta no existe entre las opciones"),
    UNICA_CORRECTA_ESPERADA("El item admite una sola respuesta correcta y hay mas de una marcada"),
    PESO_DE_OPCION_INEXISTENTE("Un porcentaje apunta a una opcion que no existe"),
    PESO_FUERA_DE_RANGO("El porcentaje de una opcion tiene que estar entre -100 y 100"),
    POSITIVOS_NO_SUMAN_100("Las calificaciones positivas tienen que sumar exactamente 100%"),
    CORRECTAS_NO_SUMAN_100("Las opciones marcadas como correctas tienen que sumar 100% entre sus porcentajes"),
    DEVOLUCION_SIN_OPCIONES("Este tipo de item no tiene opciones: la retroalimentacion por opcion no aplica"),
    DEVOLUCION_DE_OPCION_INEXISTENTE("Una retroalimentacion apunta a una opcion que no existe"),
    AFIRMACION_REQUERIDA("La afirmacion no puede estar vacia"),
    VALOR_VERDADERO_REQUERIDO("Hay que indicar si la afirmacion es verdadera o falsa"),
    PAR_IZQUIERDA_REPETIDA("Un concepto de la izquierda no puede emparejarse dos veces"),
    PAR_INEXISTENTE("Un par apunta a un elemento que no existe"),
    PARES_INCOMPLETOS("Faltan pares: cada concepto de la izquierda necesita el suyo"),
    SECUENCIA_NO_ES_PERMUTACION("La secuencia correcta tiene que usar todos los elementos, una vez cada uno"),
    ELEMENTOS_INSUFICIENTES("Hacen falta al menos dos elementos"),
    ID_DUPLICADO("Hay ids repetidos"),
    SIN_RESPUESTA_ACEPTADA("Hay que cargar al menos una respuesta que se acepte como correcta"),
    SIN_ACEPTADA_AL_100("Alguna de las respuestas aceptadas tiene que valer el 100%: si no, contestar perfecto no da el puntaje completo"),
    RESPUESTA_ACEPTADA_VACIA("Una respuesta aceptada no puede estar vacia"),
    RESPUESTA_ACEPTADA_REPETIDA("Esa respuesta ya esta en la lista: con esta configuracion las dos son la misma y la segunda no se alcanza nunca"),
    PORCENTAJE_ACEPTADA_FUERA_DE_RANGO("El porcentaje de una respuesta aceptada tiene que estar entre 1 y 100"),
    VALOR_NUMERICO_REQUERIDO("Hace falta el valor numerico correcto"),
    TOLERANCIA_NEGATIVA("La tolerancia no puede ser negativa"),
    RESPUESTA_NO_NUMERICA("Eso no es un numero"),

    CONSIGNA_REQUERIDA("La consigna no puede estar vacia"),
    RUBRICA_REQUERIDA("Una respuesta abierta necesita una rubrica: es lo unico que guia a quien corrige"),
    EXTENSION_NO_POSITIVA("La extension maxima tiene que ser mayor que cero"),

    CONTENIDO_SIN_ITEMS("Un cuestionario necesita al menos un item"),
    ORDEN_NO_CONSECUTIVO("El orden de los items tiene que ser consecutivo desde 1"),
    PUNTAJE_NO_POSITIVO("El puntaje de cada item tiene que ser mayor que cero"),
    PESOS_NO_SUMAN_100("Con escala porcentual los pesos tienen que sumar exactamente 100"),
    NAVEGACION_INVALIDA("La navegacion tiene que ser LIBRE o SECUENCIAL"),
    SORTEO_SIN_PREGUNTAS("Hay que sortear al menos una pregunta"),
    SORTEO_SIN_CANDIDATOS("No hay suficientes preguntas listas con esa etiqueta para sortear"),
    ITEM_INEXISTENTE("El item no existe, esta dado de baja o no es de este profesor"),
    ITEM_REPETIDO("Un item no puede aparecer dos veces en el mismo cuestionario"),
    ITEM_EN_BORRADOR("El item esta en borrador: terminalo y marcalo listo antes de usarlo"),
    ITEM_YA_COMPUESTO("El item ya esta en un cuestionario: no se puede volver a borrador"),
    ETIQUETA_VACIA("Una etiqueta no puede estar vacia"),
    ETIQUETA_DEMASIADO_LARGA("Una etiqueta no puede pasar de 40 caracteres"),
    DEMASIADAS_ETIQUETAS("Un item no puede tener mas de 8 etiquetas"),

    CONTENIDO_INEXISTENTE("El cuestionario no existe"),
    VALE_REQUERIDO("Hace falta un vale de lectura emitido por el Tema 03 (CI-18)"),
    VALE_INVALIDO("El vale de lectura no es valido"),
    VALE_VENCIDO("El vale de lectura vencio"),
    VALE_DE_OTRO_CONTENIDO("El vale no habilita a leer este cuestionario"),

    RESPUESTAS_INCOMPLETAS("Falta la respuesta de al menos un item del cuestionario"),
    RESPUESTA_DE_ITEM_AJENO("Llego la respuesta de un item que no pertenece al cuestionario"),
    EVALUACION_INEXISTENTE("No hay ninguna evaluacion para esa entrega"),
    DETALLE_INEXISTENTE("No hay ninguna correccion pendiente con ese id"),
    DETALLE_YA_CORREGIDO("Ese item ya fue corregido y una correccion no se pisa (CI-44)"),
    PUNTAJE_FUERA_DE_RANGO("El puntaje tiene que estar entre 0 y el peso del item"),
    NO_AUTORIZADO("No autorizado");

    private final String texto;

    ClaveError(String texto) { this.texto = texto; }

    public String texto() { return texto; }
}
