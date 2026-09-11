import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { SesionService } from '../core/sesion.service';
import { Curso } from '../core/modelos';

/**
 * Los cursos son del Tema 02, no nuestros.
 *
 * Esta pantalla existe para que la demo empiece donde empieza el profesor de
 * verdad —en su curso— y no en el medio, con un cuestionario suelto.
 */
@Component({
  selector: 'app-cursos',
  standalone: true,
  template: `
    <section class="tarjeta ancho">
      <h2>Mis cursos</h2>
      <p class="ayuda">
        <span class="fuente">Tema 02</span>
        Los cursos, las cohortes y el roadmap son del Tema 02. Nosotros no los guardamos: acá los
        sirve un stub de ese grupo.
      </p>

      @if (cargando()) {
        <p class="vacio">Cargando cursos…</p>
      } @else if (cursos().length === 0 && !error()) {
        <p class="vacio">El Tema 02 no te tiene asignado a ningún curso.</p>
      }

      <ul class="lista">
        @for (c of cursos(); track c.cursoCohorteId; let idx = $index) {
          <li [style.--orden]="idx">
            <div>
              <p class="titulo-desafio">{{ c.nombre }}</p>
              <span class="etiqueta">{{ c.periodo }}</span>
              <span class="etiqueta">{{ c.unidades.length }} unidades</span>
            </div>
            <button type="button" (click)="entrar(c)">Entrar</button>
          </li>
        }
      </ul>

      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
    </section>
  `,
})
export class CursosPage {
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SesionService);
  private readonly router = inject(Router);

  readonly cursos = signal<Curso[]>([]);
  readonly error = signal('');
  /**
   * Estado explicito y no `cursos().length === 0`: una lista vacia y una lista
   * que todavia no llego se ven igual, y sin esto la pantalla se queda
   * "Cargando…" para siempre cuando el Tema 02 responde [].
   */
  readonly cargando = signal(true);

  constructor() {
    this.api.cursos().subscribe({
      next: (c) => {
        this.cursos.set(c);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.error.set('No se pudo hablar con el Tema 02.');
      },
    });
  }

  entrar(curso: Curso): void {
    const base = this.sesion.esProfesor() ? '/profesor/cursos/' : '/alumno/cursos/';
    this.router.navigateByUrl(base + curso.cursoCohorteId);
  }
}
