import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { JsonPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ContenidoRef, ErrorApi, ItemResumen, TIPOS, TipoDeItem } from '../core/modelos';

/** Los pesos que se ofrecen de un clic, para que no haya que tipear. */
const CHIPS = [5, 10, 15, 20, 25, 50];

interface Elegido {
  item: ItemResumen;
  puntaje: number;
}

@Component({
  selector: 'app-profesor-armar',
  standalone: true,
  imports: [FormsModule, JsonPipe],
  template: `
    <div class="columnas">
      <section class="tarjeta">
        <h2>Ítems disponibles</h2>
        <p class="ayuda">Elegí de tu banco. Se agregan al final del cuestionario.</p>
        <ul class="lista">
          @for (i of disponibles(); track i.id) {
            <li>
              <div>
                <span class="etiqueta">{{ etiqueta(i.tipo) }}</span>
                <p>{{ i.enunciado }}</p>
              </div>
              <button type="button" (click)="agregar(i)">Agregar</button>
            </li>
          }
          @if (disponibles().length === 0) {
            <li class="vacio">No queda ningún ítem sin usar.</li>
          }
        </ul>
      </section>

      <section class="tarjeta">
        <h2>Armar cuestionario</h2>
        <p class="ayuda">
          <span class="fuente">Unidad {{ unidadId() }}</span>
          {{ unidadTitulo() }}
        </p>

        <label>
          Título
          <input
            name="titulo"
            [ngModel]="titulo()"
            (ngModelChange)="titulo.set($event)"
            placeholder="Parcial 1 — Arquitectura"
          />
        </label>

        @if (elegidos().length === 0) {
          <p class="vacio">Agregá al menos un ítem.</p>
        }

        @for (e of elegidos(); track e.item.id; let i = $index) {
          <div class="linea-peso">
            <span class="posicion">{{ i + 1 }}</span>
            <div class="linea-texto">
              <p>{{ e.item.enunciado }}</p>
              <div class="chips">
                @for (c of chips; track c) {
                  <button
                    type="button"
                    class="chip"
                    [class.activo]="e.puntaje === c"
                    (click)="fijar(i, c)"
                  >
                    {{ c }}%
                  </button>
                }
              </div>
            </div>
            <input
              class="peso"
              type="number"
              min="1"
              max="100"
              [ngModel]="e.puntaje"
              (ngModelChange)="fijar(i, $event)"
              [name]="'peso' + i"
            />
            <button type="button" class="secundario" (click)="quitar(i)">×</button>
          </div>
        }

        @if (elegidos().length > 0) {
          <div class="totalizador" [class.mal]="suma() !== 100">
            <strong>{{ suma() }}%</strong>
            <span>{{ mensajeDeSuma() }}</span>
            <button type="button" class="secundario" (click)="repartirParejo()">
              Repartir parejo
            </button>
          </div>
        }

        @if (error()) {
          <p class="error">
            {{ error()!.mensaje }}
            @if (error()!.campo) {
              <span class="campo">({{ error()!.campo }})</span>
            }
          </p>
        }

        <button type="button" [disabled]="!listo() || publicando()" (click)="publicar()">
          {{ publicando() ? 'Publicando…' : 'Publicar cuestionario' }}
        </button>

        @if (ficha()) {
          <div class="ficha">
            <h3>Publicado</h3>
            <p class="ayuda">
              Esto es lo único que viaja al Tema 03: cinco campos, y el único que interpreta es
              <code>tipo</code>. No sabe qué preguntas hay adentro.
            </p>
            <pre>{{ ficha() | json }}</pre>
            <p class="ok">Desafío creado en el Tema 03. Ya lo puede ver el alumno.</p>
          </div>
        }
      </section>
    </div>
  `,
})
export class ProfesorArmarPage {
  private readonly api = inject(ApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly chips = CHIPS;
  readonly banco = signal<ItemResumen[]>([]);
  readonly elegidos = signal<Elegido[]>([]);
  readonly error = signal<ErrorApi | null>(null);
  readonly ficha = signal<ContenidoRef | null>(null);
  readonly publicando = signal(false);

  /**
   * Signal y no un campo comun: `listo` es un computed y solo recalcula cuando
   * cambia una signal. Con un campo suelto, escribir el titulo no habilitaba el
   * boton hasta que algo mas tocara la lista.
   */
  readonly titulo = signal('');

  /**
   * El curso y la unidad vienen en la URL. Un cuestionario no se compone en el
   * vacio: pertenece a un nodo del roadmap, y sin eso no se puede crear el
   * desafio en el Tema 03.
   */
  readonly cursoId = signal('');
  readonly unidadId = signal('');
  readonly unidadTitulo = signal('');

  readonly disponibles = computed(() => {
    const usados = new Set(this.elegidos().map((e) => e.item.id));
    return this.banco().filter((i) => !usados.has(i.id));
  });

  readonly suma = computed(() => this.elegidos().reduce((t, e) => t + (e.puntaje || 0), 0));

  readonly listo = computed(
    () => this.elegidos().length > 0 && this.suma() === 100 && this.titulo().trim().length > 0,
  );

  constructor() {
    const q = this.ruta.snapshot.queryParamMap;
    const curso = q.get('curso');
    const unidad = q.get('unidad');

    if (!curso || !unidad) {
      // Se llego aca a mano, sin pasar por el roadmap. No inventamos un curso.
      this.router.navigateByUrl('/profesor/cursos');
      return;
    }
    this.cursoId.set(curso);
    this.unidadId.set(unidad);
    this.unidadTitulo.set(q.get('unidadTitulo') ?? '');

    this.api.items().subscribe((i) => this.banco.set(i));
  }

  etiqueta(tipo: TipoDeItem): string {
    return TIPOS.find((t) => t.valor === tipo)?.etiqueta ?? tipo;
  }

  mensajeDeSuma(): string {
    const diferencia = 100 - this.suma();
    if (diferencia === 0) return 'los pesos suman 100';
    if (diferencia > 0) return `faltan ${diferencia}%`;
    return `te pasaste ${-diferencia}%`;
  }

  agregar(item: ItemResumen): void {
    this.elegidos.update((lista) => [...lista, { item, puntaje: 0 }]);
    this.repartirParejo();
  }

  quitar(i: number): void {
    this.elegidos.update((lista) => lista.filter((_, indice) => indice !== i));
    if (this.elegidos().length > 0) this.repartirParejo();
  }

  /**
   * Reparto equitativo con el resto al último: 3 ítems dan 34/33/33 y nunca
   * decimales. Es la regla que hace que agregar un ítem no obligue a recalcular
   * todo a mano.
   */
  repartirParejo(): void {
    const total = this.elegidos().length;
    if (total === 0) return;
    const base = Math.floor(100 / total);
    const resto = 100 - base * total;
    this.elegidos.update((lista) =>
      lista.map((e, i) => ({ ...e, puntaje: i === total - 1 ? base + resto : base })),
    );
  }

  /**
   * Fijar un peso NO reajusta los demás: el profesor que toca un peso quiere
   * ese peso, y el contador de arriba le dice cuánto le falta repartir.
   */
  fijar(i: number, valor: number): void {
    this.elegidos.update((lista) =>
      lista.map((e, indice) => (indice === i ? { ...e, puntaje: Number(valor) || 0 } : e)),
    );
  }

  publicar(): void {
    this.error.set(null);
    this.publicando.set(true);

    const cuerpo = {
      cursoCohorteId: this.cursoId(),
      titulo: this.titulo(),
      escala: 'PORCENTUAL',
      items: this.elegidos().map((e, i) => ({
        itemId: e.item.id,
        orden: i + 1,
        puntaje: e.puntaje,
      })),
    };

    // Dos pasos, y en este orden (CI-03): primero el contenido acá, que
    // devuelve la ficha; después el desafío en el Tema 03 con la referencia ya
    // formada. Así no existe ningún contrato de composición entre los dos.
    this.api.componer(cuerpo).subscribe({
      next: (ficha) => {
        this.api
          .crearDesafio({
            titulo: this.titulo(),
            cursoCohorteId: this.cursoId(),
            unidadId: this.unidadId(),
            contenidoRef: ficha,
          })
          .subscribe({
            next: () => {
              this.publicando.set(false);
              this.ficha.set(ficha);
              this.elegidos.set([]);
              this.titulo.set('');
            },
            error: () => {
              this.publicando.set(false);
              this.error.set({
                clave: 'TEMA_03',
                campo: null,
                mensaje:
                  'El contenido quedó guardado, pero el Tema 03 no respondió. ' +
                  'Es exactamente el modo de fallar que CI-03 elige: se puede armar contenido aunque el 03 esté caído.',
              });
            },
          });
      },
      error: (e) => {
        this.publicando.set(false);
        this.error.set(
          e.error?.mensaje
            ? e.error
            : { clave: 'ERROR', campo: null, mensaje: 'No se pudo publicar.' },
        );
      },
    });
  }
}
