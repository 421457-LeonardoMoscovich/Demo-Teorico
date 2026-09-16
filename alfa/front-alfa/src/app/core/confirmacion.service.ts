import { Injectable, signal } from '@angular/core';

/** Lo que se está confirmando en pantalla. */
export interface Confirmacion {
  /** El mensaje grande: qué pasó. */
  mensaje: string;
  /** La línea chica de abajo: qué sigue. Opcional. */
  detalle?: string;
  /** Se ejecuta cuando el cartel se va, se haya ido solo o lo hayan cerrado. */
  alCerrar?: () => void;
}

/**
 * El cartel de "salió bien".
 *
 * Existe porque el aviso inline que teníamos —un párrafo verde abajo del
 * formulario— se lo perdía justo quien más lo necesitaba: el que apretó
 * Guardar mira el botón, no el pie de la página. Un cartel al medio, que tapa
 * todo por un segundo, no se puede no ver.
 *
 * Es un servicio y no un componente por pantalla para que el cartel sea uno
 * solo, montado en el shell: si cada página trajera el suyo, dos avisos
 * simultáneos se pisarían y el markup estaría copiado cuatro veces.
 *
 * Solo para el éxito. El error necesita quedarse en pantalla, decir qué campo
 * está mal y dejar leerlo con calma: eso sigue siendo el `.error` inline de
 * cada formulario, y taparlo con un modal que se va solo sería peor.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmacionService {
  private readonly actual = signal<Confirmacion | null>(null);

  readonly confirmacion = this.actual.asReadonly();

  mostrar(confirmacion: Confirmacion): void {
    this.actual.set(confirmacion);
  }

  /**
   * Cierra y recién entonces corre lo que seguía. El orden importa: si el
   * `alCerrar` navega, hacerlo antes dejaría el cartel colgado sobre la
   * pantalla nueva.
   */
  cerrar(): void {
    const abierta = this.actual();
    if (!abierta) return;
    this.actual.set(null);
    abierta.alCerrar?.();
  }
}
