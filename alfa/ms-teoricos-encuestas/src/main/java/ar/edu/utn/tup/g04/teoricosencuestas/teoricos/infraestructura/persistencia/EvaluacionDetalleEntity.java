package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Cuanto saco el alumno en cada item.
 *
 * CI-28: esto NO viaja en el evento hacia el Tema 03 (a el le alcanza la nota
 * para su XP). Se lo servimos al front, que es quien tiene al alumno adelante.
 */
@Entity
@Table(name = "evaluacion_detalle")
public class EvaluacionDetalleEntity {

    @Id
    private UUID id;

    @Column(name = "evaluacion_id", nullable = false)
    private UUID evaluacionId;

    @Column(name = "item_version_id", nullable = false)
    private UUID itemVersionId;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private int puntaje;

    @Column(nullable = false)
    private int obtenido;

    protected EvaluacionDetalleEntity() {
    }

    public EvaluacionDetalleEntity(UUID id, UUID evaluacionId, UUID itemVersionId,
                                   int orden, int puntaje, int obtenido) {
        this.id = id;
        this.evaluacionId = evaluacionId;
        this.itemVersionId = itemVersionId;
        this.orden = orden;
        this.puntaje = puntaje;
        this.obtenido = obtenido;
    }

    public UUID getId() { return id; }
    public UUID getEvaluacionId() { return evaluacionId; }
    public UUID getItemVersionId() { return itemVersionId; }
    public int getOrden() { return orden; }
    public int getPuntaje() { return puntaje; }
    public int getObtenido() { return obtenido; }
}
