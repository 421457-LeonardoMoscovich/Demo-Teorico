package ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web;

import ar.edu.utn.tup.g04.teoricosencuestas.comun.identidad.IdentidadActual;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.aplicacion.BancoDeItemsService;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.TipoDeItem;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.dominio.payload.MapeadorJson;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.persistencia.ItemVersionEntity;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.CrearItemRequest;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.ItemDetalleResponse;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.ItemResumenResponse;
import ar.edu.utn.tup.g04.teoricosencuestas.teoricos.infraestructura.web.dto.VistaAlumnoResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * El banco es del profesor y solo del profesor: el profesorId sale SIEMPRE del
 * token y nunca de un parametro, asi que un GET del banco ajeno no se puede ni
 * siquiera expresar (RF-USR-07).
 */
@Tag(name = "Banco de ítems")
@SecurityRequirement(name = "token")
@RestController
@RequestMapping("/teoricos/items")
public class ItemController {

    private final BancoDeItemsService banco;
    private final IdentidadActual identidad;
    private final MapeadorJson json;

    public ItemController(BancoDeItemsService banco, IdentidadActual identidad, MapeadorJson json) {
        this.banco = banco;
        this.identidad = identidad;
        this.json = json;
    }

    @PostMapping
    public ResponseEntity<ItemDetalleResponse> crear(@Valid @RequestBody CrearItemRequest req) {
        ItemVersionEntity version = banco.crear(identidad.id(), req.tipo(), req.aContenido());
        ItemDetalleResponse cuerpo = detalle(banco.exigirPropio(identidad.id(), version.getItemId()), version);
        return ResponseEntity.created(URI.create("/teoricos/items/" + version.getItemId())).body(cuerpo);
    }

    /** No edita: publica la version siguiente y no toca ninguna anterior (D-04). */
    @PutMapping("/{id}")
    public ItemDetalleResponse publicarVersion(@PathVariable UUID id,
                                               @Valid @RequestBody CrearItemRequest req) {
        ItemVersionEntity version = banco.publicarVersion(identidad.id(), id, req.aContenido());
        return detalle(banco.exigirPropio(identidad.id(), id), version);
    }

    @GetMapping
    public List<ItemResumenResponse> listar(@RequestParam(required = false) TipoDeItem tipo) {
        return banco.listar(identidad.id(), tipo).stream()
                .map(item -> resumen(item, banco.versionVigente(item)))
                .toList();
    }

    @GetMapping("/{id}")
    public ItemDetalleResponse ver(@PathVariable UUID id) {
        ItemEntity item = banco.exigirPropio(identidad.id(), id);
        return detalle(item, banco.versionVigente(item));
    }

    @Operation(summary = "El ítem como lo va a ver el alumno",
            description = """
                    Vista previa para el profesor, antes de componer (CI-59).

                    **Devuelve la misma clase que el endpoint del alumno** (`ItemParaAlumno`,
                    de `VistaAlumnoResponse`), y eso es lo que la hace valer: no es una
                    maqueta de cómo se vería, es literalmente la proyección que se sirve en
                    el examen. Si algún día filtrara un campo de corrección, filtraría en
                    los dos lados o en ninguno — no puede haber una vista previa "limpia"
                    sobre un endpoint que no lo está.

                    El `orden` y el `puntaje` van en cero: los dos los decide el profesor al
                    componer, y todavía no compuso nada.
                    """)
    @GetMapping("/{id}/vista-previa")
    public VistaAlumnoResponse.ItemParaAlumno vistaPrevia(@PathVariable UUID id) {
        ItemEntity item = banco.exigirPropio(identidad.id(), id);
        ItemVersionEntity version = banco.versionVigente(item);
        return new VistaAlumnoResponse.ItemParaAlumno(
                version.getId(), item.getTipo(), version.getEnunciado(),
                0, 0,
                json.leerPayload(item.getTipo(), json.aNodo(version.getPayload())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable UUID id) {
        banco.darDeBaja(identidad.id(), id);
    }

    private ItemResumenResponse resumen(ItemEntity item, ItemVersionEntity version) {
        return new ItemResumenResponse(item.getId(), item.getTipo(), version.getEnunciado(),
                version.getVersion(), item.getTipo().esAutocorregible(), item.getEstado());
    }

    private ItemDetalleResponse detalle(ItemEntity item, ItemVersionEntity version) {
        return new ItemDetalleResponse(item.getId(), item.getTipo(), version.getEnunciado(),
                version.getVersion(), version.getId(),
                json.aNodo(version.getPayload()),
                version.getCriterio() == null ? null : json.aNodo(version.getCriterio()),
                version.getDevolucion() == null ? null : json.aNodo(version.getDevolucion()),
                item.getEstado());
    }
}
