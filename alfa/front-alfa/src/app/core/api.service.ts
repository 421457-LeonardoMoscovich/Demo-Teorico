import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Apertura,
  ContenidoRef,
  Curso,
  Desafio,
  ItemDetalle,
  EntregaDelAlumno,
  EtiquetaConUso,
  EventoPublicado,
  ItemResumen,
  MiContenido,
  Navegacion,
  ReglaDeSorteo,
  Pendiente,
  Resultado,
  Sesion,
  TipoDeItem,
  VistaAlumno,
  VistaPreviaItem,
} from './modelos';

/** Nuestro microservicio. */
export const URL_TEORICOS = 'http://localhost:8081';
/**
 * Los STUBS de los otros grupos. En la plataforma real son dos microservicios
 * distintos y de dos equipos distintos; en la alfa comparten proceso, pero se
 * los llama por separado a proposito, para que se vea cual es cual.
 */
export const URL_TEMA_02 = 'http://localhost:8082';
export const URL_TEMA_03 = 'http://localhost:8082';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // ---- identidad (adaptador falso) ----

  login(usuario: string, clave: string): Observable<Sesion> {
    return this.http.post<Sesion>(`${URL_TEORICOS}/auth/login`, { usuario, clave });
  }

  // ---- banco de ítems (Tema 04, profesor) ----

  /** Los dos filtros son opcionales y se combinan con Y. */
  items(tipo?: TipoDeItem | '', etiqueta?: string): Observable<ItemResumen[]> {
    const partes: string[] = [];
    if (tipo) partes.push(`tipo=${tipo}`);
    if (etiqueta) partes.push(`etiqueta=${encodeURIComponent(etiqueta)}`);
    const query = partes.length > 0 ? `?${partes.join('&')}` : '';
    return this.http.get<ItemResumen[]>(`${URL_TEORICOS}/teoricos/items${query}`);
  }

  /** El vocabulario del profesor: alimenta el autocompletado y el filtro. */
  etiquetas(): Observable<EtiquetaConUso[]> {
    return this.http.get<EtiquetaConUso[]>(`${URL_TEORICOS}/teoricos/items/etiquetas`);
  }

  item(id: string): Observable<ItemDetalle> {
    return this.http.get<ItemDetalle>(`${URL_TEORICOS}/teoricos/items/${id}`);
  }

  /** El ítem como se lo va a servir al alumno, antes de componerlo (CI-59). */
  vistaPrevia(id: string): Observable<VistaPreviaItem> {
    return this.http.get<VistaPreviaItem>(`${URL_TEORICOS}/teoricos/items/${id}/vista-previa`);
  }

  crearItem(cuerpo: {
    tipo: TipoDeItem;
    enunciado: string;
    payload: any;
    criterio: any;
  }): Observable<ItemDetalle> {
    return this.http.post<ItemDetalle>(`${URL_TEORICOS}/teoricos/items`, cuerpo);
  }

  /** No edita: publica la versión siguiente (D-04). */
  publicarVersion(
    id: string,
    cuerpo: { tipo: TipoDeItem; enunciado: string; payload: any; criterio: any },
  ): Observable<ItemDetalle> {
    return this.http.put<ItemDetalle>(`${URL_TEORICOS}/teoricos/items/${id}`, cuerpo);
  }

  bajaItem(id: string): Observable<void> {
    return this.http.delete<void>(`${URL_TEORICOS}/teoricos/items/${id}`);
  }

  // ---- composición del cuestionario (Tema 04, profesor) ----

  componer(cuerpo: {
    cursoCohorteId: string;
    titulo: string;
    escala: string;
    navegacion: Navegacion;
    items: { itemId: string; orden: number; puntaje: number }[];
    /** El sorteo por etiqueta, o null si el cuestionario es todo fijo. */
    regla: ReglaDeSorteo | null;
  }): Observable<ContenidoRef> {
    return this.http.post<ContenidoRef>(`${URL_TEORICOS}/teoricos/contenidos`, cuerpo);
  }

  /** Los cuestionarios ya armados, para colgar uno nuevo de otra unidad o cohorte. */
  misContenidos(): Observable<MiContenido[]> {
    return this.http.get<MiContenido[]>(`${URL_TEORICOS}/teoricos/contenidos`);
  }

  /** Todo lo que el alumno entregó. CI-44: cada intento se conserva. */
  historial(): Observable<EntregaDelAlumno[]> {
    return this.http.get<EntregaDelAlumno[]>(`${URL_TEORICOS}/teoricos/evaluaciones/historial`);
  }

  /**
   * La lectura del alumno. El vale va como header aparte del token de sesión,
   * porque son dos cosas distintas: el token dice quién sos, el vale dice que
   * el Tema 03 te habilitó a leer este contenido ahora (CI-18).
   */
  vistaAlumno(contenidoId: string, vale: string): Observable<VistaAlumno> {
    return this.http.get<VistaAlumno>(
      `${URL_TEORICOS}/teoricos/contenidos/${contenidoId}/vista-alumno`,
      { headers: new HttpHeaders({ 'X-Vale-Lectura': vale }) },
    );
  }

  // ---- cola de correccion humana (Tema 04, profesor) ----

  /**
   * Lo que espera a la profesora. Es NUESTRO y no del Tema 03: para puntuar hay
   * que leer la consigna, la respuesta y la rubrica, y eso es conocimiento de
   * contenido. Al 03 le sigue llegando solo la nota, por el evento.
   */
  pendientes(): Observable<Pendiente[]> {
    return this.http.get<Pendiente[]>(`${URL_TEORICOS}/teoricos/correcciones/pendientes`);
  }

  /** Solo el numero, que es lo que sondea el badge de la barra. */
  cuantasPendientes(): Observable<{ pendientes: number }> {
    return this.http.get<{ pendientes: number }>(
      `${URL_TEORICOS}/teoricos/correcciones/pendientes/cuantas`,
    );
  }

  puntuar(detalleId: string, obtenido: number): Observable<{ cerrada: boolean; estado: string }> {
    return this.http.post<{ cerrada: boolean; estado: string }>(
      `${URL_TEORICOS}/teoricos/correcciones/${detalleId}`,
      { obtenido },
    );
  }

  resultado(entregaId: string): Observable<Resultado> {
    return this.http.get<Resultado>(`${URL_TEORICOS}/teoricos/evaluaciones/${entregaId}`);
  }

  // ---- andamiaje de la demo (se borra con Kafka y con el Tema 03 real) ----

  eventos(): Observable<EventoPublicado[]> {
    return this.http.get<EventoPublicado[]>(`${URL_TEORICOS}/teoricos/demo/eventos`);
  }

  temaCaido(): Observable<{ caido: boolean }> {
    return this.http.get<{ caido: boolean }>(`${URL_TEMA_03}/admin/caido`);
  }

  apagarTema03(): Observable<{ caido: boolean }> {
    return this.http.post<{ caido: boolean }>(`${URL_TEMA_03}/admin/caido`, {});
  }

  encenderTema03(): Observable<{ caido: boolean }> {
    return this.http.delete<{ caido: boolean }>(`${URL_TEMA_03}/admin/caido`);
  }

  // ---- Tema 02 (stub): cursos, cohortes y roadmap ----

  cursos(): Observable<Curso[]> {
    return this.http.get<Curso[]>(`${URL_TEMA_02}/cursos`);
  }

  curso(cursoCohorteId: string): Observable<Curso> {
    return this.http.get<Curso>(`${URL_TEMA_02}/cursos/${cursoCohorteId}`);
  }

  // ---- Tema 03 (stub): desafios, intentos y entregas ----

  crearDesafio(cuerpo: {
    titulo: string;
    cursoCohorteId: string;
    unidadId: string;
    contenidoRef: ContenidoRef;
    /** CI-47: con correccion diferida, el 03 rechaza si son ilimitados. */
    reintentosIlimitados: boolean;
  }): Observable<Desafio> {
    return this.http.post<Desafio>(`${URL_TEMA_03}/desafios`, cuerpo);
  }

  desafiosAbiertos(curso?: string, unidad?: string): Observable<Desafio[]> {
    const partes: string[] = [];
    if (curso) partes.push(`curso=${curso}`);
    if (unidad) partes.push(`unidad=${unidad}`);
    const query = partes.length ? `?${partes.join('&')}` : '';
    return this.http.get<Desafio[]>(`${URL_TEMA_03}/desafios${query}`);
  }

  abrirIntento(desafioId: string, alumnoId: string): Observable<Apertura> {
    return this.http.post<Apertura>(`${URL_TEMA_03}/desafios/${desafioId}/intentos`, { alumnoId });
  }

  /** El botón Entregar va al Tema 03, no a nosotros (CI-22). */
  entregar(
    desafioId: string,
    entregaId: string,
    respuestas: { itemVersionId: string; contenido: any }[],
  ): Observable<any> {
    return this.http.post(
      `${URL_TEMA_03}/desafios/${desafioId}/entregas`,
      { entregaId, respuestas },
      { responseType: 'text' as 'json' },
    );
  }
}
