import { Injectable, signal } from '@angular/core';
import { Apertura } from './modelos';

const CLAVE = 'g04-alfa-intento';

/**
 * El intento en curso: entregaId, número de intento y el vale de lectura.
 *
 * Vive en el front porque los tres los emite el Tema 03 al abrir el desafío y
 * el front es quien los usa: el vale para pedirnos el contenido, el entregaId
 * para entregar y para consultar el resultado.
 *
 * El vale dura minutos por diseño (CI-18): si el alumno deja la pantalla
 * abierta y vuelve mañana, la lectura falla, y eso es lo correcto.
 */
@Injectable({ providedIn: 'root' })
export class IntentoStore {
  private readonly actual = signal<Apertura | null>(leerGuardado());

  readonly intento = this.actual.asReadonly();

  guardar(apertura: Apertura): void {
    this.actual.set(apertura);
    sessionStorage.setItem(CLAVE, JSON.stringify(apertura));
  }

  limpiar(): void {
    this.actual.set(null);
    sessionStorage.removeItem(CLAVE);
    // El borrador se va con el intento: no tiene sentido sin el.
    this.limpiarBorrador();
  }

  // ---- el borrador de lo que el alumno lleva contestado ----
  //
  // Rendir un examen y perder todo por recargar la pagina no es un detalle de
  // UX: es el tipo de cosa que en una evaluacion real termina en un reclamo.
  // Va en sessionStorage y no en localStorage porque es de esta sesion y de
  // este intento, igual que el vale.

  private claveBorrador(): string | null {
    const entregaId = this.actual()?.entregaId;
    return entregaId ? `${CLAVE}-borrador-${entregaId}` : null;
  }

  guardarBorrador(borradores: unknown): void {
    const clave = this.claveBorrador();
    if (!clave) return;
    try {
      sessionStorage.setItem(clave, JSON.stringify(borradores));
    } catch {
      // Almacenamiento lleno o bloqueado. Se pierde el autoguardado, no la
      // pantalla: el alumno puede seguir contestando igual.
    }
  }

  leerBorrador<T>(): T | null {
    const clave = this.claveBorrador();
    if (!clave) return null;
    try {
      const crudo = sessionStorage.getItem(clave);
      return crudo ? (JSON.parse(crudo) as T) : null;
    } catch {
      return null;
    }
  }

  limpiarBorrador(): void {
    const clave = this.claveBorrador();
    if (clave) sessionStorage.removeItem(clave);
  }
}

function leerGuardado(): Apertura | null {
  try {
    const crudo = sessionStorage.getItem(CLAVE);
    return crudo ? (JSON.parse(crudo) as Apertura) : null;
  } catch {
    return null;
  }
}
