import { Injectable, computed, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { SesionService } from './sesion.service';

/** Cada cuánto se le pregunta al backend si hay algo nuevo para corregir. */
const CADA_MS = 10_000;

/**
 * El contador de la barra: cuántas correcciones esperan a la profesora.
 *
 * Sondea, y eso es una decisión con fecha de vencimiento. Lo correcto es que el
 * aviso viaje por el mismo bus que ya publica `TEORICO_CORREGIDO` —el evento de
 * entrega existe— y que el front lo reciba por WebSocket o SSE. Pero eso es
 * infraestructura que la alfa no tiene (no hay Kafka todavía), y un sondeo cada
 * diez segundos contra un endpoint que devuelve un número es honesto para dos
 * usuarios y una demo. Lo que NO habría que hacer es dejarlo así cuando entre
 * Kafka: ahí el sondeo se borra, no se le baja el intervalo.
 *
 * Y una asimetría que es contrato, no descuido: a la profesora se le avisa, al
 * alumno NO (CI-37). Si el alumno se enterara de su nota por nosotros, la vería
 * antes de que el Tema 03 aplique la penalidad por tardanza y el Tema 10 el XP.
 */
@Injectable({ providedIn: 'root' })
export class PendientesService {
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SesionService);

  private readonly cuantas = signal(0);
  private timer: ReturnType<typeof setInterval> | null = null;

  readonly pendientes = this.cuantas.asReadonly();
  readonly hay = computed(() => this.cuantas() > 0);

  /** Idempotente: llamarlo dos veces no deja dos timers corriendo. */
  arrancar(): void {
    if (this.timer !== null) return;
    this.refrescar();
    this.timer = setInterval(() => this.refrescar(), CADA_MS);
  }

  detener(): void {
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.cuantas.set(0);
  }

  refrescar(): void {
    if (!this.sesion.esProfesor()) {
      this.cuantas.set(0);
      return;
    }
    this.api.cuantasPendientes().subscribe({
      next: (r) => this.cuantas.set(r.pendientes),
      // Un sondeo que falla se calla: no vamos a llenarle la pantalla de
      // errores a la profesora porque un GET de fondo no salió.
      error: () => {},
    });
  }
}
