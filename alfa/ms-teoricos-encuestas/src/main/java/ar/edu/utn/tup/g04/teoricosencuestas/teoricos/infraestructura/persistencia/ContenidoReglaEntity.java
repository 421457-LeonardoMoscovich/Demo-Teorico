package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * La regla de sorteo de un cuestionario: "N preguntas al azar de esta
 * etiqueta, y cada una vale esto" (V10).
 *
 * Es la parte del cuestionario que NO es una lista de items. Lo que el alumno
 * termina recibiendo se deriva de (contenidoId, alumnoId) al leer y no se
 * guarda en ningun lado, asi que aca no hay —ni puede haber— una fila por
 * alumno: eso seria estado de lectura (CI-19).
 */
@Entity
@Table(name = "contenido_regla")
public class ContenidoReglaEntity {

    @Id
    @Column(name = "contenido_id")
    private UUID contenidoId;

    @Column(nullable = false, length = 40)
    private String etiqueta;

    @Column(nullable = false)
    private int cuantos;

    /** Lo que vale CADA sorteada. Uniforme: ver el comentario de la V10. */
    @Column(nullable = false)
    private int puntaje;

    protected ContenidoReglaEntity() {
    }

    public ContenidoReglaEntity(UUID contenidoId, String etiqueta, int cuantos, int puntaje) {
        this.contenidoId = contenidoId;
        this.etiqueta = etiqueta;
        this.cuantos = cuantos;
        this.puntaje = puntaje;
    }

    public UUID getContenidoId() { return contenidoId; }
    public String getEtiqueta() { return etiqueta; }
    public int getCuantos() { return cuantos; }
    public int getPuntaje() { return puntaje; }

    /** Lo que aporta al total del cuestionario, sin saber quien pregunta. */
    public int puntajeTotal() { return cuantos * puntaje; }
}
