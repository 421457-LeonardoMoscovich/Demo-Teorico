package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que contesto el alumno, con LA ESTAMPA de la version que vio (CI-12).
 *
 * Esta fila es la que hace innecesario congelar la composicion: dice
 * exactamente sobre que enunciado se lo evaluo, aunque el profesor haya
 * editado el item en el medio (CI-53).
 *
 * Aca el timestamp SI va: el registro academico es nominal por diseno. La
 * ausencia de marca temporal es una regla del modulo encuestas, no de este.
 */
@Entity
@Table(name = "respuesta")
public class RespuestaEntity {

    @Id
    private UUID id;

    @Column(name = "evaluacion_id", nullable = false)
    private UUID evaluacionId;

    @Column(name = "item_version_id", nullable = false)
    private UUID itemVersionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String contenido;

    @Column(name = "respondida_en", nullable = false)
    private Instant respondidaEn;

    protected RespuestaEntity() {
    }

    public RespuestaEntity(UUID id, UUID evaluacionId, UUID itemVersionId,
                           String contenido, Instant respondidaEn) {
        this.id = id;
        this.evaluacionId = evaluacionId;
        this.itemVersionId = itemVersionId;
        this.contenido = contenido;
        this.respondidaEn = respondidaEn;
    }

    public UUID getId() { return id; }
    public UUID getEvaluacionId() { return evaluacionId; }
    public UUID getItemVersionId() { return itemVersionId; }
    public String getContenido() { return contenido; }
    public Instant getRespondidaEn() { return respondidaEn; }
}
