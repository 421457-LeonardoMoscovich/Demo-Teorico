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
  texto: string;
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
          <span class="contestadas" role="status" aria-live="polite">
            {{ contestadas() }} de {{ v.items.length }} contestadas
          </span>
        </header>

        @if (avisoDeRecuperacion(); as aviso) {
          <p [class]="descartadas() > 0 ? 'error' : 'ok'" role="status">{{ aviso }}</p>
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

              @case ('ABIERTA') {
                <p class="afirmacion">{{ i.payload.consigna }}</p>
                <textarea
                  rows="6"
                  [name]="'a' + i.itemVersionId"
                  [ngModel]="borrador(i).texto"
                  (ngModelChange)="escribir(i, $event)"
                  placeholder="Escribí tu respuesta"
                ></textarea>
                <p class="ayuda contador-palabras" [class.pasado]="excedido(i)">
                  {{ palabras(i) }}
                  @if (i.payload.extensionMaxima) {
                    de {{ i.payload.extensionMaxima }}
                  }
                  palabras · esta la corrige tu profesor, así que la nota no sale al entregar
                </p>
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
                      [attr.aria-label]="'Subir ' + texto(i, id)"
                      (click)="subir(i, k)"
                    >
                      <span aria-hidden="true">↑</span>
                    </button>
                    <button
                      type="button"
                      class="secundario"
                      [disabled]="k === borrador(i).secuencia.length - 1"
                      [attr.aria-label]="'Bajar ' + texto(i, id)"
                      (click)="bajar(i, k)"
                    >
                      <span aria-hidden="true">↓</span>
                    </button>
                  </div>
                }
              }
            }
          </article>
        }

        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }

        <div class="acciones">
          @if (confirmando()) {
            <p class="error" role="alert">
              Te quedan {{ v.items.length - contestadas() }} sin contestar. Se entregan en blanco y
              valen 0.
            </p>
            <div class="acciones-fila">
              <button type="button" (click)="entregar(true)" [disabled]="entregando()">
                {{ entregando() ? 'Entregando…' : 'Entregar igual' }}
              </button>
              <button type="button" class="secundario" (click)="confirmando.set(false)">
                Seguir contestando
              </button>
            </div>
          } @else {
            <button type="button" (click)="entregar()" [disabled]="entregando()">
              {{ entregando() ? 'Entregando…' : 'Entregar' }}
            </button>
          }
          <p class="ayuda">
            El botón Entregar va al Tema 03, no a nosotros: es él quien valida que el intento siga
            siendo válido y recién entonces nos despacha la respuesta.
          </p>
        </div>
      </section>
    } @else {
      <section class="tarjeta">
        <p class="vacio">{{ error() || 'Cargando el cuestionario…' }}</p>
        @if (error()) {
          <div class="acciones-fila">
            <button type="button" (click)="volverAMisCursos()">Volver a mis cursos</button>
          </div>
        }
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
  /** Se pidio entregar con preguntas en blanco y falta que el alumno confirme. */
  readonly confirmando = signal(false);

  /** Cuantas respuestas se recuperaron del borrador, y cuantas se cayeron. */
  readonly recuperadas = signal(0);
  readonly descartadas = signal(0);

  private readonly borradores = signal<Record<string, Borrador>>({});

  /**
   * Una respuesta se descarta cuando el profesor publico una version nueva de
   * ese item mientras el alumno contestaba (CI-13). No es un error nuestro,
   * pero el alumno tiene que enterarse: si no, cree que sigue contestada y la
   * entrega en blanco.
   */
  readonly avisoDeRecuperacion = computed(() => {
    const recuperadas = this.recuperadas();
    const descartadas = this.descartadas();
    if (recuperadas === 0 && descartadas === 0) return '';

    const total = this.vista()?.items.length ?? 0;
    if (descartadas === 0) {
      return `Recuperamos las ${recuperadas} respuestas que tenías antes de recargar.`;
    }
    const cambiadas =
      descartadas === 1 ? 'Una pregunta cambió' : `${descartadas} preguntas cambiaron`;
    const contestarlas = descartadas === 1 ? 'contestarla' : 'contestarlas';
    return (
      `Recuperamos ${recuperadas} de ${total} respuestas. ` +
      `${cambiadas} mientras respondías —el profesor publicó una versión nueva— ` +
      `y hay que ${contestarlas} de nuevo.`
    );
  });

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
            texto: '',
          };
        }

        // Si el alumno ya habia empezado a contestar, se recupera lo suyo. Se
        // acepta ítem por ítem y solo para los que siguen estando en la vista:
        // si el profesor edito el cuestionario en el medio, la version cambia y
        // esa respuesta vieja ya no aplica (CI-13).
        const guardado = this.intentos.leerBorrador<Record<string, Borrador>>();
        if (guardado) {
          const vigentes = new Set(v.items.map((i) => i.itemVersionId));
          let recuperadas = 0;
          for (const i of v.items) {
            const previo = guardado[i.itemVersionId];
            if (previo) {
              iniciales[i.itemVersionId] = { ...iniciales[i.itemVersionId], ...previo };
              recuperadas++;
            }
          }
          // Lo guardado que ya no esta en la vista es una respuesta a una
          // version que dejo de ser vigente: se pierde, y hay que decirlo.
          const descartadas = Object.keys(guardado).filter((id) => !vigentes.has(id)).length;
          this.recuperadas.set(recuperadas);
          this.descartadas.set(descartadas);
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

  /**
   * La lectura fallo, casi siempre porque el vale vencio (CI-18). El intento
   * que quedo guardado ya no sirve para nada: si no se limpia, volver a entrar
   * aca reintenta con el mismo vale muerto y falla igual.
   */
  volverAMisCursos(): void {
    this.intentos.limpiar();
    this.router.navigateByUrl('/alumno/cursos');
  }

  borrador(i: ItemParaAlumno): Borrador {
    return (
      this.borradores()[i.itemVersionId] ?? {
        seleccionadas: [],
        valor: null,
        pares: {},
        secuencia: [],
        texto: '',
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

  escribir(i: ItemParaAlumno, texto: string): void {
    this.actualizar(i, { texto });
  }

  palabras(i: ItemParaAlumno): number {
    const texto = this.borrador(i).texto.trim();
    return texto ? texto.split(/\s+/).length : 0;
  }

  /**
   * Se avisa pero NO se bloquea. La extension maxima es una indicacion del
   * profesor, no una regla del sistema: cortarle la respuesta a un alumno que
   * se pasa por tres palabras seria inventar una sancion que nadie definio.
   */
  excedido(i: ItemParaAlumno): boolean {
    const maximo = i.payload.extensionMaxima;
    return !!maximo && this.palabras(i) > maximo;
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
      case 'ABIERTA':
        return b.texto.trim().length > 0;
    }
  }

  /**
   * Se envían TODOS los ítems, también los que quedaron en blanco. El que no
   * contestó viaja con su forma vacía y el corrector le pone 0 explícitamente,
   * en vez de tener que distinguir "no contestó" de "no llegó el dato".
   */
  entregar(confirmado = false): void {
    const v = this.vista();
    const apertura = this.intentos.intento();
    if (!v || !apertura) return;

    // Entregar en blanco es legitimo —el corrector le pone 0 explicitamente— pero
    // no puede pasar por accidente: en una evaluacion real eso termina en reclamo.
    if (!confirmado && this.contestadas() < v.items.length) {
      this.confirmando.set(true);
      return;
    }

    this.confirmando.set(false);
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
        case 'ABIERTA':
          return { itemVersionId: i.itemVersionId, contenido: { texto: b.texto } };
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
