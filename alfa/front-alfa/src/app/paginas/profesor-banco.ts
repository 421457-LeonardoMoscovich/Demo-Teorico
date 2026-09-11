import { Component, inject, signal } from '@angular/core';
import { JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import {
  ErrorApi,
  ItemDetalle,
  ItemResumen,
  TIPOS,
  TipoDeItem,
  VistaPreviaItem,
} from '../core/modelos';

interface FilaSimple {
  id: string;
  texto: string;
  correcta: boolean;
  /** Cuanto paga marcarla, en % del peso del item. Solo se usa en modo parcial. */
  porcentaje: number;
  /** Que se le explica a quien la marque (CI-58). Vacio = no se le explica nada. */
  devolucion: string;
}

interface FilaPar {
  id: string;
  texto: string;
  /** id del elemento de la derecha con el que empareja */
  parId: string;
}

@Component({
  selector: 'app-profesor-banco',
  standalone: true,
  imports: [FormsModule, JsonPipe],
  template: `
    <div class="columnas">
      <section class="tarjeta">
        <h2>Banco de ítems</h2>
        <p class="ayuda">
          Cada ítem tiene identidad propia y contenido versionado: editar publica una versión nueva
          y nunca toca lo ya respondido.
        </p>

        <label class="filtro">
          Filtrar por tipo
          <select [(ngModel)]="filtro" name="filtro" (ngModelChange)="cargar()">
            <option value="">Todos</option>
            @for (t of tipos; track t.valor) {
              <option [value]="t.valor">{{ t.etiqueta }}</option>
            }
          </select>
        </label>

        @if (items().length === 0) {
          <p class="vacio">
            Todavía no hay ítems. Cargá el primero con el formulario de la derecha.
          </p>
        }

        <ul class="lista">
          @for (i of items(); track i.id; let idx = $index) {
            <li [style.--orden]="idx">
              <div>
                <span class="etiqueta">{{ etiqueta(i.tipo) }}</span>
                @if (i.estado === 'BORRADOR') {
                  <span class="etiqueta diferida">borrador</span>
                }
                <span class="version">v{{ i.version }}</span>
                <p>{{ i.enunciado }}</p>

                <!--
                  La vista previa (CI-59). Lo que se pinta acá sale del MISMO
                  endpoint que se le sirve al alumno en el examen, así que si
                  alguna vez filtrara la clave de corrección, se vería acá.
                -->
                @if (previaDe() === i.id && previa(); as p) {
                  <div class="previa">
                    <p class="ayuda">
                      <span class="fuente">Así lo ve el alumno</span>
                      servido por el mismo endpoint que el examen
                    </p>
                    <p class="titulo-desafio">{{ p.enunciado }}</p>
                    @for (o of opcionesDe(p); track o.id) {
                      <label class="check">
                        <input type="checkbox" disabled />
                        {{ o.texto }}
                      </label>
                    }
                    <details>
                      <summary>Lo que viaja, tal cual</summary>
                      <pre>{{ p | json }}</pre>
                    </details>
                  </div>
                }
              </div>
              <div class="acciones-fila">
                @if (porDarDeBaja() === i.id) {
                  <span class="ayuda">¿Seguro?</span>
                  <button class="peligro" type="button" (click)="confirmarBaja(i)">
                    Sí, dar de baja
                  </button>
                  <button type="button" class="secundario" (click)="porDarDeBaja.set(null)">
                    Cancelar
                  </button>
                } @else {
                  <button type="button" class="secundario" (click)="alternarPrevia(i)">
                    {{ previaDe() === i.id ? 'Cerrar previa' : 'Vista previa' }}
                  </button>
                  <button type="button" class="secundario" (click)="editar(i)">Editar</button>
                  <button class="peligro" type="button" (click)="porDarDeBaja.set(i.id)">
                    Dar de baja
                  </button>
                }
              </div>
            </li>
          }
        </ul>
      </section>

      <section class="tarjeta">
        @if (editando(); as e) {
          <h2>Editar ítem</h2>
          <p class="ayuda">
            Guardar <strong>no modifica</strong> la versión {{ e.version }}: publica la
            {{ e.version + 1 }} y deja la anterior intacta. Quien ya respondió sobre la
            {{ e.version }} se sigue corrigiendo con esa.
          </p>
          <button type="button" class="secundario" (click)="cancelarEdicion()">
            Cancelar y crear uno nuevo
          </button>
        } @else {
          <h2>Nuevo ítem</h2>
        }

        <form (ngSubmit)="guardar()" #f="ngForm">
          <label>
            Tipo
            <select
              name="tipo"
              [(ngModel)]="tipo"
              (ngModelChange)="reiniciarPorTipo()"
              [disabled]="editando() !== null"
            >
              @for (t of tipos; track t.valor) {
                <option [value]="t.valor">{{ t.etiqueta }}</option>
              }
            </select>
          </label>

          <label>
            Enunciado
            <textarea
              name="enunciado"
              rows="2"
              [(ngModel)]="enunciado"
              placeholder="La consigna que lee el alumno"
            ></textarea>
          </label>

          @if (tipo === 'OPCION_MULTIPLE') {
            <label class="check">
              <input type="checkbox" name="multiple" [(ngModel)]="multiple" />
              Admite más de una respuesta correcta
            </label>

            <label class="check">
              <input
                type="checkbox"
                name="parcial"
                [ngModel]="parcial"
                (ngModelChange)="alternarParcial($event)"
              />
              Puntaje parcial por opción
            </label>

            @if (parcial) {
              <p class="ayuda">
                Cada opción paga un porcentaje del peso de la pregunta. Las negativas descuentan:
                sin ellas, marcar todas las opciones garantiza el 100% y la pregunta no evalúa nada.
              </p>
              <div class="totalizador" [class.mal]="positivos() !== 100">
                <strong>{{ positivos() }}%</strong>
                <span>
                  suman las calificaciones positivas.
                  {{ positivos() === 100 ? 'Listo.' : 'Tienen que sumar exactamente 100.' }}
                </span>
              </div>
            }

            @for (o of opciones; track o.id; let i = $index) {
              <div class="fila">
                <input [name]="'op' + i" [(ngModel)]="o.texto" placeholder="Opción {{ i + 1 }}" />
                @if (parcial) {
                  <label class="sr-solo" [attr.for]="'pc' + i">
                    Porcentaje de la opción {{ i + 1 }}
                  </label>
                  <input
                    class="peso"
                    type="number"
                    min="-100"
                    max="100"
                    [id]="'pc' + i"
                    [name]="'pc' + i"
                    [(ngModel)]="o.porcentaje"
                  />
                  <span class="ayuda">%</span>
                }
                <label class="check">
                  <input
                    [type]="multiple ? 'checkbox' : 'radio'"
                    [name]="multiple ? 'ok' + i : 'ok'"
                    [checked]="o.correcta"
                    (change)="marcar(o)"
                  />
                  correcta
                </label>
                <button
                  type="button"
                  class="secundario"
                  (click)="quitar(opciones, i)"
                  attr.aria-label="Quitar opción {{ i + 1 }}"
                >
                  <span aria-hidden="true">×</span>
                </button>
              </div>
              @if (conDevolucion) {
                <label class="sr-solo" [attr.for]="'dv' + i">
                  Qué se le explica a quien marque la opción {{ i + 1 }}
                </label>
                <input
                  class="devolucion-opcion"
                  [id]="'dv' + i"
                  [name]="'dv' + i"
                  [(ngModel)]="o.devolucion"
                  placeholder="Qué se le explica a quien marque esta opción"
                />
              }
            }
            <button type="button" class="secundario" (click)="agregar(opciones, 'o')">
              + agregar opción
            </button>
          }

          @if (tipo === 'VERDADERO_FALSO') {
            <label>
              Afirmación
              <input
                name="afirmacion"
                [(ngModel)]="afirmacion"
                placeholder="La afirmación a juzgar"
              />
            </label>
            <label>
              Respuesta correcta
              <select name="esVerdadero" [(ngModel)]="esVerdadero">
                <option [ngValue]="true">Verdadero</option>
                <option [ngValue]="false">Falso</option>
              </select>
            </label>
          }

          @if (tipo === 'EMPAREJAR') {
            <p class="ayuda">Cargá primero la columna derecha; después emparejá cada concepto.</p>
            <h3>Derecha</h3>
            @for (d of derecha; track d.id; let i = $index) {
              <div class="fila">
                <input
                  [name]="'der' + i"
                  [(ngModel)]="d.texto"
                  placeholder="Definición {{ i + 1 }}"
                />
                <button
                  type="button"
                  class="secundario"
                  (click)="quitar(derecha, i)"
                  attr.aria-label="Quitar definición {{ i + 1 }}"
                >
                  <span aria-hidden="true">×</span>
                </button>
              </div>
            }
            <button type="button" class="secundario" (click)="agregar(derecha, 'd')">
              + agregar definición
            </button>

            <h3>Izquierda</h3>
            @for (z of izquierda; track z.id; let i = $index) {
              <div class="fila">
                <input
                  [name]="'izq' + i"
                  [(ngModel)]="z.texto"
                  placeholder="Concepto {{ i + 1 }}"
                />
                <select [name]="'par' + i" [(ngModel)]="z.parId">
                  <option value="">empareja con…</option>
                  @for (d of derecha; track d.id) {
                    <option [value]="d.id">{{ d.texto || d.id }}</option>
                  }
                </select>
                <button
                  type="button"
                  class="secundario"
                  (click)="quitar(izquierda, i)"
                  attr.aria-label="Quitar concepto {{ i + 1 }}"
                >
                  <span aria-hidden="true">×</span>
                </button>
              </div>
            }
            <button type="button" class="secundario" (click)="agregar(izquierda, 'i')">
              + agregar concepto
            </button>
          }

          @if (tipo === 'ABIERTA') {
            <p class="ayuda">
              La única que no se corrige sola. Cuando un alumno la entregue, el cuestionario queda
              esperando en tu cola hasta que le pongas puntaje.
            </p>
            <label>
              Consigna
              <textarea
                name="consigna"
                rows="2"
                [(ngModel)]="consigna"
                placeholder="Qué tiene que desarrollar, y con qué alcance"
              ></textarea>
            </label>
            <label>
              Extensión máxima en palabras <span class="ayuda">— opcional</span>
              <input name="extension" type="number" min="1" [(ngModel)]="extensionMaxima" />
            </label>
            <label>
              Rúbrica
              <textarea
                name="rubrica"
                rows="4"
                [(ngModel)]="rubrica"
                placeholder="Qué tiene que decir para el puntaje completo, y qué para el parcial"
              ></textarea>
            </label>
            <p class="ayuda">
              La rúbrica es la clave de corrección de este ítem:
              <strong>el alumno no la ve nunca</strong>. Es obligatoria, y no por burocracia — sin
              ella no hay forma de puntuar parejo a veinte alumnos según en qué orden los leíste.
            </p>
          }

          @if (tipo === 'ORDENAR') {
            <p class="ayuda">Cargalos en el orden correcto. Al alumno se le muestran mezclados.</p>
            @for (e of elementos; track e.id; let i = $index) {
              <div class="fila">
                <span class="posicion">{{ i + 1 }}</span>
                <input [name]="'el' + i" [(ngModel)]="e.texto" placeholder="Paso {{ i + 1 }}" />
                <button
                  type="button"
                  class="secundario"
                  (click)="quitar(elementos, i)"
                  attr.aria-label="Quitar paso {{ i + 1 }}"
                >
                  <span aria-hidden="true">×</span>
                </button>
              </div>
            }
            <button type="button" class="secundario" (click)="agregar(elementos, 'e')">
              + agregar paso
            </button>
          }

          @if (error()) {
            <p class="error" role="alert">
              {{ error()!.mensaje }}
              @if (error()!.campo) {
                <span class="campo">({{ error()!.campo }})</span>
              }
            </p>
          }
          @if (ok()) {
            <p class="ok" role="status">{{ ok() }}</p>
          }

          <label class="check">
            <input type="checkbox" name="borrador" [(ngModel)]="borrador" />
            Guardar como borrador
          </label>
          @if (borrador) {
            <p class="ayuda">
              Queda en el banco pero no se puede componer en ningún cuestionario. Es para la
              pregunta a medio escribir: hoy una así entra a un examen igual que una terminada.
            </p>
          }

          <label class="check">
            <input
              type="checkbox"
              name="conDevolucion"
              [ngModel]="conDevolucion"
              (ngModelChange)="alternarDevolucion($event)"
            />
            Escribir retroalimentación
          </label>

          @if (conDevolucion) {
            <p class="ayuda">
              El alumno la lee recién cuando su respuesta ya está corregida, y de los textos por
              opción solo ve los de las que marcó: los del resto le dirían cuál era la correcta.
            </p>
            <label>
              Para cualquiera que haya contestado
              <textarea
                name="devolucionGeneral"
                rows="2"
                [(ngModel)]="devolucionGeneral"
                placeholder="Por qué la respuesta es la que es"
              ></textarea>
            </label>
          }

          <button type="submit" [disabled]="guardando()">
            {{
              guardando() ? 'Guardando…' : editando() ? 'Publicar versión nueva' : 'Guardar ítem'
            }}
          </button>
        </form>
      </section>
    </div>
  `,
})
export class ProfesorBancoPage {
  private readonly api = inject(ApiService);

  readonly tipos = TIPOS;
  readonly items = signal<ItemResumen[]>([]);
  readonly error = signal<ErrorApi | null>(null);
  readonly ok = signal('');
  readonly guardando = signal(false);

  filtro: TipoDeItem | '' = '';
  tipo: TipoDeItem = 'OPCION_MULTIPLE';
  enunciado = '';

  multiple = false;
  opciones: FilaSimple[] = [];
  /** Modo ponderado. Apagado = el todo-o-nada de siempre, sin `pesos` en el criterio. */
  parcial = false;
  /** Retroalimentacion (CI-58). Apagada = el item no devuelve nada al corregir. */
  conDevolucion = false;
  devolucionGeneral = '';
  /** CI-59. El que no dice nada publica listo, que es lo que todos hacian hasta ahora. */
  borrador = false;

  /** La vista previa abierta, si hay alguna. */
  readonly previa = signal<VistaPreviaItem | null>(null);
  readonly previaDe = signal<string | null>(null);

  afirmacion = '';
  esVerdadero = true;

  consigna = '';
  extensionMaxima: number | null = null;
  rubrica = '';

  izquierda: FilaPar[] = [];
  derecha: FilaSimple[] = [];

  elementos: FilaSimple[] = [];

  private contador = 0;

  /**
   * El item que se esta editando, o null si se esta creando uno nuevo.
   *
   * Editar NO muta: publica la version siguiente y no toca ninguna anterior
   * (D-04). Por eso la pantalla lo dice con todas las letras — es la regla mas
   * facil de malinterpretar de todo el modulo.
   */
  readonly editando = signal<ItemDetalle | null>(null);

  /** El item cuya baja se esta confirmando, o null. */
  readonly porDarDeBaja = signal<string | null>(null);

  constructor() {
    this.reiniciarPorTipo();
    this.cargar();
  }

  etiqueta(tipo: TipoDeItem): string {
    return TIPOS.find((t) => t.valor === tipo)?.etiqueta ?? tipo;
  }

  cargar(): void {
    this.porDarDeBaja.set(null);
    this.api.items(this.filtro).subscribe((i) => this.items.set(i));
  }

  reiniciarPorTipo(): void {
    this.error.set(null);
    this.ok.set('');
    this.contador = 0;
    this.opciones = [this.nueva('o'), this.nueva('o')];
    this.derecha = [this.nueva('d'), this.nueva('d')];
    this.izquierda = [this.nuevoPar(), this.nuevoPar()];
    this.elementos = [this.nueva('e'), this.nueva('e')];
    this.parcial = false;
    this.conDevolucion = false;
    this.devolucionGeneral = '';
    this.borrador = false;
  }

  private nueva(prefijo: string): FilaSimple {
    return {
      id: prefijo + ++this.contador,
      texto: '',
      correcta: false,
      porcentaje: 0,
      devolucion: '',
    };
  }

  /** Al apagarla se borra todo: un texto guardado que no se publica es peor que nada. */
  alternarDevolucion(prendida: boolean): void {
    this.conDevolucion = prendida;
    if (!prendida) {
      this.devolucionGeneral = '';
      this.opciones.forEach((o) => (o.devolucion = ''));
    }
  }

  /**
   * `null` cuando no hay nada escrito, y esa ausencia es la que hace que el
   * item se comporte como los que ya estan en la base.
   */
  private devolucionArmada(): any {
    if (!this.conDevolucion) return null;
    const porOpcion = this.opciones
      .filter((o) => o.devolucion.trim() !== '')
      .map((o) => ({ id: o.id, texto: o.devolucion.trim() }));
    const general = this.devolucionGeneral.trim();
    if (general === '' && porOpcion.length === 0) return null;
    return { general: general || null, porOpcion };
  }

  /**
   * Abre o cierra la vista previa. Se pide al backend cada vez y no se cachea:
   * es barato, y si el profesor acaba de publicar una version nueva tiene que
   * ver ESA, no la que estaba en memoria.
   */
  alternarPrevia(i: ItemResumen): void {
    if (this.previaDe() === i.id) {
      this.previaDe.set(null);
      this.previa.set(null);
      return;
    }
    this.previaDe.set(i.id);
    this.previa.set(null);
    this.api.vistaPrevia(i.id).subscribe({
      next: (p) => this.previa.set(p),
      error: () => {
        this.previaDe.set(null);
        this.error.set({
          clave: 'ERROR',
          campo: null,
          mensaje: 'No se pudo traer la vista previa del ítem.',
        });
      },
    });
  }

  /** Lo unico que la previa sabe pintar de a uno: las opciones, si el tipo tiene. */
  opcionesDe(p: VistaPreviaItem): { id: string; texto: string }[] {
    const carga = p.payload ?? {};
    return carga.opciones ?? carga.elementos ?? carga.izquierda ?? [];
  }

  /** Lo que tiene que dar 100 para que el item sea valido (CI-55). */
  positivos(): number {
    return this.opciones.reduce((t, o) => t + (o.porcentaje > 0 ? o.porcentaje : 0), 0);
  }

  /**
   * Al prender el modo, reparte 100 entre las que ya estan marcadas correctas.
   * Es el unico reparto que no hace falta explicar, y deja el formulario valido
   * de entrada en vez de rojo. El resto —cuanto descuenta cada distractora— es
   * decision del profesor y no se adivina.
   *
   * Al apagarlo los porcentajes se borran: si quedaran, el criterio guardado
   * seguiria sin `pesos` pero el formulario mostraria numeros que no se usan.
   */
  alternarParcial(prendido: boolean): void {
    this.parcial = prendido;
    const correctas = this.opciones.filter((o) => o.correcta);
    if (!prendido || correctas.length === 0) {
      this.opciones.forEach((o) => (o.porcentaje = 0));
      return;
    }
    // El resto se lo lleva la primera: con tres correctas, 34/33/33 y no 33/33/33.
    const parte = Math.floor(100 / correctas.length);
    this.opciones.forEach((o) => (o.porcentaje = 0));
    correctas.forEach((o) => (o.porcentaje = parte));
    correctas[0].porcentaje += 100 - parte * correctas.length;
  }

  private nuevoPar(): FilaPar {
    return { id: 'i' + ++this.contador, texto: '', parId: '' };
  }

  agregar(lista: any[], prefijo: string): void {
    lista.push(prefijo === 'i' ? this.nuevoPar() : this.nueva(prefijo));
  }

  quitar(lista: any[], i: number): void {
    lista.splice(i, 1);
  }

  /**
   * Carga un item del banco en el formulario para publicar una version nueva.
   *
   * El `tipo` NO se puede cambiar: vive en `item` y no en `item_version`, asi
   * que es inmutable entre versiones por modelo. El select se deshabilita en
   * vez de validarlo despues.
   */
  editar(item: ItemResumen): void {
    this.error.set(null);
    this.ok.set('');
    this.api.item(item.id).subscribe({
      next: (d) => {
        this.editando.set(d);
        this.tipo = d.tipo;
        this.enunciado = d.enunciado;
        // Alto para que los ids nuevos no choquen con los que ya trae el item.
        this.contador = 1000;
        this.cargarFormulario(d);
      },
      error: () =>
        this.error.set({
          clave: 'ERROR',
          campo: null,
          mensaje: 'No se pudo abrir el ítem para editar.',
        }),
    });
  }

  cancelarEdicion(): void {
    this.editando.set(null);
    this.enunciado = '';
    this.afirmacion = '';
    this.consigna = '';
    this.rubrica = '';
    this.extensionMaxima = null;
    this.reiniciarPorTipo();
  }

  private cargarFormulario(d: ItemDetalle): void {
    const p = d.payload ?? {};
    const c = d.criterio ?? {};

    this.borrador = d.estado === 'BORRADOR';

    // Comun a los cinco tipos: la devolucion no depende del tipo.
    this.devolucionGeneral = d.devolucion?.general ?? '';
    this.conDevolucion =
      this.devolucionGeneral !== '' || (d.devolucion?.porOpcion?.length ?? 0) > 0;

    switch (d.tipo) {
      case 'OPCION_MULTIPLE': {
        const correctas: string[] = c.correctas ?? [];
        const pesos: { id: string; porcentaje: number }[] = c.pesos ?? [];
        this.multiple = !!p.multiple;
        this.parcial = pesos.length > 0;
        const porOpcion: { id: string; texto: string }[] = d.devolucion?.porOpcion ?? [];
        this.opciones = (p.opciones ?? []).map((o: any) => ({
          id: o.id,
          texto: o.texto,
          correcta: correctas.includes(o.id),
          porcentaje: pesos.find((x) => x.id === o.id)?.porcentaje ?? 0,
          devolucion: porOpcion.find((x) => x.id === o.id)?.texto ?? '',
        }));
        break;
      }
      case 'VERDADERO_FALSO': {
        this.afirmacion = p.afirmacion ?? '';
        this.esVerdadero = c.esVerdadero === true;
        break;
      }
      case 'EMPAREJAR': {
        const pares: string[][] = c.pares ?? [];
        this.derecha = (p.derecha ?? []).map((x: any) => ({
          id: x.id,
          texto: x.texto,
          correcta: false,
          porcentaje: 0,
          devolucion: '',
        }));
        this.izquierda = (p.izquierda ?? []).map((z: any) => ({
          id: z.id,
          texto: z.texto,
          parId: pares.find((par) => par[0] === z.id)?.[1] ?? '',
        }));
        break;
      }
      case 'ABIERTA': {
        this.consigna = p.consigna ?? '';
        this.extensionMaxima = p.extensionMaxima ?? null;
        this.rubrica = c.rubrica ?? '';
        break;
      }
      case 'ORDENAR': {
        // El payload viaja MEZCLADO; el orden correcto es el del criterio, y es
        // el que el profesor tiene que ver para poder corregirlo.
        const secuencia: string[] = c.secuencia ?? [];
        const porId = new Map<string, any>((p.elementos ?? []).map((e: any) => [e.id, e]));
        this.elementos = secuencia
          .map((id) => porId.get(id))
          .filter(Boolean)
          .map((e: any) => ({
            id: e.id,
            texto: e.texto,
            correcta: false,
            porcentaje: 0,
            devolucion: '',
          }));
        break;
      }
    }
  }

  marcar(o: FilaSimple): void {
    if (this.multiple) {
      o.correcta = !o.correcta;
      return;
    }
    this.opciones.forEach((x) => (x.correcta = false));
    o.correcta = true;
  }

  guardar(): void {
    this.error.set(null);
    this.ok.set('');
    this.guardando.set(true);

    const cuerpo = {
      tipo: this.tipo,
      enunciado: this.enunciado,
      ...this.payloadYCriterio(),
      devolucion: this.devolucionArmada(),
      estado: this.borrador ? 'BORRADOR' : 'LISTO',
    };

    const enEdicion = this.editando();
    const peticion = enEdicion
      ? this.api.publicarVersion(enEdicion.id, cuerpo as any)
      : this.api.crearItem(cuerpo as any);

    peticion.subscribe({
      next: (guardado) => {
        this.guardando.set(false);
        this.ok.set(
          enEdicion
            ? `Publicada la versión ${guardado.version}. La ${enEdicion.version} queda intacta.`
            : 'Ítem guardado.',
        );
        this.editando.set(null);
        this.enunciado = '';
        this.afirmacion = '';
        this.consigna = '';
        this.rubrica = '';
        this.extensionMaxima = null;
        this.reiniciarPorTipo();
        this.cargar();
      },
      error: (e) => {
        this.guardando.set(false);
        this.error.set(
          e.error?.mensaje
            ? e.error
            : { clave: 'ERROR', campo: null, mensaje: 'No se pudo guardar.' },
        );
      },
    });
  }

  private payloadYCriterio(): { payload: any; criterio: any } {
    switch (this.tipo) {
      case 'OPCION_MULTIPLE':
        return {
          payload: {
            opciones: this.opciones.map((o) => ({ id: o.id, texto: o.texto })),
            multiple: this.multiple,
          },
          criterio: {
            correctas: this.opciones.filter((o) => o.correcta).map((o) => o.id),
            // Ausente cuando el modo esta apagado, y esa ausencia ES el
            // contrato: sin `pesos` el backend corrige todo o nada (CI-55).
            pesos: this.parcial
              ? this.opciones
                  .filter((o) => o.porcentaje !== 0)
                  .map((o) => ({ id: o.id, porcentaje: o.porcentaje }))
              : null,
          },
        };

      case 'VERDADERO_FALSO':
        return {
          payload: { afirmacion: this.afirmacion },
          criterio: { esVerdadero: this.esVerdadero },
        };

      case 'EMPAREJAR':
        return {
          payload: {
            izquierda: this.izquierda.map((z) => ({ id: z.id, texto: z.texto })),
            derecha: this.derecha.map((d) => ({ id: d.id, texto: d.texto })),
          },
          criterio: { pares: this.izquierda.map((z) => [z.id, z.parId]) },
        };

      case 'ABIERTA':
        return {
          payload: {
            consigna: this.consigna,
            extensionMaxima: this.extensionMaxima ? Number(this.extensionMaxima) : null,
          },
          criterio: { rubrica: this.rubrica },
        };

      case 'ORDENAR':
        return {
          // El payload va MEZCLADO: si mandáramos los elementos en el orden en
          // que los cargó el profesor, el orden correcto viajaría dentro de la
          // vista del alumno sin ser formalmente la clave de corrección.
          payload: {
            elementos: mezclar(this.elementos.map((e) => ({ id: e.id, texto: e.texto }))),
          },
          criterio: { secuencia: this.elementos.map((e) => e.id) },
        };
    }
  }

  /**
   * La unica accion destructiva de la pantalla, y por eso pide confirmacion en
   * linea: `confirm()` bloquea el hilo del navegador y en una demo se nota.
   */
  confirmarBaja(item: ItemResumen): void {
    this.porDarDeBaja.set(null);
    this.api.bajaItem(item.id).subscribe({
      next: () => {
        this.ok.set('Ítem dado de baja. Lo ya respondido con él se sigue corrigiendo igual.');
        this.cargar();
      },
      error: () =>
        this.error.set({
          clave: 'ERROR',
          campo: null,
          mensaje: 'No se pudo dar de baja el ítem.',
        }),
    });
  }
}

function mezclar<T>(lista: T[]): T[] {
  const copia = [...lista];
  for (let i = copia.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copia[i], copia[j]] = [copia[j], copia[i]];
  }
  return copia;
}
