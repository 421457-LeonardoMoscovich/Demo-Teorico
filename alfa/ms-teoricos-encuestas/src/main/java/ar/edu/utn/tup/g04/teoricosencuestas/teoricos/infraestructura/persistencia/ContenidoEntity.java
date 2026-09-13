package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * El cuestionario. Antes se llamaba desafio_teorico; con CI-03 el desafio es
 * del Tema 03 y lo nuestro es el CONTENIDO al que ese desafio apunta.
 *
 * La version del contenido sube cuando cambia su composicion, y es la que
 * viaja en la ficha (contenidoRef.version) para que el 03 pueda cachearla.
 */
@Entity
@Table(name = "contenido")
public class ContenidoEntity {

    @Id
    private UUID id;

    @Column(name = "profesor_id", nullable = false)
    private UUID profesorId;

    @Column(name = "curso_cohorte_id", nullable = false)
    private UUID cursoCohorteId;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, length = 16)
    private String escala;

    /** LIBRE | SECUENCIAL. Como recorre el alumno las consignas (V9). */
    @Column(nullable = false, length = 16)
    private String navegacion;

    @Column(name = "baja_logica")
    private Instant bajaLogica;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected ContenidoEntity() {
    }

    public ContenidoEntity(UUID id, UUID profesorId, UUID cursoCohorteId, String titulo,
                           String escala, String navegacion, Instant creadoEn) {
        this.id = id;
        this.profesorId = profesorId;
        this.cursoCohorteId = cursoCohorteId;
        this.titulo = titulo;
        this.version = 1;
        this.escala = escala;
        this.navegacion = navegacion;
        this.creadoEn = creadoEn;
    }

    public UUID getId() { return id; }
    public UUID getProfesorId() { return profesorId; }
    public UUID getCursoCohorteId() { return cursoCohorteId; }
    public String getTitulo() { return titulo; }
    public int getVersion() { return version; }
    public String getEscala() { return escala; }
    public String getNavegacion() { return navegacion; }
    public Instant getBajaLogica() { return bajaLogica; }
    public Instant getCreadoEn() { return creadoEn; }

    public void subirVersion() { this.version++; }

    public void darDeBaja(Instant cuando) { this.bajaLogica = cuando; }
}
