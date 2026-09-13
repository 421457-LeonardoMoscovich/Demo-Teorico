import { Component, computed, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { FormularioItemComponent } from './formulario-item';
import {
  ErrorApi,
  EtiquetaConUso,
  ItemResumen,
  TIPOS,
  TipoDeItem,
  VistaPreviaItem,
} from '../core/modelos';

/**
 * Cuantas preguntas se pintan de una. Seis y no diez: cada fila lleva
 * enunciado, etiquetas y tres acciones, asi que diez ya no entran en una
 * pantalla de portatil y volvemos al problema que la paginacion vino a sacar.
 */
const POR_PAGINA = 6;

@Component({
  selector: 'app-profesor-banco',
  standalone: true,
  imports: [FormsModule, JsonPipe, FormularioItemComponent],
  template: `
    @if (editando()) {
      <section class="tarjeta ancho">
        <div class="acciones-fila">
          <button type="button" class="secundario" (click)="volverAlBanco()">
            <span aria-hidden="true">←</span> Volver al banco
          </button>
        </div>
        <app-formulario-item
          [editarId]="editarId()"
          (guardado)="alGuardar()"
          (cancelado)="volverAlBanco()"
        />
      </section>
    } @else {
      <section class="tarjeta ancho">
        <h2>Banco de ítems</h2>
        <details class="porque">
          <summary>De quién es el banco, y qué pasa al editar</summary>
          <div class="cuerpo">
            <p class="ayuda">
              Cada ítem tiene identidad propia y contenido versionado: editar publica una versión
              nueva y nunca toca lo ya respondido.
            </p>
            <p class="ayuda">
              El banco es <strong>tuyo, no de un curso</strong>: una pregunta no se asigna a
              ninguna materia. Se asigna al armar el cuestionario, y ese cuestionario es el que se
              cuelga de la unidad de un curso — por eso el mismo ítem puede entrar en dos cursos
              distintos.
            </p>
          </div>
        </details>

        <div class="acciones-fila">
          <button type="button" (click)="escribirNueva()">+ Escribir una pregunta nueva</button>
        </div>

        <label class="filtro">
          Filtrar por tipo
          <select [(ngModel)]="filtro" name="filtro" (ngModelChange)="cargar()">
            <option value="">Todos</option>
            @for (t of tipos; track t.valor) {
              <option [value]="t.valor">{{ t.etiqueta }}</option>
            }
          </select>
        </label>

        @if (conocidas().length > 0) {
          <p class="ayuda">Filtrar por etiqueta</p>
          <div class="chips">
            <button
              type="button"
              class="chip"
              [class.activo]="porEtiqueta() === ''"
              (click)="filtrarPor('')"
            >
              todas
            </button>
            @for (e of conocidas(); track e.etiqueta) {
              <button
                type="button"
                class="chip"
                [class.activo]="porEtiqueta() === e.etiqueta"
                (click)="filtrarPor(e.etiqueta)"
              >
                {{ e.etiqueta }} · {{ e.cuantos }}
              </button>
            }
          </div>
        }

        @if (error()) {
          <p class="error" role="alert">{{ error()!.mensaje }}</p>
        }
        @if (ok()) {
          <p class="ok" role="status">{{ ok() }}</p>
        }

        @if (items().length === 0) {
          <p class="vacio">Todavía no hay ítems. Escribí el primero.</p>
        }

        <ul class="lista">
          @for (i of enPantalla(); track i.id; let idx = $index) {
            <li [style.--orden]="idx">
              <div>
                <span class="etiqueta">{{ etiqueta(i.tipo) }}</span>
                @if (i.estado === 'BORRADOR') {
                  <span class="etiqueta diferida">borrador</span>
                }
                <span class="version">v{{ i.version }}</span>
                <p>{{ i.enunciado }}</p>
                @if (i.etiquetas.length > 0) {
                  <div class="chips">
                    @for (e of i.etiquetas; track e) {
                      <button type="button" class="chip" (click)="filtrarPor(e)">{{ e }}</button>
                    }
                  </div>
                }

                <!--
                  La vista previa (CI-59). Lo que se pinta acá sale del MISMO
                  endpoint que se le sirve al alumno en el examen, así que si
                  alguna vez filtrara la clave de corrección, se vería acá.
                -->
                @if (previaDe() === i.id && previa(); as p) {
                  <div class="previa">
                    <p class="ayuda">
                      <span class="fuente">Así lo ve el alumno</span>
                      servido por el mismo endpoint que el examen
                    </p>
                    <p class="titulo-desafio">{{ p.enunciado }}</p>
                    @for (o of opcionesDe(p); track o.id) {
                      <label class="check">
                        <input type="checkbox" disabled />
                        {{ o.texto }}
                      </label>
                    }
                    <details>
                      <summary>Lo que viaja, tal cual</summary>
                      <pre>{{ p | json }}</pre>
                    </details>
                  </div>
                }
              </div>
              <div class="acciones-fila">
                @if (porDarDeBaja() === i.id) {
                  <span class="ayuda">¿Seguro?</span>
                  <button class="peligro" type="button" (click)="confirmarBaja(i)">
                    Sí, dar de baja
                  </button>
                  <button type="button" class="secundario" (click)="porDarDeBaja.set(null)">
                    Cancelar
                  </button>
                } @else {
                  <button type="button" class="secundario" (click)="alternarPrevia(i)">
                    {{ previaDe() === i.id ? 'Cerrar previa' : 'Vista previa' }}
                  </button>
                  <button type="button" class="secundario" (click)="editar(i.id)">Editar</button>
                  <button class="peligro" type="button" (click)="porDarDeBaja.set(i.id)">
                    Dar de baja
                  </button>
                }
              </div>
            </li>
          }
        </ul>

        <!--
          Paginar y no scrollear: con doscientas preguntas, la pantalla larga no
          es un problema de scroll sino de que no se encuentra nada. Los filtros
          de arriba son la forma de buscar; esto es solo para no pintar
          doscientas filas de una.
        -->
        @if (paginas() > 1) {
          <div class="paginador">
            <button
              type="button"
              class="secundario"
              [disabled]="pagina() === 0"
              (click)="pagina.set(pagina() - 1)"
            >
              <span aria-hidden="true">←</span> Anteriores
            </button>
            <span class="ayuda" role="status" aria-live="polite">
              {{ desde() + 1 }}–{{ hasta() }} de {{ items().length }}
            </span>
            <button
              type="button"
              class="secundario"
              [disabled]="pagina() >= paginas() - 1"
              (click)="pagina.set(pagina() + 1)"
            >
              Siguientes <span aria-hidden="true">→</span>
            </button>
          </div>
        }
      </section>
    }
  `,
})
export class ProfesorBancoPage {
  private readonly api = inject(ApiService);

  readonly tipos = TIPOS;
  readonly items = signal<ItemResumen[]>([]);
  /** Solo de las acciones de la lista: las del formulario las muestra el formulario. */
  readonly error = signal<ErrorApi | null>(null);
  readonly ok = signal('');

  filtro: TipoDeItem | '' = '';

  /** La etiqueta por la que se esta filtrando. Vacio = todas. */
  readonly porEtiqueta = signal('');

  /** El vocabulario del profesor, con el conteo de cada etiqueta. */
  readonly conocidas = signal<EtiquetaConUso[]>([]);

  /** La vista previa abierta, si hay alguna. */
  readonly previa = signal<VistaPreviaItem | null>(null);
  readonly previaDe = signal<string | null>(null);

  /**
   * Que ítem está abierto en el formulario. null = uno nuevo.
   *
   * El formulario ocupa la pantalla entera en vez de una columna al costado: es
   * largo —siete tipos, cada uno con lo suyo— y tenerlo siempre ahí obligaba a
   * scrollear el banco entero para llegar a la lista. Son dos tareas, no una.
   */
  readonly editarId = signal<string | null>(null);
  readonly editando = signal(false);

  /** La pagina de la lista. Ocho por pagina entran sin scroll en un portátil. */
  readonly pagina = signal(0);
  readonly porPagina = POR_PAGINA;

  readonly paginas = computed(() => Math.ceil(this.items().length / POR_PAGINA));
  readonly desde = computed(() => this.pagina() * POR_PAGINA);
  readonly hasta = computed(() => Math.min(this.desde() + POR_PAGINA, this.items().length));
  readonly enPantalla = computed(() => this.items().slice(this.desde(), this.hasta()));

  /** El item cuya baja se esta confirmando, o null. */
  readonly porDarDeBaja = signal<string | null>(null);

  constructor() {
    this.cargar();
  }

  escribirNueva(): void {
    this.editarId.set(null);
    this.editando.set(true);
  }

  editar(id: string): void {
    this.editarId.set(id);
    this.editando.set(true);
  }

  volverAlBanco(): void {
    this.editando.set(false);
    this.editarId.set(null);
  }

  etiqueta(tipo: TipoDeItem): string {
    return TIPOS.find((t) => t.valor === tipo)?.etiqueta ?? tipo;
  }

  cargar(): void {
    this.porDarDeBaja.set(null);
    // Filtrar y volver a cargar empieza de nuevo en la primera pagina: quedarse
    // en la cuarta de una lista que ahora tiene dos deja la pantalla vacia.
    this.pagina.set(0);
    this.api.items(this.filtro, this.porEtiqueta()).subscribe((i) => this.items.set(i));
    this.api.etiquetas().subscribe({
      next: (e) => this.conocidas.set(e),
      error: () => {},
    });
  }

  /** Clic en un chip: el mismo gesto filtra desde el listado o desde la fila. */
  filtrarPor(etiqueta: string): void {
    // Volver a tocar la etiqueta activa la saca. Sin esto, el unico modo de
    // deshacer el filtro es encontrar el chip "todas", que puede haber
    // quedado fuera de la pantalla.
    this.porEtiqueta.set(this.porEtiqueta() === etiqueta ? '' : etiqueta);
    this.cargar();
  }

  alGuardar(): void {
    this.volverAlBanco();
    this.cargar();
  }

  /**
   * Abre o cierra la vista previa. Se pide al backend cada vez y no se cachea:
   * es barato, y si el profesor acaba de publicar una version nueva tiene que
   * ver ESA, no la que estaba en memoria.
   */
  alternarPrevia(i: ItemResumen): void {
    if (this.previaDe() === i.id) {
      this.previaDe.set(null);
      this.previa.set(null);
      return;
    }
    this.previaDe.set(i.id);
    this.previa.set(null);
    this.api.vistaPrevia(i.id).subscribe({
      next: (p) => this.previa.set(p),
      error: () => {
        this.previaDe.set(null);
        this.error.set({
          clave: 'ERROR',
          campo: null,
          mensaje: 'No se pudo traer la vista previa del ítem.',
        });
      },
    });
  }

  /** Lo unico que la previa sabe pintar de a uno: las opciones, si el tipo tiene. */
  opcionesDe(p: VistaPreviaItem): { id: string; texto: string }[] {
    const carga = p.payload ?? {};
    return carga.opciones ?? carga.elementos ?? carga.izquierda ?? [];
  }

  /**
   * La unica accion destructiva de la pantalla, y por eso pide confirmacion en
   * linea: `confirm()` bloquea el hilo del navegador y en una demo se nota.
   */
  confirmarBaja(item: ItemResumen): void {
    this.porDarDeBaja.set(null);
    this.error.set(null);
    this.api.bajaItem(item.id).subscribe({
      next: () => {
        this.ok.set('Ítem dado de baja. Lo ya respondido con él se sigue corrigiendo igual.');
        this.cargar();
      },
      error: () =>
        this.error.set({
          clave: 'ERROR',
          campo: null,
          mensaje: 'No se pudo dar de baja el ítem.',
        }),
    });
  }
}
