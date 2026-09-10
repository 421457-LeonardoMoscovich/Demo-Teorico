package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ClaveError;
import ar.edu.utn.tup.g04.teoricosencuestas.comun.error.ExcepcionDeNegocio;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.CriterioDeCorreccion;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.MapeadorJson;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.PayloadDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.ValidadorDePayload;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemRepository;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemVersionEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * El banco de items del profesor (RF-DES-02).
 *
 * Editar NUNCA muta: publica una version nueva y no toca ninguna anterior
 * (D-04). Por eso no existe un metodo editarVersion, ni aca ni en el
 * controlador: lo que no existe no se llama por accidente.
 */
@Service
public class BancoDeItemsService {

    private final ItemRepository items;
    private final ItemVersionRepository versiones;
    private final ValidadorDePayload validador;
    private final MapeadorJson json;

    public BancoDeItemsService(ItemRepository items, ItemVersionRepository versiones,
                               ValidadorDePayload validador, MapeadorJson json) {
        this.items = items;
        this.versiones = versiones;
        this.validador = validador;
        this.json = json;
    }

    public record Contenido(String enunciado, JsonNode payload, JsonNode criterio) {}

    @Transactional
    public ItemVersionEntity crear(UUID profesorId, TipoDeItem tipo, Contenido contenido) {
        validar(tipo, contenido);
        ItemEntity item = items.save(new ItemEntity(UUID.randomUUID(), profesorId, tipo));
        return publicar(item, contenido);
    }

    /**
     * PUT sobre un item no edita: crea la version siguiente. El verbo es PUT
     * porque asi esta en el contrato, pero el efecto es crear, y eso hay que
     * documentarlo en el OpenAPI para que nadie del Tema 03 se confunda.
     */
    @Transactional
    public ItemVersionEntity publicarVersion(UUID profesorId, UUID itemId, Contenido contenido) {
        ItemEntity item = exigirPropio(profesorId, itemId);
        validar(item.getTipo(), contenido);
        return publicar(item, contenido);
    }

    private ItemVersionEntity publicar(ItemEntity item, Contenido contenido) {
        int version = item.getVersionActual() + 1;
        ItemVersionEntity nueva = versiones.save(new ItemVersionEntity(
                UUID.randomUUID(), item.getId(), version, contenido.enunciado(),
                json.escribir(contenido.payload()),
                contenido.criterio() == null ? null : json.escribir(contenido.criterio()),
                Instant.now()));
        item.marcarVersionActual(version);
        items.save(item);
        return nueva;
    }

    private void validar(TipoDeItem tipo, Contenido contenido) {
        PayloadDeItem payload = json.leerPayload(tipo, contenido.payload());
        CriterioDeCorreccion criterio = contenido.criterio() == null
                ? null
                : json.leerCriterio(tipo, contenido.criterio());
        validador.validar(tipo, contenido.enunciado(), payload, criterio);
    }

    @Transactional
    public void darDeBaja(UUID profesorId, UUID itemId) {
        ItemEntity item = exigirPropio(profesorId, itemId);
        item.darDeBaja(Instant.now());
        items.save(item);
    }

    @Transactional(readOnly = true)
    public List<ItemEntity> listar(UUID profesorId, TipoDeItem tipo) {
        return tipo == null
                ? items.findByProfesorIdAndBajaLogicaIsNullOrderByTipoAsc(profesorId)
                : items.findByProfesorIdAndTipoAndBajaLogicaIsNullOrderByTipoAsc(profesorId, tipo);
    }

    @Transactional(readOnly = true)
    public ItemEntity exigirPropio(UUID profesorId, UUID itemId) {
        return items.findByIdAndProfesorIdAndBajaLogicaIsNull(itemId, profesorId)
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.ITEM_INEXISTENTE, "itemId"));
    }

    /**
     * Sin filtro por profesor: lo usa la lectura del cuestionario, donde el que
     * mira es un alumno. La autorizacion de esa lectura la da el vale (CI-18),
     * no la pertenencia del item.
     */
    @Transactional(readOnly = true)
    public ItemEntity exigirVigente(UUID itemId) {
        return items.findByIdAndBajaLogicaIsNull(itemId)
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.ITEM_INEXISTENTE, "itemId"));
    }

    /**
     * La version vigente de un item. Se resuelve en cada lectura porque la
     * referencia del cuestionario es flotante (CI-13).
     */
    @Transactional(readOnly = true)
    public ItemVersionEntity versionVigente(ItemEntity item) {
        return versiones.findByItemIdAndVersion(item.getId(), item.getVersionActual())
                .orElseThrow(() -> new ExcepcionDeNegocio(ClaveError.ITEM_INEXISTENTE, "itemId"));
    }
}
