import { Injectable, signal } from '@angular/core';

export type Tema = 'dark' | 'light';

const CLAVE = 'g04-alfa-tema';

/**
 * El modo de la interfaz.
 *
 * El oscuro es el que manda: la paleta neón de ED-CADE nació para `#0D0B1E`,
 * y el claro es la variante. Por eso es el valor por defecto cuando el viewer
 * no eligió nada todavía.
 *
 * El tema se escribe como `data-tema` en el <html> y toda la hoja de estilos
 * cuelga de ahí: cambiar de modo no toca ni un componente.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly actual = signal<Tema>(leerGuardado());

  readonly tema = this.actual.asReadonly();

  constructor() {
    this.aplicar(this.actual());
  }

  alternar(): void {
    this.poner(this.actual() === 'dark' ? 'light' : 'dark');
  }

  poner(tema: Tema): void {
    this.actual.set(tema);
    this.aplicar(tema);
    try {
      localStorage.setItem(CLAVE, tema);
    } catch {
      // Ventana privada o almacenamiento bloqueado: el tema vale para esta
      // pestaña y nada más. No es motivo para romper la pantalla.
    }
  }

  private aplicar(tema: Tema): void {
    document.documentElement.setAttribute('data-tema', tema);
  }
}

function leerGuardado(): Tema {
  try {
    return localStorage.getItem(CLAVE) === 'light' ? 'light' : 'dark';
  } catch {
    return 'dark';
  }
}
