import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Movie, PagedMovies } from '../models/movie.model';
import { MovieService } from './movie.service';

const BASE = `${environment.apiBaseUrl}/api/movies`;

function movie(id: number, genres: string[] = ['Drama']): Movie {
  return {
    id,
    title: `Movie ${id}`,
    overview: 'An overview.',
    genres,
    releaseYear: 2010,
    runtimeMinutes: 120,
    keywords: [],
    rating: 8.1,
    posterUrl: null,
  };
}

function page(content: Movie[]): PagedMovies {
  return {
    content,
    page: 0,
    size: content.length,
    totalElements: content.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

describe('MovieService', () => {
  let service: MovieService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MovieService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads featured movies sorted by rating', () => {
    let result: Movie[] = [];
    service.getFeatured(3).subscribe((movies) => (result = movies));

    const request = httpMock.expectOne((req) => req.url === BASE);
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('3');
    expect(request.request.params.get('sortBy')).toBe('rating');
    expect(request.request.params.get('sortDir')).toBe('desc');
    request.flush(page([movie(1), movie(2)]));

    expect(result.map((m) => m.id)).toEqual([1, 2]);
  });

  it('derives a sorted, deduplicated genre list from real movie data', () => {
    let result: string[] = [];
    service.getAvailableGenres().subscribe((genres) => (result = genres));

    const request = httpMock.expectOne((req) => req.url === BASE);
    request.flush(page([
      movie(1, ['Thriller', 'Drama']),
      movie(2, ['Drama', 'Comedy']),
    ]));

    expect(result).toEqual(['Comedy', 'Drama', 'Thriller']);
  });

  it('searches by title', () => {
    let result: Movie[] = [];
    service.searchByTitle('Shutter').subscribe((movies) => (result = movies));

    const request = httpMock.expectOne((req) => req.url === `${BASE}/search`);
    expect(request.request.params.get('title')).toBe('Shutter');
    request.flush(page([movie(17, ['Thriller'])]));

    expect(result).toHaveLength(1);
  });

  it('omits empty filter parameters', () => {
    service.filter({ genre: 'Thriller', maxRuntime: 140, minRating: undefined })
      .subscribe();

    const request = httpMock.expectOne((req) => req.url === `${BASE}/filter`);
    expect(request.request.params.get('genre')).toBe('Thriller');
    expect(request.request.params.get('maxRuntime')).toBe('140');
    expect(request.request.params.has('minRating')).toBe(false);
    request.flush(page([]));
  });

  it('propagates HTTP errors to the caller', () => {
    let failed = false;
    service.getFeatured().subscribe({ error: () => (failed = true) });

    httpMock.expectOne((req) => req.url === BASE)
      .flush('down', { status: 503, statusText: 'Service Unavailable' });

    expect(failed).toBe(true);
  });
});
