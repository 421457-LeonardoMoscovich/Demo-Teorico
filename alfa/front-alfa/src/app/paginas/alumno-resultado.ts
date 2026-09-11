import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LowerCasePipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { IntentoStore } from '../core/intento.store';
import { ItemCorregido, Resultado } from '../core/modelos';

@Component({
  selector: 'app-alumno-resultado',
  standalone: true,
  imports: [RouterLink, LowerCasePipe],
  template: `
    @if (resultado(); as r) {
      <section class="tarjeta ancho">
        <header class="cabecera-resultado">
          @if (esperando()) {
            <div class="nota esperando">
              <span class="sufijo">sin nota<br />todavía</span>
            </div>
          } @else {
            <div class="nota">{{ r.nota }}<span class="sufijo">/100</span></div>
          }
          <div>
            @if (esperando()) {
              <h2>Intento {{ r.intento }} entregado</h2>
              <p class="ayuda">
                Tu cuestionario tiene {{ cuantasPendientes() }}
                {{ cuantasPendientes() === 1 ? 'pregunta' : 'preguntas' }} de respuesta abierta.
                Esas las corrige tu profesor a mano, así que la nota no está todavía.
              </p>
              <p class="ayuda">
                <strong>No te vamos a avisar cuando esté.</strong> No es un olvido: si te enteraras
                por nosotros, lo sabrías antes de que el Tema 03 aplique la penalidad por tardanza y
                el Tema 10 el XP, y verías una nota que después cambia. Volvé a entrar y miralo acá.
              </p>
            } @else {
              <h2>Intento {{ r.intento }} corregido</h2>
              <p class="ayuda">
                Corregido por <strong>{{ r.corrector | lowercase }}</strong
                >, sobre 100 puntos.
              </p>
              <p class="ayuda">
                No decimos si aprobaste: el umbral de aprobación no lo define ningún documento de la
                plataforma, y como maneja XP y vidas, es una regla de economía. La decide el Tema
                03.
              </p>
            }
          </div>
        </header>

        @for (d of r.detalle; track d.itemVersionId; let idx = $index) {
          <article
            class="pregunta"
            [style.--orden]="idx"
            [class.mal]="d.correcto === false"
            [class.espera]="d.pendiente"
          >
            <h3>
              <span class="posicion">{{ d.orden }}</span>
              {{ d.enunciado }}
              @if (d.pendiente) {
                <span class="peso-pregunta espera">— /{{ d.puntaje }}</span>
              } @else {
                <span class="peso-pregunta" [class.cero]="d.obtenido === 0">
                  {{ d.obtenido }}/{{ d.puntaje }}
                </span>
              }
            </h3>
            <p class="ayuda contestaste">
              Contestaste: <code>{{ formatear(d) }}</code>
              @if (d.pendiente) {
                <span class="marca-espera">La corrige tu profesor</span>
              } @else if (d.correcto === null) {
                <!-- Puntaje parcial: no es correcta ni incorrecta, y decir
                     cualquiera de las dos seria mentir. -->
                <span class="marca-parcial">Parcial</span>
              } @else {
                <span [class]="d.correcto ? 'marca-correcta' : 'marca-incorrecta'">
                  {{ d.correcto ? 'Correcta' : 'Incorrecta' }}
                </span>
              }
            </p>

            <!--
              La devolución (CI-58). Llega recortada del backend: solo el texto
              general y el de las opciones que ESTE alumno marcó. La del resto
              diría cuál era la correcta, y con reintentos ilimitados eso
              convierte el reintento en copiar.
            -->
            @if (d.devolucion) {
              <div class="devolucion">
                @if (d.devolucion.general) {
                  <p>{{ d.devolucion.general }}</p>
                }
                @for (o of d.devolucion.porOpcion ?? []; track o.id) {
                  <p class="por-opcion">
                    <span class="etiqueta">{{ textoDeOpcion(d, o.id) }}</span>
                    {{ o.texto }}
                  </p>
                }
              </div>
            }
          </article>
        }

        <p class="ayuda">
          El desglose te lo servimos nosotros, directo. Al Tema 03 le mandamos solo la nota: cuánto
          sacaste en cada pregunta es conocimiento del contenido, y a él no le hace falta.
        </p>

        <div class="acciones-fila">
          @if (desdeElHistorial()) {
            <a class="boton" routerLink="/alumno/historial">Volver a mis entregas</a>
          } @else {
            <a class="boton" routerLink="/alumno/cursos">Volver a mis cursos</a>
            <a class="boton secundario" routerLink="/alumno/historial">Ver todas mis entregas</a>
          }
        </div>
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
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly resultado = signal<Resultado | null>(null);
  readonly error = signal('');
  readonly desdeElHistorial = signal(false);

  readonly esperando = computed(() => this.resultado()?.estado === 'EN_ESPERA');
  readonly cuantasPendientes = computed(
    () => this.resultado()?.detalle.filter((d) => d.pendiente).length ?? 0,
  );

  constructor() {
    // Dos formas de llegar: recién entregado (el intento en curso) o desde el
    // historial (la entregaId en la URL). La segunda gana si está.
    const deLaUrl = this.ruta.snapshot.queryParamMap.get('entrega');
    const entregaId = deLaUrl ?? this.intentos.intento()?.entregaId;

    if (!entregaId) {
      this.router.navigateByUrl('/alumno/cursos');
      return;
    }
    this.desdeElHistorial.set(deLaUrl !== null);

    this.api.resultado(entregaId).subscribe({
      next: (r) => this.resultado.set(r),
      error: () => this.error.set('Todavía no hay resultado para esta entrega.'),
    });
  }

  /**
   * Traduce lo que contestó el alumno a algo que se pueda leer.
   *
   * Las respuestas viajan por id —`a`, `i1`, `e3`—, que es lo correcto: el id es
   * la identidad estable de una opción y el texto puede cambiar entre versiones.
   * Pero "Contestaste: a" no le dice nada a nadie. Los textos salen del payload
   * ESTAMPADO, o sea el de la versión que este alumno vio; si el profesor editó
   * la pregunta después, acá sigue apareciendo lo que él leyó (CI-13).
   */
  formatear(d: ItemCorregido): string {
    const r = d.respuesta;
    if (r == null) return 'nada';

    // Un diccionario id → texto con todo lo que traiga el payload, sea cual sea
    // el tipo: opciones, los dos lados de un emparejar, o los elementos a ordenar.
    const p = d.payload ?? {};
    const texto = new Map<string, string>();
    for (const lista of [p.opciones, p.izquierda, p.derecha, p.elementos]) {
      for (const o of lista ?? []) texto.set(o.id, o.texto);
    }
    const leer = (id: string) => texto.get(id) ?? id;

    if (typeof r.texto === 'string') return r.texto || 'nada';
    if (Array.isArray(r.seleccionadas)) {
      return r.seleccionadas.length ? r.seleccionadas.map(leer).join(' · ') : 'nada';
    }
    if (typeof r.valor === 'boolean') return r.valor ? 'Verdadero' : 'Falso';
    if (Array.isArray(r.pares)) {
      return (
        r.pares.map((par: string[]) => `${leer(par[0])} → ${leer(par[1])}`).join(' · ') || 'nada'
      );
    }
    if (Array.isArray(r.secuencia)) return r.secuencia.map(leer).join(' → ') || 'nada';
    return JSON.stringify(r);
  }

  /**
   * El texto de la opción que una devolución comenta.
   *
   * Sale del payload estampado por el mismo motivo que `formatear`: sin esto la
   * devolución arrancaría con "a" y el alumno tendría que volver a la pregunta
   * para saber de cuál le están hablando.
   */
  textoDeOpcion(d: ItemCorregido, id: string): string {
    for (const o of d.payload?.opciones ?? []) {
      if (o.id === id) return o.texto;
    }
    return id;
  }
}
