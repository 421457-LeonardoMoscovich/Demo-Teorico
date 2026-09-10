import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { IntentoStore } from '../core/intento.store';
import { ItemParaAlumno, Opcion, VistaAlumno } from '../core/modelos';

/** Lo que el alumno lleva contestado, por ítem. */
interface Borrador {
  seleccionadas: string[];
  valor: boolean | null;
  pares: Record<string, string>;
  secuencia: string[];
}

@Component({
  selector: 'app-alumno-responder',
  standalone: true,
  imports: [FormsModule],
  template: `
    @if (vista(); as v) {
      <section class="tarjeta ancho">
        <header class="cabecera-cuestionario">
          <div>
            <h2>{{ v.titulo }}</h2>
            <span class="etiqueta">intento {{ intento()?.intento }}</span>
            <span class="etiqueta">{{ v.puntajeTotal }} puntos</span>
          </div>
          <span class="contestadas">{{ contestadas() }} de {{ v.items.length }} contestadas</span>
        </header>

        @if (recuperado()) {
          <p class="ok">
            Recuperamos lo que habías contestado antes de recargar.
          </p>
        }

        @for (i of v.items; track i.itemVersionId) {
          <article class="pregunta">
            <h3>
              <span class="posicion">{{ i.orden }}</span>
              {{ i.enunciado }}
              <span class="peso-pregunta">{{ i.puntaje }}%</span>
            </h3>

            @switch (i.tipo) {
              @case ('OPCION_MULTIPLE') {
                @for (o of i.payload.opciones; track o.id) {
                  <label class="check" [class.elegida]="borrador(i).seleccionadas.includes(o.id)">
                    <input
                      [type]="i.payload.multiple ? 'checkbox' : 'radio'"
                      [name]="'r' + i.itemVersionId"
                      [checked]="borrador(i).seleccionadas.includes(o.id)"
                      (change)="elegir(i, o.id)"
                    />
                    {{ o.texto }}
                  </label>
                }
              }

              @case ('VERDADERO_FALSO') {
                <p class="afirmacion">{{ i.payload.afirmacion }}</p>
                <label class="check" [class.elegida]="borrador(i).valor === true">
                  <input
                    type="radio"
                    [name]="'r' + i.itemVersionId"
                    [checked]="borrador(i).valor === true"
                    (change)="marcarVF(i, true)"
                  />
                  Verdadero
                </label>
                <label class="check" [class.elegida]="borrador(i).valor === false">
                  <input
                    type="radio"
                    [name]="'r' + i.itemVersionId"
                    [checked]="borrador(i).valor === false"
                    (change)="marcarVF(i, false)"
                  />
                  Falso
                </label>
              }

              @case ('EMPAREJAR') {
                @for (z of i.payload.izquierda; track z.id) {
                  <div class="fila">
                    <span class="concepto">{{ z.texto }}</span>
                    <select
                      [name]="'p' + i.itemVersionId + z.id"
                      [ngModel]="borrador(i).pares[z.id] || ''"
                      (ngModelChange)="emparejar(i, z.id, $event)"
                    >
                      <option value="">elegí…</option>
                      @for (d of i.payload.derecha; track d.id) {
                        <option [value]="d.id">{{ d.texto }}</option>
                      }
                    </select>
                  </div>
                }
              }

              @case ('ORDENAR') {
                <p class="ayuda">Movelos hasta dejarlos en el orden correcto.</p>
                @for (id of borrador(i).secuencia; track id; let k = $index) {
                  <div class="fila orden">
                    <span class="posicion">{{ k + 1 }}</span>
                    <span class="concepto">{{ texto(i, id) }}</span>
                    <button
                      type="button"
                      class="secundario"
                      [disabled]="k === 0"
                      (click)="subir(i, k)"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      class="secundario"
                      [disabled]="k === borrador(i).secuencia.length - 1"
                      (click)="bajar(i, k)"
                    >
                      ↓
                    </button>
                  </div>
                }
              }
            }
          </article>
        }

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        <div class="acciones">
          <button type="button" (click)="entregar()" [disabled]="entregando()">
            {{ entregando() ? 'Entregando…' : 'Entregar' }}
          </button>
          <p class="ayuda">
            El botón Entregar va al Tema 03, no a nosotros: es él quien valida que el intento siga
            siendo válido y recién entonces nos despacha la respuesta.
          </p>
        </div>
      </section>
    } @else {
      <section class="tarjeta">
        <p class="vacio">{{ error() || 'Cargando el cuestionario…' }}</p>
      </section>
    }
  `,
})
export class AlumnoResponderPage {
  private readonly api = inject(ApiService);
  private readonly intentos = inject(IntentoStore);
  private readonly router = inject(Router);

  readonly intento = this.intentos.intento;
  readonly vista = signal<VistaAlumno | null>(null);
  readonly error = signal('');
  readonly entregando = signal(false);
  readonly recuperado = signal(false);

  private readonly borradores = signal<Record<string, Borrador>>({});

  readonly contestadas = computed(() => {
    const v = this.vista();
    if (!v) return 0;
    return v.items.filter((i) => this.estaContestada(i)).length;
  });

  constructor() {
    const apertura = this.intentos.intento();
    if (!apertura) {
      this.router.navigateByUrl('/alumno/cursos');
      return;
    }
    this.api.vistaAlumno(apertura.contenidoRef.contenidoId, apertura.vale).subscribe({
      next: (v) => {
        this.vista.set(v);

        const iniciales: Record<string, Borrador> = {};
        for (const i of v.items) {
          iniciales[i.itemVersionId] = {
            seleccionadas: [],
            valor: null,
            pares: {},
            secuencia: i.tipo === 'ORDENAR' ? i.payload.elementos.map((e: Opcion) => e.id) : [],
          };
        }

        // Si el alumno ya habia empezado a contestar, se recupera lo suyo. Se
        // acepta ítem por ítem y solo para los que siguen estando en la vista:
        // si el profesor edito el cuestionario en el medio, la version cambia y
        // esa respuesta vieja ya no aplica (CI-13).
        const guardado = this.intentos.leerBorrador<Record<string, Borrador>>();
        if (guardado) {
          let recuperados = 0;
          for (const i of v.items) {
            const previo = guardado[i.itemVersionId];
            if (previo) {
              iniciales[i.itemVersionId] = { ...iniciales[i.itemVersionId], ...previo };
              recuperados++;
            }
          }
          if (recuperados > 0) {
            this.recuperado.set(true);
          }
        }

        this.borradores.set(iniciales);
      },
      error: (e) =>
        this.error.set(
          e.status === 403
            ? 'El vale de lectura no es válido o venció. Volvé a empezar el desafío.'
            : 'No se pudo cargar el cuestionario.',
        ),
    });
  }

  borrador(i: ItemParaAlumno): Borrador {
    return (
      this.borradores()[i.itemVersionId] ?? {
        seleccionadas: [],
        valor: null,
        pares: {},
        secuencia: [],
      }
    );
  }

  texto(i: ItemParaAlumno, id: string): string {
    return i.payload.elementos.find((e: Opcion) => e.id === id)?.texto ?? id;
  }

  private actualizar(i: ItemParaAlumno, cambio: Partial<Borrador>): void {
    this.borradores.update((todos) => ({
      ...todos,
      [i.itemVersionId]: { ...this.borrador(i), ...cambio },
    }));
    this.intentos.guardarBorrador(this.borradores());
  }

  elegir(i: ItemParaAlumno, opcionId: string): void {
    const actuales = this.borrador(i).seleccionadas;
    if (!i.payload.multiple) {
      this.actualizar(i, { seleccionadas: [opcionId] });
      return;
    }
    this.actualizar(i, {
      seleccionadas: actuales.includes(opcionId)
        ? actuales.filter((x) => x !== opcionId)
        : [...actuales, opcionId],
    });
  }

  marcarVF(i: ItemParaAlumno, valor: boolean): void {
    this.actualizar(i, { valor });
  }

  emparejar(i: ItemParaAlumno, izquierdaId: string, derechaId: string): void {
    this.actualizar(i, { pares: { ...this.borrador(i).pares, [izquierdaId]: derechaId } });
  }

  subir(i: ItemParaAlumno, k: number): void {
    const s = [...this.borrador(i).secuencia];
    [s[k - 1], s[k]] = [s[k], s[k - 1]];
    this.actualizar(i, { secuencia: s });
  }

  bajar(i: ItemParaAlumno, k: number): void {
    const s = [...this.borrador(i).secuencia];
    [s[k + 1], s[k]] = [s[k], s[k + 1]];
    this.actualizar(i, { secuencia: s });
  }

  private estaContestada(i: ItemParaAlumno): boolean {
    const b = this.borrador(i);
    switch (i.tipo) {
      case 'OPCION_MULTIPLE':
        return b.seleccionadas.length > 0;
      case 'VERDADERO_FALSO':
        return b.valor !== null;
      case 'EMPAREJAR':
        return i.payload.izquierda.every((z: Opcion) => b.pares[z.id]);
      case 'ORDENAR':
        return b.secuencia.length > 0;
    }
  }

  /**
   * Se envían TODOS los ítems, también los que quedaron en blanco. El que no
   * contestó viaja con su forma vacía y el corrector le pone 0 explícitamente,
   * en vez de tener que distinguir "no contestó" de "no llegó el dato".
   */
  entregar(): void {
    const v = this.vista();
    const apertura = this.intentos.intento();
    if (!v || !apertura) return;

    this.entregando.set(true);
    this.error.set('');

    const respuestas = v.items.map((i) => {
      const b = this.borrador(i);
      switch (i.tipo) {
        case 'OPCION_MULTIPLE':
          return { itemVersionId: i.itemVersionId, contenido: { seleccionadas: b.seleccionadas } };
        case 'VERDADERO_FALSO':
          return { itemVersionId: i.itemVersionId, contenido: { valor: b.valor } };
        case 'EMPAREJAR':
          return {
            itemVersionId: i.itemVersionId,
            contenido: {
              pares: Object.entries(b.pares)
                .filter(([, d]) => !!d)
                .map(([z, d]) => [z, d]),
            },
          };
        case 'ORDENAR':
          return { itemVersionId: i.itemVersionId, contenido: { secuencia: b.secuencia } };
      }
    });

    this.api.entregar(apertura.desafioId, apertura.entregaId, respuestas).subscribe({
      next: () => {
        this.intentos.limpiarBorrador();
        this.router.navigateByUrl('/alumno/resultado');
      },
      error: () => {
        this.entregando.set(false);
        this.error.set('El Tema 03 no aceptó la entrega.');
      },
    });
  }
}
