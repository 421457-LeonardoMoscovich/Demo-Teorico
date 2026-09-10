import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LowerCasePipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { IntentoStore } from '../core/intento.store';
import { Resultado } from '../core/modelos';

@Component({
  selector: 'app-alumno-resultado',
  standalone: true,
  imports: [RouterLink, LowerCasePipe],
  template: `
    @if (resultado(); as r) {
      <section class="tarjeta ancho">
        <header class="cabecera-resultado">
          <div class="nota">{{ r.nota }}<span class="sufijo">/100</span></div>
          <div>
            <h2>Intento {{ r.intento }} corregido</h2>
            <p class="ayuda">
              Corregido por <strong>{{ r.corrector | lowercase }}</strong
              >, sobre 100 puntos.
            </p>
            <p class="ayuda">
              No decimos si aprobaste: el umbral de aprobación no lo define ningún documento de la
              plataforma, y como maneja XP y vidas, es una regla de economía. La decide el Tema 03.
            </p>
          </div>
        </header>

        @for (d of r.detalle; track d.itemVersionId) {
          <article class="pregunta" [class.mal]="!d.correcto">
            <h3>
              <span class="posicion">{{ d.orden }}</span>
              {{ d.enunciado }}
              <span class="peso-pregunta" [class.cero]="d.obtenido === 0">
                {{ d.obtenido }}/{{ d.puntaje }}
              </span>
            </h3>
            <p class="ayuda contestaste">
              Contestaste: <code>{{ formatear(d.respuesta) }}</code>
              <span [class]="d.correcto ? 'marca-correcta' : 'marca-incorrecta'">
                {{ d.correcto ? 'Correcta' : 'Incorrecta' }}
              </span>
            </p>
          </article>
        }

        <p class="ayuda">
          El desglose te lo servimos nosotros, directo. Al Tema 03 le mandamos solo la nota: cuánto
          sacaste en cada pregunta es conocimiento del contenido, y a él no le hace falta.
        </p>

        <a class="boton" routerLink="/alumno/cursos">Volver a mis cursos</a>
      </section>
    } @else {
      <section class="tarjeta">
        <p class="vacio">{{ error() || 'Buscando el resultado…' }}</p>
      </section>
    }
  `,
})
export class AlumnoResultadoPage {
  private readonly api = inject(ApiService);
  private readonly intentos = inject(IntentoStore);
  private readonly router = inject(Router);

  readonly resultado = signal<Resultado | null>(null);
  readonly error = signal('');

  constructor() {
    const apertura = this.intentos.intento();
    if (!apertura) {
      this.router.navigateByUrl('/alumno/cursos');
      return;
    }
    this.api.resultado(apertura.entregaId).subscribe({
      next: (r) => this.resultado.set(r),
      error: () => this.error.set('Todavía no hay resultado para esta entrega.'),
    });
  }

  formatear(respuesta: any): string {
    if (respuesta == null) return 'nada';
    if (Array.isArray(respuesta.seleccionadas)) {
      return respuesta.seleccionadas.length ? respuesta.seleccionadas.join(', ') : 'nada';
    }
    if (typeof respuesta.valor === 'boolean') return respuesta.valor ? 'Verdadero' : 'Falso';
    if (Array.isArray(respuesta.pares)) {
      return respuesta.pares.map((p: string[]) => p.join(' → ')).join(' · ') || 'nada';
    }
    if (Array.isArray(respuesta.secuencia)) return respuesta.secuencia.join(' → ') || 'nada';
    return JSON.stringify(respuesta);
  }
}
