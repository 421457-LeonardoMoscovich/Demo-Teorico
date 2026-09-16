import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { PendientesService } from '../core/pendientes.service';
import { ErrorApi, Pendiente } from '../core/modelos';

/** Lo que tarda la fila corregida en irse. Igual que `--dur-salida` en el CSS. */
const SALIDA_MS = 180;

/**
 * La cola de correccion humana (D-01).
 *
 * Es la pantalla que hace visible por que el puerto `Corrector` no alcanzaba:
 * los cuatro tipos automaticos se corrigen en el mismo milisegundo que la
 * entrega, y con eso el contrato asincronico con el Tema 03 nunca se notaba.
 * Aca se nota, porque hay un humano en el medio.
 */
@Component({
  selector: 'app-profesor-correcciones',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="tarjeta ancho">
      <header class="cabecera-cuestionario">
        <div>
          <h2>Por corregir</h2>
          <span class="etiqueta">{{ pendientes().length }} esperando</span>
        </div>
        <button type="button" class="secundario" (click)="cargar()">Actualizar</button>
      </header>

      <p class="ayuda">
        Respuestas abiertas que no puede puntuar nadie más que vos. Mientras quede una sin corregir,
        ese alumno no tiene nota — y el Tema 03 tampoco se enteró: el evento con la nota recién sale
        cuando ponés el último puntaje.
      </p>

      @if (cargando()) {
        <p class="vacio">Buscando lo que quedó pendiente…</p>
      } @else if (pendientes().length === 0) {
        <p class="vacio">No tenés nada esperando. Todo lo entregado ya está corregido.</p>
      }

      @if (error()) {
        <p class="error" role="alert">{{ error()!.mensaje }}</p>
      }
      @if (ok()) {
        <p class="ok" role="status">{{ ok() }}</p>
      }

      @for (p of pendientes(); track p.detalleId; let idx = $index) {
        <article
          class="correccion"
          [style.--orden]="idx"
          [class.saliendo]="saliendo() === p.detalleId"
        >
          <h3>
            <span class="posicion">{{ p.intento }}</span>
            {{ p.enunciado }}
            <span class="peso-pregunta">vale {{ p.puntaje }}</span>
          </h3>

          <p class="ayuda">
            <span class="etiqueta">{{ p.cuestionario }}</span>
            <span class="etiqueta">alumno {{ p.alumnoId.slice(0, 8) }}</span>
            <span class="etiqueta">intento {{ p.intento }}</span>
          </p>

          @if (p.consigna) {
            <p class="afirmacion">{{ p.consigna }}</p>
          }

          <div class="respuesta-alumno">{{ p.respuesta || '(entregó en blanco)' }}</div>

          <details class="rubrica">
            <summary>Ver la rúbrica que escribiste</summary>
            <p>{{ p.rubrica }}</p>
          </details>

          <div class="fila puntuar">
            <label class="sr-solo" [attr.for]="'p' + p.detalleId">Puntaje</label>
            <input
              class="peso"
              type="number"
              min="0"
              [max]="p.puntaje"
              [id]="'p' + p.detalleId"
              [name]="'p' + p.detalleId"
              [ngModel]="puntajes[p.detalleId] ?? null"
              (ngModelChange)="puntajes[p.detalleId] = $event"
              placeholder="0"
            />
            <span class="ayuda">de {{ p.puntaje }}</span>
            <button
              type="button"
              [disabled]="!valido(p) || guardando() === p.detalleId"
              (click)="puntuar(p)"
            >
              {{ guardando() === p.detalleId ? 'Guardando…' : 'Poner puntaje' }}
            </button>
            <button type="button" class="secundario" (click)="puntajes[p.detalleId] = p.puntaje">
              Completo
            </button>
            <button type="button" class="secundario" (click)="puntajes[p.detalleId] = 0">
              Cero
            </button>
          </div>

          <p class="ayuda">
            Una vez puesto no se puede cambiar acá: una corrección no se pisa (CI-44). Para eso
            existe el recálculo, que deja rastro, y está en backlog.
          </p>
        </article>
      }
    </section>
  `,
})
export class ProfesorCorreccionesPage {
  private readonly api = inject(ApiService);
  private readonly contador = inject(PendientesService);

  readonly pendientes = signal<Pendiente[]>([]);
  readonly cargando = signal(true);
  readonly guardando = signal<string | null>(null);
  readonly error = signal<ErrorApi | null>(null);
  readonly ok = signal('');
  /** La fila que se esta yendo: existe solo mientras dura la salida. */
  readonly saliendo = signal<string | null>(null);

  /** El puntaje tipeado, por detalle. */
  puntajes: Record<string, number | null> = {};

  constructor() {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.api.pendientes().subscribe({
      next: (p) => {
        this.pendientes.set(p);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set({
          clave: 'ERROR',
          campo: null,
          mensaje: 'No se pudo traer la cola de correcciones.',
        });
      },
    });
  }

  valido(p: Pendiente): boolean {
    const valor = this.puntajes[p.detalleId];
    return valor !== null && valor !== undefined && valor >= 0 && valor <= p.puntaje;
  }

  puntuar(p: Pendiente): void {
    this.error.set(null);
    this.ok.set('');
    this.guardando.set(p.detalleId);

    this.api.puntuar(p.detalleId, Number(this.puntajes[p.detalleId])).subscribe({
      next: (r) => {
        this.guardando.set(null);
        this.ok.set(
          r.cerrada
            ? 'Puntaje guardado. Con este se cerró el cuestionario: la nota ya salió hacia el Tema 03.'
            : 'Puntaje guardado. Todavía le falta otra pregunta a esta entrega.',
        );
        // La fila no se borra en el mismo tick: primero se va —se apaga y se
        // corre— y recien despues sale del arreglo. Sin esto la cola pega un
        // salto y la profesora no sabe cual de las tres corrigio.
        this.saliendo.set(p.detalleId);
        setTimeout(() => {
          this.pendientes.update((lista) => lista.filter((x) => x.detalleId !== p.detalleId));
          this.saliendo.set(null);
        }, SALIDA_MS);
        // Que el badge de la barra baje ya, sin esperar al próximo sondeo.
        this.contador.refrescar();
      },
      error: (e) => {
        this.guardando.set(null);
        this.error.set(
          e.error?.mensaje
            ? e.error
            : { clave: 'ERROR', campo: null, mensaje: 'No se pudo guardar el puntaje.' },
        );
      },
    });
  }
}
