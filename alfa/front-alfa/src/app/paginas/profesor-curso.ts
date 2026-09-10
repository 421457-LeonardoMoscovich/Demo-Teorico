import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LowerCasePipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { Curso, Desafio, Unidad } from '../core/modelos';

/**
 * El roadmap del curso, con los desafios que ya tiene cada unidad.
 *
 * Dos grupos distintos llenan esta pantalla y ninguno es el nuestro: las
 * unidades vienen del Tema 02 y los desafios del Tema 03. Lo nuestro empieza
 * recien cuando el profesor elige armar un cuestionario teorico.
 */
@Component({
  selector: 'app-profesor-curso',
  standalone: true,
  template: `
    @if (curso(); as c) {
      <section class="tarjeta ancho">
        <header class="cabecera-cuestionario">
          <div>
            <h2>{{ c.nombre }}</h2>
            <span class="etiqueta">{{ c.periodo }}</span>
            <span class="etiqueta">{{ c.unidades.length }} unidades</span>
          </div>
          <button type="button" class="secundario" (click)="volver()">Volver a mis cursos</button>
        </header>

        <p class="ayuda">
          <span class="fuente">Tema 02</span> el roadmap y sus unidades ·
          <span class="fuente">Tema 03</span> los desafíos de cada una
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
                      <span class="etiqueta">{{ d.contenidoRef.tipo | lowercase }}</span>
                      <span class="etiqueta">{{ d.contenidoRef.resumen }}</span>
                      <span
                        class="etiqueta"
                        [class.diferida]="d.contenidoRef.correccion === 'DIFERIDA'"
                      >
                        corrección {{ d.contenidoRef.correccion | lowercase }}
                      </span>
                    </div>
                  </li>
                }
              </ul>
            } @else {
              <p class="vacio">Esta unidad todavía no tiene desafíos.</p>
            }

            @if (abierta() === u.unidadId) {
              <div class="tipos-desafio">
                <p class="ayuda">
                  Qué tipo de desafío. La tabla de tipo → servicio la tiene el front, no el Tema 03:
                  es él quien sabe a qué grupo pedirle el contenido.
                </p>
                <button type="button" (click)="armarTeorico(c, u)">Teórico</button>
                <button type="button" class="secundario" disabled>Práctico · Tema 05</button>
              </div>
            } @else {
              <button type="button" class="secundario" (click)="abrir(u)">+ Crear desafío</button>
            }
          </article>
        }
      </section>
    } @else {
      <section class="tarjeta">
        <p class="vacio">{{ error() || 'Cargando el curso…' }}</p>
      </section>
    }
  `,
  imports: [LowerCasePipe],
})
export class ProfesorCursoPage {
  private readonly api = inject(ApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly curso = signal<Curso | null>(null);
  readonly desafios = signal<Desafio[]>([]);
  readonly abierta = signal<string | null>(null);
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

  abrir(u: Unidad): void {
    this.abierta.set(u.unidadId);
  }

  /**
   * Se va a la pantalla de armar con el curso y la unidad en la URL: el
   * cuestionario no se compone en el vacio, pertenece a un nodo del roadmap.
   */
  armarTeorico(c: Curso, u: Unidad): void {
    this.router.navigate(['/profesor/armar'], {
      queryParams: { curso: c.cursoCohorteId, unidad: u.unidadId, unidadTitulo: u.titulo },
    });
  }

  volver(): void {
    this.router.navigateByUrl('/profesor/cursos');
  }
}
