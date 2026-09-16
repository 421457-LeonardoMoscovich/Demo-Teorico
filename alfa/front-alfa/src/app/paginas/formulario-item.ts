import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { ConfirmacionService } from '../core/confirmacion.service';
import { ErrorApi, EtiquetaConUso, ItemDetalle, TIPOS, TipoDeItem } from '../core/modelos';

interface FilaSimple {
  id: string;
  texto: string;
  correcta: boolean;
  /** Cuanto paga marcarla, en % del peso del item. Solo se usa en modo parcial. */
  porcentaje: number;
  /** Que se le explica a quien la marque (CI-58). Vacio = no se le explica nada. */
  devolucion: string;
}

/** Una respuesta que el profesor acepta, y cuanto paga. */
interface FilaAceptada {
  id: string;
  texto: string;
  porcentaje: number;
}

interface FilaPar {
  id: string;
  texto: string;
  /** id del elemento de la derecha con el que empareja */
  parId: string;
}

/**
 * El formulario de alta y edicion de un item, aparte de la pantalla que lo usa.
 *
 * Vive suelto porque escribir una pregunta no es una tarea del Banco: es lo que
 * el profesor hace tambien mientras arma un cuestionario, y mandarlo al Banco a
 * mitad de camino le hace perder los pesos que ya repartio. Las dos pantallas
 * montan el MISMO componente; lo unico que cambia es que armar, ademas de
 * guardar, se lleva el item recien creado al cuestionario.
 */
@Component({
  selector: 'app-formulario-item',
  standalone: true,
  imports: [FormsModule],
  template: `
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

      @if (tipo === 'RESPUESTA_CORTA') {
        <label>
          Consigna
          <input
            name="consignaCorta"
            [(ngModel)]="consigna"
            placeholder="Qué tiene que escribir, y con qué largo — «una o dos palabras»"
          />
        </label>
        <p class="ayuda">
          Se corrige sola porque vos enumerás lo que vale. Lo que no previste, no vale — por eso
          una pregunta conceptual va en <strong>respuesta abierta</strong> y no acá.
        </p>

        @for (a of aceptadas; track a.id; let i = $index) {
          <div class="fila">
            <input
              [name]="'ac' + i"
              [(ngModel)]="a.texto"
              placeholder="Respuesta que aceptás {{ i + 1 }}"
            />
            <label class="sr-solo" [attr.for]="'acp' + i">
              Porcentaje de la respuesta {{ i + 1 }}
            </label>
            <input
              class="peso"
              type="number"
              min="1"
              max="100"
              [id]="'acp' + i"
              [name]="'acp' + i"
              [(ngModel)]="a.porcentaje"
            />
            <span class="ayuda">%</span>
            <button
              type="button"
              class="secundario"
              (click)="quitar(aceptadas, i)"
              attr.aria-label="Quitar la respuesta {{ i + 1 }}"
            >
              <span aria-hidden="true">×</span>
            </button>
          </div>
        }
        <button type="button" class="secundario" (click)="agregarAceptada()">
          + agregar una variante
        </button>

        <div class="totalizador" [class.mal]="!hayUnaAlCien()">
          <strong>{{ hayUnaAlCien() ? '100%' : '—' }}</strong>
          <span>
            {{
              hayUnaAlCien()
                ? 'hay una respuesta que vale el puntaje completo'
                : 'alguna tiene que valer 100: si no, contestar perfecto no da el puntaje entero'
            }}
          </span>
        </div>

        <p class="ayuda">
          El orden importa: gana la <strong>primera</strong> que coincide, no la que más paga. Con
          «paris» al 100 y «parís» al 80, quien escriba sin tilde saca 80.
        </p>

        <label class="check">
          <input type="checkbox" name="distMay" [(ngModel)]="distingueMayusculas" />
          Distinguir mayúsculas
        </label>
        <label class="check">
          <input type="checkbox" name="distAc" [(ngModel)]="distingueAcentos" />
          Distinguir acentos
        </label>
        <p class="ayuda">
          Apagados, que es lo normal: escribir «Circuit Breaker» no es un error de concepto.
          Prendelos solo si la grafía exacta <em>es</em> lo que evaluás. La ñ nunca se confunde con
          la n, aunque los dos estén apagados.
        </p>
      }

      @if (tipo === 'NUMERICA') {
        <label>
          Consigna
          <input
            name="consignaNumerica"
            [(ngModel)]="consigna"
            placeholder="Qué tiene que calcular — «respondé con un número»"
          />
        </label>
        <div class="fila">
          <label>
            Valor correcto
            <input name="valor" type="number" step="any" [(ngModel)]="valor" />
          </label>
          <label>
            Tolerancia ±
            <input name="tolerancia" type="number" step="any" min="0" [(ngModel)]="tolerancia" />
          </label>
          <label>
            Unidad <span class="ayuda">— opcional</span>
            <input name="unidad" [(ngModel)]="unidad" placeholder="kg, ms, %" />
          </label>
        </div>
        <p class="ayuda">
          Los bordes entran: con {{ valor || 0 }} y tolerancia {{ tolerancia || 0 }} vale desde
          {{ (valor || 0) - (tolerancia || 0) }} hasta {{ (valor || 0) + (tolerancia || 0) }}.
          Tolerancia 0 es exacto. La unidad es un rótulo al lado del campo: el alumno escribe solo
          el número.
        </p>
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

      <!--
        Las etiquetas. Van acá y no en un tipo concreto porque no dependen del
        tipo: son cómo el profesor encuentra la pregunta cuando el banco tiene
        doscientas. Moodle usa una Categoría —una sola—; acá son varias, así no
        hay que decidir hoy si el eje es el tema, la unidad o la dificultad.
      -->
      <label>
        Etiquetas <span class="ayuda">— opcional</span>
        <div class="fila">
          <input
            name="etiquetaNueva"
            list="etiquetas-conocidas"
            [(ngModel)]="etiquetaNueva"
            (keydown.enter)="agregarEtiqueta($event)"
            placeholder="microservicios, parcial 1, difícil…"
          />
          <button type="button" class="secundario" (click)="agregarEtiqueta()">+ agregar</button>
        </div>
      </label>
      <datalist id="etiquetas-conocidas">
        @for (e of conocidas(); track e.etiqueta) {
          <option [value]="e.etiqueta">{{ e.cuantos }} ítems</option>
        }
      </datalist>

      @if (etiquetas().length > 0) {
        <div class="chips">
          @for (e of etiquetas(); track e) {
            <button
              type="button"
              class="chip activo"
              (click)="quitarEtiqueta(e)"
              attr.aria-label="Quitar la etiqueta {{ e }}"
            >
              {{ e }} <span aria-hidden="true">×</span>
            </button>
          }
        </div>
      }
      <p class="ayuda">
        No es un vínculo a un curso: una pregunta no pertenece a ninguna materia. El
        <strong>cuestionario</strong> es el que se cuelga de la unidad de un curso, y por eso la
        misma pregunta puede entrar en dos materias.
      </p>

      @if (error()) {
        <p class="error" role="alert">
          {{ error()!.mensaje }}
          @if (error()!.campo) {
            <span class="campo">({{ error()!.campo }})</span>
          }
        </p>
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
          guardando() ? 'Guardando…' : editando() ? 'Publicar versión nueva' : textoCrear()
        }}
      </button>
    </form>
  `,
})
export class FormularioItemComponent {
  private readonly api = inject(ApiService);
  private readonly confirmaciones = inject(ConfirmacionService);

  /** Id del item a editar. null = se esta creando uno nuevo. */
  readonly editarId = input<string | null>(null);

  /** Rotulo del boton de alta: armar dice otra cosa que el banco. */
  readonly textoCrear = input('Guardar ítem');

  /** Sale el item guardado, sea alta o version nueva. */
  readonly guardado = output<ItemDetalle>();

  /** El profesor abandono la edicion y vuelve a crear uno nuevo. */
  readonly cancelado = output<void>();

  readonly tipos = TIPOS;
  readonly error = signal<ErrorApi | null>(null);
  readonly guardando = signal(false);

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

  afirmacion = '';
  esVerdadero = true;

  /** Respuesta corta. Las variantes que el profesor acepta, en orden. */
  aceptadas: FilaAceptada[] = [];
  distingueMayusculas = false;
  distingueAcentos = false;

  /** Numerica. */
  valor: number | null = null;
  tolerancia: number | null = 0;
  unidad = '';

  /** Etiquetas del item, ya agregadas, y lo que se esta tipeando. */
  readonly etiquetas = signal<string[]>([]);
  etiquetaNueva = '';

  /** El vocabulario del profesor, para el autocompletado. */
  readonly conocidas = signal<EtiquetaConUso[]>([]);

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

  constructor() {
    this.reiniciarPorTipo();
    this.refrescarVocabulario();
    // El padre dice QUE item editar; el detalle lo trae el formulario, que es
    // el unico que sabe desarmarlo en campos.
    effect(() => {
      const id = this.editarId();
      if (id === null) {
        this.editando.set(null);
        return;
      }
      if (this.editando()?.id === id) return;
      this.abrir(id);
    });
  }

  private abrir(id: string): void {
    this.error.set(null);
    this.api.item(id).subscribe({
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
    this.limpiar();
    this.cancelado.emit();
  }

  private limpiar(): void {
    this.etiquetas.set([]);
    this.etiquetaNueva = '';
    this.enunciado = '';
    this.afirmacion = '';
    this.consigna = '';
    this.rubrica = '';
    this.extensionMaxima = null;
    this.reiniciarPorTipo();
  }

  reiniciarPorTipo(): void {
    this.error.set(null);
    this.contador = 0;
    this.opciones = [this.nueva('o'), this.nueva('o')];
    this.derecha = [this.nueva('d'), this.nueva('d')];
    this.izquierda = [this.nuevoPar(), this.nuevoPar()];
    this.elementos = [this.nueva('e'), this.nueva('e')];
    // La primera nace en 100: es la respuesta esperada, y deja el formulario
    // valido de entrada en vez de rojo.
    this.aceptadas = [{ id: 'ac' + ++this.contador, texto: '', porcentaje: 100 }];
    this.distingueMayusculas = false;
    this.distingueAcentos = false;
    this.valor = null;
    this.tolerancia = 0;
    this.unidad = '';
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

  private nuevoPar(): FilaPar {
    return { id: 'i' + ++this.contador, texto: '', parId: '' };
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

  agregar(lista: any[], prefijo: string): void {
    lista.push(prefijo === 'i' ? this.nuevoPar() : this.nueva(prefijo));
  }

  quitar(lista: any[], i: number): void {
    lista.splice(i, 1);
  }

  marcar(o: FilaSimple): void {
    if (this.multiple) {
      o.correcta = !o.correcta;
      return;
    }
    this.opciones.forEach((x) => (x.correcta = false));
    o.correcta = true;
  }

  /** Las variantes se agregan al final: el orden es la regla de desempate. */
  agregarAceptada(): void {
    this.aceptadas.push({ id: 'ac' + ++this.contador, texto: '', porcentaje: 100 });
  }

  /** Lo que el backend exige (SIN_ACEPTADA_AL_100), dicho antes de guardar. */
  hayUnaAlCien(): boolean {
    return this.aceptadas.some((a) => Number(a.porcentaje) === 100 && a.texto.trim() !== '');
  }

  /**
   * Agrega la etiqueta tipeada. El `preventDefault` importa: sin el, Enter en un
   * input adentro de un <form> lo envia, y el profesor pierde todo por querer
   * agregar una etiqueta.
   */
  agregarEtiqueta(evento?: Event): void {
    evento?.preventDefault();
    const limpia = this.etiquetaNueva.trim().toLowerCase();
    if (limpia === '') return;
    // El backend normaliza igual; esto es solo para no mostrar un duplicado
    // obvio antes de mandarlo.
    if (!this.etiquetas().includes(limpia)) {
      this.etiquetas.update((e) => [...e, limpia]);
    }
    this.etiquetaNueva = '';
  }

  quitarEtiqueta(cual: string): void {
    this.etiquetas.update((e) => e.filter((x) => x !== cual));
  }

  private refrescarVocabulario(): void {
    // Si falla, se etiqueta igual: el autocompletado es una ayuda, no un requisito.
    this.api.etiquetas().subscribe({
      next: (e) => this.conocidas.set(e),
      error: () => {},
    });
  }

  private cargarFormulario(d: ItemDetalle): void {
    const p = d.payload ?? {};
    const c = d.criterio ?? {};

    this.borrador = d.estado === 'BORRADOR';
    this.etiquetas.set([...(d.etiquetas ?? [])]);

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
      case 'RESPUESTA_CORTA': {
        this.consigna = p.consigna ?? '';
        this.distingueMayusculas = c.distingueMayusculas === true;
        this.distingueAcentos = c.distingueAcentos === true;
        this.aceptadas = (c.aceptadas ?? []).map((a: any) => ({
          id: 'ac' + ++this.contador,
          texto: a.texto,
          porcentaje: a.porcentaje,
        }));
        break;
      }
      case 'NUMERICA': {
        this.consigna = p.consigna ?? '';
        this.unidad = p.unidad ?? '';
        this.valor = c.valor ?? null;
        this.tolerancia = c.tolerancia ?? 0;
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

  guardar(): void {
    this.error.set(null);
    this.guardando.set(true);

    const cuerpo = {
      tipo: this.tipo,
      enunciado: this.enunciado,
      ...this.payloadYCriterio(),
      devolucion: this.devolucionArmada(),
      estado: this.borrador ? 'BORRADOR' : 'LISTO',
      etiquetas: this.etiquetas(),
    };

    const enEdicion = this.editando();
    const peticion = enEdicion
      ? this.api.publicarVersion(enEdicion.id, cuerpo as any)
      : this.api.crearItem(cuerpo as any);

    peticion.subscribe({
      next: (guardado) => {
        this.guardando.set(false);
        // El aviso es un cartel al medio y ya no un parrafo al pie: el
        // formulario se limpia solo, y una linea chica abajo de un formulario
        // vacio no alcanza para distinguir "se guardo" de "se borro lo que
        // escribi". Se va solo porque cargar items es repetitivo.
        this.confirmaciones.mostrar(
          enEdicion
            ? {
                mensaje: `Versión ${guardado.version} publicada`,
                detalle:
                  `La versión ${enEdicion.version} queda intacta: quien la esté ` +
                  'contestando se corrige contra la que vio (D-04).',
              }
            : {
                mensaje: 'Ítem creado',
                detalle: 'Ya está en tu banco y lo podés agregar a un cuestionario.',
              },
        );
        this.editando.set(null);
        this.limpiar();
        // La etiqueta recien inventada tiene que aparecer en el autocompletado
        // de la proxima pregunta, que es cuando mas sirve.
        this.refrescarVocabulario();
        this.guardado.emit(guardado);
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

      case 'RESPUESTA_CORTA':
        return {
          // La consigna es lo UNICO que ve el alumno: las respuestas aceptadas
          // son clave de correccion y viajan en el criterio (CI-17).
          payload: { consigna: this.consigna },
          criterio: {
            aceptadas: this.aceptadas
              .filter((a) => a.texto.trim() !== '')
              .map((a) => ({ texto: a.texto.trim(), porcentaje: Number(a.porcentaje) })),
            distingueMayusculas: this.distingueMayusculas,
            distingueAcentos: this.distingueAcentos,
          },
        };

      case 'NUMERICA':
        return {
          payload: {
            consigna: this.consigna,
            unidad: this.unidad.trim() === '' ? null : this.unidad.trim(),
          },
          criterio: {
            valor: this.valor === null ? null : Number(this.valor),
            tolerancia: this.tolerancia === null ? 0 : Number(this.tolerancia),
          },
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
}

function mezclar<T>(lista: T[]): T[] {
  const copia = [...lista];
  for (let i = copia.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copia[i], copia[j]] = [copia[j], copia[i]];
  }
  return copia;
}
