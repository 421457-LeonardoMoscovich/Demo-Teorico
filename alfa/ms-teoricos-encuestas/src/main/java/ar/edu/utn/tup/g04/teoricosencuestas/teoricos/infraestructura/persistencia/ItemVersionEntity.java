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
 * El contenido que se responde. INMUTABLE (D-04).
 *
 * La inmutabilidad se defiende en tres lugares y en este orden:
 *  1. la forma de la clase: no hay un solo setter;
 *  2. el trigger t_item_version_no_update, que la garantiza el motor;
 *  3. el test que intenta el UPDATE por JDBC y afirma que explota.
 */
@Entity
@Table(name = "item_version")
public class ItemVersionEntity {

    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private String enunciado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String payload;

    /** La clave de correccion. NUNCA sale en una respuesta que ve un alumno. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private String criterio;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected ItemVersionEntity() {
    }

    public ItemVersionEntity(UUID id, UUID itemId, int version, String enunciado,
                             String payload, String criterio, Instant creadaEn) {
        this.id = id;
        this.itemId = itemId;
        this.version = version;
        this.enunciado = enunciado;
        this.payload = payload;
        this.criterio = criterio;
        this.creadaEn = creadaEn;
    }

    public UUID getId() { return id; }
    public UUID getItemId() { return itemId; }
    public int getVersion() { return version; }
    public String getEnunciado() { return enunciado; }
    public String getPayload() { return payload; }
    public String getCriterio() { return criterio; }
    public Instant getCreadaEn() { return creadaEn; }
}
