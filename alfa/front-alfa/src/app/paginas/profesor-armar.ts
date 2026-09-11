import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { JsonPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import {
  ContenidoRef,
  ErrorApi,
  ItemResumen,
  MiContenido,
  TIPOS,
  TipoDeItem,
} from '../core/modelos';

/** Los pesos que se ofrecen de un clic, para que no haya que tipear. */
const CHIPS = [5, 10, 15, 20, 25, 50];

interface Elegido {
  item: ItemResumen;
  puntaje: number;
  /**
   * El profesor escribio o clickeo ESTE peso a mano. Un peso manual no se
   * vuelve a tocar solo: agregar o quitar otro item reparte el remanente entre
   * los que quedaron libres, y nunca encima de una decision ya tomada.
   */
  manual: boolean;
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

        @if (yaArmados().length > 0 && elegidos().length === 0) {
          <div class="reutilizar">
            <h3>Reutilizar uno que ya armaste</h3>
            <p class="ayuda">
              El desafío guarda una <strong>referencia</strong> al cuestionario, no una copia. Si
              colgás el mismo de dos unidades o de dos cohortes, editar una pregunta las cambia en
              las dos — y cada alumno se sigue corrigiendo contra la versión que vio.
            </p>
            @for (c of yaArmados(); track c.contenidoId) {
              <div class="fila">
                <span class="concepto">
                  {{ c.titulo }}
                  <span class="version">{{ c.ficha.resumen }}</span>
                </span>
                <button type="button" class="secundario" (click)="colgarExistente(c)">
                  Colgar acá
                </button>
              </div>
            }
            <p class="ayuda">O armá uno nuevo desde cero, agregando ítems de la izquierda.</p>
          </div>
        }

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
              attr.aria-label="Peso en porcentaje de {{ e.item.enunciado }}"
            />
            <button
              type="button"
              class="secundario"
              attr.aria-label="Quitar {{ e.item.enunciado }}"
              (click)="quitar(i)"
            >
              <span aria-hidden="true">×</span>
            </button>
          </div>
        }

        @if (elegidos().length > 0) {
          <div
            class="totalizador"
            role="status"
            aria-live="polite"
            [class.mal]="suma() !== 100 || hayEnCero()"
          >
            <strong>{{ suma() }}%</strong>
            <span>{{ mensajeDeSuma() }}</span>
            <button type="button" class="secundario" (click)="repartirParejo()">
              Repartir parejo
            </button>
          </div>
        }

        <label class="check">
          <input
            type="checkbox"
            name="ilimitados"
            [ngModel]="reintentosIlimitados()"
            (ngModelChange)="reintentosIlimitados.set($event)"
          />
          Reintentos ilimitados
        </label>
        <p class="ayuda">
          CI-47: un desafío con reintentos ilimitados no admite corrección humana —pondría al
          profesor a corregir la misma entrega infinitas veces—. La regla la aplica el Tema 03, que
          es quien sabe de reintentos; nosotros solo le mandamos <code>correccion</code> en la
          ficha.
        </p>

        <!--
          Las cuatro pantallas que Moodle pone en este mismo formulario y que acá
          no estan. Se muestran en vez de omitirse: la ausencia sola parece un
          agujero, la ausencia rotulada es la frontera (ver 5c del contrato).
        -->
        <div class="frontera">
          <h3>Lo que no se configura acá</h3>
          <p class="ayuda">
            En Moodle todo esto vive en el mismo formulario que las preguntas. Acá cada cosa es de
            quien la sabe, y el cuestionario es una sola de las partes del desafío.
          </p>
          <p class="linea-frontera">
            <span class="fuente">Tema 03</span> Cuándo abre, cuándo cierra y el límite de tiempo
            <span class="version">CI-15 · CI-20</span>
          </p>
          <p class="linea-frontera">
            <span class="fuente">Tema 03</span> Cuántos intentos hay y cuál de ellos cuenta
            <span class="version">CI-45</span>
          </p>
          <p class="linea-frontera">
            <span class="fuente">Tema 03</span> La nota con la que se aprueba
            <span class="version">CI-39</span>
          </p>
          <p class="linea-frontera">
            <span class="fuente">Tema 02</span> Quiénes pueden entrar: curso, cohorte y grupo
            <span class="version">CI-01</span>
          </p>
        </div>

        @if (error()) {
          <p class="error" role="alert">
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
            <p class="ok" role="status">Desafío creado en el Tema 03. Ya lo puede ver el alumno.</p>
            <div class="acciones-fila">
              <button type="button" (click)="volverAlRoadmap()">Ver la unidad en el roadmap</button>
            </div>
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
  readonly reintentosIlimitados = signal(false);
  readonly yaArmados = signal<MiContenido[]>([]);

  readonly cursoId = signal('');
  readonly unidadId = signal('');
  readonly unidadTitulo = signal('');

  readonly disponibles = computed(() => {
    const usados = new Set(this.elegidos().map((e) => e.item.id));
    return this.banco().filter((i) => !usados.has(i.id));
  });

  readonly suma = computed(() => this.elegidos().reduce((t, e) => t + (e.puntaje || 0), 0));

  /** Ningun item puede quedar en 0: el backend lo rechaza (PUNTAJE_NO_POSITIVO). */
  readonly hayEnCero = computed(() => this.elegidos().some((e) => e.puntaje <= 0));

  readonly listo = computed(
    () =>
      this.elegidos().length > 0 &&
      this.suma() === 100 &&
      !this.hayEnCero() &&
      this.titulo().trim().length > 0,
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
    this.api.misContenidos().subscribe({
      next: (c) => this.yaArmados.set(c),
      // Si no se puede traer la lista, se arma uno nuevo y ya: no es bloqueante.
      error: () => {},
    });
  }

  /**
   * Cierra el circulo del recorrido: el desafio recien creado se ve DENTRO de
   * la unidad, que es lo que prueba que el Tema 03 lo colgo del nodo correcto
   * del roadmap. Sin este boton hay que acordarse de navegar por la barra.
   */
  volverAlRoadmap(): void {
    this.router.navigateByUrl('/profesor/cursos/' + this.cursoId());
  }

  /**
   * 422 es el rechazo de CI-47 y no una caida: el contenido quedo bien guardado,
   * lo que el 03 no acepta es este desafio.
   */
  private errorDelTema03(e: any): ErrorApi {
    const rechazo = e.status === 422;
    return {
      clave: rechazo ? 'CI_47' : 'TEMA_03',
      campo: rechazo ? 'reintentosIlimitados' : null,
      mensaje: rechazo
        ? 'El Tema 03 rechazó el desafío: tiene reintentos ilimitados y este cuestionario ' +
          'lleva corrección diferida (CI-47). El cuestionario quedó guardado — destildá ' +
          'reintentos ilimitados y volvé a publicar.'
        : 'El contenido quedó guardado, pero el Tema 03 no respondió. ' +
          'Es exactamente el modo de fallar que CI-03 elige: se puede armar contenido aunque el 03 esté caído.',
    };
  }

  etiqueta(tipo: TipoDeItem): string {
    return TIPOS.find((t) => t.valor === tipo)?.etiqueta ?? tipo;
  }

  mensajeDeSuma(): string {
    const diferencia = 100 - this.suma();
    if (diferencia === 0 && this.hayEnCero()) {
      return 'suman 100, pero hay una pregunta en 0: sacale peso a otra o quitala';
    }
    if (diferencia === 0) return 'los pesos suman 100';
    if (diferencia > 0) return `faltan ${diferencia}%`;
    return `te pasaste ${-diferencia}%`;
  }

  agregar(item: ItemResumen): void {
    this.elegidos.update((lista) => [...lista, { item, puntaje: 0, manual: false }]);
    this.reajustarLibres();
  }

  quitar(i: number): void {
    this.elegidos.update((lista) => lista.filter((_, indice) => indice !== i));
    this.reajustarLibres();
  }

  /**
   * Reparto equitativo con el resto al ultimo: 3 items dan 34/33/33 y nunca
   * decimales. Es el boton explicito, asi que pisa TODO —incluidos los pesos
   * manuales— y los vuelve a dejar libres: el profesor pidio empezar de cero.
   */
  repartirParejo(): void {
    const total = this.elegidos().length;
    if (total === 0) return;
    const base = Math.floor(100 / total);
    const resto = 100 - base * total;
    this.elegidos.update((lista) =>
      lista.map((e, i) => ({
        ...e,
        puntaje: i === total - 1 ? base + resto : base,
        manual: false,
      })),
    );
  }

  /**
   * Reparte lo que sobra despues de los pesos manuales entre los items libres.
   * Es lo que corre solo al agregar o quitar: si la profesora ya le puso 40 a
   * la primera pregunta, agregar una segunda no puede borrarle ese 40.
   */
  private reajustarLibres(): void {
    const lista = this.elegidos();
    const libres = lista.filter((e) => !e.manual).length;
    if (libres === 0) return;

    const fijado = lista.filter((e) => e.manual).reduce((t, e) => t + e.puntaje, 0);
    const aRepartir = Math.max(0, 100 - fijado);
    const base = Math.floor(aRepartir / libres);
    const resto = aRepartir - base * libres;

    let visto = 0;
    this.elegidos.update((l) =>
      l.map((e) => {
        if (e.manual) return e;
        visto++;
        return { ...e, puntaje: visto === libres ? base + resto : base };
      }),
    );
  }

  /**
   * Fijar un peso NO reajusta los demas: el profesor que toca un peso quiere
   * ese peso, y el contador de arriba le dice cuanto le falta repartir. Queda
   * marcado como manual para que agregar otro item tampoco se lo pise.
   */
  fijar(i: number, valor: number): void {
    this.elegidos.update((lista) =>
      lista.map((e, indice) =>
        indice === i ? { ...e, puntaje: Number(valor) || 0, manual: true } : e,
      ),
    );
  }

  /**
   * Cuelga un cuestionario YA COMPUESTO de esta unidad. No se vuelve a componer
   * nada: se le pasa al Tema 03 la misma ficha, con el mismo contenidoId. Eso es
   * lo que hace que sea una referencia y no una copia.
   */
  colgarExistente(c: MiContenido): void {
    this.error.set(null);
    this.publicando.set(true);

    this.api
      .crearDesafio({
        titulo: c.titulo,
        cursoCohorteId: this.cursoId(),
        unidadId: this.unidadId(),
        contenidoRef: c.ficha,
        reintentosIlimitados: this.reintentosIlimitados(),
      })
      .subscribe({
        next: () => {
          this.publicando.set(false);
          this.ficha.set(c.ficha);
        },
        error: (e) => {
          this.publicando.set(false);
          this.error.set(this.errorDelTema03(e));
        },
      });
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
            reintentosIlimitados: this.reintentosIlimitados(),
          })
          .subscribe({
            next: () => {
              this.publicando.set(false);
              this.ficha.set(ficha);
              this.elegidos.set([]);
              this.titulo.set('');
            },
            error: (e) => {
              this.publicando.set(false);
              this.error.set(this.errorDelTema03(e));
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
