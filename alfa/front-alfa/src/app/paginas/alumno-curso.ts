import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LowerCasePipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { SesionService } from '../core/sesion.service';
import { IntentoStore } from '../core/intento.store';
import { Curso, Desafio, Unidad } from '../core/modelos';

/**
 * El mismo roadmap que ve la profesora, del lado del alumno.
 *
 * Empezar un desafio es un acto del Tema 03: crea el intento y devuelve el
 * vale con el que despues le pedimos el contenido al Tema 04 (CI-21).
 */
@Component({
  selector: 'app-alumno-curso',
  standalone: true,
  imports: [LowerCasePipe],
  template: `
    @if (curso(); as c) {
      <section class="tarjeta ancho">
        <header class="cabecera-cuestionario">
          <div>
            <h2>{{ c.nombre }}</h2>
            <span class="etiqueta">{{ c.periodo }}</span>
            <span class="etiqueta">{{ c.docente }}</span>
          </div>
          <button type="button" class="secundario" (click)="volver()">Volver a mis cursos</button>
        </header>

        <p class="ayuda">
          <span class="fuente">Tema 02</span> el roadmap · <span class="fuente">Tema 03</span> qué
          desafíos están abiertos y cuántos intentos te quedan. Nosotros no sabemos ninguna de las
          dos cosas.
        </p>

        @for (u of c.unidades; track u.unidadId) {
          <article class="unidad">
            <h3>
              <span class="posicion">{{ u.orden }}</span>
              {{ u.titulo }}
            </h3>
            <p class="ayuda descripcion-unidad">{{ u.descripcion }}</p>

            @if (desafiosDe(u).length > 0) {
              <ul class="lista compacta">
                @for (d of desafiosDe(u); track d.desafioId) {
                  <li>
                    <div>
                      <p class="titulo-desafio">{{ d.titulo }}</p>
                      <span class="etiqueta">{{ d.contenidoRef.resumen }}</span>
                      <span
                        class="etiqueta"
                        [class.diferida]="d.contenidoRef.correccion === 'DIFERIDA'"
                      >
                        corrección {{ d.contenidoRef.correccion | lowercase }}
                      </span>
                    </div>
                    <button type="button" (click)="empezar(d)">Empezar</button>
                  </li>
                }
              </ul>
            } @else {
              <p class="vacio">Sin desafíos abiertos en esta unidad.</p>
            }
          </article>
        }

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }
      </section>
    } @else {
      <section class="tarjeta">
        <p class="vacio">{{ error() || 'Cargando el curso…' }}</p>
      </section>
    }
  `,
})
export class AlumnoCursoPage {
  private readonly api = inject(ApiService);
  private readonly sesion = inject(SesionService);
  private readonly intentos = inject(IntentoStore);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly curso = signal<Curso | null>(null);
  readonly desafios = signal<Desafio[]>([]);
  readonly error = signal('');

  constructor() {
    const id = this.ruta.snapshot.paramMap.get('cursoId')!;
    this.api.curso(id).subscribe({
      next: (c) => this.curso.set(c),
      error: () => this.error.set('El Tema 02 no encontró ese curso.'),
    });
    this.api.desafiosAbiertos(id).subscribe({
      next: (d) => this.desafios.set(d),
      error: () => this.error.set('No se pudo hablar con el Tema 03.'),
    });
  }

  desafiosDe(u: Unidad): Desafio[] {
    return this.desafios().filter((d) => d.unidadId === u.unidadId);
  }

  empezar(desafio: Desafio): void {
    const alumnoId = this.sesion.sesion()?.id;
    if (!alumnoId) return;
    this.api.abrirIntento(desafio.desafioId, alumnoId).subscribe({
      next: (apertura) => {
        this.intentos.guardar(apertura);
        this.router.navigateByUrl('/alumno/responder');
      },
      error: () => this.error.set('El Tema 03 no dejó abrir el intento.'),
    });
  }

  volver(): void {
    this.router.navigateByUrl('/alumno/cursos');
  }
}
