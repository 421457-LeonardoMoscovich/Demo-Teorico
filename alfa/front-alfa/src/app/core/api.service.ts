import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Apertura,
  ContenidoRef,
  Curso,
  Desafio,
  ItemDetalle,
  ItemResumen,
  Resultado,
  Sesion,
  TipoDeItem,
  VistaAlumno,
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

  items(tipo?: TipoDeItem | ''): Observable<ItemResumen[]> {
    const query = tipo ? `?tipo=${tipo}` : '';
    return this.http.get<ItemResumen[]>(`${URL_TEORICOS}/teoricos/items${query}`);
  }

  item(id: string): Observable<ItemDetalle> {
    return this.http.get<ItemDetalle>(`${URL_TEORICOS}/teoricos/items/${id}`);
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
    items: { itemId: string; orden: number; puntaje: number }[];
  }): Observable<ContenidoRef> {
    return this.http.post<ContenidoRef>(`${URL_TEORICOS}/teoricos/contenidos`, cuerpo);
  }

  misContenidos(): Observable<ContenidoRef[]> {
    return this.http.get<ContenidoRef[]>(`${URL_TEORICOS}/teoricos/contenidos`);
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

  resultado(entregaId: string): Observable<Resultado> {
    return this.http.get<Resultado>(`${URL_TEORICOS}/teoricos/evaluaciones/${entregaId}`);
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
