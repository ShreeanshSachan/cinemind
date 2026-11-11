import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Movie, MovieFilterParams, PagedMovies } from '../models/movie.model';

/** All movie metadata reads go through the API Gateway. */
@Injectable({ providedIn: 'root' })
export class MovieService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/movies`;

  /** Highest-rated movies for the Home page featured section. */
  getFeatured(size = 8): Observable<Movie[]> {
    return this.http
      .get<PagedMovies>(this.baseUrl, {
        params: this.buildParams({ page: 0, size, sortBy: 'rating', sortDir: 'desc' }),
      })
      .pipe(map((page) => page.content ?? []));
  }

  /** Genre choices come from the real catalogue, not a hardcoded list. */
  getAvailableGenres(): Observable<string[]> {
    return this.http
      .get<PagedMovies>(this.baseUrl, { params: this.buildParams({ page: 0, size: 100 }) })
      .pipe(
        map((page) =>
          Array.from(
            new Set((page.content ?? []).flatMap((movie) => movie.genres ?? [])),
          ).sort((a, b) => a.localeCompare(b)),
        ),
      );
  }

  getById(id: number): Observable<Movie> {
    return this.http.get<Movie>(`${this.baseUrl}/${id}`);
  }

  list(params: MovieFilterParams = {}): Observable<PagedMovies> {
    return this.http.get<PagedMovies>(this.baseUrl, { params: this.buildParams(params) });
  }

  searchByTitle(title: string, size = 20): Observable<Movie[]> {
    return this.http
      .get<PagedMovies>(`${this.baseUrl}/search`, {
        params: this.buildParams({ title, page: 0, size }),
      })
      .pipe(map((page) => page.content ?? []));
  }

  filter(params: MovieFilterParams): Observable<Movie[]> {
    return this.http
      .get<PagedMovies>(`${this.baseUrl}/filter`, { params: this.buildParams(params) })
      .pipe(map((page) => page.content ?? []));
  }

  private buildParams(values: object): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(values)) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
