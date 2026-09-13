import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { JsonPipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../core/api.service';
import { ConfirmacionService } from '../core/confirmacion.service';
import { FormularioItemComponent } from './formulario-item';
import {
  ContenidoRef,
  ErrorApi,
  ItemDetalle,
  ItemResumen,
  MiContenido,
  NAVEGACIONES,
  Navegacion,
  TIPOS,
  TipoDeItem,
} from '../core/modelos';

/** Los pesos que se ofrecen de un clic, para que no haya que tipear. */
const CHIPS = [5, 10, 15, 20, 25, 50];

/**
 * Las cantidades de preguntas que reparten 100 en partes iguales y enteras. Con
 * cualquier otra, un cuestionario que es SOLO sorteo no puede cerrar.
 */
const DIVISORES_DE_100 = [1, 2, 4, 5, 10, 20, 25, 50, 100];

/** El divisor de 100 mas cercano a `cuantos` que entre en una bolsa de `tope`. */
function divisorMasCercano(cuantos: number, tope: number): number {
  const posibles = DIVISORES_DE_100.filter((d) => d <= Math.max(1, tope));
  if (posibles.length === 0) return cuantos;
  return posibles.reduce((mejor, d) =>
    Math.abs(d - cuantos) < Math.abs(mejor - cuantos) ? d : mejor,
  );
}

interface Elegido {
  item: ItemResumen;
  puntaje: number;
  /**
   * El profesor escribio o clickeo ESTE peso a mano. Un peso manual no se
   * vuelve a tocar solo: agregar o quitar otro item reparte el remanente entre
   * los que quedaron libres, y nunca encima de una decision ya tomada.
   */
  manual: boolean;
}

@Component({
  selector: 'app-profesor-armar',
  standalone: true,
  imports: [FormsModule, JsonPipe, FormularioItemComponent],
  template: `
    <section class="tarjeta ancho">
      <h2>Armar cuestionario</h2>
      <p class="ayuda">
        <span class="fuente">Unidad {{ unidadId() }}</span>
        {{ unidadTitulo() }}
      </p>

      @if (yaArmados().length > 0 && elegidos().length === 0 && !regla()) {
        <div class="reutilizar">
          <h3>Reutilizar uno que ya armaste</h3>
          <details class="porque">
            <summary>Es una referencia, no una copia</summary>
            <div class="cuerpo">
              <p class="ayuda">
                El desafío guarda una <strong>referencia</strong> al cuestionario, no una copia. Si
                colgás el mismo de dos unidades o de dos cohortes, editar una pregunta las cambia
                en las dos — y cada alumno se sigue corrigiendo contra la versión que vio.
              </p>
            </div>
          </details>
          @for (c of aReutilizar(); track c.contenidoId) {
            <div class="fila">
              <span class="concepto">
                {{ c.titulo }}
                <span class="version">{{ c.ficha.resumen }}</span>
              </span>
              <button type="button" class="secundario" (click)="colgarExistente(c)">
                Colgar acá
              </button>
            </div>
          }
          @if (yaArmados().length > aReutilizar().length) {
            <button type="button" class="secundario" (click)="verTodosLosArmados.set(true)">
              Ver los otros {{ yaArmados().length - aReutilizar().length }}
            </button>
          }
          <p class="ayuda">O armá uno nuevo desde cero.</p>
        </div>
      }

      <label>
        Título
        <input
          name="titulo"
          [ngModel]="titulo()"
          (ngModelChange)="titulo.set($event)"
          placeholder="Parcial 1 — Arquitectura"
        />
      </label>

      <div class="acciones-fila">
        <button type="button" (click)="abrirCajon()">+ Elegir preguntas del banco</button>
        @if (!regla()) {
          <button type="button" class="secundario" (click)="abrirSorteo()">
            + Sortear por etiqueta
          </button>
        }
      </div>

      @if (elegidos().length === 0 && !regla()) {
        <p class="vacio">
          El cuestionario está vacío. Agregá preguntas del banco, o sorteá unas cuantas de una
          etiqueta.
        </p>
      }

      @for (e of elegidos(); track e.item.id; let i = $index) {
        <div class="linea-peso">
          <span class="posicion">{{ i + 1 }}</span>
          <div class="linea-texto">
            <p>{{ e.item.enunciado }}</p>
            <div class="chips">
              @for (c of chips; track c) {
                <button
                  type="button"
                  class="chip"
                  [class.activo]="e.puntaje === c"
                  (click)="fijar(i, c)"
                >
                  {{ c }}%
                </button>
              }
            </div>
          </div>
          <input
            class="peso"
            type="number"
            min="1"
            max="100"
            [ngModel]="e.puntaje"
            (ngModelChange)="fijar(i, $event)"
            [name]="'peso' + i"
            attr.aria-label="Peso en porcentaje de {{ e.item.enunciado }}"
          />
          <button
            type="button"
            class="secundario"
            attr.aria-label="Quitar {{ e.item.enunciado }}"
            (click)="quitar(i)"
          >
            <span aria-hidden="true">×</span>
          </button>
        </div>
      }

      <!--
        La regla de sorteo. No se listan preguntas porque todavía no existen:
        se eligen cuando hay un alumno, y cada uno recibe las suyas.
      -->
      @if (regla(); as r) {
        <div class="regla-sorteo" [class.mal]="sorteoImposible()">
          <h3>
            <span aria-hidden="true">⚄</span>
            Al azar de <span class="chip activo">{{ r.etiqueta }}</span>
          </h3>

          <div class="fila">
            <label>
              Cuántas
              <input
                class="peso"
                type="number"
                min="1"
                name="cuantos"
                [ngModel]="r.cuantos"
                (ngModelChange)="cambiarCuantos($event)"
              />
            </label>
            <label>
              Cada una vale
              <input
                class="peso"
                type="number"
                min="1"
                max="100"
                name="puntajeSorteo"
                [ngModel]="r.puntaje"
                (ngModelChange)="cambiarPuntajeDelSorteo($event)"
              />
            </label>
            <span class="concepto">
              @if (!sorteoImposible()) {
                {{ r.cuantos }} de {{ candidatas().length }} candidatas · aporta
                {{ r.cuantos * r.puntaje }}%
              } @else {
                Con esa etiqueta hay {{ candidatas().length }} preguntas listas y pediste
                {{ r.cuantos }}.
              }
            </span>
            <button
              type="button"
              class="secundario"
              aria-label="Quitar el sorteo"
              (click)="quitarSorteo()"
            >
              <span aria-hidden="true">×</span>
            </button>
          </div>

          <details class="porque">
            <summary>Qué preguntas le van a tocar a cada alumno</summary>
            <div class="cuerpo">
              <p class="ayuda">
                No se sortean ahora: se sortean cuando cada alumno abre el desafío, y a cada uno le
                tocan otras. El sorteo se <strong>deriva</strong> del cuestionario y del alumno, así
                que el mismo alumno recibe siempre las mismas —recargue cuando recargue— sin que
                guardemos en ningún lado qué le tocó a quién.
              </p>
              <p class="ayuda">
                La bolsa está <strong>viva</strong>: si cargás preguntas nuevas con esta etiqueta,
                entran al sorteo de los que todavía no rindieron. Si sacás tantas que no alcanzan,
                el cuestionario deja de poder abrirse y hay que volver a armarlo.
              </p>
            </div>
          </details>
        </div>
      }

      <fieldset class="navegacion">
        <legend>Cómo lo recorre el alumno</legend>
        @for (n of navegaciones; track n.valor) {
          <label class="check" [class.elegida]="navegacion() === n.valor">
            <input
              type="radio"
              name="navegacion"
              [checked]="navegacion() === n.valor"
              (change)="navegacion.set(n.valor)"
            />
            <span>
              <strong>{{ n.etiqueta }}</strong>
              <span class="ayuda-inline">{{ n.ayuda }}</span>
            </span>
          </label>
        }
      </fieldset>
      <details class="porque">
        <summary>Cómo funciona cada modo</summary>
        <div class="cuerpo">
          <p class="ayuda">
            Las consignas se sirven de a una en los dos modos. Lo que cambia es si puede volver.
            Avanzar sin contestar se permite siempre: esa pregunta se entrega en blanco y vale 0
            —igual que hoy—, y el alumno ve cuántas le quedan antes de entregar.
          </p>
        </div>
      </details>

      <label class="check">
        <input
          type="checkbox"
          name="ilimitados"
          [ngModel]="reintentosIlimitados()"
          (ngModelChange)="reintentosIlimitados.set($event)"
        />
        Reintentos ilimitados
      </label>
      <details class="porque">
        <summary>Por qué los reintentos los decide el Tema 03</summary>
        <div class="cuerpo">
          <p class="ayuda">
            CI-47: un desafío con reintentos ilimitados no admite corrección humana —pondría al
            profesor a corregir la misma entrega infinitas veces—. La regla la aplica el Tema 03,
            que es quien sabe de reintentos; nosotros solo le mandamos <code>correccion</code> en la
            ficha.
          </p>
        </div>
      </details>

      <!--
        Las cuatro pantallas que Moodle pone en este mismo formulario y que acá
        no estan. Se muestran en vez de omitirse: la ausencia sola parece un
        agujero, la ausencia rotulada es la frontera (ver 5c del contrato).
      -->
      <details class="porque frontera">
        <summary>Lo que no se configura acá</summary>
        <div class="cuerpo">
          <p class="ayuda">
            En Moodle todo esto vive en el mismo formulario que las preguntas. Acá cada cosa es de
            quien la sabe, y el cuestionario es una sola de las partes del desafío.
          </p>
          <p class="linea-frontera">
            <span class="fuente">Tema 03</span> Cuándo abre, cuándo cierra y el límite de tiempo
            <span class="version">CI-15 · CI-20</span>
          </p>
          <p class="linea-frontera">
            <span class="fuente">Tema 03</span> Cuántos intentos hay y cuál de ellos cuenta
            <span class="version">CI-45</span>
          </p>
          <p class="linea-frontera">
            <span class="fuente">Tema 03</span> La nota con la que se aprueba
            <span class="version">CI-39</span>
          </p>
          <p class="linea-frontera">
            <span class="fuente">Tema 02</span> Quiénes pueden entrar: curso, cohorte y grupo
            <span class="version">CI-01</span>
          </p>
        </div>
      </details>

      @if (error()) {
        <p class="error" role="alert">
          {{ error()!.mensaje }}
          @if (error()!.campo) {
            <span class="campo">({{ error()!.campo }})</span>
          }
        </p>
      }

      @if (ficha()) {
        <div class="ficha">
          <h3>Publicado</h3>
          <p class="ayuda">
            Esto es lo único que viaja al Tema 03: cinco campos, y el único que interpreta es
            <code>tipo</code>. No sabe qué preguntas hay adentro, ni que algunas se sortean.
          </p>
          <pre>{{ ficha() | json }}</pre>
          <div class="acciones-fila">
            <button type="button" (click)="volverAlRoadmap()">Ver la unidad en el roadmap</button>
          </div>
        </div>
      }

      <div class="barra-accion">
        @if (hayAlgo()) {
          <div
            class="totalizador"
            role="status"
            aria-live="polite"
            [class.mal]="suma() !== 100 || hayEnCero() || sorteoImposible()"
          >
            <strong>{{ suma() }}%</strong>
            <span>{{ mensajeDeSuma() }}</span>
            @if (cuantosQueCierra(); as sugerido) {
              <button type="button" class="secundario" (click)="usarCuantosQueCierra()">
                Sortear {{ sugerido }} y cerrar en 100
              </button>
            } @else {
              <button type="button" class="secundario" (click)="repartirParejo()">
                Repartir parejo
              </button>
            }
          </div>
        }
        <button type="button" [disabled]="!listo() || publicando()" (click)="publicar()">
          {{ publicando() ? 'Publicando…' : 'Publicar cuestionario' }}
        </button>
      </div>
    </section>

    <!--
      El banco entra en un cajón y no en una columna fija. El catálogo completo
      al costado obligaba a scrollear la pantalla entera para mirar lo que se
      estaba armando; acá aparece cuando se lo pide y se va cuando terminó.
    -->
    @if (cajon()) {
      <div class="cajon-fondo" (click)="cerrarCajon()"></div>
      <aside class="cajon" role="dialog" aria-modal="true" aria-label="Elegir preguntas del banco">
        <header class="cajon-cabecera">
          <h3>Elegir del banco</h3>
          <button type="button" class="secundario" aria-label="Cerrar" (click)="cerrarCajon()">
            <span aria-hidden="true">×</span>
          </button>
        </header>

        <div class="cajon-cuerpo">
          @if (creando()) {
            <app-formulario-item textoCrear="Guardar y agregar" (guardado)="alCrear($event)" />
            <button type="button" class="secundario" (click)="creando.set(false)">
              Cerrar el formulario
            </button>
          } @else {
            <input
              name="busqueda"
              class="buscador"
              placeholder="Buscar por enunciado…"
              autocomplete="off"
              [ngModel]="busqueda()"
              (ngModelChange)="busqueda.set($event)"
            />

            @if (etiquetasDelBanco().length > 0) {
              <div class="chips">
                <button
                  type="button"
                  class="chip"
                  [class.activo]="porEtiqueta() === ''"
                  (click)="porEtiqueta.set('')"
                >
                  todas
                </button>
                @for (e of etiquetasDelBanco(); track e) {
                  <button
                    type="button"
                    class="chip"
                    [class.activo]="porEtiqueta() === e"
                    (click)="porEtiqueta.set(porEtiqueta() === e ? '' : e)"
                  >
                    {{ e }}
                  </button>
                }
              </div>
            }

            <ul class="lista">
              @for (i of disponibles(); track i.id) {
                <li>
                  <label class="check" [class.elegida]="marcados().includes(i.id)">
                    <input
                      type="checkbox"
                      [checked]="marcados().includes(i.id)"
                      (change)="marcar(i.id)"
                    />
                    <span>
                      <span class="etiqueta">{{ etiqueta(i.tipo) }}</span>
                      {{ i.enunciado }}
                    </span>
                  </label>
                </li>
              }
              @if (disponibles().length === 0) {
                <li class="vacio">
                  @if (busqueda() || porEtiqueta()) {
                    Ninguna pregunta sin usar con ese filtro.
                  } @else {
                    No queda ninguna pregunta sin usar.
                  }
                </li>
              }
            </ul>

            <button type="button" class="secundario" (click)="creando.set(true)">
              + Escribir una pregunta nueva
            </button>
          }
        </div>

        @if (!creando()) {
          <footer class="cajon-pie">
            <button type="button" [disabled]="marcados().length === 0" (click)="agregarMarcados()">
              Agregar
              @if (marcados().length > 0) {
                {{ marcados().length }}
              }
            </button>
            <button type="button" class="secundario" (click)="cerrarCajon()">Cancelar</button>
          </footer>
        }
      </aside>
    }

    <!-- El sorteo se configura en su propio cajón: es una decisión aparte. -->
    @if (eligiendoEtiqueta()) {
      <div class="cajon-fondo" (click)="eligiendoEtiqueta.set(false)"></div>
      <aside class="cajon" role="dialog" aria-modal="true" aria-label="Sortear por etiqueta">
        <header class="cajon-cabecera">
          <h3>Sortear por etiqueta</h3>
          <button
            type="button"
            class="secundario"
            aria-label="Cerrar"
            (click)="eligiendoEtiqueta.set(false)"
          >
            <span aria-hidden="true">×</span>
          </button>
        </header>

        <div class="cajon-cuerpo">
          <p class="ayuda">
            Cada alumno recibe preguntas distintas de la etiqueta que elijas. Elegí una:
          </p>
          @for (e of etiquetasConCuantas(); track e.etiqueta) {
            <button
              type="button"
              class="fila etiqueta-sorteable"
              [disabled]="e.cuantas === 0"
              (click)="sortearDe(e.etiqueta)"
            >
              <span class="concepto">{{ e.etiqueta }}</span>
              <span class="version">
                {{ e.cuantas }}
                {{ e.cuantas === 1 ? 'pregunta lista' : 'preguntas listas' }}
              </span>
            </button>
          }
          @if (etiquetasConCuantas().length === 0) {
            <p class="vacio">
              No hay etiquetas en tu banco todavía. Etiquetá algunas preguntas y el sorteo se
              vuelve útil.
            </p>
          }
        </div>
      </aside>
    }
  `,
})
export class ProfesorArmarPage {
  private readonly api = inject(ApiService);
  private readonly ruta = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly confirmaciones = inject(ConfirmacionService);

  readonly chips = CHIPS;
  readonly banco = signal<ItemResumen[]>([]);
  readonly elegidos = signal<Elegido[]>([]);
  readonly error = signal<ErrorApi | null>(null);
  readonly ficha = signal<ContenidoRef | null>(null);
  readonly publicando = signal(false);

  /** Si el formulario de alta esta abierto, adentro del cajon. */
  readonly creando = signal(false);

  /** El cajon del banco, y el de elegir etiqueta para sortear. */
  readonly cajon = signal(false);
  readonly eligiendoEtiqueta = signal(false);

  /** Lo marcado en el cajon, que recien entra al cuestionario al confirmar. */
  readonly marcados = signal<string[]>([]);
  readonly busqueda = signal('');

  /**
   * La regla de sorteo, o null si el cuestionario es todo fijo. Nunca se
   * resuelve acá: lo que se manda al backend es la regla, y quien elige las
   * preguntas es la lectura de cada alumno.
   */
  readonly regla = signal<{ etiqueta: string; cuantos: number; puntaje: number } | null>(null);

  /**
   * Signal y no un campo comun: `listo` es un computed y solo recalcula cuando
   * cambia una signal. Con un campo suelto, escribir el titulo no habilitaba el
   * boton hasta que algo mas tocara la lista.
   */
  readonly titulo = signal('');

  readonly reintentosIlimitados = signal(false);

  /**
   * LIBRE por defecto, que es como se comportaba antes de que esta opcion
   * existiera: lo restrictivo se elige, no se hereda por descuido.
   */
  readonly navegacion = signal<Navegacion>('LIBRE');
  readonly navegaciones = NAVEGACIONES;

  readonly yaArmados = signal<MiContenido[]>([]);
  readonly verTodosLosArmados = signal(false);

  readonly cursoId = signal('');
  readonly unidadId = signal('');
  readonly unidadTitulo = signal('');

  /** La etiqueta por la que se filtra el cajon. Vacio = todas. */
  readonly porEtiqueta = signal('');

  /** Los cuestionarios viejos llenaban media pantalla: se muestran cinco. */
  readonly aReutilizar = computed(() =>
    this.verTodosLosArmados() ? this.yaArmados() : this.yaArmados().slice(0, 5),
  );

  /** Solo las etiquetas que aparecen en el banco de esta pantalla. */
  readonly etiquetasDelBanco = computed(() => {
    const todas = new Set<string>();
    this.banco().forEach((i) => i.etiquetas?.forEach((e) => todas.add(e)));
    return [...todas].sort();
  });

  /** Cada etiqueta con cuántas preguntas sorteables tiene hoy. */
  readonly etiquetasConCuantas = computed(() =>
    this.etiquetasDelBanco().map((etiqueta) => ({
      etiqueta,
      cuantas: this.sorteablesDe(etiqueta).length,
    })),
  );

  /**
   * La bolsa del sorteo tal como la ve el front. Es una COPIA de la regla que
   * aplica el backend —ítems LISTOS, de este profesor, con esa etiqueta, menos
   * los que ya entran fijos—, y existe solo para poder avisar antes de publicar
   * en vez de mostrar un error del servidor.
   */
  readonly candidatas = computed(() => {
    const r = this.regla();
    return r ? this.sorteablesDe(r.etiqueta) : [];
  });

  readonly disponibles = computed(() => {
    const usados = new Set(this.elegidos().map((e) => e.item.id));
    const etiqueta = this.porEtiqueta();
    const texto = this.busqueda().trim().toLowerCase();
    return this.banco().filter(
      (i) =>
        !usados.has(i.id) &&
        (etiqueta === '' || (i.etiquetas ?? []).includes(etiqueta)) &&
        (texto === '' || i.enunciado.toLowerCase().includes(texto)),
    );
  });

  /** Lo que suman las fijas mas lo que el sorteo va a aportar. */
  readonly suma = computed(() => {
    const fijas = this.elegidos().reduce((t, e) => t + (e.puntaje || 0), 0);
    const r = this.regla();
    return fijas + (r ? r.cuantos * r.puntaje : 0);
  });

  readonly hayAlgo = computed(() => this.elegidos().length > 0 || !!this.regla());

  /** Ningun item puede quedar en 0: el backend lo rechaza (PUNTAJE_NO_POSITIVO). */
  readonly hayEnCero = computed(
    () => this.elegidos().some((e) => e.puntaje <= 0) || (this.regla()?.puntaje ?? 1) <= 0,
  );

  /** Se pidieron mas preguntas de las que hay con esa etiqueta. */
  readonly sorteoImposible = computed(() => {
    const r = this.regla();
    return !!r && this.candidatas().length < r.cuantos;
  });

  readonly listo = computed(
    () =>
      this.hayAlgo() &&
      this.suma() === 100 &&
      !this.hayEnCero() &&
      !this.sorteoImposible() &&
      this.titulo().trim().length > 0,
  );

  constructor() {
    const q = this.ruta.snapshot.queryParamMap;
    const curso = q.get('curso');
    const unidad = q.get('unidad');

    if (!curso || !unidad) {
      // Se llego aca a mano, sin pasar por el roadmap. No inventamos un curso.
      this.router.navigateByUrl('/profesor/cursos');
      return;
    }
    this.cursoId.set(curso);
    this.unidadId.set(unidad);
    this.unidadTitulo.set(q.get('unidadTitulo') ?? '');

    this.api.items().subscribe((i) => this.banco.set(i));
    this.api.misContenidos().subscribe({
      next: (c) => this.yaArmados.set(c),
      // Si no se puede traer la lista, se arma uno nuevo y ya: no es bloqueante.
      error: () => {},
    });
  }

  // ---------------------------------------------------------------- cajon ---

  abrirCajon(): void {
    this.marcados.set([]);
    this.busqueda.set('');
    this.cajon.set(true);
  }

  cerrarCajon(): void {
    this.cajon.set(false);
    this.creando.set(false);
  }

  marcar(id: string): void {
    this.marcados.update((ids) => (ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]));
  }

  /**
   * Se agregan TODAS juntas y recien despues se reparten los pesos: hacerlo de
   * a una dejaria el numero del totalizador saltando en cada casilla marcada.
   */
  agregarMarcados(): void {
    const ids = new Set(this.marcados());
    const nuevos = this.banco().filter((i) => ids.has(i.id));
    this.elegidos.update((lista) => [
      ...lista,
      ...nuevos.map((item) => ({ item, puntaje: 0, manual: false })),
    ]);
    this.reajustarLibres();
    this.cerrarCajon();
  }

  // --------------------------------------------------------------- sorteo ---

  abrirSorteo(): void {
    this.eligiendoEtiqueta.set(true);
  }

  /**
   * Arranca en un numero que CIERRE en 100 si el cuestionario es solo sorteo.
   *
   * Tres preguntas parece el arranque natural, pero 3 x 33 da 99: el peso de
   * las sorteadas es uniforme —no puede depender de cual le toco a quien— y sin
   * una pregunta fija no hay donde poner el resto. Publicar quedaba
   * deshabilitado sin explicar por que. Asi que se arranca en 4 —o en el
   * divisor de 100 mas cercano que entre en la bolsa—, y si despues se cambia a
   * un numero que no cierra, el totalizador lo dice y ofrece el arreglo.
   */
  sortearDe(etiqueta: string): void {
    const disponibles = this.sorteablesDe(etiqueta).length;
    const cuantos = this.elegidos().length > 0
      ? Math.min(3, disponibles)
      : divisorMasCercano(Math.min(4, disponibles), disponibles);
    this.regla.set({ etiqueta, cuantos, puntaje: 1 });
    this.eligiendoEtiqueta.set(false);
    this.repartirParejo();
  }

  /**
   * Cuantas preguntas habria que sortear para que el reparto cierre. Null si ya
   * cierra, si hay preguntas fijas —ahi el resto tiene donde ir— o si no hay
   * ningun divisor que entre en la bolsa.
   */
  readonly cuantosQueCierra = computed(() => {
    const r = this.regla();
    if (!r || this.elegidos().length > 0 || this.suma() === 100) return null;
    const sugerido = divisorMasCercano(r.cuantos, this.candidatas().length);
    return sugerido === r.cuantos ? null : sugerido;
  });

  /** Acepta la sugerencia: cambia cuantas se sortean y reparte de nuevo. */
  usarCuantosQueCierra(): void {
    const sugerido = this.cuantosQueCierra();
    const r = this.regla();
    if (!sugerido || !r) return;
    this.regla.set({ ...r, cuantos: sugerido });
    this.repartirParejo();
  }

  quitarSorteo(): void {
    this.regla.set(null);
    this.reajustarLibres();
  }

  cambiarCuantos(valor: number): void {
    const r = this.regla();
    if (!r) return;
    this.regla.set({ ...r, cuantos: Math.max(1, Number(valor) || 1) });
  }

  cambiarPuntajeDelSorteo(valor: number): void {
    const r = this.regla();
    if (!r) return;
    this.regla.set({ ...r, puntaje: Math.max(0, Number(valor) || 0) });
  }

  /** Las que podrian salir sorteadas con esa etiqueta, sin contar las fijas. */
  private sorteablesDe(etiqueta: string): ItemResumen[] {
    const fijas = new Set(this.elegidos().map((e) => e.item.id));
    return this.banco().filter(
      (i) => i.estado === 'LISTO' && (i.etiquetas ?? []).includes(etiqueta) && !fijas.has(i.id),
    );
  }

  // ------------------------------------------------------------------ pesos -

  mensajeDeSuma(): string {
    if (this.sorteoImposible()) {
      return 'no hay tantas preguntas con esa etiqueta';
    }
    const sugerido = this.cuantosQueCierra();
    if (sugerido) {
      const r = this.regla()!;
      return (
        `${r.cuantos} preguntas de igual peso no reparten 100 exacto. ` +
        'Todas las sorteadas valen lo mismo, así que no hay dónde poner el resto: ' +
        `sorteá ${sugerido}, o agregá una pregunta fija que se lo lleve.`
      );
    }
    const diferencia = 100 - this.suma();
    if (diferencia === 0 && this.hayEnCero()) {
      return 'suman 100, pero hay una pregunta en 0: sacale peso a otra o quitala';
    }
    if (diferencia === 0) return 'los pesos suman 100';
    if (diferencia > 0) return `faltan ${diferencia}%`;
    return `te pasaste ${-diferencia}%`;
  }

  /**
   * Reparto equitativo con el resto al ultimo: 3 preguntas dan 34/33/33 y nunca
   * decimales. Es el boton explicito, asi que pisa TODO —incluidos los pesos
   * manuales— y los vuelve a dejar libres: el profesor pidio empezar de cero.
   *
   * Las sorteadas cuentan una por una, pero todas valen lo mismo: el peso no
   * puede depender de cual le toco a quien. Por eso el resto va siempre a una
   * FIJA, y si no hay ninguna queda faltando — el totalizador lo dice, y se
   * arregla cambiando cuantas se sortean.
   */
  repartirParejo(): void {
    const r = this.regla();
    const fijas = this.elegidos().length;
    const cuantas = fijas + (r ? r.cuantos : 0);
    if (cuantas === 0) return;

    const base = Math.floor(100 / cuantas);
    const resto = 100 - base * cuantas;

    if (r) {
      this.regla.set({ ...r, puntaje: base });
    }
    this.elegidos.update((lista) =>
      lista.map((e, i) => ({
        ...e,
        puntaje: i === fijas - 1 ? base + resto : base,
        manual: false,
      })),
    );
  }

  /**
   * Reparte lo que sobra despues de los pesos manuales —y de lo que se lleva el
   * sorteo— entre los items libres. Es lo que corre solo al agregar o quitar:
   * si la profesora ya le puso 40 a la primera pregunta, agregar una segunda no
   * puede borrarle ese 40.
   */
  private reajustarLibres(): void {
    const lista = this.elegidos();
    const libres = lista.filter((e) => !e.manual).length;
    if (libres === 0) return;

    const r = this.regla();
    const fijado =
      lista.filter((e) => e.manual).reduce((t, e) => t + e.puntaje, 0) +
      (r ? r.cuantos * r.puntaje : 0);
    const aRepartir = Math.max(0, 100 - fijado);
    const base = Math.floor(aRepartir / libres);
    const resto = aRepartir - base * libres;

    let visto = 0;
    this.elegidos.update((l) =>
      l.map((e) => {
        if (e.manual) return e;
        visto++;
        return { ...e, puntaje: visto === libres ? base + resto : base };
      }),
    );
  }

  /**
   * Fijar un peso NO reajusta los demas: el profesor que toca un peso quiere
   * ese peso, y el contador de abajo le dice cuanto le falta repartir. Queda
   * marcado como manual para que agregar otro item tampoco se lo pise.
   */
  fijar(i: number, valor: number): void {
    this.elegidos.update((lista) =>
      lista.map((e, indice) =>
        indice === i ? { ...e, puntaje: Number(valor) || 0, manual: true } : e,
      ),
    );
  }

  quitar(i: number): void {
    this.elegidos.update((lista) => lista.filter((_, indice) => indice !== i));
    this.reajustarLibres();
  }

  // ------------------------------------------------------------------ resto -

  etiqueta(tipo: TipoDeItem): string {
    return TIPOS.find((t) => t.valor === tipo)?.etiqueta ?? tipo;
  }

  /**
   * La pregunta recien escrita sin salir de la pantalla. Entra al banco igual
   * que cualquier otra —es el mismo POST— y ademas se agrega al cuestionario
   * con el peso repartido, que es lo unico que esta pantalla hace de mas.
   *
   * El borrador es la excepcion: queda en el banco pero NO se agrega, porque el
   * backend rechaza componer con un BORRADOR (CI-59). Agregarlo daria un error
   * recien al publicar, con los pesos ya repartidos.
   */
  alCrear(item: ItemDetalle): void {
    this.banco.update((b) => [item, ...b]);
    if (item.estado === 'BORRADOR') {
      this.creando.set(false);
      this.error.set({
        clave: 'ITEM_EN_BORRADOR',
        campo: null,
        mensaje:
          'Guardada en el banco como borrador. Un borrador no entra a ningún cuestionario: ' +
          'publicala desde el Banco y después agregala.',
      });
      return;
    }
    this.error.set(null);
    this.elegidos.update((lista) => [...lista, { item, puntaje: 0, manual: false }]);
    this.reajustarLibres();
    this.cerrarCajon();
  }

  /**
   * El cartel sale recien ACA, cuando el desafio ya existe en el Tema 03 y el
   * alumno lo puede ver. Sacarlo un paso antes —al componer el contenido—
   * mentiria: si el 03 rechaza el desafio (CI-47) o esta caido, la profesora ya
   * habria visto un tilde verde sobre algo que nadie puede abrir.
   */
  private confirmarPublicado(): void {
    this.confirmaciones.mostrar({
      mensaje: 'Cuestionario publicado',
      detalle: 'El desafío ya está en el Tema 03 y el alumno lo puede abrir.',
    });
  }

  /**
   * Cierra el circulo del recorrido: el desafio recien creado se ve DENTRO de
   * la unidad, que es lo que prueba que el Tema 03 lo colgo del nodo correcto
   * del roadmap. Sin este boton hay que acordarse de navegar por la barra.
   */
  volverAlRoadmap(): void {
    this.router.navigateByUrl('/profesor/cursos/' + this.cursoId());
  }

  /**
   * 422 es el rechazo de CI-47 y no una caida: el contenido quedo bien guardado,
   * lo que el 03 no acepta es este desafio.
   */
  private errorDelTema03(e: any): ErrorApi {
    const rechazo = e.status === 422;
    return {
      clave: rechazo ? 'CI_47' : 'TEMA_03',
      campo: rechazo ? 'reintentosIlimitados' : null,
      mensaje: rechazo
        ? 'El Tema 03 rechazó el desafío: tiene reintentos ilimitados y este cuestionario ' +
          'lleva corrección diferida (CI-47). El cuestionario quedó guardado — destildá ' +
          'reintentos ilimitados y volvé a publicar.'
        : 'El contenido quedó guardado, pero el Tema 03 no respondió. ' +
          'Es exactamente el modo de fallar que CI-03 elige: se puede armar contenido aunque el 03 esté caído.',
    };
  }

  /**
   * Cuelga un cuestionario YA COMPUESTO de esta unidad. No se vuelve a componer
   * nada: se le pasa al Tema 03 la misma ficha, con el mismo contenidoId. Eso es
   * lo que hace que sea una referencia y no una copia.
   */
  colgarExistente(c: MiContenido): void {
    this.error.set(null);
    this.publicando.set(true);

    this.api
      .crearDesafio({
        titulo: c.titulo,
        cursoCohorteId: this.cursoId(),
        unidadId: this.unidadId(),
        contenidoRef: c.ficha,
        reintentosIlimitados: this.reintentosIlimitados(),
      })
      .subscribe({
        next: () => {
          this.publicando.set(false);
          this.ficha.set(c.ficha);
          this.confirmarPublicado();
        },
        error: (e) => {
          this.publicando.set(false);
          this.error.set(this.errorDelTema03(e));
        },
      });
  }

  publicar(): void {
    this.error.set(null);
    this.publicando.set(true);

    const cuerpo = {
      cursoCohorteId: this.cursoId(),
      titulo: this.titulo(),
      escala: 'PORCENTUAL',
      navegacion: this.navegacion(),
      items: this.elegidos().map((e, i) => ({
        itemId: e.item.id,
        orden: i + 1,
        puntaje: e.puntaje,
      })),
      regla: this.regla(),
    };

    // Dos pasos, y en este orden (CI-03): primero el contenido acá, que
    // devuelve la ficha; después el desafío en el Tema 03 con la referencia ya
    // formada. Así no existe ningún contrato de composición entre los dos.
    this.api.componer(cuerpo).subscribe({
      next: (ficha) => {
        this.api
          .crearDesafio({
            titulo: this.titulo(),
            cursoCohorteId: this.cursoId(),
            unidadId: this.unidadId(),
            contenidoRef: ficha,
            reintentosIlimitados: this.reintentosIlimitados(),
          })
          .subscribe({
            next: () => {
              this.publicando.set(false);
              this.ficha.set(ficha);
              this.elegidos.set([]);
              this.regla.set(null);
              this.titulo.set('');
              this.confirmarPublicado();
            },
            error: (e) => {
              this.publicando.set(false);
              this.error.set(this.errorDelTema03(e));
            },
          });
      },
      error: (e) => {
        this.publicando.set(false);
        this.error.set(
          e.error?.mensaje
            ? e.error
            : { clave: 'ERROR', campo: null, mensaje: 'No se pudo publicar.' },
        );
      },
    });
  }
}
