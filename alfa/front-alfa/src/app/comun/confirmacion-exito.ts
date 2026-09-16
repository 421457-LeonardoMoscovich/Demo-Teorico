import { Component, OnDestroy, effect, inject, signal } from '@angular/core';
import { ConfirmacionService } from '../core/confirmacion.service';

/** Cuánto queda en pantalla antes de irse solo. */
const DURACION_MS = 1600;

/**
 * El cartel de confirmación: el velo gris, el tilde y el mensaje.
 *
 * Se monta UNA vez, en el shell. Las pantallas no lo importan: piden el cartel
 * por el servicio y no saben cómo se dibuja.
 *
 * Se va solo. Un "Listo" que hay que apretar convierte cada alta de ítem en dos
 * clics, y la profesora carga ítems de a diez. Igual se puede apurar —clic o
 * Esc—: nadie tiene que esperar a una animación.
 */
@Component({
  selector: 'app-confirmacion-exito',
  standalone: true,
  template: `
    @if (confirmaciones.confirmacion(); as c) {
      <div
        class="velo-confirmacion"
        [class.yendose]="yendose()"
        (click)="cerrar()"
        role="status"
        aria-live="polite"
      >
        <div class="cartel-confirmacion">
          <!-- Decorativo: lo que se anuncia es el texto de abajo, no el dibujo. -->
          <svg class="tilde" viewBox="0 0 52 52" aria-hidden="true">
            <circle class="tilde-aro" cx="26" cy="26" r="23" />
            <path class="tilde-trazo" d="M15 27 l8 8 l15 -16" />
          </svg>
          <p class="cartel-mensaje">{{ c.mensaje }}</p>
          @if (c.detalle) {
            <p class="cartel-detalle">{{ c.detalle }}</p>
          }
        </div>
      </div>
    }
  `,
})
export class ConfirmacionExito implements OnDestroy {
  protected readonly confirmaciones = inject(ConfirmacionService);

  /** Encendido mientras corre el fundido de salida. */
  protected readonly yendose = signal(false);

  private reloj: ReturnType<typeof setTimeout> | null = null;
  private readonly conEscape = (e: KeyboardEvent) => {
    if (e.key === 'Escape') this.cerrar();
  };

  constructor() {
    effect(() => {
      const abierta = !!this.confirmaciones.confirmacion();
      this.limpiarReloj();
      if (!abierta) {
        document.removeEventListener('keydown', this.conEscape);
        return;
      }
      this.yendose.set(false);
      document.addEventListener('keydown', this.conEscape);
      this.reloj = setTimeout(() => this.cerrar(), DURACION_MS);
    });
  }

  /**
   * Cierra ya. No se espera al fundido para avisarle al servicio: si el
   * `alCerrar` navega, hacerlo 180ms después dejaría el cartel encima de la
   * pantalla siguiente, que es exactamente lo que se ve mal.
   */
  protected cerrar(): void {
    this.limpiarReloj();
    this.confirmaciones.cerrar();
  }

  ngOnDestroy(): void {
    this.limpiarReloj();
    document.removeEventListener('keydown', this.conEscape);
  }

  private limpiarReloj(): void {
    if (this.reloj !== null) {
      clearTimeout(this.reloj);
      this.reloj = null;
    }
  }
}
